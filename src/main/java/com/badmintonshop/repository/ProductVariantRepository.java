package com.badmintonshop.repository;

import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProductVariant entity
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Find variants by product ID
     */
    List<ProductVariant> findByProductProductIdOrderByVariantIdAsc(Long productId);

    /**
     * Find active variants by product ID
     */
    @Query("SELECT v FROM ProductVariant v WHERE v.product.productId = :productId AND v.status = com.badmintonshop.entity.enums.VariantStatus.ACTIVE")
    List<ProductVariant> findActiveVariantsByProductId(@Param("productId") Long productId);

    /**
     * Find variant by SKU
     */
    Optional<ProductVariant> findBySku(String sku);

    /**
     * Check if SKU exists
     */
    boolean existsBySku(String sku);

    /**
     * Find variants by status
     */
    List<ProductVariant> findByStatus(VariantStatus status);

    /**
     * Count variants by product
     */
    long countByProductProductId(Long productId);

    /**
     * Check if variant belongs to product
     */
    boolean existsByVariantIdAndProductProductId(Long variantId, Long productId);

    /**
     * Delete variants by product ID
     */
    void deleteByProductProductId(Long productId);

    /**
     * Direct update variant status
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.status = :status WHERE v.variantId = :variantId")
    void updateStatus(@Param("variantId") Long variantId, @Param("status") VariantStatus status);

    /**
     * Find deleted variants (bypassing @Where filter) for trash page
     */
    @Query(value = "SELECT pv.*, p.name as product_name FROM product_variants pv " +
            "JOIN products p ON pv.product_id = p.product_id " +
            "WHERE pv.deleted_at IS NOT NULL ORDER BY pv.deleted_at DESC", nativeQuery = true)
    List<ProductVariant> findDeleted();

    /**
     * Find variant by ID including deleted (bypassing @Where filter)
     */
    @Query(value = "SELECT * FROM product_variants WHERE variant_id = :variantId", nativeQuery = true)
    Optional<ProductVariant> findByIdIncludingDeleted(@Param("variantId") Long variantId);

    /**
     * Update status of all variants for a product
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.status = :status WHERE v.product.productId = :productId")
    void updateStatusByProductId(@Param("productId") Long productId, @Param("status") VariantStatus status);
}
