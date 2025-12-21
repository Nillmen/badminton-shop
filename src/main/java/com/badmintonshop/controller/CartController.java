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
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final DTOMapper dtoMapper;

    @PostConstruct
    public void init() {
        log.info("CartController initialized");
    }

    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUserId();
        }
        // For guest cart, we might want to return null, but service doesn't support it
        // yet.
        // Throwing exception for now to force login.
        throw new RuntimeException("User must be logged in");
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Cart cart = cartService.getOrCreateCart(getUserId());
        return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@RequestBody CartItemRequest request) {
        log.info("Received addToCart request: productId={}, variantId={}, quantity={}",
                request.getProductId(), request.getVariantId(), request.getQuantity());
        try {
            Cart cart = cartService.addToCart(getUserId(), request);
            log.info("Successfully added to cart. CartId: {}", cart.getCartId());
            return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
        } catch (Exception e) {
            log.error("Failed to add to cart: ", e);
            throw e;
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeFromCart(getUserId(), itemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable Long itemId, @RequestParam Integer quantity) {
        Cart cart = cartService.updateItemQuantity(getUserId(), itemId, quantity);
        return ResponseEntity.ok(dtoMapper.toCartResponse(cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart(getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCartCount() {
        return ResponseEntity.ok(cartService.getCartItemCount(getUserId()));
    }

    @PostMapping("/items/{itemId}/stringing")
    public ResponseEntity<Void> addStringingService(@PathVariable Long itemId,
            @RequestBody Map<String, String> payload) {
        cartService.addStringingService(getUserId(), itemId, payload.get("info"));
        return ResponseEntity.ok().build();
    }
}
