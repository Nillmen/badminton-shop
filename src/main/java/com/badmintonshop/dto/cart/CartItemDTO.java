package com.badmintonshop.dto.cart;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for cart item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {
    
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private String categorySlug; // To identify racket products for stringing
    private Long variantId;
    private String variantName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Promotion info
    private BigDecimal promotionPrice;
    private BigDecimal promotionDiscount;
    private String promotionName;
    
    // Stringing service info
    private Long stringingServiceId;
    private String stringingServiceName;
    private BigDecimal stringingPrice;
    private Long stringProductId;
    private String stringProductName;
    private BigDecimal stringPrice;
    private BigDecimal tension;
    private String stringingNotes;
    
    // Stock info
    private Boolean inStock;
    private Integer availableQuantity;
}
