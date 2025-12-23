package com.badmintonshop.controller.api;

import com.badmintonshop.dto.cart.*;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for Shopping Cart
 * Supports both form login (CustomUserDetails) and OAuth2 (CustomOAuth2User)
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    /**
     * Get current cart
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpSession session) {
        Long userId = getCurrentUserId();
        
        CartResponse cart;
        if (userId != null) {
            cart = cartService.getCartResponse(userId);
        } else {
            String sessionId = getOrCreateGuestSession(session);
            cart = cartService.getGuestCartResponse(sessionId);
        }

        return ResponseEntity.ok(cart);
    }

    /**
     * Add item to cart (legacy endpoint - kept for backward compatibility)
     * POST /api/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCartLegacy(
            @Valid @RequestBody AddToCartRequest request,
            HttpSession session) {
        return addToCart(request, session);
    }

    /**
     * Add item to cart - supports both guests and logged-in users
     * POST /api/cart/items
     */
    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpSession session) {

        try {
            Long userId = getCurrentUserId();
            String sessionId = userId == null ? getOrCreateGuestSession(session) : null;
            
            log.info("API addToCart - userId: {}, sessionId: {}", userId, sessionId);
            
            CartResponse cart = cartService.addToCart(userId, sessionId, request);
            return ResponseEntity.ok(cart);
        } catch (com.badmintonshop.exception.InsufficientStockException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "availableQuantity", e.getAvailableQuantity()));
        }
    }

    /**
     * Update stringing options for a cart item
     * POST /api/cart/items/{itemId}/stringing
     */
    @PostMapping("/items/{itemId}/stringing")
    public ResponseEntity<?> updateStringing(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateStringingRequest request,
            HttpSession session) {

        try {
            cartService.updateStringingOption(
                    itemId,
                    request.getStringingServiceId(),
                    request.getStringProductId(),
                    request.getTension(),
                    request.getStringingNotes());

            // Return updated cart
            Long userId = getCurrentUserId();
            if (userId != null) {
                return ResponseEntity.ok(cartService.getCartResponse(userId));
            } else {
                String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
                return ResponseEntity.ok(cartService.getGuestCartResponse(sessionId));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()));
        }
    }

    /**
     * Update cart item quantity - supports both guests and logged-in users
     * PUT /api/cart/items/{itemId}
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpSession session) {

        try {
            Long userId = getCurrentUserId();
            String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

            CartResponse cart = cartService.updateCartItem(itemId, userId, sessionId, request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (IllegalStateException e) {
            // Stock validation error or permission error
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()));
        } catch (com.badmintonshop.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", true,
                    "message", e.getMessage()));
        }
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long itemId,
            HttpSession session) {

        Long userId = getCurrentUserId();
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        CartResponse cart = cartService.removeFromCart(itemId, userId, sessionId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Clear cart
     * DELETE /api/cart
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpSession session) {
        Long userId = getCurrentUserId();
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        cartService.clearCart(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count
     * GET /api/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartCount(HttpSession session) {
        Long userId = getCurrentUserId();
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);

        int count = cartService.getCartItemCount(userId, sessionId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Merge guest cart when user logs in (called after successful login)
     * POST /api/cart/merge
     */
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(HttpSession session) {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String guestSessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
        if (guestSessionId != null) {
            cartService.mergeGuestCart(guestSessionId, userId);
            session.removeAttribute(GUEST_SESSION_KEY);
        }

        CartResponse cart = cartService.getCartResponse(userId);
        return ResponseEntity.ok(cart);
    }

    // ===== HELPER METHODS =====

    /**
     * Get current user ID from SecurityContext
     * Supports both CustomUserDetails (form login) and CustomOAuth2User (OAuth2)
     */
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
        
        // anonymousUser or other types
        return null;
    }

    /**
     * Get or create guest session ID
     */
    private String getOrCreateGuestSession(HttpSession session) {
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute(GUEST_SESSION_KEY, sessionId);
        }
        return sessionId;
    }
}

