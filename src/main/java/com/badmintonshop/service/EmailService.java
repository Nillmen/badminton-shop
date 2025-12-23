package com.badmintonshop.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from:noreply@badmintonshop.com}")
    private String fromEmail;

    @Value("${app.email.from-name:Badminton Shop}")
    private String fromName;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send email verification email
     */
    @Async("emailExecutor")
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("verificationLink", baseUrl + "/verify-email?token=" + token);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/verification", context);

            sendHtmlEmail(toEmail, "Xác thực email - Badminton Shop", htmlContent);
            log.info("Verification email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }

    /**
     * Send password reset email
     */
    @Async("emailExecutor")
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("resetLink", baseUrl + "/reset-password?token=" + token);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendHtmlEmail(toEmail, "Đặt lại mật khẩu - Badminton Shop", htmlContent);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Format: "Badminton Shop <email@gmail.com>"
        String fromWithName = String.format("%s <%s>", fromName, fromEmail);
        helper.setFrom(fromWithName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Send promotional email (coupon, promotion notifications)
     * 
     * @param toEmail      Recipient email
     * @param userName     User's name
     * @param title        Email title
     * @param message      Email message (HTML supported)
     * @param type         Type: PROMOTION or SYSTEM
     * @param couponCode   Optional coupon code
     * @param discountText Optional discount description
     * @param couponExpiry Optional expiry date string
     * @param actionUrl    Optional CTA link
     */
    @Async("emailExecutor")
    public void sendPromotionEmail(String toEmail, String userName, String title,
            String message, String type,
            String couponCode, String discountText,
            String couponExpiry, String actionUrl) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("title", title);
            context.setVariable("message", message);
            context.setVariable("type", type != null ? type : "PROMOTION");
            context.setVariable("baseUrl", baseUrl);

            if (couponCode != null && !couponCode.trim().isEmpty()) {
                context.setVariable("couponCode", couponCode);
            }
            if (discountText != null && !discountText.trim().isEmpty()) {
                context.setVariable("discountText", discountText);
            }
            if (couponExpiry != null && !couponExpiry.trim().isEmpty()) {
                context.setVariable("couponExpiry", couponExpiry);
            }
            if (actionUrl != null && !actionUrl.trim().isEmpty()) {
                context.setVariable("actionUrl", actionUrl.startsWith("/") ? baseUrl + actionUrl : actionUrl);
            }

            String htmlContent = templateEngine.process("email/promotion", context);
            sendHtmlEmail(toEmail, title + " - Badminton Shop", htmlContent);
            log.info("Promotion email sent to: {} - Title: {}", toEmail, title);

        } catch (Exception e) {
            log.error("Failed to send promotion email to: {}", toEmail, e);
        }
    }

    /**
     * Send simple promotion email (without coupon details)
     */
    @Async("emailExecutor")
    public void sendPromotionEmail(String toEmail, String userName, String title,
            String message, String actionUrl) {
        sendPromotionEmail(toEmail, userName, title, message, "PROMOTION", null, null, null, actionUrl);
    }

    /**
     * Send order confirmation email
     */
    @Async("emailExecutor")
    public void sendOrderConfirmation(String toEmail, String customerName, String orderNumber,
            String orderDate, String paymentMethod, java.util.List<java.util.Map<String, String>> orderItems,
            String subtotal, String discount, String shippingFee, String totalAmount,
            String recipientName, String recipientPhone, String shippingAddress, String orderDetailLink) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("orderDate", orderDate);
            context.setVariable("paymentMethod", paymentMethod);
            context.setVariable("orderItems", orderItems);
            context.setVariable("subtotal", subtotal);
            context.setVariable("discount", discount);
            context.setVariable("shippingFee", shippingFee);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("recipientName", recipientName);
            context.setVariable("recipientPhone", recipientPhone);
            context.setVariable("shippingAddress", shippingAddress);
            context.setVariable("orderDetailLink", orderDetailLink != null ? 
                    (orderDetailLink.startsWith("/") ? baseUrl + orderDetailLink : orderDetailLink) : 
                    baseUrl + "/account/orders");
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("email/order-confirmation", context);
            sendHtmlEmail(toEmail, "Xác nhận đơn hàng #" + orderNumber + " - Badminton Shop", htmlContent);
            log.info("Order confirmation email sent to: {} for order {}", toEmail, orderNumber);

        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {} for order {}", toEmail, orderNumber, e);
        }
    }
}
