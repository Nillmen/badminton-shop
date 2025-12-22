package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.request.StringProductRequest;
import com.badmintonshop.dto.request.StringingServiceRequest;
import com.badmintonshop.dto.response.StringProductResponse;
import com.badmintonshop.dto.response.StringingServiceResponse;
import com.badmintonshop.repository.BrandRepository;
import com.badmintonshop.service.StringProductService;
import com.badmintonshop.service.StringingServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminStringManagementController {

    private final StringProductService stringProductService;
    private final StringingServiceService stringingServiceService;
    private final BrandRepository brandRepository;

    // --- String Products ---

    @GetMapping("/products/strings")
    public String listStrings(Model model) {
        model.addAttribute("strings", stringProductService.getAll());
        return "admin/products/strings/index";
    }

    @GetMapping("/products/strings/new")
    public String newStringForm(Model model) {
        model.addAttribute("stringProduct", new StringProductRequest());
        model.addAttribute("brands", brandRepository.findAll());
        return "admin/products/strings/form";
    }

    @PostMapping("/products/strings")
    public String createString(@Valid @ModelAttribute("stringProduct") StringProductRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("brands", brandRepository.findAll());
            return "admin/products/strings/form";
        }
        try {
            stringProductService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm cước mới thành công");
        } catch (Exception e) {
            model.addAttribute("brands", brandRepository.findAll());
            model.addAttribute("error", e.getMessage());
            return "admin/products/strings/form";
        }
        return "redirect:/admin/products/strings";
    }

    @GetMapping("/products/strings/{id}/edit")
    public String editStringForm(@PathVariable Long id, Model model) {
        StringProductResponse response = stringProductService.getById(id);
        // Map response to request for form binding (manual or use mapper if exists)
        // Since fields overlap mostly, we can populate manually or modify DTOs.
        // For simplicity, we'll create a request object and populate it.
        StringProductRequest request = new StringProductRequest();
        request.setName(response.getName());
        request.setSku(response.getSku());
        request.setDescription(response.getDescription());
        request.setBrandId(response.getBrandId());
        request.setStringType(response.getStringType());
        request.setGauge(response.getGauge());
        request.setMaterial(response.getMaterial());
        request.setDurabilityRating(response.getDurabilityRating());
        request.setRepulsionRating(response.getRepulsionRating());
        request.setControlRating(response.getControlRating());
        request.setHittingSoundRating(response.getHittingSoundRating());
        request.setRecommendedTensionMin(response.getRecommendedTensionMin());
        request.setRecommendedTensionMax(response.getRecommendedTensionMax());
        request.setRetailPrice(response.getRetailPrice());
        request.setLengthPerRoll(response.getLengthPerRoll());
        request.setQuantityInStock(response.getQuantityInStock());
        request.setColor(response.getColor());
        request.setImageUrl(response.getImageUrl());
        request.setIsActive(response.getIsActive());

        model.addAttribute("stringProduct", request);
        model.addAttribute("brands", brandRepository.findAll());
        model.addAttribute("id", id);
        return "admin/products/strings/form";
    }

    @PostMapping("/products/strings/{id}")
    public String updateString(@PathVariable Long id,
            @Valid @ModelAttribute("stringProduct") StringProductRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("brands", brandRepository.findAll());
            model.addAttribute("id", id);
            return "admin/products/strings/form";
        }
        try {
            stringProductService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật cước thành công");
        } catch (Exception e) {
            model.addAttribute("brands", brandRepository.findAll());
            model.addAttribute("id", id);
            model.addAttribute("error", e.getMessage());
            return "admin/products/strings/form";
        }
        return "redirect:/admin/products/strings";
    }

    @PostMapping("/products/strings/{id}/delete")
    public String deleteString(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stringProductService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa cước thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/products/strings";
    }

    // --- Stringing Services ---

    @GetMapping("/services/stringing")
    public String listServices(Model model) {
        model.addAttribute("services", stringingServiceService.getAll());
        return "admin/services/stringing/index";
    }

    @GetMapping("/services/stringing/new")
    public String newServiceForm(Model model) {
        model.addAttribute("service", new StringingServiceRequest());
        return "admin/services/stringing/form";
    }

    @PostMapping("/services/stringing")
    public String createService(@Valid @ModelAttribute("service") StringingServiceRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/services/stringing/form";
        }
        try {
            stringingServiceService.create(request);
            redirectAttributes.addFlashAttribute("success", "Thêm dịch vụ mới thành công");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/services/stringing/form";
        }
        return "redirect:/admin/services/stringing";
    }

    @GetMapping("/services/stringing/{id}/edit")
    public String editServiceForm(@PathVariable Long id, Model model) {
        StringingServiceResponse response = stringingServiceService.getById(id);
        StringingServiceRequest request = new StringingServiceRequest();
        request.setServiceName(response.getServiceName());
        request.setServiceType(response.getServiceType());
        request.setDescription(response.getDescription());
        request.setBasePrice(response.getBasePrice());
        request.setEstimatedTimeMinutes(response.getEstimatedTimeMinutes());
        request.setIsActive(response.getIsActive());

        model.addAttribute("service", request);
        model.addAttribute("id", id);
        return "admin/services/stringing/form";
    }

    @PostMapping("/services/stringing/{id}")
    public String updateService(@PathVariable Long id,
            @Valid @ModelAttribute("service") StringingServiceRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            return "admin/services/stringing/form";
        }
        try {
            stringingServiceService.update(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật dịch vụ thành công");
        } catch (Exception e) {
            model.addAttribute("id", id);
            model.addAttribute("error", e.getMessage());
            return "admin/services/stringing/form";
        }
        return "redirect:/admin/services/stringing";
    }

    @PostMapping("/services/stringing/{id}/delete")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            stringingServiceService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa dịch vụ thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/services/stringing";
    }
}
