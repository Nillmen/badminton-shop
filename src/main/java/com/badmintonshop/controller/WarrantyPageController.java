package com.badmintonshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/warranty")
public class WarrantyPageController {

    /**
     * Display warranty policy page
     * GET /warranty
     */
    @GetMapping
    public String showWarrantyPage() {
        return "shop/warranty";
    }
}
