package com.badmintonshop.dto.order;

import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private Long userId; // Added for Admin
    private String customerName; // Added for Admin
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private String paymentUrl; // Optional: for VNPAY redirect
}
