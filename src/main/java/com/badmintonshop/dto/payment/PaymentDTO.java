package com.badmintonshop.dto.payment;

import com.badmintonshop.entity.Payment;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    
    // Bank transfer info
    private String bankName;
    private String bankAccountNumber;
    private String transferReference;
    private String transferProofUrl;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private LocalDateTime refundedAt;

    // Customer info (for admin view)
    private String customerName;
    private String customerEmail;

    public static PaymentDTO fromEntity(Payment entity) {
        PaymentDTO dto = PaymentDTO.builder()
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrder().getOrderId())
                .orderNumber(entity.getOrder().getOrderNumber())
                .paymentMethod(entity.getPaymentMethod())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .transactionId(entity.getTransactionId())
                .bankName(entity.getBankName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .transferReference(entity.getTransferReference())
                .transferProofUrl(entity.getTransferProofUrl())
                .paidAt(entity.getPaidAt())
                .failedAt(entity.getFailedAt())
                .refundedAt(entity.getRefundedAt())
                .build();
        
        if (entity.getOrder() != null && entity.getOrder().getUser() != null) {
            dto.setCustomerName(entity.getOrder().getUser().getFullName());
            dto.setCustomerEmail(entity.getOrder().getUser().getEmail());
        }
        
        return dto;
    }
}
