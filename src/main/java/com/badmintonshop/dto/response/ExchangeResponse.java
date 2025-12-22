package com.badmintonshop.dto.response;

import com.badmintonshop.entity.enums.ExchangeReason;
import com.badmintonshop.entity.enums.ExchangeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExchangeResponse {
    private Long exchangeId;
    private String exchangeNumber;
    private Long orderId;
    private String orderNumber;

    private String oldProductName;
    private String oldVariantName;

    private String newProductName;
    private String newVariantName;

    private ExchangeStatus status;
    private ExchangeReason reason;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime pickupScheduledAt;
}
