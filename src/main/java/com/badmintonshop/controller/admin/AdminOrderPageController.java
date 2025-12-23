package com.badmintonshop.controller.admin;

import com.badmintonshop.entity.Order;
import com.badmintonshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin Order Page Controller (renders HTML pages)
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderPageController {

    private final OrderRepository orderRepository;

    /**
     * Order list page
     */
    @GetMapping
    public String orderListPage() {
        return "admin/orders/index";
    }

    /**
     * Order detail page
     */
    @GetMapping("/{id}")
    public String orderDetailPage(@PathVariable Long id, Model model) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        
        model.addAttribute("order", order);
        return "admin/orders/detail";
    }
}
