package com.badmintonshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderTrackingUpdateRequest {

    @NotBlank(message = "Mã vận đơn không được để trống")
    @Size(max = 50, message = "Mã vận đơn không được vượt quá 50 ký tự")
    private String trackingNumber;

    @NotBlank(message = "Đơn vị vận chuyển không được để trống")
    @Size(max = 50, message = "Đơn vị vận chuyển không được vượt quá 50 ký tự")
    private String carrier; // Ví dụ: GHN, GHTK, Viettel Post

    // Thêm trường staffNote nếu muốn ghi chú lý do cập nhật (tùy chọn)
    // private String staffNote;
}