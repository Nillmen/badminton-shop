package com.badmintonshop.controller;

import com.badmintonshop.dto.DTOMapper;
import com.badmintonshop.dto.cart.CartItemRequest;
import com.badmintonshop.dto.cart.CartResponse;
import com.badmintonshop.entity.Cart;
import com.badmintonshop.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.security.CustomOAuth2User;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final DTOMapper dtoMapper;

    private final HttpServletRequest httpServletRequest;

    @PostConstruct
    public void init() {
        log.info("CartController initialized");
    }

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            } else if (principal instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) principal).getUserId();
            }
        }
        return null;
    }

    private String getSessionId() {
        String sessionId = httpServletRequest.getHeader("X-Session-ID");
        if (sessionId == null || sessionId.isEmpty()) {
            // In a real app we might generate one or expect client to handle it.
            // For now, return null, service will handle if both are null (exception)
            return null;
        }
        return sessionId;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Cart cart = cartService.getOrCreateCart(getUserId(), getSessionId());
        return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@RequestBody CartItemRequest request) {
        log.info("Received addToCart request: productId={}, variantId={}, quantity={}",
                request.getProductId(), request.getVariantId(), request.getQuantity());
        try {
            // Check if user is logged out AND no session ID provided?
            // Service handles logic.
            Cart cart = cartService.addToCart(getUserId(), getSessionId(), request);
            log.info("Successfully added to cart. CartId: {}", cart.getCartId());
            return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
        } catch (Exception e) {
            log.error("Failed to add to cart: ", e);
            throw e;
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeFromCart(getUserId(), getSessionId(), itemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable Long itemId, @RequestParam Integer quantity) {
        Cart cart = cartService.updateItemQuantity(getUserId(), getSessionId(), itemId, quantity);
        return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart(getUserId(), getSessionId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCartCount() {
        return ResponseEntity.ok(cartService.getCartItemCount(getUserId(), getSessionId()));
    }

    @PostMapping("/items/{itemId}/stringing")
    public ResponseEntity<Void> addStringingService(@PathVariable Long itemId,
            @RequestBody Map<String, String> payload) {
        cartService.addStringingService(getUserId(), getSessionId(), itemId, payload.get("info"));
        return ResponseEntity.ok().build();
    }
}
