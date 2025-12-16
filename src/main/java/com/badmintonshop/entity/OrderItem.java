package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.StringingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity OrderItem - Chi tiết đơn hàng - có đầy đủ thông tin dịch vụ đan vợt
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_items_order", columnList = "order_id"),
    @Index(name = "idx_order_items_product", columnList = "product_id"),
    @Index(name = "idx_order_items_variant", columnList = "variant_id"),
    @Index(name = "idx_order_items_stringing_status", columnList = "stringing_status"),
    @Index(name = "idx_order_items_assigned_staff", columnList = "assigned_to_staff")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // Product snapshot
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "product_image", length = 500)
    private String productImage;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Stringing Service Info
    @Column(name = "has_stringing_service")
    @Builder.Default
    private Boolean hasStringingService = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stringing_service_id")
    private StringingService stringingService;

    @Column(name = "stringing_service_name")
    private String stringingServiceName;

    @Column(name = "stringing_service_price", precision = 10, scale = 2)
    private BigDecimal stringingServicePrice;

    // Thay thế dòng private BadmintonString badmintonString; bằng:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "string_id")
    private StringProduct stringProduct; // <-- Đã đổi tên Entity và Field

    @Column(name = "string_name")
    private String stringName;

    @Column(name = "string_price", precision = 10, scale = 2)
    private BigDecimal stringPrice;

    @Column(name = "tension", precision = 4, scale = 1)
    private BigDecimal tension; // Độ căng (lbs)

    @Column(name = "stringing_notes", columnDefinition = "TEXT")
    private String stringingNotes;

    // Stringing Status
    @Enumerated(EnumType.STRING)
    @Column(name = "stringing_status")
    private StringingStatus stringingStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_staff")
    private Staff assignedStaff; // Thợ đan được phân công

    @Column(name = "stringing_started_at")
    private LocalDateTime stringingStartedAt;

    @Column(name = "stringing_completed_at")
    private LocalDateTime stringingCompletedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Helper methods
    public BigDecimal getTotalPrice() {
        BigDecimal total = subtotal;
        if (stringingServicePrice != null) {
            total = total.add(stringingServicePrice.multiply(BigDecimal.valueOf(quantity)));
        }
        if (stringPrice != null) {
            total = total.add(stringPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        return total;
    }

    public void assignStringingStaff(Staff staff) {
        this.assignedStaff = staff;
        this.stringingStatus = StringingStatus.ASSIGNED;
    }

    public void startStringing() {
        this.stringingStatus = StringingStatus.IN_PROGRESS;
        this.stringingStartedAt = LocalDateTime.now();
    }

    public void completeStringing() {
        this.stringingStatus = StringingStatus.COMPLETED;
        this.stringingCompletedAt = LocalDateTime.now();
    }
}
