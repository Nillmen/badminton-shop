package com.badmintonshop.dto.product;

import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.VariantStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for ProductVariant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantDTO {

    private Long variantId;
    private Long productId;
    private String sku;
    private String barcode;
    private String attributes;
    private String variantName;
    private BigDecimal priceAdjustment;
    private BigDecimal finalPrice;
    private BigDecimal promotionPrice; // Price after promotion applied
    private String imageUrl;
    private VariantStatus status;
    private Integer stockQuantity;
    private Boolean isActive; // Derived from status for frontend convenience

    // Frontend sends these separately for convenience
    private String color;
    private String size;

    /**
     * Setter for isActive that also sets the status
     * This allows frontend to send isActive boolean and have it converted to status
     * enum
     */
    @JsonProperty("isActive")
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        if (isActive != null) {
            this.status = isActive ? VariantStatus.ACTIVE : VariantStatus.INACTIVE;
        }
    }

    /**
     * Convert from Entity to DTO
     */
    public static ProductVariantDTO fromEntity(ProductVariant variant) {
        if (variant == null)
            return null;

        return ProductVariantDTO.builder()
                .variantId(variant.getVariantId())
                .productId(variant.getProduct() != null ? variant.getProduct().getProductId() : null)
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .attributes(variant.getAttributes())
                .variantName(variant.getVariantName())
                .priceAdjustment(variant.getPriceAdjustment())
                .finalPrice(variant.getFinalPrice())
                .imageUrl(variant.getImageUrl())
                .status(variant.getStatus())
                .stockQuantity(variant.getStockQuantity())
                .isActive(variant.getStatus() == VariantStatus.ACTIVE)
                .build();
    }
}
