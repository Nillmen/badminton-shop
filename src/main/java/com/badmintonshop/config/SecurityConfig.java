package com.badmintonshop.config;

import com.badmintonshop.security.CustomUserDetailsService;
// TODO: Uncomment when OAuth2 is configured
// import com.badmintonshop.security.OAuth2UserService;
// import com.badmintonshop.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // <--- ĐÃ THÊM IMPORT NÀY
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

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
        // TODO: Uncomment when OAuth2 is configured
        // private final OAuth2UserService oAuth2UserService;
        // private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final DataSource dataSource;

        /**
         * Password encoder using BCrypt
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        /**
         * Authentication provider
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        /**
         * Authentication manager
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
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
         */
        @Bean
        @Order(1)
        public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                // .csrf(AbstractHttpConfigurer::disable)
                                .securityMatcher("/admin/**")
                                .authorizeHttpRequests(auth -> auth
                                                // .requestMatchers("/admin/api/payments/**").permitAll() // <-- DÒNG BỔ
                                                // SUNG
                                                .requestMatchers("/admin/login", "/admin/forgot-password").permitAll()
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")

                                )
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
        public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 🔥 ĐÃ THÊM DÒNG NÀY ĐỂ TẮT CSRF CHO BRO TEST API 🔥
                                .csrf(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                // Public pages
                                                .requestMatchers(
                                                                "/", "/home",
                                                                "/products/**", "/categories/**", "/brands/**",
                                                                "/search", "/compare",
                                                                "/cart/**", // Đảm bảo dòng này có để API Cart được
                                                                            // public
                                                                "/login", "/register", "/forgot-password",
                                                                "/reset-password",
                                                                "/oauth2/**",
                                                                "/static/**", "/css/**", "/js/**", "/images/**",
                                                                "/fonts/**", "/vendor/**",
                                                                "/api/public/**",
                                                                "/error", "/404", "/500")
                                                .permitAll()

                                                // Mở cửa API Cart cho chắc cú (nếu dòng trên chưa đủ)
                                                .requestMatchers("/api/**").permitAll()

                                                // Authenticated pages
                                                // .requestMatchers("/orders/**").authenticated() // Moved to permitAll
                                                // for Guest Access

                                                // Allow Guest Checkout & Payment
                                                .requestMatchers(
                                                                "/checkout/**",
                                                                "/payment/**",
                                                                "/orders/**", // Allow Guest to access My Orders page
                                                                "/api/orders", // Allow Guest to list their orders
                                                                "/api/orders/create", // Allow creating orders as guest
                                                                "/api/orders/{orderNumber}" // View Order Detail
                                                                                            // (Controller checks
                                                                                            // ownership/session if we
                                                                                            // implemented it, currently
                                                                                            // checks User 1)
                                                ).permitAll()
                                                .anyRequest().permitAll())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/", false)
                                                .failureUrl("/login?error=true")
                                                .usernameParameter("email")
                                                .passwordParameter("password"))
                                // TODO: Enable OAuth2 login once Google Client ID/Secret are configured in
                                // application.properties
                                // .oauth2Login(oauth2 -> oauth2
                                // .loginPage("/login")
                                // .userInfoEndpoint(userInfo -> userInfo
                                // .userService(oAuth2UserService)
                                // )
                                // .successHandler(oAuth2SuccessHandler)
                                // .failureUrl("/login?oauth2_error=true")
                                // )
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