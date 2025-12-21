package com.badmintonshop.repository;

import com.badmintonshop.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

       /**
        * Tìm tồn kho cho sản phẩm CÓ biến thể.
        * 
        * @param productId ID sản phẩm
        * @param variantId ID biến thể
        */
       Optional<Inventory> findByProduct_ProductIdAndVariant_VariantId(Long productId, Long variantId);

       /**
        * Tìm tồn kho cho sản phẩm KHÔNG CÓ biến thể (Variant ID là NULL).
        * 
        * @param productId ID sản phẩm
        */
       Optional<Inventory> findByProduct_ProductIdAndVariantIsNull(Long productId);

       @Modifying
       @Query("UPDATE Inventory i SET i.quantityAvailable = i.quantityAvailable - :qty, i.quantitySold = i.quantitySold + :qty, i.updatedAt = CURRENT_TIMESTAMP "
                     +
                     "WHERE i.product.productId = :productId " +
                     "AND (:variantId IS NULL OR i.variant.variantId = :variantId) " +
                     "AND i.quantityAvailable >= :qty")
       int atomicDecrease(@Param("productId") Long productId,
                     @Param("variantId") Long variantId,
                     @Param("qty") int qty);

       @Modifying
       @Query("UPDATE Inventory i SET i.quantityAvailable = i.quantityAvailable + :qty, i.updatedAt = CURRENT_TIMESTAMP "
                     +
                     "WHERE i.product.productId = :productId " +
                     "AND (:variantId IS NULL OR i.variant.variantId = :variantId)")
       int atomicIncrease(@Param("productId") Long productId,
                     @Param("variantId") Long variantId,
                     @Param("qty") int qty);
}