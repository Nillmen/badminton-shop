package com.badmintonshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Supplier - Nhà cung cấp - quản lý nguồn nhập hàng
 */
@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_suppliers_active", columnList = "is_active"),
        @Index(name = "idx_suppliers_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code; // Mã nhà cung cấp (optional, auto-generated if not provided)

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "payment_terms")
    private String paymentTerms; // VD: Net 30, COD

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Relationships
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSupplier> productSuppliers = new ArrayList<>();
}
