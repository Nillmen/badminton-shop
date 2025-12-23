package com.badmintonshop.dto.order;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Order status history DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryDTO {

    private Long historyId;
    private String fromStatus;
    private String toStatus;
    private String notes;
    private String changedByType;
    private Long changedById;
    private String changedByName;
    private LocalDateTime changedAt;
}
