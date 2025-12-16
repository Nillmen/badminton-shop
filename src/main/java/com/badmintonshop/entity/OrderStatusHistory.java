package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.ChangedByType;
import com.badmintonshop.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.User;
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

    // 🛑 SỬA: Đổi từ String sang OrderStatus Enum và đổi tên field
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus oldStatus; // <-- Khớp với history.getOldStatus() trong Service

    // 🛑 SỬA: Đổi từ String sang OrderStatus Enum và đổi tên field
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus newStatus; // <-- Khớp với history.getNewStatus() trong Service

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false)
    private ChangedByType changedByType; // <-- Khớp với history.getChangedByType()

    // 🛑 THAY THẾ changedById BẰNG MỐI QUAN HỆ STAFF
    // @Column(name = "changed_by_id")
    // private Long changedById;

    // Mối quan hệ Staff (nếu changedByType là STAFF)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id") // Hoặc cột tương ứng trong DB (ví dụ: changed_by_staff_id)
    private Staff staff; // <-- Khớp với history.getStaff()

    // Mối quan hệ User (nếu changedByType là USER, ví dụ: khách hàng hủy)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Hoặc cột tương ứng trong DB
    private User user;


    @Column(name = "changed_at")
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    // 🛑 ĐÃ XÓA TẤT CẢ HÀM GETTER THỦ CÔNG (getOldStatus, getStaff,...)
    // VÌ ĐÃ CÓ @Getter CỦA LOMBOK.
}