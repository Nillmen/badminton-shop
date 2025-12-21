package com.badmintonshop.controller.web;

import com.badmintonshop.dto.CartResponse;
import com.badmintonshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CartWebController {

    private final CartService cartService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        // Lấy cart hiện tại (nếu chưa login giả sử null hoặc xử lý sau vói cookie, ở
        // đây gọi service)
        // Lưu ý: service.getCart() hiện tại đang lấy theo User login hoặc Session.
        // Trong context WebController đơn giản này, ta sẽ để Cart page tự fetch API
        // bằng JS
        // HOẶC trả về view rỗng để JS handle cho dynamic.
        // Tuy nhiên để tốt cho SEO/User, ta fetch data sơ bộ nếu có thể.
        // Build này ta sẽ làm Client-Side Rendering bên trong Thymeleaf cho Cart để dễ
        // sync với API.
        return "cart";
    }
}
