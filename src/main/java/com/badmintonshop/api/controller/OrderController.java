package com.badmintonshop.api.controller;

import com.badmintonshop.dto.OrderRequest;
import com.badmintonshop.dto.OrderResponse;
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.User;
import com.badmintonshop.repository.UserRepository;
import com.badmintonshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // --- Giả lập User ID 1 (Tạm thời) ---
    private Long getCurrentUserId() {
        // TODO: Thay thế bằng cơ chế lấy User ID từ JWT/Security Context
        return 1L;
    }
    // ------------------------------------

    // 1. POST /api/orders/create (Checkout)
    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        try {
            // Find or Create User based on Email
            String email = request.getEmail();
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Email là bắt buộc để đặt hàng.");
            }

            User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                    .orElseGet(() -> createGuestUser(request));

            Order newOrder = orderService.createOrder(user, request.getSessionId(), request);

            return ResponseEntity.ok(mapToResponse(newOrder));
        } catch (Exception e) {
            e.printStackTrace(); // Log for server
            throw new com.badmintonshop.exception.BusinessException("Order Failed: " + e.getMessage()); // Return to
                                                                                                        // client
        }
    }

    private User createGuestUser(OrderRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName() != null ? request.getFullName() : "Guest");
        user.setPhone(request.getPhone());
        // Set dummy password for guest
        user.setPasswordHash("$2a$10$DUMMYPASSWORDHASHFORGUESTUSERONLY123456");
        user.setStatus(com.badmintonshop.entity.enums.UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    // 2. GET /api/orders (Lịch sử đơn hàng của User / Guest)
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(@RequestParam(required = false) String sessionId) {
        Long userId = getCurrentUserId();
        // If guest, userId might be 1L (dummy) or null depending on auth.
        // We pass both to service. Service logic handles priority.

        List<Order> orders = orderService.getUserOrders(userId, sessionId);

        List<OrderResponse> responseList = orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    // 3. GET /api/orders/{orderNumber} (Chi tiết đơn hàng)
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable String orderNumber,
            @RequestParam(required = false) String sessionId) {
        Long userId = getCurrentUserId();
        Order order = orderService.getOrderByOrderNumber(orderNumber, userId, sessionId);

        return ResponseEntity.ok(mapToResponse(order));
    }

    // 4. POST /api/orders/{id}/cancel (Hủy đơn hàng)
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId,
            @RequestParam(required = false) String sessionId) {
        Long userId = getCurrentUserId();

        // Hủy đơn hàng và hoàn lại tồn kho
        Order CANCELLEDOrder = orderService.cancelOrder(orderId, userId, sessionId);

        return ResponseEntity.ok(mapToResponse(CANCELLEDOrder));
    }

    // Hàm chuyển đổi dữ liệu (Mapping)
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getOrderId());
        response.setOrderNumber(order.getOrderNumber());

        if (order.getStatus() != null) {
            response.setStatus(order.getStatus().name());
        }

        response.setTotalAmount(order.getTotalAmount());

        if (order.getPaymentMethod() != null) {
            response.setPaymentMethod(order.getPaymentMethod().name());
        }

        if (order.getPaymentStatus() != null) {
            response.setPaymentStatus(order.getPaymentStatus().name());
        }

        // Map thông tin giao hàng
        response.setRecipientName(order.getShippingRecipientName());
        response.setPhone(order.getShippingPhone());

        // Tạo chuỗi địa chỉ đầy đủ
        String fullAddress = order.getShippingAddress();
        if (order.getShippingWard() != null)
            fullAddress += ", " + order.getShippingWard();
        if (order.getShippingDistrict() != null)
            fullAddress += ", " + order.getShippingDistrict();
        if (order.getShippingCity() != null)
            fullAddress += ", " + order.getShippingCity();
        response.setAddress(fullAddress);

        response.setCreatedAt(order.getCreatedAt());

        // Map thông tin User
        if (order.getUser() != null) {
            response.setUserId(order.getUser().getUserId());
            // Giả sử User Entity có hàm getFullName()
            // response.setUserName(order.getUser().getFullName());
        }

        // Map Items
        if (order.getItems() != null) {
            List<com.badmintonshop.dto.OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(item -> {
                        return com.badmintonshop.dto.OrderItemResponse.builder()
                                .productName(item.getProductName())
                                .variantName(item.getVariantName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice()) // Assuming unitPrice is the price at purchase
                                // Map other fields as needed for display
                                .build();
                    })
                    .collect(Collectors.toList());
            response.setItems(itemResponses);
        }

        return response;
    }
}