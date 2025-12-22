package com.badmintonshop.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 Success Handler - redirects to home with userId
 */
@Component
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Long userId = null;

        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            log.info("OAuth2 login successful for user: {}", oAuth2User.getFullName());

            // Update last login time
            oAuth2User.getUser().recordLogin();

            userId = oAuth2User.getUserId();
        }

        // Redirect to home without uid parameter
        response.sendRedirect("/");
    }
}
