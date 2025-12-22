package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.VariantStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ProductVariant - Biến thể sản phẩm - size, màu, trọng lượng vợt...
 */
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variants_product", columnList = "product_id"),
        @Index(name = "idx_variants_status", columnList = "status"),
        @Index(name = "idx_variants_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "barcode", length = 100)
    private String barcode;

    // Variant Attributes (JSON)
    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes; // JSON: {"weight":"4U", "grip_size":"G5"} hoặc {"size":"42", "color":"red"}

    @Column(name = "variant_name")
    private String variantName; // VD: "4U G5" hoặc "Size 42 - Đỏ"

    // Pricing
    @Column(name = "price_adjustment", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO; // Chênh lệch giá so với base price

    // Images
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private VariantStatus status = VariantStatus.ACTIVE;

    // Relationships
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Inventory> inventories = new ArrayList<>();

    // Helper methods
    public BigDecimal getFinalPrice() {
        if (product != null) {
            return product.getBasePrice().add(priceAdjustment);
        }
        return priceAdjustment;
    }

    public boolean isActive() {
        return status == VariantStatus.ACTIVE;
    }

    /**
     * Get total available stock quantity from all inventory records
     */
    public Integer getStockQuantity() {
        if (inventories == null || inventories.isEmpty()) {
            return 0;
        }
        return inventories.stream()
                .mapToInt(inv -> inv.getActualAvailable())
                .sum();
    }
}
