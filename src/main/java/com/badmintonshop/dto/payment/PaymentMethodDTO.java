package com.badmintonshop.dto.payment;

import com.badmintonshop.entity.PaymentMethodConfig;
import com.badmintonshop.entity.enums.PaymentMethodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment method configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDTO {
    private Long methodId;
    private String name;
    private String code;
    private PaymentMethodType type;
    private String logoUrl;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;

    public static PaymentMethodDTO fromEntity(PaymentMethodConfig entity) {
        return PaymentMethodDTO.builder()
                .methodId(entity.getMethodId())
                .name(entity.getName())
                .code(entity.getCode())
                .type(entity.getType())
                .logoUrl(entity.getLogoUrl())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
