package com.badmintonshop.controller.api;

import com.badmintonshop.dto.cart.CartResponse;
import com.badmintonshop.dto.coupon.CouponDTO;
import com.badmintonshop.dto.coupon.CouponValidationResponse;
import com.badmintonshop.entity.Coupon;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.CartService;
import com.badmintonshop.service.CouponService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Coupons (Public - for guests and users)
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;

    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    /**
     * Get current user ID from authentication
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }
        return null;
    }

    /**
     * Validate a coupon code
     * POST /api/coupons/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {

        Long userId = getCurrentUserId();

        CouponValidationResponse response = couponService.validateCoupon(code, userId, orderTotal);
        return ResponseEntity.ok(response);
    }

    /**
     * Apply a coupon during checkout - PERSISTS TO CART
     * POST /api/coupons/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal,
            HttpSession session) {

        Long userId = getCurrentUserId();
        String sessionId = getOrCreateGuestSession(session);

        // Validate coupon first
        CouponValidationResponse validation = couponService.validateCoupon(code, userId, orderTotal);

        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", validation.getErrorMessage()));
        }

        try {
            // Apply coupon to cart - PERSIST TO DB
            CartResponse updatedCart = cartService.applyCouponToCart(userId, sessionId, 
                    validation.getCode(), validation.getDiscountAmount());

            log.info("Coupon {} applied to cart for user/session {}/{}, discount: {}",
                    code, userId, sessionId, validation.getDiscountAmount());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Áp dụng mã giảm giá thành công",
                    "couponCode", validation.getCode(),
                    "couponName", validation.getName(),
                    "discountAmount", validation.getDiscountAmount(),
                    "newTotal", updatedCart.getTotal()));
        } catch (Exception e) {
            log.error("Error applying coupon: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi áp dụng mã giảm giá: " + e.getMessage()));
        }
    }

    /**
     * Remove applied coupon
     * DELETE /api/coupons/remove
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeCoupon(HttpSession session) {
        Long userId = getCurrentUserId();
        String sessionId = getOrCreateGuestSession(session);

        try {
            CartResponse updatedCart = cartService.removeCouponFromCart(userId, sessionId);
            log.info("Coupon removed from cart by user/session {}/{}", userId, sessionId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy mã giảm giá",
                    "newTotal", updatedCart.getTotal()));
        } catch (Exception e) {
            log.error("Error removing coupon: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy mã giảm giá"));
        }
    }
    
    /**
     * Clear coupon (for sendBeacon - POST method only)
     * POST /api/coupons/clear
     */
    @PostMapping("/clear")
    public ResponseEntity<?> clearCoupon(HttpSession session) {
        Long userId = getCurrentUserId();
        String sessionId = getOrCreateGuestSession(session);

        try {
            cartService.removeCouponFromCart(userId, sessionId);
            log.info("Coupon cleared from cart by user/session {}/{} (page unload)", userId, sessionId);
        } catch (Exception e) {
            log.debug("Error clearing coupon on page unload: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // Helper to get or create guest session ID
    private String getOrCreateGuestSession(HttpSession session) {
        String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
        if (sessionId == null) {
            sessionId = java.util.UUID.randomUUID().toString();
            session.setAttribute(GUEST_SESSION_KEY, sessionId);
        }
        return sessionId;
    }

    /**
     * Get available coupons for checkout
     * GET /api/coupons/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<CouponDTO>> getAvailableCoupons() {
        Long userId = getCurrentUserId();
        List<CouponDTO> coupons = couponService.getAvailableCoupons(userId);
        return ResponseEntity.ok(coupons);
    }
}
