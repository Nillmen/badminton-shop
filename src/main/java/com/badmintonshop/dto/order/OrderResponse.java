package com.badmintonshop.dto.order;

import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order response DTO for order detail view
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    
    // User info
    private Long userId;
    private String userName;
    private String userEmail;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    // Payment
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;

    // Shipping
    private String recipientName;
    private String phone;
    private String address;
    private String ward;
    private String district;
    private String city;
    private String fullAddress;

    // Notes
    private String customerNotes;
    private String adminNotes;

    // Cancellation
    private String cancelledReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime processingAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // Items
    private List<OrderItemDTO> items;
    private int totalItems;

    // Tracking
    private OrderTrackingDTO tracking;

    // Flags
    private boolean hasStringingItems;
    private boolean isCancellable;
}
