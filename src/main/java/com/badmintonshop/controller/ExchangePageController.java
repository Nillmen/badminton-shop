package com.badmintonshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/exchange")
public class ExchangePageController {

    /**
     * Display exchange policy page
     * GET /exchange
     */
    @GetMapping
    public String showExchangePage() {
        return "shop/exchange";
    }
}
