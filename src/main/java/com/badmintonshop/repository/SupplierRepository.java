package com.badmintonshop.repository;

import com.badmintonshop.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Supplier entity
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Find supplier by name
     */
    Optional<Supplier> findByName(String name);

    /**
     * Check if name exists
     */
    boolean existsByName(String name);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);

    /**
     * Find all active suppliers
     */
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true ORDER BY s.name ASC")
    List<Supplier> findAllActive();

    /**
     * Search suppliers by keyword (searches name and contactName)
     */
    @Query("SELECT s FROM Supplier s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Supplier> searchSuppliers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find suppliers by active status
     */
    List<Supplier> findByIsActive(Boolean isActive);
}
