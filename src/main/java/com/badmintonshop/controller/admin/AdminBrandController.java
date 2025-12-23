package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.product.BrandDTO;
import com.badmintonshop.entity.enums.BrandStatus;
import com.badmintonshop.service.BrandService;
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
 * Admin API Controller for Brand CRUD operations
 * Requires products.* permissions (brands are part of product management)
 */
@RestController
@RequestMapping("/admin/api/brands")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminBrandController {

    private final BrandService brandService;

    /**
     * Get all brands with pagination
     * GET /admin/api/brands
     */
    @GetMapping
    public ResponseEntity<Page<BrandDTO>> getAllBrands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BrandStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(brandService.searchBrands(keyword, status, pageable));
    }

    /**
     * Get brand by ID
     * GET /admin/api/brands/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BrandDTO> getBrandById(@PathVariable Long id) {
        return brandService.getBrandById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new brand
     * POST /admin/api/brands
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.create')")
    public ResponseEntity<?> createBrand(@Valid @RequestBody BrandDTO dto) {
        try {
            BrandDTO created = brandService.createBrand(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update brand
     * PUT /admin/api/brands/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.update')")
    public ResponseEntity<?> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandDTO dto) {
        try {
            BrandDTO updated = brandService.updateBrand(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete brand (soft delete)
     * DELETE /admin/api/brands/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.delete')")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(Map.of("message", "Xóa thương hiệu thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
