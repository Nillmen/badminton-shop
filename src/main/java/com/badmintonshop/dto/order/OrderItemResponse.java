package com.badmintonshop.dto.order;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private String productName;
    private String variantInfo;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}
