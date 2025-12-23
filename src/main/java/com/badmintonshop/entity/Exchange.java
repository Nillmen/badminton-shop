package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.ExchangeReason;
import com.badmintonshop.entity.enums.ExchangeStatus;
import com.badmintonshop.entity.enums.ItemCondition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Entity Exchange - Đổi hàng - có old_item_condition để xử lý tồn kho
 */
@Entity
@Table(name = "exchanges", indexes = {
    @Index(name = "idx_exchanges_order", columnList = "order_id"),
    @Index(name = "idx_exchanges_order_item", columnList = "order_item_id"),
    @Index(name = "idx_exchanges_status", columnList = "status"),
    @Index(name = "idx_exchanges_condition", columnList = "old_item_condition"),
    @Index(name = "idx_exchanges_created", columnList = "created_at"),
    @Index(name = "idx_exchanges_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Exchange extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_id")
    private Long exchangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "exchange_number", nullable = false, unique = true, length = 50)
    private String exchangeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ExchangeStatus status = ExchangeStatus.REQUESTED;

    // Old Item Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_variant_id")
    private ProductVariant oldVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_item_condition")
    private ItemCondition oldItemCondition; // Quyết định xử lý tồn kho

    // New Item Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_variant_id")
    private ProductVariant newVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ExchangeReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Images
    @Column(name = "images", columnDefinition = "JSON")
    @Builder.Default
    private String images = "[]"; // Array of image URLs - default empty JSON array

    // Pickup
    @Column(name = "pickup_scheduled_at")
    private LocalDateTime pickupScheduledAt;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    // Timestamps
    @Column(name = "old_item_received_at")
    private LocalDateTime oldItemReceivedAt; // Thời điểm nhận hàng cũ

    @Column(name = "new_item_shipped_at")
    private LocalDateTime newItemShippedAt; // Thời điểm gửi hàng mới

    // Notes
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Helper methods
    public void approve() {
        this.status = ExchangeStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = ExchangeStatus.REJECTED;
        this.rejectedReason = reason;
    }

    public void complete() {
        this.status = ExchangeStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
