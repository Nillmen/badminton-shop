package com.badmintonshop.dto;

import com.badmintonshop.entity.Product;
import com.badmintonshop.entity.ProductVariant;
import com.badmintonshop.entity.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ProductDTO {
    private Long productId;
    private String name;
    private BigDecimal basePrice;
    private String imageUrl;
    private ProductStatus status;
    private CategoryDTO category;
    private List<VariantDTO> variants;

    @Data
    @Builder
    public static class CategoryDTO {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class VariantDTO {
        private Long variantId;
        private String variantName;
    }

    public static ProductDTO fromEntity(Product product) {
        String imageUrl = product.getPrimaryImage() != null ? product.getPrimaryImage().getImageUrl() : null;

        CategoryDTO categoryDTO = null;
        if (product.getCategory() != null) {
            categoryDTO = CategoryDTO.builder()
                    .id(product.getCategory().getCategoryId())
                    .name(product.getCategory().getName())
                    .build();
        }

        List<VariantDTO> variantDTOS = product.getVariants().stream()
                .map(v -> VariantDTO.builder()
                        .variantId(v.getVariantId())
                        .variantName(v.getVariantName())
                        .build())
                .collect(Collectors.toList());

        return ProductDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .imageUrl(imageUrl)
                .status(product.getStatus())
                .category(categoryDTO)
                .variants(variantDTOS)
                .build();
    }
}
