package com.badmintonshop.controller;

import com.badmintonshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller // Use Controller for potential redirects, or RestController if just API
@RequestMapping("/payment")
@RequiredArgsConstructor
public class VnPayReturnController {

    private final PaymentService paymentService;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleVnpayCallback(params);
            // Redirect to a success page on the frontend/home
            // Assuming there is a view or route for order success
            return "redirect:/orders?payment=success";
        } catch (Exception e) {
            // Redirect to error page
            return "redirect:/orders?payment=error&msg=" + e.getMessage();
        }
    }
}
