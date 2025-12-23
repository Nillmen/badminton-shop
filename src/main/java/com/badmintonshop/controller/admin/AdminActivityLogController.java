package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.response.ActivityLogResponse;
import com.badmintonshop.dto.response.ApiResponse;
import com.badmintonshop.entity.enums.ActivityAction;
import com.badmintonshop.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin API controller for activity logs
 */
@RestController
@RequestMapping("/admin/api/activity-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Activity Logs", description = "APIs for viewing staff activity logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminActivityLogController {

    private final AuditService auditService;

    /**
     * Get activity logs with filters
     */
    @GetMapping
    @Operation(summary = "Get activity logs", description = "Retrieve activity logs with optional filters")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLogs(
            @Parameter(description = "Filter by staff ID") @RequestParam(required = false) Long staffId,

            @Parameter(description = "Filter by entity type (e.g., Product, Order)") @RequestParam(required = false) String entityType,

            @Parameter(description = "Filter by action (CREATE, UPDATE, DELETE, etc.)") @RequestParam(required = false) ActivityAction action,

            @Parameter(description = "Filter from date (ISO format: yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "Filter to date (ISO format: yyyy-MM-ddTHH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching activity logs - staffId: {}, entityType: {}, action: {}, from: {}, to: {}",
                staffId, entityType, action, from, to);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityLogResponse> logs = auditService.getLogs(staffId, entityType, action, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get logs by staff
     */
    @GetMapping("/by-staff/{staffId}")
    @Operation(summary = "Get logs by staff", description = "Retrieve activity logs for a specific staff member")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLogsByStaff(
            @PathVariable Long staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching activity logs for staff: {}", staffId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityLogResponse> logs = auditService.getLogsByStaff(staffId, pageable);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get logs by entity type
     */
    @GetMapping("/by-entity/{entityType}")
    @Operation(summary = "Get logs by entity type", description = "Retrieve activity logs for a specific entity type")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getLogsByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching activity logs for entity type: {}", entityType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityLogResponse> logs = auditService.getLogsByEntity(entityType, pageable);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get history for specific entity
     */
    @GetMapping("/history/{entityType}/{entityId}")
    @Operation(summary = "Get entity history", description = "Retrieve change history for a specific entity")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        log.info("Fetching history for {} with id: {}", entityType, entityId);

        List<ActivityLogResponse> logs = auditService.getLogsForEntity(entityType, entityId);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get recent activity logs
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent logs", description = "Retrieve most recent activity logs")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching recent activity logs");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityLogResponse> logs = auditService.getRecentLogs(pageable);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get activity count for staff in date range
     */
    @GetMapping("/count/{staffId}")
    @Operation(summary = "Get activity count", description = "Count activities for a staff member in date range")
    public ResponseEntity<ApiResponse<Long>> getActivityCount(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("Counting activities for staff {} from {} to {}", staffId, from, to);

        long count = auditService.countByStaffInDateRange(staffId, from, to);

        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
