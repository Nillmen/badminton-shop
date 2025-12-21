package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.ChangedByType;
import com.badmintonshop.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity OrderStatusHistory - Lịch sử thay đổi trạng thái đơn hàng
 * Đã sửa để khớp với Database gốc của nhóm
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

    // 🛑 SỬA: Dùng String để khớp với VARCHAR(50) trong DB gốc của nhóm
    // Logic Enum sẽ được xử lý ở tầng Service (gọi .name() hoặc .toString())
    @Column(name = "from_status", length = 50)
    private String oldStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String newStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false)
    private ChangedByType changedByType;

    // 🛑 CHIÊU QUAN TRỌNG: Map 2 quan hệ vào chung 1 cột vật lý changed_by_id
    // insertable = false và updatable = false để tránh xung đột khi lưu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", insertable = false, updatable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", insertable = false, updatable = false)
    private User user;

    // Đây là cột thực tế trong DB gốc của nhóm
    @Column(name = "changed_by_id")
    private Long changedById;

    @Column(name = "changed_at")
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}