package com.badmintonshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImage; // URL ảnh
    private BigDecimal price; // Giá tại thời điểm thêm
    private Integer quantity;
    private BigDecimal subtotal; // Thành tiền (giá * số lượng)

    // Thông tin đan vợt (nếu có)
    private String stringingServiceName;
    private BigDecimal tension;
}