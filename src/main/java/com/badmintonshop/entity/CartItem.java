package com.badmintonshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity CartItem - Chi tiết giỏ hàng - có thêm thông tin dịch vụ đan vợt
 */
@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart", columnList = "cart_id"),
        @Index(name = "idx_cart_items_product", columnList = "product_id"),
        @Index(name = "idx_cart_items_variant", columnList = "variant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    @EqualsAndHashCode.Include
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Price snapshot
    @Column(name = "price_at_add", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtAdd; // Giá lúc thêm vào giỏ

    // Stringing Service (nếu là vợt)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stringing_service_id")
    private StringingService stringingService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "string_id")
    private StringProduct stringProduct;

    @Column(name = "tension", precision = 4, scale = 1)
    private BigDecimal tension; // Độ căng (lbs)

    @Column(name = "stringing_notes", columnDefinition = "TEXT")
    private String stringingNotes; // Ghi chú đặc biệt về đan vợt

    @Column(name = "added_at")
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    // Helper methods
    public boolean hasStringingService() {
        return stringingService != null;
    }

    public BigDecimal getSubtotal() {
        BigDecimal subtotal = priceAtAdd.multiply(BigDecimal.valueOf(quantity));

        if (stringingService != null) {
            subtotal = subtotal.add(stringingService.getBasePrice().multiply(BigDecimal.valueOf(quantity)));
        }
        if (stringProduct != null && stringProduct.getRetailPrice() != null) {
            subtotal = subtotal.add(stringProduct.getRetailPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        return subtotal;
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        this.quantity = Math.max(1, this.quantity - amount);
    }
}
