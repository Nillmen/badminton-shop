package com.badmintonshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/orders")
public class AdminWebController {

    @GetMapping
    public String listOrders() {
        return "admin/orders/list";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId) {
        return "admin/orders/detail";
    }
}
