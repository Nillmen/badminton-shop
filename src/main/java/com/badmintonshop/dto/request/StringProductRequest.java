package com.badmintonshop.dto.request;

import com.badmintonshop.entity.enums.StringType;
import jakarta.validation.constraints.*;
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

    @DecimalMin(value = "0.01", message = "Độ dày phải lớn hơn 0")
    @DecimalMax(value = "9.99", message = "Độ dày không được vượt quá 9.99mm")
    private BigDecimal gauge;

    private String material;

    @Min(value = 1, message = "Độ bền phải từ 1-10")
    @Max(value = 10, message = "Độ bền phải từ 1-10")
    private Integer durabilityRating;

    @Min(value = 1, message = "Độ nảy phải từ 1-10")
    @Max(value = 10, message = "Độ nảy phải từ 1-10")
    private Integer repulsionRating;

    @Min(value = 1, message = "Độ kiểm soát phải từ 1-10")
    @Max(value = 10, message = "Độ kiểm soát phải từ 1-10")
    private Integer controlRating;

    @Min(value = 1, message = "Âm thanh va đập phải từ 1-10")
    @Max(value = 10, message = "Âm thanh va đập phải từ 1-10")
    private Integer hittingSoundRating;

    @DecimalMin(value = "10.0", message = "Lực căng tối thiểu phải ít nhất 10 lbs")
    @DecimalMax(value = "40.0", message = "Lực căng tối thiểu không được vượt quá 40 lbs")
    private BigDecimal recommendedTensionMin;

    @DecimalMin(value = "10.0", message = "Lực căng tối đa phải ít nhất 10 lbs")
    @DecimalMax(value = "40.0", message = "Lực căng tối đa không được vượt quá 40 lbs")
    private BigDecimal recommendedTensionMax;

    @NotNull(message = "Giá bán là bắt buộc")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    private BigDecimal retailPrice;

    @DecimalMin(value = "0.0", message = "Chiều dài cuộn không được âm")
    @DecimalMax(value = "9999.99", message = "Chiều dài cuộn không được vượt quá 9999.99m")
    private BigDecimal lengthPerRoll;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer quantityInStock;

    @Size(max = 50, message = "Màu sắc không được vượt quá 50 ký tự")
    private String color;

    @Size(max = 500, message = "URL hình ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    private Boolean isActive;
}
