package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.banner.BannerDTO;
import com.badmintonshop.entity.enums.BannerPosition;
import com.badmintonshop.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Admin REST API Controller for Banner CRUD operations
 */
@RestController
@RequestMapping("/admin/api/banners")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('banners.view')")
public class AdminBannerApiController {

    private final BannerService bannerService;

    /**
     * Get all banners with pagination and filters
     * GET /admin/api/banners
     */
    @GetMapping
    public ResponseEntity<Page<BannerDTO>> getAllBanners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BannerPosition position,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BannerDTO> banners = bannerService.searchBanners(keyword, position, status, pageable);
        return ResponseEntity.ok(banners);
    }

    /**
     * Get banner by ID
     * GET /admin/api/banners/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BannerDTO> getBannerById(@PathVariable Long id) {
        return bannerService.getBannerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new banner
     * POST /admin/api/banners
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('banners.create')")
    public ResponseEntity<?> createBanner(@RequestBody BannerDTO dto) {
        try {
            BannerDTO created = bannerService.createBanner(dto);
            log.info("Created banner: {}", created.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update banner
     * PUT /admin/api/banners/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('banners.update')")
    public ResponseEntity<?> updateBanner(@PathVariable Long id, @RequestBody BannerDTO dto) {
        try {
            BannerDTO updated = bannerService.updateBanner(id, dto);
            log.info("Updated banner: {} (ID: {})", updated.getTitle(), id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete banner (soft delete)
     * DELETE /admin/api/banners/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('banners.delete')")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        try {
            bannerService.deleteBanner(id);
            log.info("Deleted banner: {}", id);
            return ResponseEntity.ok(Map.of("message", "Xóa banner thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
