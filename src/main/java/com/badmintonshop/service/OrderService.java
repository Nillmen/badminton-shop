package com.badmintonshop.service;

import com.badmintonshop.dto.order.*;
import com.badmintonshop.entity.*;
import com.badmintonshop.entity.enums.*;
import com.badmintonshop.exception.BadRequestException;
import com.badmintonshop.exception.InsufficientStockException;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.*;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderTrackingRepository trackingRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final StaffRepository staffRepository;
    private final CouponService couponService;
    private final EmailService emailService;

    // Valid status transitions
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.STRINGING, OrderStatus.READY_TO_SHIP),
            OrderStatus.STRINGING, Set.of(OrderStatus.READY_TO_SHIP),
            OrderStatus.READY_TO_SHIP, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.REFUNDED, Set.of());

    // =============================================
    // CUSTOMER APIs
    // =============================================

    /**
     * Create order from cart with full transaction safety
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(entityType = "Order", action = ActivityAction.CREATE, description = "Created order for userId: {0}")
    public OrderResponse createOrder(Long userId, String sessionId, OrderRequest request) {
        log.info("Creating order for userId: {}, sessionId: {}", userId, sessionId);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        // 1. Get cart
        Cart cart = cartService.getCart(user, sessionId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        // 2. Validate & decrease inventory (atomic)
        for (CartItem item : cart.getItems()) {
            boolean success = decreaseInventory(
                    item.getProduct().getProductId(),
                    item.getVariant() != null ? item.getVariant().getVariantId() : null,
                    item.getQuantity());
            if (!success) {
                throw new InsufficientStockException(
                        item.getProduct().getProductId(),
                        item.getQuantity(),
                        0 // Will be updated with actual available
                );
            }
        }

        // 3. Generate order number
        String orderNumber = generateOrderNumber();

        // 4. Get coupon discount from cart (if applied)
        BigDecimal couponDiscount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;
        String couponCode = cart.getCouponCode();

        // 5. Create order
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .shippingRecipientName(request.getRecipientName())
                .shippingPhone(request.getPhone())
                .shippingEmail(request.getEmail())
                .shippingAddress(request.getAddress())
                .shippingWard(request.getWard())
                .shippingDistrict(request.getDistrict())
                .shippingCity(request.getCity())
                .customerNotes(request.getCustomerNotes())
                .subtotal(BigDecimal.ZERO)
                .discountAmount(couponDiscount) // Use coupon discount from cart
                .shippingFee(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);

        // 6. Create order items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = createOrderItem(order, cartItem);
            orderItemRepository.save(orderItem);
            order.addItem(orderItem);
            subtotal = subtotal.add(orderItem.getTotalPrice());
        }

        // 7. Calculate shipping fee and totals
        order.setSubtotal(subtotal);
        BigDecimal shippingFee = cartService.calculateShippingFee(subtotal);
        order.setShippingFee(shippingFee);
        BigDecimal totalAfterDiscount = subtotal.add(shippingFee).subtract(order.getDiscountAmount());
        if (totalAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            totalAfterDiscount = BigDecimal.ZERO;
        }
        order.setTotalAmount(totalAfterDiscount);

        // Store coupon code in admin notes for tracking
        if (couponCode != null && !couponCode.isEmpty()) {
            order.setAdminNotes("Coupon applied: " + couponCode);
            log.info("Applied coupon {} to order {} with discount {}", couponCode, orderNumber, couponDiscount);
        }

        orderRepository.save(order);

        // 8. Record coupon usage (increment times_used, create CouponUsage record)
        if (couponCode != null && !couponCode.isEmpty() && user != null) {
            couponService.recordCouponUsage(couponCode, user, order, couponDiscount);
        }

        // 9. Create initial status history
        createStatusHistory(order, null, OrderStatus.PENDING, "Đơn hàng được tạo", ChangedByType.SYSTEM, null);

        // 10. Clear cart (also clears coupon)
        cartService.clearCart(user, sessionId);

        log.info("Order created successfully: {}", orderNumber);

        // 11. Send notification (async)
        try {
            // Prepare order items map
            java.util.List<java.util.Map<String, String>> orderItemsMap = new java.util.ArrayList<>();
            for (OrderItem item : order.getItems()) {
                java.util.Map<String, String> itemMap = new java.util.HashMap<>();
                itemMap.put("productName", item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm");

                String variantInfo = "";
                if (item.getVariant() != null) {
                    variantInfo = item.getVariant().getVariantName();
                }
                if (item.getHasStringingService() && item.getStringingService() != null) {
                    if (!variantInfo.isEmpty())
                        variantInfo += ", ";
                    variantInfo += "Đan vợt: " + item.getStringingService().getServiceName() + " (" + item.getTension()
                            + "kg)";
                }
                itemMap.put("variantInfo", variantInfo);

                itemMap.put("quantity", String.valueOf(item.getQuantity()));
                itemMap.put("totalPrice", couponService.formatPrice(item.getTotalPrice()));
                orderItemsMap.add(itemMap);
            }

            String customerName = request.getRecipientName();
            String paymentMethodDisplay = switch (order.getPaymentMethod()) {
                case COD -> "Thanh toán khi nhận hàng (COD)";
                case BANK_TRANSFER -> "Chuyển khoản ngân hàng";
                case MOMO -> "Ví MoMo";
                case ZALOPAY -> "ZaloPay";
                case VNPAY -> "VNPAY";
            };

            String emailToSend = order.getShippingEmail();
            if (emailToSend == null && user != null) {
                emailToSend = user.getEmail();
            }

            if (emailToSend != null && !emailToSend.isEmpty()) {
                emailService.sendOrderConfirmation(
                        emailToSend,
                        customerName,
                        order.getOrderNumber(),
                        order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        paymentMethodDisplay,
                        orderItemsMap,
                        couponService.formatPrice(order.getSubtotal()),
                        couponService.formatPrice(order.getDiscountAmount()),
                        couponService.formatPrice(order.getShippingFee()),
                        couponService.formatPrice(order.getTotalAmount()),
                        order.getShippingRecipientName(),
                        order.getShippingPhone(),
                        order.getFullShippingAddress(),
                        "/account/orders/" + order.getOrderNumber());
            } else {
                log.warn("No email found for order confirmation: {}", order.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("Error preparing/sending order confirmation email", e);
        }

        return mapToOrderResponse(order);
    }

    /**
     * Get orders for a user
     */
    @Transactional(readOnly = true)
    public Page<OrderListDTO> getOrdersByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderRepository.findByUserUserId(userId, pageable);
        return orders.map(this::mapToOrderListDTO);
    }

    /**
     * Get order by order number (for customer)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        // Verify ownership
        if (userId != null && !order.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Cancel order by customer
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        // Verify ownership
        if (!order.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }

        // Validate cancellable
        if (!order.isCancellable()) {
            throw new BadRequestException("Đơn hàng không thể hủy ở trạng thái hiện tại");
        }

        // Restore inventory
        restoreInventory(order);

        // Update order
        OrderStatus previousStatus = order.getStatus();
        order.cancel(CancelledBy.CUSTOMER, reason);
        orderRepository.save(order);

        // Create status history
        createStatusHistory(order, previousStatus, OrderStatus.CANCELLED, reason, ChangedByType.CUSTOMER, userId);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);

        return mapToOrderResponse(order);
    }

    // =============================================
    // ADMIN APIs
    // =============================================

    /**
     * Get all orders with optional filters (admin)
     */
    @Transactional(readOnly = true)
    public Page<OrderListDTO> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(this::mapToOrderListDTO);
    }

    /**
     * Get all orders with complex filters (admin)
     */
    @Transactional(readOnly = true)
    public Page<OrderListDTO> getAllOrdersWithFilters(
            OrderStatus status,
            PaymentStatus paymentStatus,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search,
            Pageable pageable) {
        Page<Order> orders = orderRepository.findAllWithFilters(status, paymentStatus, fromDate, toDate, search,
                pageable);
        return orders.map(this::mapToOrderListDTO);
    }

    /**
     * Get order by ID (admin)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
        return mapToOrderResponse(order);
    }

    /**
     * Update order status (admin)
     */
    @Transactional(rollbackFor = Exception.class)
    @Auditable(entityType = "Order", action = ActivityAction.UPDATE, description = "Updated order status ID: {0}")
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long staffId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    String.format("Không thể chuyển từ trạng thái %s sang %s", currentStatus, newStatus));
        }

        // Update status
        order.updateStatus(newStatus);

        // Auto-mark COD payment as PAID when order is DELIVERED
        if (newStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            log.info("COD order {} auto-marked as PAID upon delivery", order.getOrderNumber());
        }

        orderRepository.save(order);

        // Create history
        createStatusHistory(order, currentStatus, newStatus, request.getNotes(), ChangedByType.STAFF, staffId);

        log.info("Order {} status updated from {} to {} by staff {}",
                order.getOrderNumber(), currentStatus, newStatus, staffId);

        return mapToOrderResponse(order);
    }

    /**
     * Get order status history (admin)
     */
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(Long orderId) {
        // Verify order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Đơn hàng không tồn tại");
        }

        List<OrderStatusHistory> histories = statusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
        return histories.stream()
                .map(this::mapToStatusHistoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update order tracking (admin)
     */
    @Transactional
    public OrderResponse updateOrderTracking(Long orderId, OrderTrackingDTO trackingDTO) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        OrderTracking tracking = order.getTracking();
        if (tracking == null) {
            tracking = new OrderTracking();
            tracking.setOrder(order);
        }

        tracking.setCarrier(trackingDTO.getCarrier());
        tracking.setTrackingNumber(trackingDTO.getTrackingNumber());
        tracking.setCurrentStatus(trackingDTO.getCurrentStatus());
        tracking.setEstimatedDelivery(trackingDTO.getEstimatedDelivery());
        tracking.setTrackingUrl(trackingDTO.getTrackingUrl());
        tracking.setEvents(trackingDTO.getEvents());

        trackingRepository.save(tracking);
        order.setTracking(tracking);

        log.info("Order {} tracking updated", order.getOrderNumber());

        return mapToOrderResponse(order);
    }

    // =============================================
    // HELPER METHODS
    // =============================================

    private String generateOrderNumber() {
        LocalDate today = LocalDate.now();
        long todayCount = orderRepository.countByCreatedDate(today);
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("ORD%s%04d", dateStr, todayCount + 1);
    }

    private boolean decreaseInventory(Long productId, Long variantId, int quantity) {
        if (variantId != null) {
            return inventoryRepository.findByVariantVariantId(variantId)
                    .map(inv -> {
                        if (inv.getActualAvailable() >= quantity) {
                            inv.setQuantityReserved(inv.getQuantityReserved() + quantity);
                            inventoryRepository.save(inv);
                            return true;
                        }
                        return false;
                    })
                    .orElse(false);
        } else {
            // For products without variants, check total inventory
            int available = inventoryRepository.getTotalAvailableQuantity(productId);
            if (available >= quantity) {
                // Decrease from first available inventory
                List<Inventory> inventories = inventoryRepository.findByProductProductId(productId);
                int remaining = quantity;
                for (Inventory inv : inventories) {
                    if (remaining <= 0)
                        break;
                    int canReserve = Math.min(inv.getActualAvailable(), remaining);
                    inv.setQuantityReserved(inv.getQuantityReserved() + canReserve);
                    inventoryRepository.save(inv);
                    remaining -= canReserve;
                }
                return remaining <= 0;
            }
            return false;
        }
    }

    private void restoreInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                inventoryRepository.findByVariantVariantId(item.getVariant().getVariantId())
                        .ifPresent(inv -> {
                            inv.setQuantityReserved(Math.max(0, inv.getQuantityReserved() - item.getQuantity()));
                            inventoryRepository.save(inv);
                        });
            } else {
                // Restore to first inventory of product
                inventoryRepository.findByProductProductId(item.getProduct().getProductId())
                        .stream().findFirst()
                        .ifPresent(inv -> {
                            inv.setQuantityReserved(Math.max(0, inv.getQuantityReserved() - item.getQuantity()));
                            inventoryRepository.save(inv);
                        });
            }
        }
        log.info("Inventory restored for cancelled order {}", order.getOrderNumber());
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }

    private OrderItem createOrderItem(Order order, CartItem cartItem) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .variant(cartItem.getVariant())
                .productName(cartItem.getProduct().getName())
                .productSku(cartItem.getProduct().getSku())
                .variantName(cartItem.getVariant() != null ? cartItem.getVariant().getVariantName() : null)
                .productImage(cartItem.getProduct().getPrimaryImage() != null
                        ? cartItem.getProduct().getPrimaryImage().getImageUrl()
                        : null)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getPriceAtAdd())
                .subtotal(cartItem.getSubtotal())
                .build();

        // Stringing info
        if (cartItem.hasStringingService()) {
            item.setHasStringingService(true);
            item.setStringingService(cartItem.getStringingService());
            item.setStringingServiceName(cartItem.getStringingService().getServiceName());
            item.setStringingServicePrice(cartItem.getStringingService().getBasePrice());

            if (cartItem.getStringProduct() != null) {
                item.setStringProduct(cartItem.getStringProduct());
                item.setStringName(cartItem.getStringProduct().getName());
                item.setStringPrice(cartItem.getStringProduct().getRetailPrice());
            }

            item.setTension(cartItem.getTension());
            item.setStringingNotes(cartItem.getStringingNotes());
            item.setStringingStatus(StringingStatus.PENDING);
        }

        return item;
    }

    private void createStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus,
            String notes, ChangedByType changedByType, Long changedById) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus.name())
                .notes(notes)
                .changedByType(changedByType)
                .changedById(changedById)
                .build();
        statusHistoryRepository.save(history);
    }

    // =============================================
    // MAPPING METHODS
    // =============================================

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .userId(order.getUser().getUserId())
                .userName(order.getUser().getFullName())
                .userEmail(order.getUser().getEmail())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .recipientName(order.getShippingRecipientName())
                .phone(order.getShippingPhone())
                .address(order.getShippingAddress())
                .ward(order.getShippingWard())
                .district(order.getShippingDistrict())
                .city(order.getShippingCity())
                .fullAddress(order.getFullShippingAddress())
                .customerNotes(order.getCustomerNotes())
                .adminNotes(order.getAdminNotes())
                .cancelledReason(order.getCancelledReason())
                .cancelledBy(order.getCancelledBy() != null ? order.getCancelledBy().name() : null)
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .confirmedAt(order.getConfirmedAt())
                .processingAt(order.getProcessingAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .items(order.getItems().stream().map(this::mapToOrderItemDTO).collect(Collectors.toList()))
                .totalItems(order.getItems().size())
                .tracking(order.getTracking() != null ? mapToTrackingDTO(order.getTracking()) : null)
                .hasStringingItems(order.hasStringingItems())
                .isCancellable(order.isCancellable())
                .build();
    }

    private OrderListDTO mapToOrderListDTO(Order order) {
        return OrderListDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .userId(order.getUser().getUserId())
                .userName(order.getUser().getFullName())
                .userEmail(order.getUser().getEmail())
                .userPhone(order.getUser().getPhone())
                .totalAmount(order.getTotalAmount())
                .totalItems(order.getItems().size())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .hasStringingItems(order.hasStringingItems())
                .build();
    }

    private OrderItemDTO mapToOrderItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProduct().getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .productImage(item.getProductImage())
                .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .totalPrice(item.getTotalPrice())
                .hasStringingService(item.getHasStringingService() != null && item.getHasStringingService())
                .stringingServiceId(
                        item.getStringingService() != null ? item.getStringingService().getServiceId() : null)
                .stringingServiceName(item.getStringingServiceName())
                .stringingServicePrice(item.getStringingServicePrice())
                .stringProductId(item.getStringProduct() != null ? item.getStringProduct().getStringId() : null)
                .stringProductName(item.getStringName())
                .stringPrice(item.getStringPrice())
                .tension(item.getTension())
                .stringingNotes(item.getStringingNotes())
                .stringingStatus(item.getStringingStatus())
                .assignedStaffName(item.getAssignedStaff() != null ? item.getAssignedStaff().getFullName() : null)
                .build();
    }

    private OrderTrackingDTO mapToTrackingDTO(OrderTracking tracking) {
        return OrderTrackingDTO.builder()
                .trackingId(tracking.getTrackingId())
                .carrier(tracking.getCarrier())
                .trackingNumber(tracking.getTrackingNumber())
                .currentStatus(tracking.getCurrentStatus())
                .estimatedDelivery(tracking.getEstimatedDelivery())
                .trackingUrl(tracking.getTrackingUrl())
                .events(tracking.getEvents())
                .updatedAt(tracking.getUpdatedAt())
                .build();
    }

    private OrderStatusHistoryDTO mapToStatusHistoryDTO(OrderStatusHistory history) {
        String changedByName = null;
        if (history.getChangedById() != null) {
            if (history.getChangedByType() == ChangedByType.CUSTOMER) {
                changedByName = userRepository.findById(history.getChangedById())
                        .map(User::getFullName).orElse("Unknown User");
            } else if (history.getChangedByType() == ChangedByType.STAFF) {
                changedByName = staffRepository.findById(history.getChangedById())
                        .map(Staff::getFullName).orElse("Unknown Staff");
            }
        } else if (history.getChangedByType() == ChangedByType.SYSTEM) {
            changedByName = "Hệ thống";
        }

        return OrderStatusHistoryDTO.builder()
                .historyId(history.getHistoryId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .notes(history.getNotes())
                .changedByType(history.getChangedByType().name())
                .changedById(history.getChangedById())
                .changedByName(changedByName)
                .changedAt(history.getChangedAt())
                .build();
    }

    // =============================================
    // LEGACY METHODS (from original implementation)
    // =============================================

    /**
     * @deprecated Use createOrder with OrderRequest instead
     */
    @Deprecated
    public Order createOrderFromCart(User user, String sessionId, String address, String note) {
        OrderRequest request = OrderRequest.builder()
                .recipientName(user != null ? user.getFullName() : "Guest")
                .phone(user != null ? user.getPhone() : "0000000000")
                .address(address != null ? address : "No Address")
                .district("District 1")
                .city("Ho Chi Minh")
                .paymentMethod(PaymentMethod.COD)
                .customerNotes(note)
                .build();

        OrderResponse response = createOrder(user != null ? user.getUserId() : null, sessionId, request);
        return orderRepository.findById(response.getOrderId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(User user, int page, int size) {
        return orderRepository.findByUser(user, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public Order getOrderForUser(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
        return order;
    }
}
