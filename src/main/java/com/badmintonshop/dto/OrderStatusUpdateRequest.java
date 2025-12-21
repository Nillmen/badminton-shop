package com.badmintonshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.badmintonshop.entity.enums.OrderStatus; // Đảm bảo import đúng

@Data
public class OrderStatusUpdateRequest {

    // Trạng thái mới (Phải là một giá trị hợp lệ của OrderStatus Enum)
    @NotBlank(message = "Trạng thái mới không được để trống")
    private String newStatus;

    // Ghi chú của nhân viên (Tùy chọn)
    private String staffNote;
}