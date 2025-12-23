package com.badmintonshop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to create VNPay payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCreateRequest {
    private Long orderId;
    private BigDecimal amount;
    private String orderInfo;
    private String bankCode; // Optional: NCB, VNPAYQR, etc.
    private String language; // vn or en
}
