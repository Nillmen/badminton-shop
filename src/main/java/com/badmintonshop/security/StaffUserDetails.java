package com.badmintonshop.security;

import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.StaffStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails for Staff (Admin/Staff users)
 */
@Getter
public class StaffUserDetails implements UserDetails {

    private final Staff staff;

    public StaffUserDetails(Staff staff) {
        this.staff = staff;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map StaffRole to Spring Security role
        // SUPER_ADMIN -> ROLE_ADMIN, others -> ROLE_STAFF
        String role;
        switch (staff.getRole()) {
            case SUPER_ADMIN:
                role = "ROLE_ADMIN";
                break;
            default:
                role = "ROLE_STAFF";
                break;
        }
        System.out.println("DEBUG: Granting authority: " + role + " for staff: " + staff.getEmail());
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return staff.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return staff.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return staff.getStatus() != StaffStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return staff.getStatus() == StaffStatus.ACTIVE;
    }

    // Helper methods
    public Long getStaffId() {
        return staff.getStaffId();
    }

    public String getFullName() {
        return staff.getFullName();
    }

    public String getEmail() {
        return staff.getEmail();
    }

    public String getAvatarUrl() {
        return staff.getAvatarUrl();
    }

    public String getRole() {
        return staff.getRole().name();
    }
}
