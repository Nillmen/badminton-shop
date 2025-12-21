package com.badmintonshop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "strings") // Map với bảng strings trong DB
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadmintonString {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "string_id")
    private Long stringId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "retail_price", precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "quantity_in_stock")
    private Integer quantityInStock;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}