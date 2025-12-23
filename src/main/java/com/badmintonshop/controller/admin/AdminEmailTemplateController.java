package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.request.EmailTemplateRequest;
import com.badmintonshop.dto.response.ApiResponse;
import com.badmintonshop.dto.response.EmailTemplateResponse;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.ActivityAction;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.service.AuditService;
import com.badmintonshop.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin API controller for email templates
 */
@RestController
@RequestMapping("/admin/api/email-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Email Templates", description = "APIs for managing email templates")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminEmailTemplateController {

    private final EmailTemplateService templateService;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    /**
     * Get all email templates
     */
    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieve all email templates")
    public ResponseEntity<ApiResponse<List<EmailTemplateResponse>>> getAll() {
        log.info("Fetching all email templates");
        List<EmailTemplateResponse> templates = templateService.getAll();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * Get template by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieve email template by ID")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> getById(@PathVariable Long id) {
        log.info("Fetching email template by id: {}", id);
        EmailTemplateResponse template = templateService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    /**
     * Get template by key
     */
    @GetMapping("/key/{key}")
    @Operation(summary = "Get template by key", description = "Retrieve email template by template key")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> getByKey(@PathVariable String key) {
        log.info("Fetching email template by key: {}", key);
        EmailTemplateResponse template = templateService.getByKey(key);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    /**
     * Create new email template
     */
    @PostMapping
    @Operation(summary = "Create template", description = "Create a new email template")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> create(
            @Valid @RequestBody EmailTemplateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Creating email template: {}", request.getTemplateKey());

        EmailTemplateResponse created = templateService.create(request);

        // Log audit
        Staff staff = getStaffFromAuth(authentication);
        auditService.logActivity(
                staff,
                ActivityAction.CREATE,
                "EmailTemplate",
                created.getTemplateId(),
                "Created email template: " + request.getTemplateKey(),
                null,
                request,
                httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Email template created successfully", created));
    }

    /**
     * Update email template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Update an existing email template")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Updating email template: {}", id);

        // Get old value for audit
        EmailTemplateResponse oldTemplate = templateService.getById(id);
        EmailTemplateResponse updated = templateService.update(id, request);

        // Log audit
        Staff staff = getStaffFromAuth(authentication);
        auditService.logActivity(
                staff,
                ActivityAction.UPDATE,
                "EmailTemplate",
                id,
                "Updated email template: " + updated.getTemplateKey(),
                oldTemplate,
                request,
                httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Email template updated successfully", updated));
    }

    /**
     * Delete email template
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Delete an email template")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Deleting email template: {}", id);

        // Get template info for audit before deletion
        EmailTemplateResponse template = templateService.getById(id);
        templateService.delete(id);

        // Log audit
        Staff staff = getStaffFromAuth(authentication);
        auditService.logActivity(
                staff,
                ActivityAction.DELETE,
                "EmailTemplate",
                id,
                "Deleted email template: " + template.getTemplateKey(),
                template,
                null,
                httpRequest);

        return ResponseEntity.ok(ApiResponse.success("Email template deleted successfully"));
    }

    /**
     * Preview template with sample data
     */
    @PostMapping("/preview")
    @Operation(summary = "Preview template", description = "Preview email template with sample data")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(
            @RequestParam String templateKey,
            @RequestBody Map<String, Object> sampleData) {

        log.info("Previewing email template: {}", templateKey);
        Map<String, String> preview = templateService.preview(templateKey, sampleData);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    /**
     * Toggle template active status
     */
    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle active", description = "Toggle email template active status")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> toggleActive(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("Toggling active status for template: {}", id);

        EmailTemplateResponse updated = templateService.toggleActive(id);

        // Log audit
        Staff staff = getStaffFromAuth(authentication);
        auditService.logActivity(
                staff,
                ActivityAction.UPDATE,
                "EmailTemplate",
                id,
                "Toggled template active status to: " + updated.getIsActive(),
                null,
                updated.getIsActive(),
                httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Template " + (updated.getIsActive() ? "activated" : "deactivated"), updated));
    }

    /**
     * Get current staff from authentication
     */
    private Staff getStaffFromAuth(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String email = authentication.getName();
        return staffRepository.findByEmail(email).orElse(null);
    }
}
