package com.badmintonshop.controller.admin;

import com.badmintonshop.entity.enums.BannerPosition;
import com.badmintonshop.service.BannerService;
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
 * Admin Web Controller for Banner Management Page
 */
@Controller
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('banners.view')")
public class AdminBannerController {

    private final BannerService bannerService;

    /**
     * Display banners management page
     * GET /admin/banners
     */
    @GetMapping
    public String listBanners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BannerPosition position,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());

        model.addAttribute("banners", bannerService.searchBanners(keyword, position, status, pageable));
        model.addAttribute("positions", BannerPosition.values());

        return "admin/banners";
    }
}

