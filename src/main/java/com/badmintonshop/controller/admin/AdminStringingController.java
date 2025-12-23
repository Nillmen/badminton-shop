package com.badmintonshop.controller.admin;

import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.StaffRole;

import java.util.List;
import com.badmintonshop.entity.enums.StringingStatus;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.service.StringingBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Stringing Controller
 * SALE_STAFF: Can assign work, STRINGING_STAFF: Can do assigned work
 */
@Controller
@RequestMapping("/admin/stringing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALE_STAFF', 'STRINGING_STAFF')")
public class AdminStringingController {

    private final OrderItemRepository orderItemRepository;
    private final StaffRepository staffRepository;
    private final StringingBookingService bookingService;

    @GetMapping
    public String listRequests(Model model, Authentication authentication,
            @PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);

        if (isAdmin) {
            // Admin: Lấy tất cả đơn và phân loại
            var allItems = orderItemRepository.findByHasStringingServiceTrue(pageable).getContent();

            // Tab 1: Đơn chờ giao (PENDING)
            var pendingItems = allItems.stream()
                    .filter(item -> item.getStringingStatus() == StringingStatus.PENDING)
                    .toList();

            // Tab 2: Đơn đang xử lý (ASSIGNED, IN_PROGRESS, COMPLETED, QUALITY_CHECKED)
            var progressItems = allItems.stream()
                    .filter(item -> item.getStringingStatus() != StringingStatus.PENDING)
                    .toList();

            model.addAttribute("pendingItems", pendingItems);
            model.addAttribute("progressItems", progressItems);
            model.addAttribute("staffs", staffRepository.findAll());
        } else {
            // Staff chỉ thấy đơn được gán cho mình (không cần tab Giao)
            String email = authentication.getName();
            Staff staff = staffRepository.findByEmail(email).orElse(null);
            if (staff != null) {
                Page<com.badmintonshop.entity.OrderItem> assignedItems = orderItemRepository
                        .findByAssignedStaffAndStringingStatus(staff, StringingStatus.ASSIGNED, pageable);
                Page<com.badmintonshop.entity.OrderItem> inProgressItems = orderItemRepository
                        .findByAssignedStaffAndStringingStatus(staff, StringingStatus.IN_PROGRESS, pageable);

                var progressItems = new java.util.ArrayList<>(assignedItems.getContent());
                progressItems.addAll(inProgressItems.getContent());
                model.addAttribute("progressItems", progressItems);
            } else {
                model.addAttribute("progressItems", List.of());
            }
            model.addAttribute("pendingItems", List.of());
            model.addAttribute("staffs", List.of());
        }

        return "admin/stringing/list";
    }

    @PostMapping("/assign")
    public String assignStaff(@RequestParam Long orderItemId,
            @RequestParam Long staffId,
            RedirectAttributes redirectAttributes) {
        try {
            bookingService.assignStaff(orderItemId, staffId);
            redirectAttributes.addFlashAttribute("success", "Assigned successfully");
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/stringing";
    }

    @PostMapping("/start")
    public String startStringing(@RequestParam Long orderItemId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.startStringing(orderItemId);
            redirectAttributes.addFlashAttribute("success", "Đã bắt đầu đan vợt");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/stringing";
    }

    @PostMapping("/complete")
    public String completeStringing(@RequestParam Long orderItemId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.completeStringing(orderItemId);
            redirectAttributes.addFlashAttribute("success", "Đã hoàn thành đan vợt");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/stringing";
    }

    @PostMapping("/qc-approve")
    public String approveQuality(@RequestParam Long orderItemId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.approveQuality(orderItemId);
            redirectAttributes.addFlashAttribute("success", "Quality Checked & Approved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stringing";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("workload", bookingService.getStaffWorkload());
        return "admin/stringing/dashboard";
    }
}
