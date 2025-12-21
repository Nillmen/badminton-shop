package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long orderId; // Chỉ cần Order ID thay vì toàn bộ Order Entity
    private String orderNumber; // Thêm Order Number để Admin dễ nhìn
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;

    // Bank Transfer Info
    private String bankName;
    private String bankAccountNumber;
    private String transferReference;

    // Timestamps
    private LocalDateTime PAIDAt;
    private LocalDateTime FAILEDAt;
    private LocalDateTime REFUNDEDAt;
}