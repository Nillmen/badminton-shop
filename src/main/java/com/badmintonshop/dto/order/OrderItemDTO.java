package com.badmintonshop.dto.order;

import com.badmintonshop.entity.enums.StringingStatus;
import lombok.*;

import java.math.BigDecimal;

/**
 * Order item DTO with product and stringing details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    private Long orderItemId;
    
    // Product info
    private Long productId;
    private String productName;
    private String productSku;
    private String productImage;
    
    // Variant info
    private Long variantId;
    private String variantName;

    // Pricing
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;

    // Stringing info
    private boolean hasStringingService;
    private Long stringingServiceId;
    private String stringingServiceName;
    private BigDecimal stringingServicePrice;
    private Long stringProductId;
    private String stringProductName;
    private BigDecimal stringPrice;
    private BigDecimal tension;
    private String stringingNotes;
    private StringingStatus stringingStatus;
    private String assignedStaffName;
}
