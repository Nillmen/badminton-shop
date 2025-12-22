package com.badmintonshop.repository;

import com.badmintonshop.entity.StringProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StringProductRepository extends JpaRepository<StringProduct, Long> {

    @Query(value = """
        SELECT
            s.string_id,
            s.name,
            s.sku,
            s.description,
            CASE s.string_type
                WHEN 'synthetic' THEN 'SYNTHETIC'
                WHEN 'multifilament' THEN 'MULTIFILAMENT'
                WHEN 'nylon' THEN 'NYLON'
                ELSE s.string_type
            END AS string_type,
            s.gauge,
            s.material,
            s.durability_rating,
            s.repulsion_rating,
            s.control_rating,
            s.hitting_sound_rating,
            s.recommended_tension_min,
            s.recommended_tension_max,
            s.retail_price,
            s.length_per_roll,
            s.quantity_in_stock,
            s.color,
            s.image_url,
            s.is_active,
            b.brand_id,
            b.name AS brand_name
        FROM strings s
        JOIN brands b ON s.brand_id = b.brand_id
        WHERE s.is_active = 1
          AND s.deleted_at IS NULL
        """, nativeQuery = true)
    List<Object[]> findAllActiveWithBrand();

    @Query(value = """
        SELECT
            s.string_id,
            s.name,
            s.sku,
            s.description,
            CASE s.string_type
                WHEN 'synthetic' THEN 'SYNTHETIC'
                WHEN 'multifilament' THEN 'MULTIFILAMENT'
                WHEN 'nylon' THEN 'NYLON'
                ELSE s.string_type
            END AS string_type,
            s.gauge,
            s.material,
            s.durability_rating,
            s.repulsion_rating,
            s.control_rating,
            s.hitting_sound_rating,
            s.recommended_tension_min,
            s.recommended_tension_max,
            s.retail_price,
            s.length_per_roll,
            s.quantity_in_stock,
            s.color,
            s.image_url,
            s.is_active,
            b.brand_id,
            b.name AS brand_name
        FROM strings s
        JOIN brands b ON s.brand_id = b.brand_id
        WHERE s.string_id = :id
          AND s.deleted_at IS NULL
        """, nativeQuery = true)
    Object findByIdWithBrand(@Param("id") Long id);


    @Query(value = """
        SELECT
            s.string_id,
            s.name,
            s.sku,
            s.description,
            CASE s.string_type
                WHEN 'synthetic' THEN 'SYNTHETIC'
                WHEN 'multifilament' THEN 'MULTIFILAMENT'
                WHEN 'nylon' THEN 'NYLON'
                ELSE s.string_type
            END AS string_type,
            s.gauge,
            s.material,
            s.durability_rating,
            s.repulsion_rating,
            s.control_rating,
            s.hitting_sound_rating,
            s.recommended_tension_min,
            s.recommended_tension_max,
            s.retail_price,
            s.length_per_roll,
            s.quantity_in_stock,
            s.color,
            s.image_url,
            s.is_active,
            b.brand_id,
            b.name AS brand_name
        FROM strings s
        JOIN brands b ON s.brand_id = b.brand_id
        WHERE b.brand_id = :brandId
          AND s.is_active = 1
          AND s.deleted_at IS NULL
        """, nativeQuery = true)
    List<Object[]> findByBrandWithBrand(@Param("brandId") Long brandId);
    
    @Query("SELECT s FROM StringProduct s WHERE s.isActive = true")
    List<StringProduct> findAllActive();
}
