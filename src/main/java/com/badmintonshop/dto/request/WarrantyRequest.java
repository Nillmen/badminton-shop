package com.badmintonshop.dto.request;

import com.badmintonshop.entity.enums.IssueType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarrantyRequest {
    @NotNull(message = "Order Item ID is required")
    private Long orderItemId;

    @NotNull(message = "Issue type is required")
    private IssueType issueType;

    @NotNull(message = "Description is required")
    private String description;

    private String images; // JSON string of image URLs
}
