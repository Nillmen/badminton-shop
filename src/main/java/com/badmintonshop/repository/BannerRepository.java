package com.badmintonshop.repository;

import com.badmintonshop.entity.Banner;
import com.badmintonshop.entity.enums.BannerPosition;
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
public interface BannerRepository extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {

        /**
         * Find displayable banners by position
         * A banner is displayable if: isActive=true, within date range (startsAt <= now
         * <= endsAt)
         */
        @Query("SELECT b FROM Banner b WHERE b.isActive = true " +
                        "AND b.position = :position " +
                        "AND (b.startsAt IS NULL OR b.startsAt <= :now) " +
                        "AND (b.endsAt IS NULL OR b.endsAt > :now) " +
                        "ORDER BY b.displayOrder ASC")
        List<Banner> findDisplayableBanners(@Param("position") BannerPosition position,
                        @Param("now") LocalDateTime now);

        /**
         * Find all displayable banners (any position)
         */
        @Query("SELECT b FROM Banner b WHERE b.isActive = true " +
                        "AND (b.startsAt IS NULL OR b.startsAt <= :now) " +
                        "AND (b.endsAt IS NULL OR b.endsAt > :now) " +
                        "ORDER BY b.displayOrder ASC")
        List<Banner> findAllDisplayableBanners(@Param("now") LocalDateTime now);

        // Find by position (for admin)
        Page<Banner> findByPosition(BannerPosition position, Pageable pageable);

        // Find by isActive (for admin)
        Page<Banner> findByIsActive(Boolean isActive, Pageable pageable);

        // Find by position and isActive (for admin)
        Page<Banner> findByPositionAndIsActive(BannerPosition position, Boolean isActive, Pageable pageable);

        // ===== TRASH METHODS =====

        /**
         * Find deleted banners (for trash) - uses native query to bypass @Where filter
         */
        @Query(value = "SELECT * FROM banners WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", countQuery = "SELECT COUNT(*) FROM banners WHERE deleted_at IS NOT NULL", nativeQuery = true)
        Page<Banner> findDeletedBanners(Pageable pageable);

        /**
         * Find banner by ID including deleted (ignore @Where clause)
         * Must use nativeQuery to bypass Hibernate @Where filter
         */
        @Query(value = "SELECT * FROM banners WHERE banner_id = :id", nativeQuery = true)
        java.util.Optional<Banner> findByIdIncludingDeleted(@Param("id") Long id);
}
