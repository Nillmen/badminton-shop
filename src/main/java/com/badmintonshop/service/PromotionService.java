package com.badmintonshop.service;

import com.badmintonshop.dto.promotion.PromotionDTO;
import com.badmintonshop.entity.Promotion;
import com.badmintonshop.entity.enums.DiscountType;
import com.badmintonshop.entity.enums.PromotionType;
import com.badmintonshop.repository.PromotionRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionNotificationService promotionNotificationService;

    /**
     * Get all active promotions (for guests and users)
     */
    @Transactional(readOnly = true)
    public List<PromotionDTO> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(now);
        return promotions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active promotions by type (e.g., flash sales)
     */
    @Transactional(readOnly = true)
    public List<PromotionDTO> getActivePromotionsByType(PromotionType type) {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotionsByType(now, type);
        return promotions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get promotion by ID (for guests and users)
     */
    @Transactional(readOnly = true)
    public Optional<PromotionDTO> getPromotionById(Long id) {
        return promotionRepository.findById(id).map(this::mapToDTO);
    }

    /**
     * Get active promotion by ID (only if currently active)
     */
    @Transactional(readOnly = true)
    public Optional<PromotionDTO> getActivePromotionById(Long id) {
        return promotionRepository.findById(id)
                .filter(Promotion::isActive)
                .map(this::mapToDTO);
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Search promotions for admin using JPA Specification
     * Status filter: ACTIVE (within date range), EXPIRED (past end date),
     * NOT_STARTED (before start date)
     */
    @Transactional(readOnly = true)
    public Page<PromotionDTO> searchPromotions(String keyword, PromotionType type, String status,
            Pageable pageable) {
        // Convert empty strings to null for proper handling
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        log.info("Searching promotions with: keyword={}, type={}, status={}", searchKeyword, type, searchStatus);

        LocalDateTime now = LocalDateTime.now();

        // Build specification dynamically
        Specification<Promotion> spec = Specification.where(null);

        if (searchKeyword != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")),
                    "%" + searchKeyword.toLowerCase() + "%"));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (searchStatus != null) {
            switch (searchStatus) {
                case "ACTIVE" -> spec = spec.and((root, query, cb) -> cb.and(
                        cb.equal(root.get("isActive"), true),
                        cb.lessThanOrEqualTo(root.get("startsAt"), now),
                        cb.greaterThan(root.get("endsAt"), now)));
                case "EXPIRED" -> spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endsAt"), now));
                case "NOT_STARTED" -> spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("startsAt"), now));
            }
        }

        Page<Promotion> promotions = promotionRepository.findAll(spec, pageable);

        log.info("Found {} promotions", promotions.getTotalElements());

        return promotions.map(this::mapToDTO);
    }

    /**
     * Create new promotion
     */
    @Transactional
    @Auditable(entityType = "Promotion", action = ActivityAction.CREATE, description = "Created promotion: {0}")
    public PromotionDTO createPromotion(PromotionDTO dto) {
        if (dto.getStartsAt() == null || dto.getEndsAt() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc là bắt buộc");
        }
        if (dto.getStartsAt().isAfter(dto.getEndsAt())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Promotion promotion = Promotion.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .applicableProducts(dto.getApplicableProducts())
                .applicableCategories(dto.getApplicableCategories())
                .bundleProducts(dto.getBundleProducts())
                .bundlePrice(dto.getBundlePrice())
                .buyQuantity(dto.getBuyQuantity())
                .getQuantity(dto.getGetQuantity())
                .startsAt(dto.getStartsAt())
                .endsAt(dto.getEndsAt())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        log.info("Created promotion: {}", saved.getName());

        // Send notifications to all users (async)
        promotionNotificationService.notifyNewPromotion(saved);

        return mapToDTO(saved);
    }

    /**
     * Update promotion
     */
    @Transactional
    @Auditable(entityType = "Promotion", action = ActivityAction.UPDATE, description = "Updated promotion ID: {0}")
    public PromotionDTO updatePromotion(Long id, PromotionDTO dto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));

        if (dto.getStartsAt() != null && dto.getEndsAt() != null && dto.getStartsAt().isAfter(dto.getEndsAt())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        promotion.setName(dto.getName());
        promotion.setDescription(dto.getDescription());
        promotion.setType(dto.getType());
        promotion.setDiscountType(dto.getDiscountType());
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setApplicableProducts(dto.getApplicableProducts());
        promotion.setApplicableCategories(dto.getApplicableCategories());
        promotion.setBundleProducts(dto.getBundleProducts());
        promotion.setBundlePrice(dto.getBundlePrice());
        promotion.setBuyQuantity(dto.getBuyQuantity());
        promotion.setGetQuantity(dto.getGetQuantity());
        if (dto.getStartsAt() != null)
            promotion.setStartsAt(dto.getStartsAt());
        if (dto.getEndsAt() != null)
            promotion.setEndsAt(dto.getEndsAt());
        if (dto.getIsActive() != null)
            promotion.setIsActive(dto.getIsActive());

        Promotion saved = promotionRepository.save(promotion);
        log.info("Updated promotion: {}", saved.getName());
        return mapToDTO(saved);
    }

    /**
     * Delete promotion (soft delete)
     */
    @Transactional
    @Auditable(entityType = "Promotion", action = ActivityAction.DELETE, description = "Soft deleted promotion ID: {0}")
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));

        promotion.setDeletedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
        log.info("Deleted promotion: {}", promotion.getName());
    }

    // ==================== MAPPER ====================

    private PromotionDTO mapToDTO(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        Long remainingSeconds = null;

        if (promotion.getEndsAt().isAfter(now)) {
            remainingSeconds = Duration.between(now, promotion.getEndsAt()).getSeconds();
        }

        String typeText = switch (promotion.getType()) {
            case FLASH_SALE -> "Flash Sale";
            case BUNDLE -> "Combo";
            case BUY_X_GET_Y -> "Mua " + promotion.getBuyQuantity() + " tặng " + promotion.getGetQuantity();
            case DISCOUNT -> "Giảm giá";
        };

        String discountText = "";
        if (promotion.getDiscountType() != null && promotion.getDiscountValue() != null) {
            discountText = promotion.getDiscountType() == DiscountType.PERCENTAGE
                    ? "-" + promotion.getDiscountValue() + "%"
                    : "-" + formatPrice(promotion.getDiscountValue());
        }

        return PromotionDTO.builder()
                .promotionId(promotion.getPromotionId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .type(promotion.getType())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .applicableProducts(promotion.getApplicableProducts())
                .applicableCategories(promotion.getApplicableCategories())
                .bundleProducts(promotion.getBundleProducts())
                .bundlePrice(promotion.getBundlePrice())
                .buyQuantity(promotion.getBuyQuantity())
                .getQuantity(promotion.getGetQuantity())
                .startsAt(promotion.getStartsAt())
                .endsAt(promotion.getEndsAt())
                .isActive(promotion.getIsActive())
                .typeText(typeText)
                .discountText(discountText)
                .remainingTimeSeconds(remainingSeconds)
                .build();
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%,d₫", price.longValue());
    }

    // ==================== TRASH METHODS ====================

    /**
     * Get deleted promotions (for trash)
     */
    @Transactional(readOnly = true)
    public Page<PromotionDTO> getDeletedPromotions(Pageable pageable) {
        return promotionRepository.findDeletedPromotions(pageable).map(this::mapToDTO);
    }

    /**
     * Restore promotion from trash
     */
    @Transactional
    @Auditable(entityType = "Promotion", action = ActivityAction.UPDATE, description = "Restored promotion ID: {0}")
    public void restorePromotion(Long id) {
        Promotion promotion = promotionRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Khuyến mãi không tồn tại: " + id));

        if (promotion.getDeletedAt() == null) {
            throw new IllegalArgumentException("Khuyến mãi chưa bị xóa");
        }

        promotion.setDeletedAt(null);
        promotionRepository.save(promotion);
        log.info("Restored promotion: {} (ID: {})", promotion.getName(), id);
    }

    /**
     * Hard delete promotion (permanently)
     */
    @Transactional
    @Auditable(entityType = "Promotion", action = ActivityAction.DELETE, description = "Hard deleted promotion ID: {0}")
    public void hardDeletePromotion(Long id) {
        Promotion promotion = promotionRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Khuyến mãi không tồn tại: " + id));

        if (promotion.getDeletedAt() == null) {
            throw new IllegalArgumentException("Khuyến mãi chưa bị xóa mềm, không thể xóa vĩnh viễn");
        }

        promotionRepository.delete(promotion);
        log.info("Hard deleted promotion: {} (ID: {})", promotion.getName(), id);
    }
}
