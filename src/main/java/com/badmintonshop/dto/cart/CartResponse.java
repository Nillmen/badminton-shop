package com.badmintonshop.dto.cart;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for cart details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    
    private Long cartId;
    private Long userId;
    private String sessionId;
    private List<CartItemDTO> items;
    private Integer totalItems;
    private Integer totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal promotionDiscount;
    private String couponCode;
    private BigDecimal freeShippingThreshold;
    private BigDecimal total;
    private Boolean isEmpty;
}
