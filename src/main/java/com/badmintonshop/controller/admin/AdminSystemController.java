package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.request.SettingsUpdateRequest;
import com.badmintonshop.dto.response.ApiResponse;
import com.badmintonshop.dto.response.SystemSettingResponse;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.ActivityAction;
import com.badmintonshop.exception.StaffNotFoundException;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.service.AuditService;
import com.badmintonshop.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin API controller for system settings
 */
@RestController
@RequestMapping("/admin/api/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - System Settings", description = "APIs for managing system settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSystemController {

    private final SystemSettingService settingService;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    /**
     * Get all system settings
     */
    @GetMapping
    @Operation(summary = "Get all settings", description = "Retrieve all system settings")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> getAllSettings() {
        log.info("Fetching all system settings");
        List<SystemSettingResponse> settings = settingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Get setting by key
     */
    @GetMapping("/{key}")
    @Operation(summary = "Get setting by key", description = "Retrieve a specific setting by its key")
    public ResponseEntity<ApiResponse<SystemSettingResponse>> getByKey(@PathVariable String key) {
        log.info("Fetching setting by key: {}", key);
        SystemSettingResponse setting = settingService.getByKey(key);
        return ResponseEntity.ok(ApiResponse.success(setting));
    }

    /**
     * Get public settings only
     */
    @GetMapping("/public")
    @Operation(summary = "Get public settings", description = "Retrieve only public settings (no auth required)")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicSettings() {
        Map<String, String> settings = settingService.getPublicSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update multiple settings at once
     */
    @PutMapping
    @Operation(summary = "Update settings", description = "Update multiple settings at once")
    public ResponseEntity<ApiResponse<Map<String, SystemSettingResponse>>> updateSettings(
            @Valid @RequestBody SettingsUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Updating {} settings", request.getSettings().size());

        Staff staff = getStaffFromAuth(authentication);
        Map<String, SystemSettingResponse> updated = settingService.updateSettings(
                request.getSettings(), staff);

        // Log audit
        auditService.logActivity(
                staff,
                ActivityAction.UPDATE,
                "SystemSetting",
                null,
                "Updated " + updated.size() + " settings",
                null,
                request.getSettings(),
                httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Updated " + updated.size() + " settings successfully", updated));
    }

    /**
     * Update single setting
     */
    @PutMapping("/{key}")
    @Operation(summary = "Update single setting", description = "Update a specific setting by key")
    public ResponseEntity<ApiResponse<SystemSettingResponse>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Missing 'value' in request body"));
        }

        log.info("Updating setting: {} = {}", key, value);

        Staff staff = getStaffFromAuth(authentication);
        SystemSettingResponse updated = settingService.updateSetting(key, value, staff);

        // Log audit
        auditService.logActivity(
                staff,
                ActivityAction.UPDATE,
                "SystemSetting",
                updated.getSettingId(),
                "Updated setting: " + key,
                null,
                value,
                httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }

    /**
     * Get current staff from authentication
     */
    private Staff getStaffFromAuth(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String email = authentication.getName();
        return staffRepository.findByEmail(email)
                .orElse(null);
    }
}
