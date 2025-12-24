package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.StringType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Entity StringProduct - Loại cước - sản phẩm đặc thù của shop cầu lông
 */
@Entity
@Table(name = "strings", indexes = {
        @Index(name = "idx_strings_brand", columnList = "brand_id"),
        @Index(name = "idx_strings_type", columnList = "string_type"),
        @Index(name = "idx_strings_active", columnList = "is_active"),
        @Index(name = "idx_strings_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class StringProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "string_id")
    private Long stringId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "name", nullable = false)
    private String name; // VD: BG80, BG65, Nanogy 99

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // String Properties
    @Enumerated(EnumType.STRING)
    @Column(name = "string_type")
    @Builder.Default
    private StringType stringType = StringType.SYNTHETIC;

    @Column(name = "gauge", precision = 3, scale = 2)
    private BigDecimal gauge; // Độ dày (mm) - VD: 0.66mm

    @Column(name = "material")
    private String material; // VD: Multifilament Nylon

    // Characteristics
    @Column(name = "durability_rating")
    private Integer durabilityRating; // 1-10

    @Column(name = "repulsion_rating")
    private Integer repulsionRating; // 1-10

    @Column(name = "control_rating")
    private Integer controlRating; // 1-10

    @Column(name = "hitting_sound_rating")
    private Integer hittingSoundRating; // 1-10

    // Tension Recommendation
    @Column(name = "recommended_tension_min", precision = 4, scale = 1)
    private BigDecimal recommendedTensionMin; // lbs

    @Column(name = "recommended_tension_max", precision = 4, scale = 1)
    private BigDecimal recommendedTensionMax; // lbs

    // Pricing
    @Column(name = "retail_price", precision = 10, scale = 2)
    private BigDecimal retailPrice;

    // Stock
    @Column(name = "length_per_roll", precision = 6, scale = 2)
    private BigDecimal lengthPerRoll; // Meters - VD: 200m

    @Column(name = "quantity_in_stock")
    @Builder.Default
    private Integer quantityInStock = 0;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Helper methods
    public String getTensionRange() {
        if (recommendedTensionMin != null && recommendedTensionMax != null) {
            return recommendedTensionMin + " - " + recommendedTensionMax + " lbs";
        }
        return null;
    }
}
