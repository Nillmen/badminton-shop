package com.badmintonshop.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin controller for page routing
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    /**
     * Admin login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * Admin dashboard
     */
    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model) {
        log.info("Loading admin dashboard");

        // TODO: Add real stats from services
        model.addAttribute("totalOrders", 0);
        model.addAttribute("totalRevenue", 0);
        model.addAttribute("totalCustomers", 0);
        model.addAttribute("pendingStringing", 0);

        return "admin/dashboard";
    }

    /**
     * Access denied page
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }

    /**
     * System Settings page
     */
    @GetMapping("/settings")
    public String settingsPage() {
        log.info("Loading admin settings page");
        return "admin/settings";
    }

    /**
     * Email Templates page
     */
    @GetMapping("/email-templates")
    public String emailTemplatesPage() {
        log.info("Loading admin email templates page");
        return "admin/email-templates";
    }

    /**
     * Activity Logs page
     */
    @GetMapping("/activity-logs")
    public String activityLogsPage() {
        log.info("Loading admin activity logs page");
        return "admin/activity-logs";
    }

    /**
     * Staff Management page
     */
    @GetMapping("/staff")
    public String staffPage() {
        log.info("Loading admin staff management page");
        return "admin/staff";
    }

    /**
     * Users (Customers) Management page
     */
    @GetMapping("/users")
    public String usersPage() {
        log.info("Loading admin users management page");
        return "admin/users";
    }

    /**
     * Orders Management page
     */
    @GetMapping("/orders")
    public String ordersPage() {
        log.info("Loading admin orders management page");
        return "admin/orders";
    }

    /**
     * Payments Management page
     */
    @GetMapping("/payments")
    public String paymentsPage() {
        log.info("Loading admin payments management page");
        return "admin/payments";
    }

    /**
     * Stringing Services Management page
     */
    @GetMapping("/stringing")
    public String stringingPage() {
        log.info("Loading admin stringing services page");
        return "admin/stringing";
    }

    /**
     * Placeholder for unimplemented features
     */
    @GetMapping({
            "/products", "/products/new",
            "/categories",
            "/brands",
            "/inventory",
            "/reviews",
            "/coupons", "/coupons/new",
            "/banners"
    })
    public String comingSoon() {
        return "admin/coming-soon";
    }
}
