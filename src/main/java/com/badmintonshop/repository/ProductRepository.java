package com.badmintonshop.repository;

import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.enums.ProductStatus;
import com.badmintonshop.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity with search, filter, and pagination
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

        /**
         * Find product by slug
         */
        Optional<Product> findBySlug(String slug);

        /**
         * Find product by slug with variants and their inventories eagerly loaded
         */
        @Query("SELECT DISTINCT p FROM Product p " +
                        "LEFT JOIN FETCH p.variants v " +
                        "LEFT JOIN FETCH v.inventories " +
                        "WHERE p.slug = :slug")
        Optional<Product> findBySlugWithVariantsAndInventory(@Param("slug") String slug);

        /**
         * Find product by SKU
         */
        Optional<Product> findBySku(String sku);

        /**
         * Check if slug exists
         */
        boolean existsBySlug(String slug);

        /**
         * Check if SKU exists
         */
        boolean existsBySku(String sku);

        /**
         * Find all active products
         */
        @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.createdAt DESC")
        List<Product> findAllActive();

        /**
         * Find products by category
         */
        @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId AND p.status = 'ACTIVE' AND p.hasVariants = true")
        Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

        /**
         * Find products by brand
         */
        @Query("SELECT p FROM Product p WHERE p.brand.brandId = :brandId AND p.status = 'ACTIVE' AND p.hasVariants = true")
        Page<Product> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

        /**
         * Find featured products
         */
        @Query("SELECT p FROM Product p WHERE p.isFeatured = true AND p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.displayOrder ASC, p.createdAt DESC")
        List<Product> findFeaturedProducts();

        /**
         * Find featured products with limit
         */
        @Query("SELECT p FROM Product p WHERE p.isFeatured = true AND p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.displayOrder ASC")
        Page<Product> findFeaturedProducts(Pageable pageable);

        /**
         * Find new arrivals (recent products marked as new arrival)
         */
        @Query("SELECT p FROM Product p WHERE p.isNewArrival = true AND p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.createdAt DESC")
        Page<Product> findNewArrivals(Pageable pageable);

        /**
         * Find best sellers (by sold count)
         */
        @Query("SELECT p FROM Product p WHERE p.isBestSeller = true AND p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.soldCount DESC")
        Page<Product> findBestSellers(Pageable pageable);

        /**
         * Search products by keyword (name, short description)
         */
        @Query("SELECT p FROM Product p WHERE " +
                        "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND p.status = 'ACTIVE' AND p.hasVariants = true")
        Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Advanced search with filters (public-facing, only shows products with
         * variants)
         */
        @Query("SELECT p FROM Product p WHERE " +
                        "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
                        "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
                        "AND (:productType IS NULL OR p.productType = :productType) " +
                        "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
                        "AND (:status IS NULL OR p.status = :status) " +
                        "AND p.hasVariants = true")
        Page<Product> searchProducts(
                        @Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("brandId") Long brandId,
                        @Param("productType") ProductType productType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("status") ProductStatus status,
                        Pageable pageable);

        /**
         * Advanced search with filters for admin (shows ALL products including those
         * without variants)
         */
        @Query("SELECT p FROM Product p WHERE " +
                        "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
                        "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
                        "AND (:productType IS NULL OR p.productType = :productType) " +
                        "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
                        "AND (:status IS NULL OR p.status = :status)")
        Page<Product> searchProductsAdmin(
                        @Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("brandId") Long brandId,
                        @Param("productType") ProductType productType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("status") ProductStatus status,
                        Pageable pageable);

        /**
         * Find products by status
         */
        List<Product> findByStatus(ProductStatus status);

        /**
         * Find products by type
         */
        List<Product> findByProductType(ProductType productType);

        /**
         * Count products by category
         */
        long countByCategoryCategoryId(Long categoryId);

        /**
         * Count products by brand
         */
        long countByBrandBrandId(Long brandId);

        /**
         * Count by status
         */
        long countByStatus(ProductStatus status);

        /**
         * Increment view count
         */
        @Modifying
        @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.productId = :productId")
        void incrementViewCount(@Param("productId") Long productId);

        /**
         * Increment sold count
         */
        @Modifying
        @Query("UPDATE Product p SET p.soldCount = p.soldCount + :quantity WHERE p.productId = :productId")
        void incrementSoldCount(@Param("productId") Long productId, @Param("quantity") int quantity);

        /**
         * Find products on sale (has compare at price higher than base price)
         */
        @Query("SELECT p FROM Product p WHERE p.compareAtPrice IS NOT NULL AND p.compareAtPrice > p.basePrice AND p.status = 'ACTIVE' AND p.hasVariants = true ORDER BY p.createdAt DESC")
        Page<Product> findProductsOnSale(Pageable pageable);

        /**
         * Find low stock products (products that have inventory with low stock)
         */
        @Query("SELECT DISTINCT p FROM Product p JOIN p.inventories i WHERE i.quantityAvailable <= :threshold AND p.status = 'ACTIVE'")
        List<Product> findLowStockProducts(@Param("threshold") int threshold);

        /**
         * Find products with images eagerly loaded
         */
        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.productId = :productId")
        Optional<Product> findByIdWithImages(@Param("productId") Long productId);

        /**
         * Find products with variants eagerly loaded
         */
        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.productId = :productId")
        Optional<Product> findByIdWithVariants(@Param("productId") Long productId);

        /**
         * Find soft-deleted products for trash (uses native query to bypass @Where
         * filter)
         */
        @Query(value = "SELECT * FROM products WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM products WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Product> findDeleted(Pageable pageable);

        /**
         * Find product by ID including deleted (bypasses @Where filter)
         */
        @Query(value = "SELECT * FROM products WHERE product_id = :id", nativeQuery = true)
        Optional<Product> findByIdIncludingDeleted(@Param("id") Long id);

        /**
         * Count ALL products by category (including soft-deleted) - bypasses @Where
         * filter
         * Used to check if category can be hard deleted
         */
        @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
        long countAllByCategoryId(@Param("categoryId") Long categoryId);

        /**
         * Count ALL products by brand (including soft-deleted) - bypasses @Where filter
         * Used to check if brand can be hard deleted
         */
        @Query(value = "SELECT COUNT(*) FROM products WHERE brand_id = :brandId", nativeQuery = true)
        long countAllByBrandId(@Param("brandId") Long brandId);
}
