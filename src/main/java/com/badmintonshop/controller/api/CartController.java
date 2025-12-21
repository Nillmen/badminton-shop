package com.badmintonshop.controller.api;

import com.badmintonshop.dto.CartItemRequest;
import com.badmintonshop.dto.CartResponse;
import com.badmintonshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 1. Xem giỏ
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(cartService.getCartResponse(userId, sessionId));
    }

    // 2. Thêm vào giỏ
    @PostMapping("/items") // Đổi path theo đúng Task list của bro (/api/cart/items)
    public ResponseEntity<CartResponse> addToCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(userId, sessionId, request));
    }

    // 3. Cập nhật số lượng (Ví dụ: Tăng lên 2 cái)
    // URL: PUT /api/cart/items/1?quantity=2
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(userId, sessionId, cartItemId, quantity));
    }

    // 4. Xóa 1 món
    // URL: DELETE /api/cart/items/1
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, sessionId, cartItemId));
    }

    // 5. Xóa sạch giỏ
    // URL: DELETE /api/cart
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        cartService.clearCart(userId, sessionId);
        return ResponseEntity.ok().build();
    }

    // 6. Thêm dịch vụ đan vợt vào một item trong giỏ
    // URL: POST /api/cart/items/1/stringing
    @PostMapping("/items/{cartItemId}/stringing")
    public ResponseEntity<CartResponse> updateStringing(
            @PathVariable Long cartItemId,
            @RequestBody com.badmintonshop.dto.StringingRequest request,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {

        return ResponseEntity.ok(cartService.updateStringingInfo(userId, sessionId, cartItemId, request));
    }

    // 7. Lấy tổng số lượng item (Dùng cho Badge icon giỏ hàng trên Header)
    // URL: GET /api/cart/count
    @GetMapping("/count")
    public ResponseEntity<Integer> getCartCount(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(cartService.getCartCount(userId, sessionId));
    }
}