package com.badmintonshop.dto.request;

import com.badmintonshop.entity.enums.ExchangeReason;
import com.badmintonshop.entity.enums.ItemCondition;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExchangeRequest {
    @NotNull(message = "Order Item ID is required")
    private Long orderItemId;

    @NotNull(message = "Reason is required")
    private ExchangeReason reason;

    private String description;

    private String images; // JSON string of image URLs

    private ItemCondition oldItemCondition; // Optional, set by admin or inferred

    private Long newVariantId; // If switching to a different variant

    private String pickupAddress;
}
