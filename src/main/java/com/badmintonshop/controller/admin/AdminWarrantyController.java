package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.response.WarrantyResponse;
import com.badmintonshop.entity.enums.WarrantyStatus;
import com.badmintonshop.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/warranties")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALE_STAFF')")
public class AdminWarrantyController {

    private final WarrantyService warrantyService;

    @GetMapping
    public String listWarranties(Model model) {
        model.addAttribute("warranties", warrantyService.getAllWarranties());
        return "admin/warranties/index";
    }

    @GetMapping("/{id}")
    public String viewWarranty(@PathVariable Long id, Model model) {
        WarrantyResponse warranty = warrantyService.getWarrantyById(id);
        model.addAttribute("warranty", warranty);
        model.addAttribute("statuses", WarrantyStatus.values());
        return "admin/warranties/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam WarrantyStatus status,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            warrantyService.updateStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Warranty status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/warranties/" + id;
    }
}
