package com.badmintonshop.dto.cart;

import lombok.Data;

@Data
public class CartItemRequest {
    private Long productId;
    private Long variantId;
    private Integer quantity;
}
