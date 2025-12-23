package com.badmintonshop.dto.order;

import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight order DTO for list views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListDTO {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    
    // User info (for admin view)
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    // Pricing
    private BigDecimal totalAmount;
    private int totalItems;

    // Payment
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // Timestamps
    private LocalDateTime createdAt;
    
    // Flags
    private boolean hasStringingItems;
}
