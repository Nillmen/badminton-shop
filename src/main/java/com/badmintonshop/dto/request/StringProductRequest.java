package com.badmintonshop.dto.request;

import com.badmintonshop.entity.enums.StringType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StringProductRequest {

    @NotBlank(message = "Tên cước không được để trống")
    private String name;

    @NotBlank(message = "Mã SKU không được để trống")
    private String sku;

    private String description;

    private Long brandId;

    @NotNull(message = "Loại cước là bắt buộc")
    private StringType stringType;

    @DecimalMin(value = "0.0", message = "Độ dày phải lớn hơn 0")
    private BigDecimal gauge;

    private String material;

    @Min(0)
    private Integer durabilityRating;

    @Min(0)
    private Integer repulsionRating;

    @Min(0)
    private Integer controlRating;

    @Min(0)
    private Integer hittingSoundRating;

    @DecimalMin(value = "0.0")
    private BigDecimal recommendedTensionMin;

    @DecimalMin(value = "0.0")
    private BigDecimal recommendedTensionMax;

    @NotNull(message = "Giá bán là bắt buộc")
    @DecimalMin(value = "0.0")
    private BigDecimal retailPrice;

    private BigDecimal lengthPerRoll;

    @Min(0)
    private Integer quantityInStock;

    private String color;

    private String imageUrl;

    private Boolean isActive;
}
