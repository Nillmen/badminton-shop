package com.badmintonshop.controller;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.User;
import com.badmintonshop.repository.UserRepository;
import com.badmintonshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/place")
    public String placeOrder(@RequestParam(required = false) String address,
            @RequestParam(required = false) String note,
            Principal principal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login"; // Force login
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(principal.getName())
                .orElse(null);

        if (user == null) {
            return "redirect:/login";
        }

        try {
            Order order = orderService.createOrderFromCart(user, session.getId(), address, note);
            redirectAttributes.addFlashAttribute("success", "Placed Order Successfully: " + order.getOrderNumber());
            // Redirect to cart or specific success page. Using cart for now to see message.
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error placing order: " + e.getMessage());
            return "redirect:/cart";
        }
    }
}
