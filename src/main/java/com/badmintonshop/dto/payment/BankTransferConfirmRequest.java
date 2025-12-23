package com.badmintonshop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request to confirm bank transfer with proof
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferConfirmRequest {
    private Long orderId;
    private String bankName;
    private String bankAccountNumber;
    private String transferReference;
    private String proofImageUrl; // URL after upload
}
