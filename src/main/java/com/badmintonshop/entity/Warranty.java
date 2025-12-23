package com.badmintonshop.entity;

import com.badmintonshop.entity.enums.IssueType;
import com.badmintonshop.entity.enums.Resolution;
import com.badmintonshop.entity.enums.WarrantyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity Warranty - Bảo hành - chủ yếu cho vợt
 */
@Entity
@Table(name = "warranties", indexes = {
    @Index(name = "idx_warranties_order", columnList = "order_id"),
    @Index(name = "idx_warranties_order_item", columnList = "order_item_id"),
    @Index(name = "idx_warranties_status", columnList = "status"),
    @Index(name = "idx_warranties_issue", columnList = "issue_type"),
    @Index(name = "idx_warranties_created", columnList = "created_at"),
    @Index(name = "idx_warranties_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Warranty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warranty_id")
    private Long warrantyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "warranty_number", nullable = false, unique = true, length = 50)
    private String warrantyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private WarrantyStatus status = WarrantyStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    // Images
    @Column(name = "images", columnDefinition = "JSON")
    @Builder.Default
    private String images = "[]"; // Array of image URLs showing defect - default empty JSON array

    // Warranty Period Check
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(name = "is_within_warranty")
    private Boolean isWithinWarranty;

    // Manufacturer Info
    @Column(name = "manufacturer_reference_number", length = 100)
    private String manufacturerReferenceNumber;

    @Column(name = "sent_to_manufacturer_at")
    private LocalDateTime sentToManufacturerAt;

    @Column(name = "returned_from_manufacturer_at")
    private LocalDateTime returnedFromManufacturerAt;

    // Resolution
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution")
    private Resolution resolution;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Helper methods
    public boolean checkWarrantyValidity() {
        if (warrantyExpiryDate == null) return false;
        this.isWithinWarranty = LocalDate.now().isBefore(warrantyExpiryDate) || 
                                 LocalDate.now().isEqual(warrantyExpiryDate);
        return this.isWithinWarranty;
    }

    public void approve() {
        this.status = WarrantyStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = WarrantyStatus.REJECTED;
        this.rejectedReason = reason;
    }

    public void sendToManufacturer(String refNumber) {
        this.status = WarrantyStatus.SENT_TO_MANUFACTURER;
        this.manufacturerReferenceNumber = refNumber;
        this.sentToManufacturerAt = LocalDateTime.now();
    }

    public void complete(Resolution resolution, String notes) {
        this.status = WarrantyStatus.COMPLETED;
        this.resolution = resolution;
        this.resolutionNotes = notes;
        this.completedAt = LocalDateTime.now();
    }
}
