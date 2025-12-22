package com.badmintonshop.controller;

import com.badmintonshop.dto.DTOMapper;
import com.badmintonshop.dto.order.OrderRequest;
import com.badmintonshop.dto.order.OrderResponse;
import com.badmintonshop.entity.Order;
import com.badmintonshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.security.CustomOAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.badmintonshop.service.PaymentService;
import com.badmintonshop.entity.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Transactional
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DTOMapper dtoMapper;
    private final HttpServletRequest httpServletRequest;

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal(); // Allow safe casting
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            } else if (principal instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) principal).getUserId();
            }
        }
        return null;
    }

    private String getSessionId() {
        return httpServletRequest.getHeader("X-Session-ID");
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Long userId = getUserId();
        String sessionId = getSessionId();

        Order order = orderService.createOrder(userId, sessionId, request);

        String paymentUrl = null;
        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentUrl = paymentService.createVnpayPaymentUrl(order);
        }

        return ResponseEntity.ok(dtoMapper.toOrderResponse(order, paymentUrl));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getHistory() {
        Long userId = getUserId();
        List<Order> orders = orderService.getOrderHistory(userId);
        // log.info("User {} has {} orders", userId, orders.size()); // Using sysout if
        // no slf4j or add slf4j
        System.out.println("User " + userId + " has " + orders.size() + " orders");
        return ResponseEntity.ok(orders.stream().map(dtoMapper::toOrderResponse).collect(Collectors.toList()));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderService.getOrderByNumber(orderNumber);
        // Security check: ensure order belongs to user
        if (!order.getUser().getUserId().equals(getUserId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dtoMapper.toOrderResponse(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, @RequestBody Map<String, String> payload) {
        orderService.cancelOrder(getUserId(), orderId, payload.get("reason"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(dtoMapper.toOrderResponse(orderService.getOrder(id)));
    }
}
