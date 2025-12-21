package com.badmintonshop.dto;

import com.badmintonshop.entity.enums.PaymentMethod;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;
    private PaymentMethod paymentMethod;
    private String customerNotes;

    // Danh sách sản phẩm muốn mua
    private List<OrderItemRequest> items;

    // Guest / User Info
    private String email;
    private String fullName;
    private String phone;

    // Session ID cho Guest Checkout
    private String sessionId;
}