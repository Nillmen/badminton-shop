package com.badmintonshop.controller;

import com.badmintonshop.dto.request.ExchangeRequest;
import com.badmintonshop.dto.request.WarrantyRequest;
import com.badmintonshop.entity.enums.ExchangeReason;
import com.badmintonshop.entity.enums.IssueType;
import com.badmintonshop.service.ExchangeService;
import com.badmintonshop.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/requests")
@RequiredArgsConstructor
public class ClientRequestController {

    private final ExchangeService exchangeService;
    private final WarrantyService warrantyService; // Re-added
    private final com.badmintonshop.repository.UserRepository userRepository;

    @GetMapping
    public String listRequests(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            com.badmintonshop.entity.User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("exchanges", exchangeService.getExchangesByUserId(user.getUserId()));
            model.addAttribute("warranties", warrantyService.getWarrantiesByUserId(user.getUserId()));
        }
        return "account/request/index";
    }

    @GetMapping("/exchange/new")
    public String newExchangeForm(@RequestParam Long orderItemId, Model model) {
        ExchangeRequest request = new ExchangeRequest();
        request.setOrderItemId(orderItemId);
        model.addAttribute("exchangeRequest", request);
        model.addAttribute("reasons", ExchangeReason.values());
        return "account/request/create_exchange";
    }

    @PostMapping("/exchange")
    public String createExchange(@ModelAttribute ExchangeRequest request, RedirectAttributes redirectAttributes) {
        try {
            exchangeService.createExchange(request);
            redirectAttributes.addFlashAttribute("successMessage", "Exchange request submitted successfully.");
            return "redirect:/account/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/account/requests/exchange/new?orderItemId=" + request.getOrderItemId();
        }
    }

    @GetMapping("/warranty/new")
    public String newWarrantyForm(@RequestParam Long orderItemId, Model model) {
        WarrantyRequest request = new WarrantyRequest();
        request.setOrderItemId(orderItemId);
        model.addAttribute("warrantyRequest", request);
        model.addAttribute("issueTypes", IssueType.values());
        return "account/request/create_warranty";
    }

    @PostMapping("/warranty")
    public String createWarranty(@ModelAttribute WarrantyRequest request, RedirectAttributes redirectAttributes) {
        try {
            warrantyService.createWarranty(request);
            redirectAttributes.addFlashAttribute("successMessage", "Warranty claim submitted successfully.");
            return "redirect:/account/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/account/requests/warranty/new?orderItemId=" + request.getOrderItemId();
        }
    }
}
