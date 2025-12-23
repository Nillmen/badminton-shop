package com.badmintonshop.controller;

import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller for notifications page
 */
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationPageController {

    /**
     * Display notifications page
     * GET /notifications
     */
    @GetMapping
    public String showNotifications(Principal principal, Model model) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("userId", userId);
        return "shop/notifications";
    }

    /**
     * Helper method to extract userId from Principal (supports both CustomUserDetails and CustomOAuth2User)
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        // For form-based login
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object principalObj = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof CustomUserDetails) {
                return ((CustomUserDetails) principalObj).getUserId();
            }
        }

        // For OAuth2 login
        if (principal instanceof OAuth2AuthenticationToken) {
            Object principalObj = ((OAuth2AuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) principalObj).getUserId();
            }
        }

        return null;
    }
}
