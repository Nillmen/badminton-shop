package com.badmintonshop.dto.cart;

import lombok.*;
import java.math.BigDecimal;

/**
 * Request DTO for updating stringing options on a cart item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStringingRequest {
    
    private Long stringingServiceId;
    private Long stringProductId;
    private BigDecimal tension;
    private String stringingNotes;
}
