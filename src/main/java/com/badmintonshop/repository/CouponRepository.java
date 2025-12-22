package com.badmintonshop.repository;

import com.badmintonshop.entity.Coupon;
import com.badmintonshop.entity.enums.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

        // Find by code
        Optional<Coupon> findByCode(String code);

        // Find by code with case insensitive
        Optional<Coupon> findByCodeIgnoreCase(String code);

        // Find active and valid coupons
        @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
                        "AND c.startsAt <= :now AND c.expiresAt > :now " +
                        "AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit)")
        List<Coupon> findValidCoupons(@Param("now") LocalDateTime now);

        // Check if coupon code exists
        boolean existsByCode(String code);

        // Find active coupons
        List<Coupon> findByIsActiveTrue();

        // Find by isActive
        Page<Coupon> findByIsActive(Boolean isActive, Pageable pageable);

        // Find by type
        Page<Coupon> findByType(CouponType type, Pageable pageable);

        // Find by type and isActive
        Page<Coupon> findByTypeAndIsActive(CouponType type, Boolean isActive, Pageable pageable);

        // ===== TRASH METHODS =====

        /**
         * Find deleted coupons (for trash) - uses native query to bypass @Where filter
         */
        @Query(value = "SELECT * FROM coupons WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM coupons WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Coupon> findDeletedCoupons(Pageable pageable);

        /**
         * Find coupon by ID including deleted (ignore @Where clause)
         */
        @Query(value = "SELECT * FROM coupons WHERE coupon_id = :id", nativeQuery = true)
        Optional<Coupon> findByIdIncludingDeleted(@Param("id") Long id);
}
