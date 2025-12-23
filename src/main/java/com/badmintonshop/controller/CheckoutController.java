package com.badmintonshop.controller;

import com.badmintonshop.dto.cart.CartResponse;
import com.badmintonshop.dto.order.OrderRequest;
import com.badmintonshop.dto.order.OrderResponse;
import com.badmintonshop.entity.User;
import com.badmintonshop.entity.enums.PaymentMethod;
import com.badmintonshop.repository.UserRepository;
import com.badmintonshop.security.CustomOAuth2User;
import com.badmintonshop.security.CustomUserDetails;
import com.badmintonshop.service.CartService;
import com.badmintonshop.service.OrderService;
import com.badmintonshop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final VNPayService vnPayService;
    
    private static final String GUEST_SESSION_KEY = "GUEST_CART_SESSION";

    /**
     * Show checkout page
     */
    @GetMapping
    public String showCheckout(Model model, HttpSession session) {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return "redirect:/login?redirect=/checkout";
        }

        User user = userRepository.findById(userId).orElse(null);
        
        CartResponse cart = cartService.getCartResponse(userId);
        
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("user", user);
        model.addAttribute("cart", cart);
        
        return "checkout/index";
    }

    /**
     * Place order
     */
    @PostMapping("/place")
    public String placeOrder(
            @RequestParam String recipientName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam(required = false) String ward,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam(defaultValue = "COD") String paymentMethod,
            @RequestParam(required = false) String customerNotes,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            String sessionId = (String) session.getAttribute(GUEST_SESSION_KEY);
            PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
            
            OrderRequest orderRequest = OrderRequest.builder()
                    .recipientName(recipientName)
                    .phone(phone)
                    .address(address)
                    .ward(ward)
                    .district(district)
                    .city(city)
                    .paymentMethod(method)
                    .customerNotes(customerNotes)
                    .build();

            OrderResponse order = orderService.createOrder(userId, sessionId, orderRequest);
            
            // Clear guest session
            session.removeAttribute(GUEST_SESSION_KEY);
            
            log.info("Order created successfully: {}", order.getOrderNumber());
            
            // Handle different payment methods
            if (method == PaymentMethod.VNPAY) {
                // Redirect to VNPay payment page
                String ipAddress = getClientIp(request);
                String paymentUrl = vnPayService.createPaymentUrl(order.getOrderId(), ipAddress);
                log.info("Redirecting to VNPay: {}", paymentUrl);
                return "redirect:" + paymentUrl;
                
            } else if (method == PaymentMethod.BANK_TRANSFER) {
                // Redirect to bank transfer info page
                return "redirect:/checkout/bank-transfer?orderNumber=" + order.getOrderNumber();
                
            } else {
                // COD - redirect to success
                return "redirect:/checkout/success?orderNumber=" + order.getOrderNumber();
            }
            
        } catch (Exception e) {
            log.error("Error placing order: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi đặt hàng: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    /**
     * Order success page
     */
    @GetMapping("/success")
    public String showSuccess(@RequestParam String orderNumber, Model model) {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("orderNumber", orderNumber);
        return "checkout/success";
    }

    // ===== HELPER =====
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUserId();
        }
        
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Bank transfer info page
     */
    @GetMapping("/bank-transfer")
    public String showBankTransfer(@RequestParam String orderNumber, Model model) {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("orderNumber", orderNumber);
        // Bank account info - hardcoded for demo
        model.addAttribute("bankName", "Vietcombank");
        model.addAttribute("accountNumber", "1234567890");
        model.addAttribute("accountName", "BADMINTON SHOP");
        model.addAttribute("transferContent", "Thanh toan " + orderNumber);
        
        return "checkout/bank-transfer";
    }
}
