package com.badmintonshop.service;

import com.badmintonshop.dto.banner.BannerDTO;
import com.badmintonshop.entity.Banner;
import com.badmintonshop.entity.enums.BannerPosition;
import com.badmintonshop.entity.enums.LinkTarget;
import com.badmintonshop.repository.BannerRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service for Banner management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get displayable banners by position (for public/frontend)
     */
    public List<BannerDTO> getDisplayableBannersByPosition(BannerPosition position) {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findDisplayableBanners(position, now)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all displayable banners (for public/frontend)
     */
    public List<BannerDTO> getAllDisplayableBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findAllDisplayableBanners(now)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get banner by ID
     */
    public Optional<BannerDTO> getBannerById(Long id) {
        return bannerRepository.findById(id)
                .map(this::mapToDTO);
    }

    /**
     * Search banners for admin with filters
     */
    public Page<BannerDTO> searchBanners(String keyword, BannerPosition position, String status, Pageable pageable) {
        Specification<Banner> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Keyword search (title)
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + keyword.toLowerCase().trim() + "%"));
            }

            // Position filter
            if (position != null) {
                predicates.add(cb.equal(root.get("position"), position));
            }

            // Status filter
            if (status != null && !status.trim().isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (status.toUpperCase()) {
                    case "ACTIVE":
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.or(
                                cb.isNull(root.get("startsAt")),
                                cb.lessThanOrEqualTo(root.get("startsAt"), now)));
                        predicates.add(cb.or(
                                cb.isNull(root.get("endsAt")),
                                cb.greaterThan(root.get("endsAt"), now)));
                        break;
                    case "INACTIVE":
                        predicates.add(cb.isFalse(root.get("isActive")));
                        break;
                    case "SCHEDULED":
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.greaterThan(root.get("startsAt"), now));
                        break;
                    case "EXPIRED":
                        predicates.add(cb.lessThan(root.get("endsAt"), now));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bannerRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    /**
     * Create new banner
     */
    @Transactional
    @Auditable(entityType = "Banner", action = ActivityAction.CREATE, description = "Created banner: {0}")
    public BannerDTO createBanner(BannerDTO dto) {
        // Use default image if not provided
        String imageUrl = dto.getImageUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "/images/no-image.png";
        }

        Banner banner = Banner.builder()
                .title(dto.getTitle())
                .imageUrl(imageUrl)
                .mobileImageUrl(dto.getMobileImageUrl())
                .linkUrl(dto.getLinkUrl())
                .linkTarget(dto.getLinkTarget())
                .position(dto.getPosition())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .startsAt(dto.getStartsAt())
                .endsAt(dto.getEndsAt())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Banner saved = bannerRepository.save(banner);
        log.info("Created banner: {} (ID: {})", saved.getTitle(), saved.getBannerId());
        return mapToDTO(saved);
    }

    /**
     * Update existing banner
     */
    @Transactional
    @Auditable(entityType = "Banner", action = ActivityAction.UPDATE, description = "Updated banner ID: {0}")
    public BannerDTO updateBanner(Long id, BannerDTO dto) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner không tồn tại: " + id));

        // Use default image if not provided
        String imageUrl = dto.getImageUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "/images/no-image.png";
        }

        banner.setTitle(dto.getTitle());
        banner.setImageUrl(imageUrl);
        banner.setMobileImageUrl(dto.getMobileImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setLinkTarget(dto.getLinkTarget());
        banner.setPosition(dto.getPosition());
        banner.setDisplayOrder(dto.getDisplayOrder());
        banner.setStartsAt(dto.getStartsAt());
        banner.setEndsAt(dto.getEndsAt());
        banner.setIsActive(dto.getIsActive());

        Banner updated = bannerRepository.save(banner);
        log.info("Updated banner: {} (ID: {})", updated.getTitle(), updated.getBannerId());
        return mapToDTO(updated);
    }

    /**
     * Delete banner (soft delete)
     */
    @Transactional
    @Auditable(entityType = "Banner", action = ActivityAction.DELETE, description = "Soft deleted banner ID: {0}")
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner không tồn tại: " + id));

        banner.setDeletedAt(LocalDateTime.now());
        bannerRepository.save(banner);
        log.info("Deleted banner: {} (ID: {})", banner.getTitle(), id);
    }

    /**
     * Map entity to DTO
     */
    private BannerDTO mapToDTO(Banner banner) {
        return BannerDTO.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .mobileImageUrl(banner.getMobileImageUrl())
                .linkUrl(banner.getLinkUrl())
                .linkTarget(banner.getLinkTarget())
                .position(banner.getPosition())
                .displayOrder(banner.getDisplayOrder())
                .startsAt(banner.getStartsAt())
                .endsAt(banner.getEndsAt())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .createdByName(banner.getCreatedByStaff() != null ? banner.getCreatedByStaff().getFullName() : null)
                .build();
    }

    // ===== TRASH METHODS =====

    /**
     * Get deleted banners (for trash) - uses JdbcTemplate to bypass @Where filter
     */
    public Page<BannerDTO> getDeletedBanners(Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM banners WHERE deleted_at IS NOT NULL";
        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class);
        if (totalCount == null || totalCount == 0) {
            return Page.empty(pageable);
        }

        String sql = """
                SELECT banner_id, title, image_url, mobile_image_url, link_url, link_target,
                       position, display_order, starts_at, ends_at, is_active, deleted_at
                FROM banners
                WHERE deleted_at IS NOT NULL
                ORDER BY deleted_at DESC
                LIMIT ? OFFSET ?
                """;

        List<BannerDTO> banners = jdbcTemplate.query(
                sql,
                new Object[] { pageable.getPageSize(), pageable.getOffset() },
                (rs, rowNum) -> BannerDTO.builder()
                        .bannerId(rs.getLong("banner_id"))
                        .title(rs.getString("title"))
                        .imageUrl(rs.getString("image_url"))
                        .mobileImageUrl(rs.getString("mobile_image_url"))
                        .linkUrl(rs.getString("link_url"))
                        .linkTarget(
                                rs.getString("link_target") != null ? LinkTarget.valueOf(rs.getString("link_target"))
                                        : null)
                        .position(rs.getString("position") != null ? BannerPosition.valueOf(rs.getString("position"))
                                : null)
                        .displayOrder(rs.getInt("display_order"))
                        .startsAt(rs.getTimestamp("starts_at") != null ? rs.getTimestamp("starts_at").toLocalDateTime()
                                : null)
                        .endsAt(rs.getTimestamp("ends_at") != null ? rs.getTimestamp("ends_at").toLocalDateTime()
                                : null)
                        .isActive(rs.getBoolean("is_active"))
                        .build());

        return new org.springframework.data.domain.PageImpl<>(banners, pageable, totalCount);
    }

    /**
     * Restore banner from trash
     */
    @Transactional
    @Auditable(entityType = "Banner", action = ActivityAction.UPDATE, description = "Restored banner ID: {0}")
    public void restoreBanner(Long id) {
        Banner banner = bannerRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner không tồn tại: " + id));

        if (banner.getDeletedAt() == null) {
            throw new IllegalArgumentException("Banner chưa bị xóa");
        }

        banner.setDeletedAt(null);
        bannerRepository.save(banner);
        log.info("Restored banner: {} (ID: {})", banner.getTitle(), id);
    }

    /**
     * Hard delete banner (permanently)
     */
    @Transactional
    @Auditable(entityType = "Banner", action = ActivityAction.DELETE, description = "Hard deleted banner ID: {0}")
    public void hardDeleteBanner(Long id) {
        Banner banner = bannerRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner không tồn tại: " + id));

        if (banner.getDeletedAt() == null) {
            throw new IllegalArgumentException("Banner chưa bị xóa mềm, không thể xóa vĩnh viễn");
        }

        bannerRepository.delete(banner);
        log.info("Hard deleted banner: {} (ID: {})", banner.getTitle(), id);
    }
}
