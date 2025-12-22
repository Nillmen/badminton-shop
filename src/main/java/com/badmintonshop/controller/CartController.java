package com.badmintonshop.controller;

import com.badmintonshop.entity.User;
import com.badmintonshop.repository.StringProductRepository;
import com.badmintonshop.repository.StringingServiceRepository;
import com.badmintonshop.repository.UserRepository;
import com.badmintonshop.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@Controller("cartPageController")
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final StringingServiceRepository stringingServiceRepository;
    private final StringProductRepository stringProductRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String viewCart(Model model, Principal principal, HttpSession session) {
        try {
            User user = getUser(principal);
            String sessionId = session.getId();

            // Use CartResponse for promotion data
            if (user != null) {
                model.addAttribute("cart", cartService.getCartResponse(user.getUserId()));
            } else {
                model.addAttribute("cart", cartService.getGuestCartResponse(sessionId));
            }
            model.addAttribute("stringingServices", stringingServiceRepository.findAllActive());
            model.addAttribute("stringProducts", stringProductRepository.findAllActive());

            return "cart/index";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "cart/index";
        }
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") int quantity,
            Principal principal, HttpSession session) {
        User user = getUser(principal);
        cartService.addToCart(user, session.getId(), productId, variantId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update-stringing")
    public String updateStringing(@RequestParam Long cartItemId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long stringId,
            @RequestParam(required = false) BigDecimal tension,
            @RequestParam(required = false) String notes) {
        try {
            cartService.updateStringingOption(cartItemId, serviceId, stringId, tension, notes);
            return "redirect:/cart";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/cart?error=" + e.getMessage();
        }
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long cartItemId) {
        cartService.removeItem(cartItemId);
        return "redirect:/cart";
    }

    private User getUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByEmailAndDeletedAtIsNull(principal.getName()).orElse(null);
    }
}
