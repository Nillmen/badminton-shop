package com.badmintonshop.dto.order;

import lombok.Data;

@Data
public class OrderRequest {
    private Long addressId; // ID of the selected address
    private String receiverName; // Optional override
    private String receiverPhone; // Optional override
    private String shippingAddress; // Optional override (if not using saved address)
    private String paymentMethod; // COD, VNPAY, BANK_TRANSFER
    private String note;
}
