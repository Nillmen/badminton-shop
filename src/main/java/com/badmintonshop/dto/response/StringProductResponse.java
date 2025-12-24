package com.badmintonshop.dto.response;

import com.badmintonshop.entity.enums.StringType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StringProductResponse {
    private Long stringId;
    private String name;
    private String sku;
    private String description;

    private Long brandId;
    private String brandName;

    private StringType stringType;
    private BigDecimal gauge;
    private String material;

    private Integer durabilityRating;
    private Integer repulsionRating;
    private Integer controlRating;
    private Integer hittingSoundRating;

    private BigDecimal recommendedTensionMin;
    private BigDecimal recommendedTensionMax;
    private String tensionRange;

    private BigDecimal retailPrice;
    private BigDecimal lengthPerRoll;
    private Integer quantityInStock;
    private String color;
    private String imageUrl;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
