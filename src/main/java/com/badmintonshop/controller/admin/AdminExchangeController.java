package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.response.ExchangeResponse;
import com.badmintonshop.entity.enums.ExchangeStatus;
import com.badmintonshop.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/exchanges")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALE_STAFF')")
public class AdminExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping
    public String listExchanges(Model model) {
        model.addAttribute("exchanges", exchangeService.getAllExchanges());
        return "admin/exchanges/index";
    }

    @GetMapping("/{id}")
    public String viewExchange(@PathVariable Long id, Model model) {
        ExchangeResponse exchange = exchangeService.getExchangeById(id);
        model.addAttribute("exchange", exchange);
        model.addAttribute("statuses", ExchangeStatus.values());
        return "admin/exchanges/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam ExchangeStatus status,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            exchangeService.updateStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Exchange status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/exchanges/" + id;
    }
}
