package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.ChangedByType;
import com.badmintonshop.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.badmintonshop.entity.User; // <-- THÊM DÒNG NÀY (hoặc tên package User của bro)

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {
    private Long historyId;
    private String oldStatus; // Đổi từ OrderStatus thành String
    private String newStatus; // Đổi từ OrderStatus thành String
    private LocalDateTime changedAt;
    private String notes;
    private ChangedByType changedBy;
    private String staffName; // Tên Staff thực hiện thay đổi (nếu có)
}