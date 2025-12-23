package com.badmintonshop.controller;

import com.badmintonshop.dto.request.ExchangeRequest;
import com.badmintonshop.dto.request.WarrantyRequest;
import com.badmintonshop.entity.enums.ExchangeReason;
import com.badmintonshop.entity.enums.IssueType;
import com.badmintonshop.service.ExchangeService;
import com.badmintonshop.service.FileStorageService;
import com.badmintonshop.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/account/requests")
@RequiredArgsConstructor
@Slf4j
public class ClientRequestController {

    private final ExchangeService exchangeService;
    private final WarrantyService warrantyService;
    private final FileStorageService fileStorageService;
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
    public String createExchange(@ModelAttribute ExchangeRequest request,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Handle image upload
            String imagesJson = fileStorageService.storeRequestImages(imageFiles);
            request.setImages(imagesJson);
            
            exchangeService.createExchange(request);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đổi hàng đã được gửi thành công!");
            return "redirect:/account/requests";
        } catch (Exception e) {
            log.error("Error creating exchange request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
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
    public String createWarranty(@ModelAttribute WarrantyRequest request,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Handle image upload
            String imagesJson = fileStorageService.storeRequestImages(imageFiles);
            request.setImages(imagesJson);
            
            warrantyService.createWarranty(request);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu bảo hành đã được gửi thành công!");
            return "redirect:/account/requests";
        } catch (Exception e) {
            log.error("Error creating warranty request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/account/requests/warranty/new?orderItemId=" + request.getOrderItemId();
        }
    }
}
