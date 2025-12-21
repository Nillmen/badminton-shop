package com.badmintonshop.dto; // Hoặc package dto của bạn

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus; // Fixed: Added paymentStatus

    // Thông tin giao hàng
    private String recipientName;
    private String phone;
    private String address;

    // Thông tin User (Quan trọng: Chỉ lấy ID và Tên, ko lấy cả Object User)
    private Long userId;
    private String userName;

    private LocalDateTime createdAt;

    // Fixed: Added items list
    private java.util.List<OrderItemResponse> items;
}