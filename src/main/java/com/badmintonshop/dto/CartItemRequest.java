package com.badmintonshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemRequest {
    private Long productId;
    private Long variantId; // Có thể null
    private Integer quantity;

    // Phần đan vợt
    private Long stringingServiceId; // ID dịch vụ đan
    private Long stringId; // ID loại cước
    private BigDecimal tension; // Số cân (lbs)
    private String stringingNotes;

}