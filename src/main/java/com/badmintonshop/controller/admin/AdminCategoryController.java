package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.product.CategoryDTO;
import com.badmintonshop.dto.product.CategoryTreeDTO;
import com.badmintonshop.entity.enums.CategoryStatus;
import com.badmintonshop.entity.enums.CategoryType;
import com.badmintonshop.service.CategoryService;
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

import java.util.List;
import java.util.Map;

/**
 * Admin API Controller for Category CRUD operations
 * Requires products.* permissions (categories are part of product management)
 */
@RestController
@RequestMapping("/admin/api/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Get all categories with pagination
     * GET /admin/api/categories
     */
    @GetMapping
    public ResponseEntity<Page<CategoryDTO>> getAllCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CategoryStatus status,
            @RequestParam(required = false) CategoryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(categoryService.searchCategories(keyword, status, type, pageable));
    }

    /**
     * Get category tree
     * GET /admin/api/categories/tree
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDTO>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    /**
     * Get category by ID
     * GET /admin/api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new category
     * POST /admin/api/categories
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.create')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDTO dto) {
        try {
            CategoryDTO created = categoryService.createCategory(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update category
     * PUT /admin/api/categories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.create')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        try {
            CategoryDTO updated = categoryService.updateCategory(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete category (soft delete)
     * DELETE /admin/api/categories/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.delete')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "Xóa danh mục thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
