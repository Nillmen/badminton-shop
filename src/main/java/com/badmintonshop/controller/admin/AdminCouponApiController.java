package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.coupon.AdminCouponDTO;
import com.badmintonshop.entity.enums.CouponType;
import com.badmintonshop.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin REST API Controller for Coupon CRUD operations
 */
@RestController
@RequestMapping("/admin/api/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('coupons.view')")
public class AdminCouponApiController {

    private final CouponService couponService;

    /**
     * Get all coupons with pagination
     * GET /admin/api/coupons
     */
    @GetMapping
    public ResponseEntity<Page<AdminCouponDTO>> getAllCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CouponType type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(couponService.searchCoupons(keyword, type, status, pageable));
    }

    /**
     * Get coupon by ID
     * GET /admin/api/coupons/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminCouponDTO> getCouponById(@PathVariable Long id) {
        return couponService.getCouponDTOById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new coupon
     * POST /admin/api/coupons
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('coupons.create')")
    public ResponseEntity<?> createCoupon(@Valid @RequestBody AdminCouponDTO dto) {
        try {
            AdminCouponDTO created = couponService.createCoupon(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update coupon
     * PUT /admin/api/coupons/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('coupons.update')")
    public ResponseEntity<?> updateCoupon(@PathVariable Long id, @Valid @RequestBody AdminCouponDTO dto) {
        try {
            AdminCouponDTO updated = couponService.updateCoupon(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete coupon (soft delete)
     * DELETE /admin/api/coupons/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('coupons.delete')")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            return ResponseEntity.ok(Map.of("message", "Xóa mã giảm giá thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get coupon usage history
     * GET /admin/api/coupons/{id}/usage
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<?> getCouponUsage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("usedAt").descending());
            return ResponseEntity.ok(couponService.getCouponUsageHistory(id, pageable));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
