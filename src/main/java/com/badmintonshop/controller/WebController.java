package com.badmintonshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/cart")
    public String cart() {
        return "cart/index";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "order/checkout";
    }

    @GetMapping("/orders")
    public String orderHistory() {
        return "order/history";
    }

    @GetMapping("/orders/detail")
    public String orderDetail() {
        return "order/detail";
    }

    @GetMapping("/test-shop")
    public String testShop() {
        return "mock-products";
    }
}
