package com.badmintonshop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private int totalItems;
    private BigDecimal totalPrice; // Tổng tiền cả giỏ
    private List<CartItemResponse> items;
}