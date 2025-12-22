package com.badmintonshop.controller;

import com.badmintonshop.dto.StringDTO;
import com.badmintonshop.dto.StringServiceDTO;
import com.badmintonshop.entity.User;
import com.badmintonshop.repository.UserRepository;
import com.badmintonshop.service.stringing.StringProductService;
import com.badmintonshop.service.stringing.StringingServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Public Controller for Stringing Services
 * - Viewing stringing services and string products is public
 * - Ordering requires authentication
 */
@Controller
@RequestMapping("/stringing")
@RequiredArgsConstructor
@Slf4j
public class StringingController {

    private final StringingServiceService stringingServiceService;
    private final StringProductService stringProductService;
    private final UserRepository userRepository;

    /**
     * Public stringing services page
     * GET /stringing
     */
    @GetMapping
    public String stringingPage(Model model, Principal principal) {
        log.info("Loading public stringing services page");

        List<StringServiceDTO> services = stringingServiceService.getAllActive();
        List<StringDTO> strings = stringProductService.getAllActive();

        model.addAttribute("services", services);
        model.addAttribute("strings", strings);
        model.addAttribute("isLoggedIn", principal != null);

        return "shop/stringing";
    }

    /**
     * Stringing order page - REQUIRES LOGIN
     * GET /stringing/order
     */
    @GetMapping("/order")
    public String orderPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login?redirect=/stringing/order";
        }

        log.info("Loading stringing order page for user: {}", principal.getName());

        User user = userRepository.findByEmailAndDeletedAtIsNull(principal.getName())
                .orElse(null);

        List<StringServiceDTO> services = stringingServiceService.getAllActive();
        List<StringDTO> strings = stringProductService.getAllActive();

        model.addAttribute("services", services);
        model.addAttribute("strings", strings);
        model.addAttribute("user", user);

        return "shop/stringing-order";
    }
}
