package com.badmintonshop.controller.admin;

import com.badmintonshop.entity.enums.ApplicableTo;
import com.badmintonshop.entity.enums.CouponType;
import com.badmintonshop.service.CouponService;
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
 * Admin Web Controller for Coupon Management Page
 */
@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_STAFF') or hasAuthority('coupons.view')")
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * Display coupons management page
     * GET /admin/coupons
     */
    @GetMapping
    public String listCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CouponType type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        model.addAttribute("coupons", couponService.searchCoupons(keyword, type, status, pageable));
        model.addAttribute("couponTypes", CouponType.values());
        model.addAttribute("applicableToOptions", ApplicableTo.values());

        return "admin/coupons";
    }
}
