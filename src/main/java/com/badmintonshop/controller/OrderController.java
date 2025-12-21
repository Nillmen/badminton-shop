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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final DTOMapper dtoMapper;

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        }
        throw new RuntimeException("User must be logged in");
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(getUserId(), request);
        return ResponseEntity.ok(dtoMapper.toOrderResponse(order));
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
