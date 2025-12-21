package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;

    // Customer Info (Lấy từ User Entity)
    private Long userId;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;

    // Shipping Info
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;

    // Financials
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Order Items (Sản phẩm trong đơn)
    private List<com.badmintonshop.dto.OrderItemResponse> items;

    // Tracking (Nếu có)
    private String trackingNumber;
    private String carrier;

    // History
    private List<com.badmintonshop.dto.OrderStatusHistoryResponse> statusHistory;
}