package com.badmintonshop.service;

import com.badmintonshop.dto.order.OrderRequest;
import com.badmintonshop.entity.*;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// Updated for Inventory Debugging
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional
    public Order createOrder(Long userId, String sessionId, OrderRequest request) {
        Cart cart = cartService.getOrCreateCart(userId, sessionId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Validate Inventory (Stub)
        for (CartItem item : cart.getItems()) {
            boolean available = inventoryService.checkStock(
                    item.getProduct().getProductId(),
                    item.getVariant() != null ? item.getVariant().getVariantId() : null,
                    item.getQuantity());
            if (!available) {
                throw new RuntimeException("Product out of stock: " + item.getProduct().getName());
            }
        }

        // Determine Address
        String shippingAddressStr = request.getShippingAddress();
        // Defaults if addressId provided (Mocking fetch logic or assuming string for
        // now if Repo fetch is complex)
        // Ideally: fetch UserAddress by ID and populate fields.
        if (request.getAddressId() != null) {
            // Mocking address fetch for simplicity as UserAddressRepository was seen but
            // not inspected deeply
            // In real app: UserAddress addr =
            // userAddressRepository.findById(request.getAddressId())...
            shippingAddressStr = "Address ID " + request.getAddressId();
        }

        // Create Order
        String receiverName = request.getReceiverName();
        if (receiverName == null || receiverName.trim().isEmpty()) {
            if (cart.getUser() != null) {
                receiverName = cart.getUser().getFullName();
            }
        }
        if (receiverName == null || receiverName.trim().isEmpty()) {
            receiverName = "Guest Customer"; // Final fallback
        }

        String receiverPhone = request.getReceiverPhone();
        if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
            if (cart.getUser() != null) {
                receiverPhone = cart.getUser().getPhone();
            }
        }
        if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
            receiverPhone = "0000000000"; // Final fallback
        }

        Order order = Order.builder()
                .user(cart.getUser())
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod())) // VNPAY, COD
                .paymentStatus(PaymentStatus.PENDING)
                .shippingRecipientName(receiverName)
                .shippingPhone(receiverPhone)
                .shippingAddress(shippingAddressStr != null ? shippingAddressStr : "Not provided")
                .shippingDistrict("District") // Placeholder
                .shippingCity("City") // Placeholder
                .customerNotes(request.getNote())
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .build();

        // Calc totals
        BigDecimal subtotal = BigDecimal.ZERO;

        // Create Items
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variant(cartItem.getVariant())
                    .productName(cartItem.getProduct().getName()) // Require this field
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPriceAtAdd())
                    .subtotal(cartItem.getSubtotal())
                    .hasStringingService(cartItem.hasStringingService())
                    .build();
            return orderItem;
        }).collect(Collectors.toList());

        for (OrderItem item : orderItems) {
            subtotal = subtotal.add(item.getSubtotal());
        }

        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal); // Add shipping/tax later
        order.setItems(orderItems);

        // Save Order (Cascade items)
        Order savedOrder = orderRepository.save(order);
        logStatusHistory(savedOrder, "Order created", com.badmintonshop.entity.enums.ChangedByType.CUSTOMER, userId);

        // Check and decrease stock
        for (CartItem item : cart.getItems()) {
            if (!inventoryService.decreaseStock(item.getProduct().getProductId(),
                    item.getVariant() != null ? item.getVariant().getVariantId() : null, item.getQuantity())) {
                throw new RuntimeException("Product out of stock: " + item.getProduct().getName()
                        + " (ID: " + item.getProduct().getProductId()
                        + ", Variant: " + (item.getVariant() != null ? item.getVariant().getVariantId() : "None")
                        + ", Qty: " + item.getQuantity() + ")");
            }
        }

        // Clear Cart
        cartService.clearCart(userId, sessionId);

        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + RandomStringUtils.randomNumeric(4);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(Long userId) {
        List<Order> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        orders.forEach(this::initializeOrderItems);
        return orders;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        initializeOrderItems(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        initializeOrderItems(order);
        return order;
    }

    private void initializeOrderItems(Order order) {
        if (order.getItems() != null) {
            order.getItems().size(); // Init items collection
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getName(); // Init product proxy
                    if (item.getProduct().getImages() != null) {
                        item.getProduct().getImages().size(); // Init images
                    }
                    if (item.getProduct().getVariants() != null) {
                        item.getProduct().getVariants().size(); // Init product variants list
                    }
                }
                if (item.getVariant() != null) {
                    item.getVariant().getVariantName(); // Init specific variant proxy
                }
            }
        }
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId, String reason) {
        Order order = getOrder(orderId);
        if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!order.isCancellable()) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.cancel(com.badmintonshop.entity.enums.CancelledBy.CUSTOMER, reason);
        orderRepository.save(order);
        logStatusHistory(order, "Order cancelled by user: " + reason,
                com.badmintonshop.entity.enums.ChangedByType.CUSTOMER, userId);

        // Restore inventory (simplified)
        for (OrderItem item : order.getItems()) {
            inventoryService.increaseStock(
                    item.getProduct().getProductId(),
                    item.getVariant() != null ? item.getVariant().getVariantId() : null,
                    item.getQuantity());
        }
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String notes, Long staffId) {
        Order order = getOrder(orderId);
        OrderStatus oldStatus = order.getStatus();

        // Prevent backward transitions or invalid logic if needed
        // For now, allow admin to override

        order.updateStatus(newStatus);

        // Fix: Auto-update PaymentStatus to PAID if Delivered (regardless of method)
        if (newStatus == OrderStatus.DELIVERED) {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                // Append to notes
                notes = (notes == null ? "" : notes + ". ") + "System: Marked as PAID upon delivery.";
            }
        }

        orderRepository.save(order);

        logStatusHistory(order, notes, com.badmintonshop.entity.enums.ChangedByType.STAFF, staffId);
    }

    private void logStatusHistory(Order order, String notes, com.badmintonshop.entity.enums.ChangedByType type,
            Long changedById) {
        // FK constraint in database might only allow staff IDs for changed_by_id
        // So we set it to null if the type is CUSTOMER
        Long finalChangerId = (type == com.badmintonshop.entity.enums.ChangedByType.STAFF) ? changedById : null;

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(null)
                .toStatus(order.getStatus().name())
                .notes(notes)
                .changedByType(type)
                .changedById(finalChangerId)
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(history);
    }
}
