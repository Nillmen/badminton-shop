package com.badmintonshop.controller;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.User;
import com.badmintonshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ClientOrderController {

    private final OrderService orderService;
    private final com.badmintonshop.repository.UserRepository userRepository;
    private final com.badmintonshop.repository.ExchangeRepository exchangeRepository;
    private final com.badmintonshop.repository.WarrantyRepository warrantyRepository;

    @GetMapping
    public String myOrders(Model model, Authentication authentication,
            @RequestParam(defaultValue = "0") int page) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Order> orderPage = orderService.getUserOrders(user, page, 10);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());

        return "account/orders/index";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderService.getOrderForUser(id, user); // Only handles ownership check

        // Build status maps without modifying OrderItem entity
        java.util.Map<Long, Boolean> exchangedMap = new java.util.HashMap<>();
        java.util.Map<Long, Boolean> warrantyMap = new java.util.HashMap<>();

        for (com.badmintonshop.entity.OrderItem item : order.getItems()) {
            boolean hasExchange = exchangeRepository.existsByOrderItem_OrderItemId(item.getOrderItemId());
            exchangedMap.put(item.getOrderItemId(), hasExchange);

            boolean hasWarranty = warrantyRepository.existsByOrderItem_OrderItemId(item.getOrderItemId());
            warrantyMap.put(item.getOrderItemId(), hasWarranty);
        }

        model.addAttribute("order", order);
        model.addAttribute("exchangedMap", exchangedMap);
        model.addAttribute("warrantyMap", warrantyMap);
        return "account/orders/detail";
    }
}
