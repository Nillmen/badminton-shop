package com.badmintonshop.service;

import com.badmintonshop.dto.product.BrandDTO;
import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.enums.BrandStatus;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service for Brand operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get all active brands
     */
    public List<BrandDTO> getAllActiveBrands() {
        return brandRepository.findAllActive().stream()
                .map(BrandDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all brands with pagination
     */
    public Page<BrandDTO> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(BrandDTO::fromEntity);
    }

    /**
     * Get brand by ID
     */
    public Optional<BrandDTO> getBrandById(Long id) {
        return brandRepository.findById(id)
                .map(BrandDTO::fromEntity);
    }

    /**
     * Get brand by slug
     */
    public Optional<BrandDTO> getBrandBySlug(String slug) {
        return brandRepository.findBySlug(slug)
                .map(BrandDTO::fromEntity);
    }

    /**
     * Search brands
     */
    public Page<BrandDTO> searchBrands(String keyword, BrandStatus status, Pageable pageable) {
        return brandRepository.searchBrands(keyword, status, pageable)
                .map(BrandDTO::fromEntity);
    }

    /**
     * Create new brand
     */
    @Transactional
    public BrandDTO createBrand(BrandDTO dto) {
        // Generate slug if not provided
        String slug = dto.getSlug();
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug(dto.getName());
        }

        // Check if slug exists
        if (brandRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + slug);
        }

        // Check if name exists
        if (brandRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại: " + dto.getName());
        }

        Brand brand = Brand.builder()
                .name(dto.getName())
                .slug(slug)
                .logoUrl(dto.getLogoUrl())
                .description(dto.getDescription())
                .country(dto.getCountry())
                .websiteUrl(dto.getWebsiteUrl())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .status(dto.getStatus() != null ? dto.getStatus() : BrandStatus.ACTIVE)
                .build();

        brand = brandRepository.save(brand);
        log.info("Created brand: {}", brand.getName());
        return BrandDTO.fromEntity(brand);
    }

    /**
     * Update brand
     */
    @Transactional
    public BrandDTO updateBrand(Long id, BrandDTO dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + id));

        // Update fields
        if (dto.getName() != null) {
            // Check if new name exists (different from current)
            if (!brand.getName().equals(dto.getName()) && brandRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Tên thương hiệu đã tồn tại: " + dto.getName());
            }
            brand.setName(dto.getName());
        }

        if (dto.getSlug() != null && !dto.getSlug().isEmpty()) {
            // Check if new slug exists (different from current)
            if (!brand.getSlug().equals(dto.getSlug()) && brandRepository.existsBySlug(dto.getSlug())) {
                throw new IllegalArgumentException("Slug đã tồn tại: " + dto.getSlug());
            }
            brand.setSlug(dto.getSlug());
        }

        if (dto.getLogoUrl() != null)
            brand.setLogoUrl(dto.getLogoUrl());
        if (dto.getDescription() != null)
            brand.setDescription(dto.getDescription());
        if (dto.getCountry() != null)
            brand.setCountry(dto.getCountry());
        if (dto.getWebsiteUrl() != null)
            brand.setWebsiteUrl(dto.getWebsiteUrl());
        if (dto.getDisplayOrder() != null)
            brand.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsActive() != null)
            brand.setIsActive(dto.getIsActive());
        if (dto.getStatus() != null)
            brand.setStatus(dto.getStatus());

        brand = brandRepository.save(brand);
        log.info("Updated brand: {}", brand.getName());
        return BrandDTO.fromEntity(brand);
    }

    /**
     * Delete brand (soft delete)
     */
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + id));

        // Check if brand has associated products
        long productCount = productRepository.countByBrandBrandId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                    String.format("Không thể xóa thương hiệu '%s' vì có %d sản phẩm đang liên kết. " +
                            "Vui lòng chuyển hoặc xóa các sản phẩm trước.",
                            brand.getName(), productCount));
        }

        brand.setDeletedAt(LocalDateTime.now());
        brand.setIsActive(false);
        brand.setStatus(BrandStatus.INACTIVE);
        brandRepository.save(brand);
        log.info("Soft deleted brand: {}", brand.getName());
    }

    /**
     * Hard delete brand
     */
    @Transactional
    public void hardDeleteBrand(Long id) {
        Brand brand = brandRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + id));

        // Check if ANY products (including deleted) reference this brand
        long productCount = productRepository.countAllByBrandId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Không thể xóa vĩnh viễn thương hiệu '%s' vì có %d sản phẩm đang liên kết (bao gồm cả sản phẩm trong thùng rác). "
                                    +
                                    "Vui lòng xóa vĩnh viễn các sản phẩm trước.",
                            brand.getName(), productCount));
        }

        brandRepository.delete(brand);
        log.info("Hard deleted brand: {}", brand.getName());
    }

    /**
     * Generate slug from name
     */
    private String generateSlug(String name) {
        if (name == null)
            return "";
        return name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Get deleted brands for trash - uses JdbcTemplate to bypass @Where filter
     */
    public Page<BrandDTO> getDeletedBrands(Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM brands WHERE deleted_at IS NOT NULL";
        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class);
        if (totalCount == null || totalCount == 0) {
            return Page.empty(pageable);
        }

        String sql = """
                SELECT brand_id, name, slug, logo_url, description, country,
                       website_url, display_order, is_active, status, deleted_at
                FROM brands
                WHERE deleted_at IS NOT NULL
                ORDER BY deleted_at DESC
                LIMIT ? OFFSET ?
                """;

        List<BrandDTO> brands = jdbcTemplate.query(
                sql,
                new Object[] { pageable.getPageSize(), pageable.getOffset() },
                (rs, rowNum) -> BrandDTO.builder()
                        .brandId(rs.getLong("brand_id"))
                        .name(rs.getString("name"))
                        .slug(rs.getString("slug"))
                        .logoUrl(rs.getString("logo_url"))
                        .description(rs.getString("description"))
                        .country(rs.getString("country"))
                        .websiteUrl(rs.getString("website_url"))
                        .displayOrder(rs.getInt("display_order"))
                        .isActive(rs.getBoolean("is_active"))
                        .status(rs.getString("status") != null ? BrandStatus.valueOf(rs.getString("status")) : null)
                        .build());

        return new org.springframework.data.domain.PageImpl<>(brands, pageable, totalCount);
    }

    /**
     * Restore brand from trash
     */
    @Transactional
    public void restoreBrand(Long id) {
        Brand brand = brandRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu: " + id));

        brand.setDeletedAt(null);
        brand.setIsActive(true);
        brand.setStatus(BrandStatus.ACTIVE);
        brandRepository.save(brand);
        log.info("Restored brand: {}", brand.getName());
    }
}
