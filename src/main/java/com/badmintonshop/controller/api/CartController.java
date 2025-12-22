package com.badmintonshop.controller.api;

import com.badmintonshop.dto.cart.*;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for Shopping Cart
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    /**
     * Get current cart
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        CartResponse cart;
        if (userDetails != null) {
            cart = cartService.getCartResponse(userDetails.getUserId());
        } else {
            String sessionId = getOrCreateGuestSession(session);
            cart = cartService.getGuestCartResponse(sessionId);
        }

        return ResponseEntity.ok(cart);
    }

    /**
     * Add item to cart - REQUIRES LOGIN
     * POST /api/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        // Require user to be logged in to add to cart
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Long userId = userDetails.getUserId();
            CartResponse cart = cartService.addToCart(userId, null, request);
            return ResponseEntity.ok(cart);
        } catch (com.badmintonshop.exception.InsufficientStockException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "availableQuantity", e.getAvailableQuantity()));
        }
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/items/{itemId}
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CartResponse cart = cartService.updateCartItem(itemId, userDetails.getUserId(), request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Long userId = userDetails != null ? userDetails.getUserId() : null;
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        CartResponse cart = cartService.removeFromCart(itemId, userId, sessionId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Clear cart
     * DELETE /api/cart
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Long userId = userDetails != null ? userDetails.getUserId() : null;
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        cartService.clearCart(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count
     * GET /api/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Long userId = userDetails != null ? userDetails.getUserId() : null;
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        int count = cartService.getCartItemCount(userId, sessionId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Merge guest cart when user logs in (called after successful login)
     * POST /api/cart/merge
     */
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String guestSessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
        if (guestSessionId != null) {
            cartService.mergeGuestCart(guestSessionId, userDetails.getUserId());
            session.removeAttribute(GUEST_SESSION_KEY);
        }

        CartResponse cart = cartService.getCartResponse(userDetails.getUserId());
        return ResponseEntity.ok(cart);
    }

    // Helper method to get or create guest session ID
    private String getOrCreateGuestSession(HttpSession session) {
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute(GUEST_SESSION_KEY, sessionId);
        }
        return sessionId;
    }
}
