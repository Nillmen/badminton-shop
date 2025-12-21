package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListResponse {
    private Long orderId;
    private String orderNumber;
    private Long userId; // ID của khách hàng
    private String customerEmail; // Email của khách hàng
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    // Có thể thêm số lượng sản phẩm hoặc tổng số lượng item nếu cần
    // private Integer totalItems;
}