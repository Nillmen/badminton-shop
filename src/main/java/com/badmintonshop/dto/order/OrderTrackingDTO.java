package com.badmintonshop.dto.order;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Order tracking DTO for shipping information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingDTO {

    private Long trackingId;
    private String carrier;
    private String trackingNumber;
    private String currentStatus;
    private LocalDateTime estimatedDelivery;
    private String trackingUrl;
    private String events; // JSON string of tracking events
    private LocalDateTime updatedAt;
}
