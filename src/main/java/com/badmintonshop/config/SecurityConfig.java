package com.badmintonshop.config;

import com.badmintonshop.security.CustomAuthenticationFailureHandler;
import com.badmintonshop.security.CustomUserDetailsService;
import com.badmintonshop.security.StaffUserDetailsService;
import com.badmintonshop.security.OAuth2UserService;
import com.badmintonshop.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;
import java.util.List;

/**
 * Security Configuration
 * - Separate security chains for Admin and Customer
 * - OAuth2 Login (Google)
 * - Remember Me functionality
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final StaffUserDetailsService staffUserDetailsService;
        private final CustomAuthenticationFailureHandler authenticationFailureHandler;
        private final OAuth2UserService oAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final DataSource dataSource;

        /**
         * Password encoder using BCrypt
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        /**
         * Customer AuthenticationManager bean - needed for AuthService AJAX login
         * Creates provider inline to avoid AOP proxy recursion
         */
        @Bean
        public AuthenticationManager authenticationManager(PasswordEncoder encoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(encoder);
                return new ProviderManager(provider);
        }

        /**
         * Remember Me token repository
         */
        @Bean
        public PersistentTokenRepository persistentTokenRepository() {
                JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
                tokenRepository.setDataSource(dataSource);
                // Create table if not exists - set to false in production
                // tokenRepository.setCreateTableOnStartup(true);
                return tokenRepository;
        }

        /**
         * Admin Security Chain
         * - Higher priority (Order 1)
         * - Only matches /admin/** paths
         * - Uses StaffUserDetailsService
         */
        @Bean
        @Order(1)
        public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, PasswordEncoder encoder)
                        throws Exception {
                // Create provider directly to avoid AOP proxy issues
                DaoAuthenticationProvider staffProvider = new DaoAuthenticationProvider();
                staffProvider.setUserDetailsService(staffUserDetailsService);
                staffProvider.setPasswordEncoder(encoder);

                // Create a separate AuthenticationManager for admin
                AuthenticationManager adminAuthManager = new ProviderManager(staffProvider);

                http
                                .securityMatcher("/admin/**")
                                .authenticationManager(adminAuthManager)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/admin/login", "/admin/forgot-password",
                                                                "/admin/access-denied")
                                                .permitAll()
                                                .requestMatchers("/admin/api/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF"))
                                .formLogin(form -> form
                                                .loginPage("/admin/login")
                                                .loginProcessingUrl("/admin/login")
                                                .defaultSuccessUrl("/admin/dashboard", true)
                                                .failureUrl("/admin/login?error=true")
                                                .usernameParameter("email")
                                                .passwordParameter("password"))
                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout"))
                                                .logoutSuccessUrl("/admin/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "remember-me"))
                                // Disable CSRF for Admin pages temporarily for debugging
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/admin/**"))
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/admin/access-denied"))
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .expiredUrl("/admin/login?expired=true"));

                return http.build();
        }

        /**
         * Customer Security Chain
         * - Lower priority (Order 2)
         * - Matches all other paths
         */
        @Bean
        @Order(2)
        public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http, PasswordEncoder encoder)
                        throws Exception {
                // Create provider directly to avoid AOP proxy issues
                DaoAuthenticationProvider customerProvider = new DaoAuthenticationProvider();
                customerProvider.setUserDetailsService(userDetailsService);
                customerProvider.setPasswordEncoder(encoder);

                http
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"))
                                // Use authenticationProvider instead of authenticationManager
                                // to preserve OAuth2 authentication capability
                                .authenticationProvider(customerProvider)
                                .authorizeHttpRequests(auth -> auth
                                                // Public pages
                                                .requestMatchers(
                                                                "/", "/home",
                                                                "/products/**", "/categories/**", "/brands/**",
                                                                "/search", "/compare",
                                                                "/cart/**",
                                                                "/login", "/perform_login", "/register",
                                                                "/forgot-password", "/reset-password",
                                                                "/verify-email", "/resend-verification",
                                                                "/verification-required",
                                                                "/oauth2/**",
                                                                "/static/**", "/css/**", "/js/**", "/images/**",
                                                                "/fonts/**", "/vendor/**",
                                                                "/api/public/**",
                                                                "/error", "/404", "/500")
                                                .permitAll()
                                                // API endpoints
                                                .requestMatchers("/api/**").permitAll()
                                                .requestMatchers("/api/products/**").permitAll()
                                                // Authenticated pages
                                                .requestMatchers(
                                                                "/account/**", "/orders/**", "/wishlist/**",
                                                                "/checkout/**", "/payment/**",
                                                                "/users/**")
                                                .authenticated()
                                                .anyRequest().permitAll())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/perform_login")
                                                .defaultSuccessUrl("/", false)
                                                .failureHandler(authenticationFailureHandler)
                                                .usernameParameter("email")
                                                .passwordParameter("password"))
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(oAuth2UserService))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureUrl("/login?oauth2_error=true"))
                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                                .logoutSuccessUrl("/?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "remember-me"))
                                .rememberMe(remember -> remember
                                                .tokenRepository(persistentTokenRepository())
                                                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                                                .userDetailsService(userDetailsService)
                                                .key("badminton-shop-remember-me-key"))
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/access-denied"))
                                .sessionManagement(session -> session
                                                .maximumSessions(3)
                                                .expiredUrl("/login?expired=true"));

                return http.build();
        }
}
