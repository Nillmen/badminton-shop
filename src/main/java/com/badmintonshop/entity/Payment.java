package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Payment - Thanh toán
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_order", columnList = "order_id"),
    @Index(name = "idx_payments_method", columnList = "payment_method"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_transaction", columnList = "transaction_id"),
    @Index(name = "idx_payments_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // Gateway Info
    @Column(name = "transaction_id")
    private String transactionId; // Transaction ID từ payment gateway

    @Column(name = "gateway_response", columnDefinition = "JSON")
    private String gatewayResponse; // Full response từ gateway

    // Bank Transfer Info
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "transfer_reference", length = 100)
    private String transferReference;

    @Column(name = "transfer_proof_url", length = 500)
    private String transferProofUrl;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper methods
    public void markAsPaid(String transactionId) {
        this.status = PaymentStatus.PAID;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }
}
