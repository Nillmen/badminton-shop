package com.badmintonshop.repository;

import com.badmintonshop.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

        @Modifying
        @Query("UPDATE Inventory i SET i.quantityAvailable = i.quantityAvailable - :qty " +
                        "WHERE i.product.productId = :productId " +
                        "AND (:variantId IS NULL OR i.variant.variantId = :variantId) " +
                        "AND i.quantityAvailable >= :qty")
        int atomicDecrease(@Param("productId") Long productId,
                        @Param("variantId") Long variantId,
                        @Param("qty") int qty);

        @Query("SELECT i.quantityAvailable FROM Inventory i " +
                        "WHERE i.product.productId = :productId " +
                        "AND (:variantId IS NULL OR i.variant.variantId = :variantId)")
        Integer getAvailable(@Param("productId") Long productId,
                        @Param("variantId") Long variantId);

        @Modifying
        @Query("UPDATE Inventory i SET i.quantityAvailable = i.quantityAvailable + :qty " +
                        "WHERE i.product.productId = :productId " +
                        "AND (:variantId IS NULL OR i.variant.variantId = :variantId)")
        int atomicIncrease(@Param("productId") Long productId,
                        @Param("variantId") Long variantId,
                        @Param("qty") int qty);
}
