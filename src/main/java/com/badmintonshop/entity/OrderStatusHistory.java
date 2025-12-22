package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.ChangedByType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity OrderStatusHistory - Lịch sử thay đổi trạng thái đơn hàng
 */
@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_history_order", columnList = "order_id"),
        @Index(name = "idx_order_history_changed", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "from_status", columnDefinition = "ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','RETURNED','REFUNDED')")
    private String fromStatus;

    @Column(name = "to_status", nullable = false, columnDefinition = "ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','RETURNED','REFUNDED')")
    private String toStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false)
    private ChangedByType changedByType;

    @Column(name = "changed_by_id")
    private Long changedById; // user_id hoặc staff_id

    @Column(name = "changed_at")
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
