package com.badmintonshop.repository;

import com.badmintonshop.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Inventory entity with atomic operations
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

        /**
         * Find inventory by product ID
         */
        List<Inventory> findByProductProductId(Long productId);

        /**
         * Find inventory by variant ID
         */
        Optional<Inventory> findByVariantVariantId(Long variantId);

        /**
         * Find inventory by product ID without variant (for products without variants)
         */
        Optional<Inventory> findByProductProductIdAndVariantIsNull(Long productId);

        /**
         * Find inventory by product ID with lock for atomic updates
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT i FROM Inventory i WHERE i.product.productId = :productId")
        List<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

        /**
         * Find low stock inventory
         */
        @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.lowStockThreshold")
        List<Inventory> findLowStock();

        /**
         * Find low stock with pagination
         */
        @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.lowStockThreshold")
        Page<Inventory> findLowStock(Pageable pageable);

        /**
         * Find out of stock inventory
         */
        @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= 0")
        List<Inventory> findOutOfStock();

        /**
         * Update quantity atomically (for race condition handling)
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE Inventory i SET i.quantityAvailable = i.quantityAvailable + :adjustment WHERE i.inventoryId = :inventoryId AND i.quantityAvailable + :adjustment >= 0")
        int adjustQuantity(@Param("inventoryId") Long inventoryId, @Param("adjustment") int adjustment);

        /**
         * Set quantity directly
         */
        @Modifying
        @Query("UPDATE Inventory i SET i.quantityAvailable = :quantity WHERE i.inventoryId = :inventoryId")
        void setQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") int quantity);

        /**
         * Count low stock items
         */
        @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityAvailable <= i.lowStockThreshold")
        long countLowStock();

        /**
         * Count out of stock items
         */
        @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityAvailable <= 0")
        long countOutOfStock();

        /**
         * Search inventory with filters
         */
        @Query("SELECT i FROM Inventory i WHERE " +
                        "(:productId IS NULL OR i.product.productId = :productId) " +
                        "AND (:lowStock IS NULL OR (:lowStock = true AND i.quantityAvailable <= i.lowStockThreshold))")
        Page<Inventory> searchInventory(@Param("productId") Long productId,
                        @Param("lowStock") Boolean lowStock,
                        Pageable pageable);

        /**
         * Search inventory with keyword and status filter
         * stockStatus: LOW_STOCK, OUT_OF_STOCK, IN_STOCK
         * Note: Excludes inventory for soft-deleted products and variants
         */
        @Query("SELECT i FROM Inventory i WHERE " +
                        "i.product.deletedAt IS NULL " +
                        "AND (i.variant IS NULL OR i.variant.deletedAt IS NULL) " +
                        "AND (:keyword IS NULL OR :keyword = '' " +
                        "   OR LOWER(i.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(i.product.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:stockStatus IS NULL OR :stockStatus = '' " +
                        "   OR (:stockStatus = 'LOW_STOCK' AND i.quantityAvailable <= i.lowStockThreshold AND i.quantityAvailable > 0) "
                        +
                        "   OR (:stockStatus = 'OUT_OF_STOCK' AND i.quantityAvailable <= 0) " +
                        "   OR (:stockStatus = 'IN_STOCK' AND i.quantityAvailable > i.lowStockThreshold))")
        Page<Inventory> searchInventoryWithKeyword(@Param("keyword") String keyword,
                        @Param("stockStatus") String stockStatus,
                        Pageable pageable);

        /**
         * Check if product has any inventory with stock > 0
         */
        @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Inventory i " +
                        "WHERE i.product.productId = :productId AND i.quantityAvailable > 0")
        boolean hasAvailableStock(@Param("productId") Long productId);

        /**
         * Count total available quantity for a product (across all variants)
         */
        @Query("SELECT COALESCE(SUM(i.quantityAvailable), 0) FROM Inventory i " +
                        "WHERE i.product.productId = :productId")
        int getTotalAvailableQuantity(@Param("productId") Long productId);

        /**
         * Find inventory by ID with variant eagerly loaded
         */
        @Query("SELECT i FROM Inventory i LEFT JOIN FETCH i.variant LEFT JOIN FETCH i.product WHERE i.inventoryId = :inventoryId")
        Optional<Inventory> findByIdWithVariant(@Param("inventoryId") Long inventoryId);

        /**
         * Get variant ID directly from inventory ID
         */
        @Query("SELECT i.variant.variantId FROM Inventory i WHERE i.inventoryId = :inventoryId")
        Optional<Long> findVariantIdByInventoryId(@Param("inventoryId") Long inventoryId);

        /**
         * Delete all inventory records by product ID (for hard delete cascade)
         */
        void deleteByProductProductId(Long productId);

        /**
         * Delete inventory by variant ID (for variant hard delete cascade)
         */
        void deleteByVariantVariantId(Long variantId);
}
