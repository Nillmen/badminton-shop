package com.badmintonshop.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class OrderWebController {

    @GetMapping("/checkout")
    public String checkoutPage() {
        return "checkout";
    }

    @GetMapping("/orders")
    public String myOrdersPage() {
        return "my-orders";
    }

    @GetMapping("/orders/{orderNumber}")
    public String orderDetailPage(@PathVariable String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        return "order-detail";
    }
}
