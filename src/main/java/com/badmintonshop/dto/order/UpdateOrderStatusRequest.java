package com.badmintonshop.dto.order;

import com.badmintonshop.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for admin to update order status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private OrderStatus newStatus;

    private String notes;
}
