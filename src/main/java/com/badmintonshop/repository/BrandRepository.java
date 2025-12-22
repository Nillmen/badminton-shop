package com.badmintonshop.repository;

import com.badmintonshop.entity.Brand;
import com.badmintonshop.entity.enums.BrandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Brand entity
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

        /**
         * Find brand by slug
         */
        Optional<Brand> findBySlug(String slug);

        /**
         * Find brand by name
         */
        Optional<Brand> findByName(String name);

        /**
         * Check if slug exists
         */
        boolean existsBySlug(String slug);

        /**
         * Check if name exists
         */
        boolean existsByName(String name);

        /**
         * Find all active brands ordered by display order
         */
        @Query("SELECT b FROM Brand b WHERE b.isActive = true AND b.status = 'ACTIVE' ORDER BY b.displayOrder ASC, b.name ASC")
        List<Brand> findAllActive();

        /**
         * Find brands by status
         */
        List<Brand> findByStatus(BrandStatus status);

        /**
         * Find brands by isActive
         */
        List<Brand> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);

        /**
         * Search brands by name (case insensitive)
         */
        @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Brand> searchByName(@Param("keyword") String keyword);

        /**
         * Search brands with pagination
         */
        @Query("SELECT b FROM Brand b WHERE " +
                        "(:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:status IS NULL OR b.status = :status)")
        Page<Brand> searchBrands(@Param("keyword") String keyword,
                        @Param("status") BrandStatus status,
                        Pageable pageable);

        /**
         * Count active brands
         */
        long countByIsActiveTrue();

        /**
         * Count by status
         */
        long countByStatus(BrandStatus status);

        /**
         * Find brands with product count
         */
        @Query("SELECT b FROM Brand b LEFT JOIN FETCH b.products WHERE b.isActive = true ORDER BY b.displayOrder ASC")
        List<Brand> findAllActiveWithProducts();

        /**
         * Find soft-deleted brands for trash (uses native query to bypass @Where
         * filter)
         */
        @Query(value = "SELECT * FROM brands WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM brands WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Brand> findDeleted(Pageable pageable);

        /**
         * Find brand by ID including deleted (bypasses @Where filter)
         */
        @Query(value = "SELECT * FROM brands WHERE brand_id = :id", nativeQuery = true)
        Optional<Brand> findByIdIncludingDeleted(@Param("id") Long id);
}
