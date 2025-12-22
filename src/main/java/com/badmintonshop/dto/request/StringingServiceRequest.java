package com.badmintonshop.dto.request;

import com.badmintonshop.entity.enums.StringingServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StringingServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String serviceName;

    @NotNull(message = "Loại dịch vụ là bắt buộc")
    private StringingServiceType serviceType;

    private String description;

    @NotNull(message = "Giá dịch vụ là bắt buộc")
    @DecimalMin(value = "0.0")
    private BigDecimal basePrice;

    @Min(value = 1, message = "Thời gian ước tính phải lớn hơn 0")
    private Integer estimatedTimeMinutes;

    private Boolean isActive;
}
