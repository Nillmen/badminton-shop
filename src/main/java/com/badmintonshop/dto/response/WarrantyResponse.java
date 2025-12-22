package com.badmintonshop.dto.response;

import com.badmintonshop.entity.enums.IssueType;
import com.badmintonshop.entity.enums.WarrantyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WarrantyResponse {
    private Long warrantyId;
    private String warrantyNumber;
    private Long orderId;
    private String orderNumber;

    private String productName;
    private String variantName;

    private WarrantyStatus status;
    private IssueType issueType;
    private String description;

    private LocalDate purchaseDate;
    private LocalDate warrantyExpiryDate;

    private LocalDateTime createdAt;
}
