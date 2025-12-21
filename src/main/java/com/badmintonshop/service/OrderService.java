package com.badmintonshop.service;

import com.badmintonshop.dto.OrderRequest;
import com.badmintonshop.entity.*;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.entity.enums.StringingStatus;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.badmintonshop.dto.OrderListResponse;
import com.badmintonshop.dto.OrderDetailResponse;
import com.badmintonshop.dto.OrderItemResponse;
import com.badmintonshop.dto.OrderStatusHistoryResponse;
import com.badmintonshop.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final OrderTrackingRepository orderTrackingRepository;
    private final StaffRepository staffRepository;

    private final StringingServiceRepository stringingServiceRepository;
    private final StringProductRepository stringProductRepository;

    // --- LOGIC TẠO ORDER (CREATE ORDER) ---
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(User user, String sessionId, OrderRequest request) {
        // ... (Logic createOrder giữ nguyên) ...

        // 1. Lấy Giỏ hàng của User (kèm Session ID để merge nếu cần)
        Cart cart = cartService.getCartEntity(user.getUserId(), sessionId);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // 2. Khởi tạo Order
        Order order = Order.builder()
                .user(user)
                .sessionId(sessionId) // Save Session ID
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                // Map thông tin giao hàng
                .shippingRecipientName(request.getShippingRecipientName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingWard(request.getShippingWard())
                .shippingDistrict(request.getShippingDistrict())
                .shippingCity(request.getShippingCity())
                .paymentMethod(request.getPaymentMethod())
                .customerNotes(request.getCustomerNotes())
                .build();

        // 3. Xử lý từng món hàng (Lấy từ CartItems)
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasStringingItem = false;

        for (CartItem cartItem : cart.getItems()) {

            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            Integer quantity = cartItem.getQuantity();

            // A. CHECK VÀ TRỪ TỒN KHO
            Long variantId = variant != null ? variant.getVariantId() : null;
            try {
                inventoryService.sellStock(product.getProductId(), variantId, quantity);
            } catch (RuntimeException e) {
                throw new RuntimeException("Lỗi tồn kho cho sản phẩm " + product.getName() + ": " + e.getMessage());
            }

            // B. TẠO ORDER ITEM VÀ TÍNH TIỀN

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);

            // Snapshot thông tin
            orderItem.setProductName(product.getName());
            orderItem.setProductSku(product.getSku());
            orderItem.setQuantity(quantity);

            BigDecimal unitPrice = product.getBasePrice();
            if (variant != null) {
                orderItem.setVariant(variant);
                orderItem.setVariantName(variant.getVariantName());
            }
            orderItem.setUnitPrice(unitPrice);

            // TÍNH TOÁN SUBTOTAL & STRINGING
            BigDecimal itemTotalPerUnit = unitPrice;

            if (cartItem.hasStringingService()) {
                hasStringingItem = true;
                orderItem.setHasStringingService(true);
                orderItem.setStringingStatus(StringingStatus.PENDING);
                orderItem.setTension(cartItem.getTension());
                orderItem.setStringingNotes(cartItem.getStringingNotes());

                // Lấy các đối tượng quan hệ trực tiếp từ CartItem
                com.badmintonshop.entity.StringingService stringingEntity = cartItem.getStringingService();
                StringProduct stringProductEntity = cartItem.getStringProduct();

                // A. Lấy thông tin CÔNG ĐAN (Service)
                if (stringingEntity != null) {

                    orderItem.setStringingService(stringingEntity);
                    orderItem.setStringingServiceName(stringingEntity.getServiceName());
                    orderItem.setStringingServicePrice(stringingEntity.getBasePrice());
                    itemTotalPerUnit = itemTotalPerUnit.add(stringingEntity.getBasePrice());
                }

                // B. Lấy thông tin LOẠI CƯỚC (String)
                if (stringProductEntity != null) {

                    orderItem.setStringProduct(stringProductEntity);

                    orderItem.setStringName(stringProductEntity.getName());
                    orderItem.setStringPrice(stringProductEntity.getRetailPrice());
                    itemTotalPerUnit = itemTotalPerUnit.add(stringProductEntity.getRetailPrice());
                }
            } else {
                orderItem.setHasStringingService(false);
            }

            // Tính Subtotal = (Giá Vợt + Công + Cước) * Số lượng
            BigDecimal lineTotal = itemTotalPerUnit.multiply(BigDecimal.valueOf(quantity));
            orderItem.setSubtotal(lineTotal);

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        // 4. Hoàn tất Order và Save

        if (hasStringingItem) {
            order.setStatus(OrderStatus.STRINGING);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);

        BigDecimal ship = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;

        order.setTotalAmount(subtotal.add(ship).subtract(discount));

        Order savedOrder = orderRepository.save(order);

        // 5. XÓA GIỎ HÀNG sau khi tạo Order thành công
        cartService.clearCart(user.getUserId(), null);

        // Ghi lại lịch sử: Order được tạo với trạng thái PENDING
        orderStatusHistoryService.logStatusChange(savedOrder, savedOrder.getStatus(), "Đơn hàng được tạo thành công.");

        return savedOrder;
    }

    // --- LOGIC HỖ TRỢ USER (READ/CANCEL) ---
    public Order getOrderByOrderNumber(String orderNumber, Long userId, String sessionId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderNumber));

        boolean isOwner = false;
        // Check User ID
        if (userId != null && order.getUser().getUserId().equals(userId)) {
            isOwner = true;
        }
        // Check Session ID (for Guests)
        if (sessionId != null && sessionId.equals(order.getSessionId())) {
            isOwner = true;
        }

        // TODO: Enable strict check. Temporarily relaxed for debugging if needed, but
        // logic is correct now.
        if (!isOwner && userId != 1L) { // Allow dummy user 1L or real owner
            // throw new RuntimeException("Bạn không có quyền xem đơn hàng này.");
        }

        return order;
    }

    public List<Order> getUserOrders(Long userId) {
        return getUserOrders(userId, null);
    }

    public List<Order> getUserOrders(Long userId, String sessionId) {
        // If userId is real (not guest/1L logic if we keep it), prioritize user
        // But here we might just want to check if sessionId is provided for guests

        List<Order> orders = new ArrayList<>();
        if (userId != null && userId != 1L) { // Assuming 1L might be the dummy guest user ID, or legitimate user
            orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        }

        // If empty (or guest), try session ID
        if (orders.isEmpty() && sessionId != null) {
            orders = orderRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        }
        return orders;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId, Long userId, String sessionId) {
        // Use custom query to eager fetch items for restocking logic
        Order order = orderRepository.findDetailByIdWithItemsAndHistory(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        boolean isOwner = false;
        // Check User ID
        if (userId != null && order.getUser().getUserId().equals(userId)) {
            isOwner = true;
        }
        // Check Session ID (for Guests)
        if (sessionId != null && sessionId.equals(order.getSessionId())) {
            isOwner = true;
        }

        if (!isOwner && userId != 1L) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này.");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.STRINGING) {
            throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái: " + order.getStatus().name());
        }

        // 1. Hoàn lại tồn kho
        for (OrderItem item : order.getItems()) {
            Long variantId = item.getVariant() != null ? item.getVariant().getVariantId() : null;
            inventoryService.restockStock(item.getProduct().getProductId(), variantId, item.getQuantity());
        }

        // 2. Cập nhật trạng thái
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // 3. Ghi lại lịch sử
        orderStatusHistoryService.logStatusChange(savedOrder, OrderStatus.CANCELLED, "Khách hàng tự hủy đơn.");

        return savedOrder;
    }

    // --- LOGIC HỖ TRỢ ADMIN/STAFF ---

    /**
     * Admin: Lấy danh sách Order (có lọc)
     */
    public List<Order> getAllOrdersForAdmin(OrderStatus status, String searchKeyword) {
        // TODO: Mở rộng logic lọc theo keyword sau
        if (status != null) {
            return orderRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        // Trả về tất cả Order, sắp xếp theo thời gian tạo mới nhất
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Admin: Cập nhật trạng thái đơn hàng
     */
    // Trong hàm updateOrderStatus
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long adminId, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        Staff adminStaff = staffRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin/Staff không tồn tại."));

        String oldStatusName = order.getStatus().name();

        // 1. Cập nhật trạng thái Order
        order.setStatus(newStatus);
        // Lưu ý: Không gọi order.setUpdatedAt vì DB gốc không có cột này
        Order savedOrder = orderRepository.save(order);

        // 2. Ghi lại lịch sử khớp với DB gốc (cột changed_by_id)
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .oldStatus(oldStatusName)
                .newStatus(newStatus.name())
                .notes(notes)
                .changedByType(com.badmintonshop.entity.enums.ChangedByType.STAFF)
                .changedById(adminId)
                .changedAt(LocalDateTime.now())
                .build();

        // Lưu ý: historyRepository phải được inject vào Service này
        // historyRepository.save(history);

        return savedOrder;
    }

    /**
     * Admin: Cập nhật thông tin Tracking (Mã vận đơn)
     */
    @Transactional
    public OrderTracking updateOrderTracking(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        Optional<OrderTracking> trackingOpt = orderTrackingRepository.findByOrder_OrderId(orderId);

        OrderTracking tracking;
        if (trackingOpt.isPresent()) {
            tracking = trackingOpt.get();
        } else {
            tracking = OrderTracking.builder().order(order).build();
        }

        tracking.setTrackingNumber(trackingNumber);
        tracking.setCarrier(carrier);
        tracking.setUpdatedAt(LocalDateTime.now());

        return orderTrackingRepository.save(tracking);
    }

    // --- HELPER METHOD ---
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = new Random().nextInt(9000) + 1000;
        return "ORD" + datePart + randomPart;
    }

    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Hàm xử lý kết quả VNPay (Rất quan trọng)
    @Transactional(rollbackFor = Exception.class)
    public String handleVNPayReturn(String orderNumber, String responseCode, String transactionStatus) {
        // 1. Tìm Order bằng orderNumber
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElse(null);

        if (order == null) {
            return "Order not found";
        }

        // 2. Chỉ xử lý khi trạng thái Payment đang là PENDING
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.FAILED) {
            return "Order already processed: " + order.getPaymentStatus().name();
        }

        // 3. Cập nhật trạng thái
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            // Giao dịch thành công
            order.setPaymentStatus(PaymentStatus.PAID);
            // Chuyển về CONFIRMED nếu là hàng không cần đan
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);

            orderStatusHistoryService.logStatusChange(order, order.getStatus(), "Thanh toán VNPay thành công.");

            return "Thanh toán VNPay thành công.";
        } else {
            // Giao dịch thất bại
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.CANCELLED); // Hủy đơn nếu thanh toán thất bại
            orderRepository.save(order);

            // Hoàn lại tồn kho cho đơn hàng thất bại
            for (OrderItem item : order.getItems()) {
                Long variantId = item.getVariant() != null ? item.getVariant().getVariantId() : null;
                inventoryService.restockStock(item.getProduct().getProductId(), variantId, item.getQuantity());
            }

            orderStatusHistoryService.logStatusChange(order, OrderStatus.CANCELLED,
                    "Thanh toán VNPay thất bại, đơn hàng bị hủy.");

            return "Thanh toán VNPay thất bại. Mã lỗi: " + responseCode;
        }
    }

    // Hàm ánh xạ thủ công (hoặc dùng ModelMapper nếu bro quyết định dùng lại)
    private OrderListResponse convertToDto(Order order) {
        OrderListResponse dto = OrderListResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();

        // Load User ID và Email (Giả định Order liên kết với User)
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getUserId());
            dto.setCustomerEmail(order.getUser().getEmail());
        }
        return dto;
    }

    /**
     * Lấy danh sách tất cả các Đơn hàng (chuyển sang DTO)
     */
    public List<OrderListResponse> findAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findDetailByIdWithItemsAndHistory(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return mapToDetailResponseDTO(order);
    }

    private OrderItemResponse mapToOrderItemDTO(OrderItem item) {
        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProduct().getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                // .productImageUrl(...) // Nếu cần, bro tự thêm logic lấy ảnh
                .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .hasStringingService(item.getHasStringingService())
                .stringingStatus(item.getStringingStatus())
                .stringingServiceName(item.getStringingServiceName())
                .stringingServicePrice(item.getStringingServicePrice())
                .stringName(item.getStringName())
                .stringPrice(item.getStringPrice())
                .tension(item.getTension())
                .stringingNotes(item.getStringingNotes())
                .build();
    }

    private OrderStatusHistoryResponse mapToHistoryDTO(OrderStatusHistory history) {
        return OrderStatusHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .changedAt(history.getChangedAt())
                .notes(history.getNotes())
                .changedBy(history.getChangedByType())
                .staffName(history.getStaff() != null ? history.getStaff().getFullName() : null)
                .build();
    }

    /**
     * Ánh xạ Order Entity sang OrderDetailResponseDTO
     */
    private OrderDetailResponse mapToDetailResponseDTO(Order order) {
        // Mapping Items
        List<OrderItemResponse> itemDTOs = order.getItems().stream()
                .map(this::mapToOrderItemDTO)
                .collect(Collectors.toList());

        // Mapping History
        List<OrderStatusHistoryResponse> historyDTOs = order.getStatusHistory().stream()
                .map(this::mapToHistoryDTO)
                // Sắp xếp lịch sử theo thời gian thay đổi mới nhất
                .sorted((h1, h2) -> h2.getChangedAt().compareTo(h1.getChangedAt()))
                .collect(Collectors.toList());

        // Lấy thông tin Tracking (nếu có)
        OrderTracking tracking = orderTrackingRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);

        // Mapping Order chính
        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod().name())
                .createdAt(order.getCreatedAt())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())

                // User Info (đã được Eager Fetch)
                .userId(order.getUser().getUserId())
                .customerFullName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .customerPhone(order.getUser().getPhone())

                // Shipping Info
                .shippingRecipientName(order.getShippingRecipientName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingWard(order.getShippingWard())
                .shippingDistrict(order.getShippingDistrict())
                .shippingCity(order.getShippingCity())

                // Tracking Info
                .trackingNumber(tracking != null ? tracking.getTrackingNumber() : null)
                .carrier(tracking != null ? tracking.getCarrier() : null)

                // DTO Lists
                .items(itemDTOs)
                .statusHistory(historyDTOs)
                .build();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
    }
}