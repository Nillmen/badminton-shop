package com.badmintonshop.service;

import com.badmintonshop.dto.coupon.AdminCouponDTO;
import com.badmintonshop.dto.coupon.CouponDTO;
import com.badmintonshop.dto.coupon.CouponValidationResponse;
import com.badmintonshop.entity.Coupon;
import com.badmintonshop.entity.CouponUsage;
import com.badmintonshop.entity.enums.ApplicableTo;
import com.badmintonshop.entity.enums.CouponType;
import com.badmintonshop.repository.CouponRepository;
import com.badmintonshop.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final PromotionNotificationService promotionNotificationService;

    /**
     * Validate a coupon code for a user and order total
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, Long userId, BigDecimal orderTotal) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResponse.invalid("Vui lòng nhập mã giảm giá");
        }

        // Find coupon
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (coupon == null) {
            return CouponValidationResponse.invalid("Mã giảm giá không tồn tại");
        }

        // Check if coupon is valid (active, not expired, not exceeded usage)
        if (!coupon.isValid()) {
            if (coupon.isExpired()) {
                return CouponValidationResponse.invalid("Mã giảm giá đã hết hạn");
            }
            if (!coupon.getIsActive()) {
                return CouponValidationResponse.invalid("Mã giảm giá không còn hoạt động");
            }
            return CouponValidationResponse.invalid("Mã giảm giá không khả dụng");
        }

        // Check minimum order value
        if (orderTotal.compareTo(coupon.getMinOrderValue()) < 0) {
            return CouponValidationResponse.invalid(
                    String.format("Đơn hàng tối thiểu %s để sử dụng mã này",
                            formatPrice(coupon.getMinOrderValue())));
        }

        // Check user usage limit
        if (userId != null && coupon.getUsagePerUser() != null) {
            long userUsage = couponUsageRepository.countByCouponCouponIdAndUserUserId(coupon.getCouponId(), userId);
            if (userUsage >= coupon.getUsagePerUser()) {
                return CouponValidationResponse.invalid("Bạn đã sử dụng hết lượt dùng mã này");
            }
        }

        // Calculate discount
        BigDecimal discountAmount = coupon.calculateDiscount(orderTotal);
        BigDecimal newTotal = orderTotal.subtract(discountAmount);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        log.info("Coupon {} validated for user {}: discount={}, newTotal={}",
                code, userId, discountAmount, newTotal);

        return CouponValidationResponse.valid(
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getType(),
                discountAmount,
                newTotal);
    }

    /**
     * Record coupon usage when order is placed
     * - Creates CouponUsage record
     * - Increments timesUsed counter on Coupon
     */
    @Transactional
    public void recordCouponUsage(String couponCode, com.badmintonshop.entity.User user, 
                                   com.badmintonshop.entity.Order order, BigDecimal discountAmount) {
        if (couponCode == null || couponCode.isEmpty()) {
            return;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode).orElse(null);
        if (coupon == null) {
            log.warn("Coupon not found for recording usage: {}", couponCode);
            return;
        }

        // Create usage record
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .discountAmount(discountAmount)
                .usedAt(java.time.LocalDateTime.now())
                .build();
        couponUsageRepository.save(usage);

        // Increment times used
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);

        log.info("Recorded coupon usage: {} for order {} by user {}, discount: {}", 
                couponCode, order.getOrderNumber(), user.getUserId(), discountAmount);
    }

    /**
     * Get coupon by code
     */
    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
    }

    /**
     * Get available coupons for a user
     */
    @Transactional(readOnly = true)
    public java.util.List<CouponDTO> getAvailableCoupons(Long userId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<Coupon> coupons = couponRepository.findValidCoupons(now);

        return coupons.stream()
                .filter(coupon -> {
                    // Check user usage limit if logged in
                    if (userId != null && coupon.getUsagePerUser() != null) {
                        long usage = couponUsageRepository.countByCouponCouponIdAndUserUserId(coupon.getCouponId(),
                                userId);
                        return usage < coupon.getUsagePerUser();
                    }
                    return true;
                })
                .map(this::mapToCouponDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Search coupons for admin with filters using JPA Specification
     * Status filter: ACTIVE (within date range), EXPIRED (past end date),
     * NOT_STARTED (before start date)
     */
    @Transactional(readOnly = true)
    public Page<AdminCouponDTO> searchCoupons(String keyword, CouponType type, String status, Pageable pageable) {
        // Convert empty strings to null for proper handling
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        log.info("Searching coupons with: keyword={}, type={}, status={}", searchKeyword, type, searchStatus);

        LocalDateTime now = LocalDateTime.now();

        // Build specification dynamically
        Specification<Coupon> spec = Specification.where(null);

        if (searchKeyword != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), "%" + searchKeyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("name")), "%" + searchKeyword.toLowerCase() + "%")));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (searchStatus != null) {
            switch (searchStatus) {
                case "ACTIVE" -> spec = spec.and((root, query, cb) -> cb.and(
                        cb.equal(root.get("isActive"), true),
                        cb.lessThanOrEqualTo(root.get("startsAt"), now),
                        cb.greaterThan(root.get("expiresAt"), now)));
                case "EXPIRED" ->
                    spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiresAt"), now));
                case "NOT_STARTED" -> spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("startsAt"), now));
            }
        }

        Page<Coupon> coupons = couponRepository.findAll(spec, pageable);

        log.info("Found {} coupons", coupons.getTotalElements());

        return coupons.map(this::mapToAdminCouponDTO);
    }

    /**
     * Get coupon by ID for admin
     */
    @Transactional(readOnly = true)
    public Optional<AdminCouponDTO> getCouponDTOById(Long id) {
        return couponRepository.findById(id).map(this::mapToAdminCouponDTO);
    }

    /**
     * Create new coupon
     */
    @Transactional
    public AdminCouponDTO createCoupon(AdminCouponDTO dto) {
        // Validate code uniqueness
        if (couponRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại");
        }

        // Validate dates
        if (dto.getStartsAt() == null || dto.getExpiresAt() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc là bắt buộc");
        }
        if (dto.getStartsAt().isAfter(dto.getExpiresAt())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Coupon coupon = Coupon.builder()
                .code(dto.getCode().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .value(dto.getValue())
                .minOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO)
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .applicableTo(dto.getApplicableTo() != null ? dto.getApplicableTo() : ApplicableTo.ALL)
                .applicableProductIds(dto.getApplicableProductIds())
                .applicableCategoryIds(dto.getApplicableCategoryIds())
                .applicableBrandIds(dto.getApplicableBrandIds())
                .usageLimit(dto.getUsageLimit())
                .usagePerUser(dto.getUsagePerUser() != null ? dto.getUsagePerUser() : 1)
                .timesUsed(0)
                .startsAt(dto.getStartsAt())
                .expiresAt(dto.getExpiresAt())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Created coupon: {}", saved.getCode());

        // Send notifications to all users (async)
        promotionNotificationService.notifyNewCoupon(saved);

        return mapToAdminCouponDTO(saved);
    }

    /**
     * Update existing coupon
     */
    @Transactional
    public AdminCouponDTO updateCoupon(Long id, AdminCouponDTO dto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá"));

        // Check code uniqueness if changed
        if (!coupon.getCode().equalsIgnoreCase(dto.getCode()) && couponRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại");
        }

        // Validate dates
        if (dto.getStartsAt() != null && dto.getExpiresAt() != null && dto.getStartsAt().isAfter(dto.getExpiresAt())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        coupon.setCode(dto.getCode().toUpperCase());
        coupon.setName(dto.getName());
        coupon.setDescription(dto.getDescription());
        coupon.setType(dto.getType());
        coupon.setValue(dto.getValue());
        coupon.setMinOrderValue(dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setApplicableTo(dto.getApplicableTo() != null ? dto.getApplicableTo() : ApplicableTo.ALL);
        coupon.setApplicableProductIds(dto.getApplicableProductIds());
        coupon.setApplicableCategoryIds(dto.getApplicableCategoryIds());
        coupon.setApplicableBrandIds(dto.getApplicableBrandIds());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setUsagePerUser(dto.getUsagePerUser() != null ? dto.getUsagePerUser() : 1);
        if (dto.getStartsAt() != null)
            coupon.setStartsAt(dto.getStartsAt());
        if (dto.getExpiresAt() != null)
            coupon.setExpiresAt(dto.getExpiresAt());
        if (dto.getIsActive() != null)
            coupon.setIsActive(dto.getIsActive());

        Coupon saved = couponRepository.save(coupon);
        log.info("Updated coupon: {}", saved.getCode());
        return mapToAdminCouponDTO(saved);
    }

    /**
     * Delete coupon (soft delete)
     */
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá"));

        coupon.setDeletedAt(LocalDateTime.now());
        couponRepository.save(coupon);
        log.info("Deleted coupon: {}", coupon.getCode());
    }

    /**
     * Get coupon usage history
     */
    @Transactional(readOnly = true)
    public Page<CouponUsage> getCouponUsageHistory(Long couponId, Pageable pageable) {
        if (!couponRepository.existsById(couponId)) {
            throw new IllegalArgumentException("Không tìm thấy mã giảm giá");
        }
        return couponUsageRepository.findByCouponCouponId(couponId, pageable);
    }

    // ==================== MAPPER METHODS ====================

    private CouponDTO mapToCouponDTO(Coupon coupon) {
        return CouponDTO.builder()
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .expiresAt(coupon.getExpiresAt())
                .build();
    }

    private AdminCouponDTO mapToAdminCouponDTO(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        String statusText;
        if (!coupon.getIsActive()) {
            statusText = "Không hoạt động";
        } else if (now.isBefore(coupon.getStartsAt())) {
            statusText = "Chưa bắt đầu";
        } else if (now.isAfter(coupon.getExpiresAt())) {
            statusText = "Đã hết hạn";
        } else if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            statusText = "Đã hết lượt";
        } else {
            statusText = "Đang hoạt động";
        }

        String typeText = switch (coupon.getType()) {
            case PERCENTAGE -> "Giảm " + coupon.getValue() + "%";
            case FIXED_AMOUNT -> "Giảm " + formatPrice(coupon.getValue());
            case FREE_SHIPPING -> "Miễn phí vận chuyển";
        };

        return AdminCouponDTO.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .applicableTo(coupon.getApplicableTo())
                .applicableProductIds(coupon.getApplicableProductIds())
                .applicableCategoryIds(coupon.getApplicableCategoryIds())
                .applicableBrandIds(coupon.getApplicableBrandIds())
                .usageLimit(coupon.getUsageLimit())
                .usagePerUser(coupon.getUsagePerUser())
                .timesUsed(coupon.getTimesUsed())
                .startsAt(coupon.getStartsAt())
                .expiresAt(coupon.getExpiresAt())
                .isActive(coupon.getIsActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .statusText(statusText)
                .typeText(typeText)
                .build();
    }

    public String formatPrice(BigDecimal price) {
        if (price == null) return "0 ₫";
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
        String formatted = formatter.format(price);
        return formatted.replace(" ", " ").replace("₫", " ₫"); // Ensure consistent spacing
    }

    // ==================== TRASH METHODS ====================

    /**
     * Get deleted coupons (for trash)
     */
    @Transactional(readOnly = true)
    public Page<AdminCouponDTO> getDeletedCoupons(Pageable pageable) {
        return couponRepository.findDeletedCoupons(pageable).map(this::mapToAdminCouponDTO);
    }

    /**
     * Restore coupon from trash
     */
    @Transactional
    public void restoreCoupon(Long id) {
        Coupon coupon = couponRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon không tồn tại: " + id));

        if (coupon.getDeletedAt() == null) {
            throw new IllegalArgumentException("Coupon chưa bị xóa");
        }

        coupon.setDeletedAt(null);
        couponRepository.save(coupon);
        log.info("Restored coupon: {} (ID: {})", coupon.getCode(), id);
    }

    /**
     * Hard delete coupon (permanently)
     */
    @Transactional
    public void hardDeleteCoupon(Long id) {
        Coupon coupon = couponRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon không tồn tại: " + id));

        if (coupon.getDeletedAt() == null) {
            throw new IllegalArgumentException("Coupon chưa bị xóa mềm, không thể xóa vĩnh viễn");
        }

        couponRepository.delete(coupon);
        log.info("Hard deleted coupon: {} (ID: {})", coupon.getCode(), id);
    }
}
