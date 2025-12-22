package com.badmintonshop.security;

import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.StaffStatus;
import com.badmintonshop.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * UserDetailsService for Staff (Admin panel authentication)
 */
@Service("staffUserDetailsService")
@RequiredArgsConstructor
@Slf4j
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading staff by email: {}", email);

        Staff staff = staffRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Staff not found with email: {}", email);
                    return new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
                });

        log.debug("Staff found: {} with role: {}. Hash: [{}]", staff.getEmail(), staff.getRole(),
                staff.getPasswordHash());

        // Check staff status
        if (staff.getStatus() == StaffStatus.RESIGNED) {
            log.warn("Staff account is resigned: {}", email);
            throw new UsernameNotFoundException("Tài khoản đã nghỉ việc");
        }

        if (staff.getStatus() == StaffStatus.BANNED) {
            log.warn("Staff account is banned: {}", email);
            throw new UsernameNotFoundException("Tài khoản đã bị cấm");
        }

        if (staff.getStatus() == StaffStatus.INACTIVE) {
            log.warn("Staff account is inactive: {}", email);
            throw new UsernameNotFoundException("Tài khoản đang tạm nghỉ");
        }

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            log.warn("Staff account is not active: {}", email);
            throw new UsernameNotFoundException("Tài khoản không hoạt động");
        }

        log.debug("Staff found: {} with role: {}", staff.getEmail(), staff.getRole());
        return new StaffUserDetails(staff);
    }

    /**
     * Update last login time
     */
    @Transactional
    public void updateLastLogin(String email) {
        staffRepository.findByEmail(email).ifPresent(staff -> {
            staff.setLastLoginAt(LocalDateTime.now());
            staffRepository.save(staff);
            log.debug("Updated last login for staff: {}", email);
        });
    }
}
