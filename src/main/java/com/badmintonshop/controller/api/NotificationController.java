package com.badmintonshop.controller.api;

import com.badmintonshop.dto.notification.NotificationDTO;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Public API for user notifications
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get current user's notifications
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {

        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(
                userId, type, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("unreadCount", 0L));
        }

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Principal principal) {

        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            NotificationDTO notification = notificationService.markAsRead(id, userId);
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả đã đọc", "count", count));
    }

    /**
     * Helper method to extract userId from Principal (supports both CustomUserDetails and CustomOAuth2User)
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        // For form-based login
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof CustomUserDetails) {
                return ((CustomUserDetails) principalObj).getUserId();
            }
        }

        // For OAuth2 login
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            Object principalObj = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) principalObj).getUserId();
            }
        }

        return null;
    }
}

