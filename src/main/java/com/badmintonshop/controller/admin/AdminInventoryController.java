package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.inventory.InventoryAdjustRequest;
import com.badmintonshop.dto.inventory.InventoryDTO;
import com.badmintonshop.dto.inventory.InventoryTransactionDTO;
import com.badmintonshop.entity.enums.TransactionType;
import com.badmintonshop.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin API Controller for Inventory management
 * Requires inventory.* permissions
 */
@RestController
@RequestMapping("/admin/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'WAREHOUSE_STAFF')")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    /**
     * Get all inventory with filters
     * GET /admin/api/inventory
     */
    @GetMapping
    public ResponseEntity<Page<InventoryDTO>> getAllInventory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(inventoryService.searchInventory(productId, lowStock, pageable));
    }

    /**
     * Get inventory by product ID
     * GET /admin/api/inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    /**
     * Get low stock inventory
     * GET /admin/api/inventory/low-stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<Page<InventoryDTO>> getLowStockInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(inventoryService.getLowStockInventory(pageable));
    }

    /**
     * Get inventory statistics
     * GET /admin/api/inventory/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<InventoryService.InventoryStats> getInventoryStats() {
        return ResponseEntity.ok(inventoryService.getInventoryStats());
    }

    /**
     * Update stock quantity (set absolute value)
     * PUT /admin/api/inventory/{inventoryId}
     */
    @PutMapping("/{inventoryId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import') or hasAuthority('inventory.export')")
    public ResponseEntity<?> updateStock(
            @PathVariable Long inventoryId,
            @RequestParam int quantity,
            @RequestParam(required = false) String reason) {
        try {
            InventoryDTO updated = inventoryService.updateStock(inventoryId, quantity, reason);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Adjust stock (add/subtract)
     * POST /admin/api/inventory/adjust
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import') or hasAuthority('inventory.export')")
    public ResponseEntity<?> adjustStock(@Valid @RequestBody InventoryAdjustRequest request) {
        try {
            InventoryDTO updated = inventoryService.adjustStock(request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Restock inventory
     * POST /admin/api/inventory/{inventoryId}/restock
     */
    @PostMapping("/{inventoryId}/restock")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('inventory.import')")
    public ResponseEntity<?> restockInventory(
            @PathVariable Long inventoryId,
            @RequestParam int quantity,
            @RequestParam(required = false) Long purchaseOrderId) {
        try {
            InventoryDTO updated = inventoryService.restock(inventoryId, quantity, purchaseOrderId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get inventory transaction history
     * GET /admin/api/inventory/history
     */
    @GetMapping("/history")
    public ResponseEntity<Page<InventoryTransactionDTO>> getTransactionHistory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity
                .ok(inventoryService.getTransactionHistory(productId, transactionType, null, null, pageable));
    }
}
