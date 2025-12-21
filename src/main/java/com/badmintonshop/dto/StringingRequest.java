package com.badmintonshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StringingRequest {
    private Long stringingServiceId; // ID dịch vụ (đan 4 nút, 2 nút...)
    private Long stringId; // ID loại cước (BG65, BG80...)
    private BigDecimal tension; // Độ căng (lbs)
    private String stringingNotes; // Ghi chú thêm
}