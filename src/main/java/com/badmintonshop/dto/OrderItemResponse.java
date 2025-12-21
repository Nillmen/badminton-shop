package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.StringingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl; // Có thể cần thêm
    private Long variantId;
    private String variantName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Stringing Details (Chi tiết đan vợt)
    private boolean hasStringingService;
    private StringingStatus stringingStatus;
    private String stringingServiceName;
    private BigDecimal stringingServicePrice;
    private String stringName;
    private BigDecimal stringPrice;
    private BigDecimal tension;
    private String stringingNotes;
}