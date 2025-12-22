package com.badmintonshop.dto.response;

import com.badmintonshop.entity.enums.StringingServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StringingServiceResponse {
    private Long serviceId;
    private String serviceName;
    private StringingServiceType serviceType;
    private String description;
    private BigDecimal basePrice;
    private Integer estimatedTimeMinutes;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
