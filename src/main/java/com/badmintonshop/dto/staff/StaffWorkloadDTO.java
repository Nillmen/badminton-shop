package com.badmintonshop.dto.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffWorkloadDTO {
    private Long staffId;
    private String staffName;
    private String email;
    private int currentAssignedCount; // PENDING + IN_PROGRESS
    private int completedTodayCount; // COMPLETED (today)
    private boolean isOnline; // Optional: if we track online status
}
