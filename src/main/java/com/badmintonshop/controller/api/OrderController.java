package com.badmintonshop.controller.api;

import com.badmintonshop.dto.order.*;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for Customer Order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    
    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    /**
     * Create order from cart (checkout)
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest request,
            HttpSession session) {
        
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "message", "Vui lòng đăng nhập để đặt hàng"));
        }

        try {
            String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
            OrderResponse order = orderService.createOrder(userId, sessionId, request);
            
            // Clear guest session after successful order
            session.removeAttribute(GUEST_SESSION_KEY);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Error creating order for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Get current user's order history
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "message", "Vui lòng đăng nhập"));
        }

        Page<OrderListDTO> orders = orderService.getOrdersByUser(userId, page, size);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order details by order number
     * GET /api/orders/{orderNumber}
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "message", "Vui lòng đăng nhập"));
        }

        try {
            OrderResponse order = orderService.getOrderByNumber(orderNumber, userId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Cancel order
     * POST /api/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "message", "Vui lòng đăng nhập"));
        }

        try {
            String reason = body != null ? body.get("reason") : null;
            OrderResponse order = orderService.cancelOrder(id, userId, reason);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    // ===== HELPER METHODS =====

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }
        
        return null;
    }
}
