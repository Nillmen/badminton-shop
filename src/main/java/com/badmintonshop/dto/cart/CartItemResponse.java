package com.badmintonshop.dto.cart;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImage; // If available
    private String variantName; // If variant exists
    private Integer quantity;
    private BigDecimal priceAtAdd;
    private BigDecimal subtotal;
}
