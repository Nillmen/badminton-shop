package com.badmintonshop.controller.admin;

import com.badmintonshop.service.FAQService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Admin page controller for FAQs
 */
@Controller
@RequestMapping("/admin/faq")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('faqs.view')")
public class AdminFAQController {

    private final FAQService faqService;

    /**
     * Display FAQs management page
     * GET /admin/faq
     */
    @GetMapping
    public String listFAQs(Model model) {
        model.addAttribute("categories", faqService.getCategories());
        return "admin/faqs";
    }
}
