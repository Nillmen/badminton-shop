package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.inventory.SupplierDTO;
import com.badmintonshop.service.SupplierService;
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
 * Admin API Controller for Supplier CRUD and Product-Supplier management
 * Requires inventory.* permissions (suppliers are part of inventory management)
 */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.view')")
public class AdminSupplierController {

    private final SupplierService supplierService;

    // ==================== SUPPLIER CRUD ====================

    /**
     * Get all suppliers with pagination
     * GET /admin/api/suppliers
     */
    @GetMapping("/suppliers")
    public ResponseEntity<Page<SupplierDTO>> getAllSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(supplierService.searchSuppliers(keyword, pageable));
    }

    /**
     * Get all active suppliers
     * GET /admin/api/suppliers/active
     */
    @GetMapping("/suppliers/active")
    public ResponseEntity<List<SupplierDTO>> getActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllActiveSuppliers());
    }

    /**
     * Get supplier by ID
     * GET /admin/api/suppliers/{id}
     */
    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new supplier
     * POST /admin/api/suppliers
     */
    @PostMapping("/suppliers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import')")
    public ResponseEntity<?> createSupplier(@Valid @RequestBody SupplierDTO dto) {
        try {
            SupplierDTO created = supplierService.createSupplier(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update supplier
     * PUT /admin/api/suppliers/{id}
     */
    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import')")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO dto) {
        try {
            SupplierDTO updated = supplierService.updateSupplier(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete supplier
     * DELETE /admin/api/suppliers/{id}
     */
    @DeleteMapping("/suppliers/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import')")
    public ResponseEntity<?> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            return ResponseEntity.ok(Map.of("message", "Xóa nhà cung cấp thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PRODUCT-SUPPLIER MANAGEMENT ====================

    /**
     * Get suppliers for a product
     * GET /admin/api/products/{productId}/suppliers
     */
    @GetMapping("/products/{productId}/suppliers")
    public ResponseEntity<List<java.util.Map<String, Object>>> getProductSuppliers(@PathVariable Long productId) {
        return ResponseEntity.ok(supplierService.getSuppliersForProduct(productId));
    }

    /**
     * Add supplier to product
     * POST /admin/api/products/{productId}/suppliers
     */
    @PostMapping("/products/{productId}/suppliers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.update')")
    public ResponseEntity<?> addSupplierToProduct(
            @PathVariable Long productId,
            @RequestParam Long supplierId,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        try {
            supplierService.addSupplierToProduct(productId, supplierId, isPrimary);
            return ResponseEntity.ok(Map.of("message", "Đã thêm nhà cung cấp vào sản phẩm"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove supplier from product
     * DELETE /admin/api/products/{productId}/suppliers/{supplierId}
     */
    @DeleteMapping("/products/{productId}/suppliers/{supplierId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('products.update')")
    public ResponseEntity<?> removeSupplierFromProduct(
            @PathVariable Long productId,
            @PathVariable Long supplierId) {
        try {
            supplierService.removeSupplierFromProduct(productId, supplierId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa nhà cung cấp khỏi sản phẩm"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
