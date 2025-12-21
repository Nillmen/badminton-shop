package com.badmintonshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private Long productId;
    private Long variantId; // Có thể null nếu sản phẩm không có biến thể
    private Integer quantity;

    // Thông tin đan vợt (nếu có)
    private Boolean hasStringingService;
    private Long stringingServiceId; // ID dịch vụ đan (công đan)
    private Long stringId; // ID loại cước (lưới)
    private BigDecimal tension; // Số ký (kg/lbs)
}