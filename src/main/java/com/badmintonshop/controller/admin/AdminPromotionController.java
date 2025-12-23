package com.badmintonshop.controller.admin;

import com.badmintonshop.entity.enums.DiscountType;
import com.badmintonshop.entity.enums.PromotionType;
import com.badmintonshop.service.CategoryService;
import com.badmintonshop.service.ProductService;
import com.badmintonshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin Web Controller for Promotion Management Page
 */
@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('promotions.view')")
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * Display promotions management page
     * GET /admin/promotions
     */
    @GetMapping
    public String listPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PromotionType type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        model.addAttribute("promotions", promotionService.searchPromotions(keyword, type, status, pageable));
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("discountTypes", DiscountType.values());
        
        // Add products and categories for multi-select dropdowns
        model.addAttribute("allProducts", productService.getAllActiveProductsBasicInfo());
        model.addAttribute("allCategories", categoryService.getAllActiveCategories());

        return "admin/promotions";
    }
}
