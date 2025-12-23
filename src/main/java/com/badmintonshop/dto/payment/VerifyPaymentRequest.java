package com.badmintonshop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin request to verify bank transfer payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {
    private Boolean approved;
    private String notes;
    private String transactionId; // Optional: bank transaction ID
}
