package com.badmintonshop.security;

import com.badmintonshop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication success handler that merges guest cart and redirects to home page
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final CartService cartService;
    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Long userId = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            userId = ((CustomOAuth2User) principal).getUserId();
        }

        // Merge guest cart if exists
        if (userId != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String guestSessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
                if (guestSessionId != null) {
                    try {
                        cartService.mergeGuestCart(guestSessionId, userId);
                        session.removeAttribute(GUEST_SESSION_KEY);
                        log.info("Merged guest cart for user: {}", userId);
                    } catch (Exception e) {
                        log.error("Failed to merge guest cart for user {}: {}", userId, e.getMessage());
                    }
                }
            }
        }

        // Redirect to home
        response.sendRedirect("/");
    }
}

