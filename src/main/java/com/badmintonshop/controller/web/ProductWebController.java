package com.badmintonshop.controller.web;

import com.badmintonshop.entity.Product;
import com.badmintonshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductRepository productRepository;

    @GetMapping({ "/", "/products" })
    public String viewProducts(Model model) {
        // Lay 20 san pham moi nhat cho UI test, fetch kem Variants de tranh
        // LazyInitException
        List<Product> products = productRepository.findAllWithVariants();
        // Trong thuc te nen phan trang, nhung day chi la UI de test flow Member 3
        model.addAttribute("products", products);
        return "products";
    }
}
