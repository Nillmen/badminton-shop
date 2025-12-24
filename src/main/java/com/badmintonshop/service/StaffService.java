package com.badmintonshop.service;

import com.badmintonshop.dto.request.StaffRequest;
import com.badmintonshop.dto.response.StaffResponse;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.StaffPermission;
import com.badmintonshop.entity.enums.StaffRole;
import com.badmintonshop.entity.enums.StaffStatus;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.StaffPermissionRepository;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.security.Auditable;
import com.badmintonshop.entity.enums.ActivityAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for Staff management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all staff with filters and pagination
     */
    public Page<StaffResponse> getAll(String search, StaffRole role, StaffStatus status, Pageable pageable) {
        log.debug("Fetching staff with filters - search: {}, role: {}, status: {}", search, role, status);

        return staffRepository.findWithFilters(search, role, status, pageable)
                .map(staff -> {
                    List<String> permissions = permissionRepository.findPermissionKeysByStaffId(staff.getStaffId());
                    return StaffResponse.fromEntityWithPermissions(staff, permissions);
                });
    }

    /**
     * Get staff by ID with permissions
     */
    public StaffResponse getById(Long id) {
        log.debug("Fetching staff by id: {}", id);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        List<String> permissions = permissionRepository.findPermissionKeysByStaffId(id);
        return StaffResponse.fromEntityWithPermissions(staff, permissions);
    }

    /**
     * Create new staff
     */
    @Transactional
    @Auditable(entityType = "Staff", action = ActivityAction.CREATE, description = "Created staff: {0}")
    public StaffResponse create(StaffRequest request) {
        log.info("Creating new staff: {}", request.getEmail());

        // Check for duplicate email
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Validate password for create
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required for new staff");
        }

        Staff staff = Staff.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(StaffStatus.ACTIVE)
                .stringingSkillLevel(request.getStringingSkillLevel())
                .dailyStringingCapacity(request.getDailyStringingCapacity())
                .build();

        Staff saved = staffRepository.save(staff);
        log.info("Created staff with id: {}", saved.getStaffId());

        return StaffResponse.fromEntity(saved);
    }

    /**
     * Update existing staff
     */
    @Transactional
    @Auditable(entityType = "Staff", action = ActivityAction.UPDATE, description = "Updated staff ID: {0}")
    public StaffResponse update(Long id, StaffRequest request) {
        log.info("Updating staff: {}", id);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        // Check for duplicate email (excluding current staff)
        if (!staff.getEmail().equals(request.getEmail()) &&
                staffRepository.existsByEmailAndStaffIdNot(request.getEmail(), id)) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        staff.setEmail(request.getEmail());
        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole());
        staff.setStringingSkillLevel(request.getStringingSkillLevel());
        staff.setDailyStringingCapacity(request.getDailyStringingCapacity());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        Staff saved = staffRepository.save(staff);
        log.info("Updated staff: {}", saved.getStaffId());

        List<String> permissions = permissionRepository.findPermissionKeysByStaffId(id);
        return StaffResponse.fromEntityWithPermissions(saved, permissions);
    }

    /**
     * Soft delete staff
     */
    @Transactional
    @Auditable(entityType = "Staff", action = ActivityAction.DELETE, description = "Soft deleted staff ID: {0}")
    public void softDelete(Long id) {
        log.info("Soft deleting staff: {}", id);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        // Use softDelete from BaseEntity
        staff.softDelete();
        staffRepository.save(staff);

        log.info("Soft deleted staff: {}", id);
    }

    /**
     * Update staff permissions
     * Replaces all existing permissions with new ones
     */
    @Transactional
    @Auditable(entityType = "Staff", action = ActivityAction.UPDATE, description = "Updated permissions for staff ID: {0}")
    public StaffResponse updatePermissions(Long id, List<String> permissions) {
        log.info("Updating permissions for staff: {}", id);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        // Delete existing permissions
        permissionRepository.deleteByStaffStaffId(id);

        // Add new permissions
        for (String permissionKey : permissions) {
            StaffPermission permission = StaffPermission.builder()
                    .staff(staff)
                    .permissionKey(permissionKey)
                    .createdAt(LocalDateTime.now())
                    .build();
            permissionRepository.save(permission);
        }

        log.info("Updated {} permissions for staff: {}", permissions.size(), id);

        return StaffResponse.fromEntityWithPermissions(staff, permissions);
    }

    /**
     * Update staff status
     */
    @Transactional
    @Auditable(entityType = "Staff", action = ActivityAction.UPDATE, description = "Updated status for staff ID: {0}")
    public StaffResponse updateStatus(Long id, StaffStatus status) {
        log.info("Updating status for staff {} to {}", id, status);

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));

        staff.setStatus(status);

        // Set resigned_at if status is RESIGNED
        if (status == StaffStatus.RESIGNED) {
            staff.setResignedAt(LocalDateTime.now());
        } else {
            staff.setResignedAt(null);
        }

        Staff saved = staffRepository.save(staff);
        log.info("Updated status for staff: {}", id);

        List<String> permissions = permissionRepository.findPermissionKeysByStaffId(id);
        return StaffResponse.fromEntityWithPermissions(saved, permissions);
    }

    /**
     * Get staff entity by ID (for internal use)
     */
    public Staff getStaffEntity(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));
    }

    /**
     * Count active staff
     */
    public long countActiveStaff() {
        return staffRepository.countByStatus(StaffStatus.ACTIVE);
    }

    /**
     * Count by role
     */
    public long countByRole(StaffRole role) {
        return staffRepository.countByRole(role);
    }
}