package com.badmintonshop.repository;

import com.badmintonshop.entity.Category;
import com.badmintonshop.entity.enums.CategoryStatus;
import com.badmintonshop.entity.enums.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity with tree structure support
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

        /**
         * Find category by slug
         */
        Optional<Category> findBySlug(String slug);

        /**
         * Check if slug exists
         */
        boolean existsBySlug(String slug);

        /**
         * Find all root categories (no parent)
         */
        @Query("SELECT c FROM Category c WHERE c.parentCategory IS NULL AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
        List<Category> findRootCategories();

        /**
         * Find all root categories with children eagerly loaded
         */
        @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.childCategories WHERE c.parentCategory IS NULL AND c.isActive = true ORDER BY c.displayOrder ASC")
        List<Category> findRootCategoriesWithChildren();

        /**
         * Find children of a category
         */
        @Query("SELECT c FROM Category c WHERE c.parentCategory.categoryId = :parentId AND c.isActive = true ORDER BY c.displayOrder ASC")
        List<Category> findChildrenByParentId(@Param("parentId") Long parentId);

        /**
         * Find categories by type
         */
        List<Category> findByCategoryTypeAndIsActiveTrueOrderByDisplayOrderAsc(CategoryType categoryType);

        /**
         * Find categories by status
         */
        List<Category> findByStatus(CategoryStatus status);

        /**
         * Find all active categories
         */
        @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.status = 'ACTIVE' ORDER BY c.displayOrder ASC")
        List<Category> findAllActive();

        /**
         * Search categories by name
         */
        @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Category> searchByName(@Param("keyword") String keyword);

        /**
         * Search categories with pagination
         */
        @Query("SELECT c FROM Category c WHERE " +
                        "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:status IS NULL OR c.status = :status) " +
                        "AND (:categoryType IS NULL OR c.categoryType = :categoryType)")
        Page<Category> searchCategories(@Param("keyword") String keyword,
                        @Param("status") CategoryStatus status,
                        @Param("categoryType") CategoryType categoryType,
                        Pageable pageable);

        /**
         * Count active categories
         */
        long countByIsActiveTrue();

        /**
         * Count by category type
         */
        long countByCategoryType(CategoryType categoryType);

        /**
         * Find categories with product count
         */
        @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN c.products p WHERE c.isActive = true GROUP BY c ORDER BY c.displayOrder ASC")
        List<Object[]> findCategoriesWithProductCount();

        /**
         * Find category tree (all levels)
         */
        @Query("SELECT c FROM Category c LEFT JOIN FETCH c.childCategories cc WHERE c.isActive = true ORDER BY c.displayOrder ASC, cc.displayOrder ASC")
        List<Category> findCategoryTree();

        /**
         * Find soft-deleted categories for trash (uses native query to bypass @Where
         * filter)
         */
        @Query(value = "SELECT * FROM categories WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM categories WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Category> findDeleted(Pageable pageable);

        /**
         * Find category by ID including deleted (bypasses @Where filter)
         */
        @Query(value = "SELECT * FROM categories WHERE category_id = :id", nativeQuery = true)
        Optional<Category> findByIdIncludingDeleted(@Param("id") Long id);
}
