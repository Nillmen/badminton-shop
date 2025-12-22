package com.badmintonshop.repository;

import com.badmintonshop.entity.Promotion;
import com.badmintonshop.entity.enums.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

        // Find active promotions (currently running)
        @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
                        "AND p.startsAt <= :now AND p.endsAt > :now")
        List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

        // Find active promotions by type
        @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
                        "AND p.startsAt <= :now AND p.endsAt > :now " +
                        "AND p.type = :type")
        List<Promotion> findActivePromotionsByType(@Param("now") LocalDateTime now, @Param("type") PromotionType type);

        // Find all active promotions (enabled, regardless of dates)
        List<Promotion> findByIsActiveTrue();

        // Find by isActive
        Page<Promotion> findByIsActive(Boolean isActive, Pageable pageable);

        // Find by type
        Page<Promotion> findByType(PromotionType type, Pageable pageable);

        // Find by type and isActive
        Page<Promotion> findByTypeAndIsActive(PromotionType type, Boolean isActive, Pageable pageable);

        // ===== TRASH METHODS =====

        /**
         * Find deleted promotions (for trash) - uses native query to bypass @Where
         * filter
         */
        @Query(value = "SELECT * FROM promotions WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM promotions WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Promotion> findDeletedPromotions(Pageable pageable);

        /**
         * Find promotion by ID including deleted (ignore @Where clause)
         */
        @Query(value = "SELECT * FROM promotions WHERE promotion_id = :id", nativeQuery = true)
        java.util.Optional<Promotion> findByIdIncludingDeleted(@Param("id") Long id);
}
