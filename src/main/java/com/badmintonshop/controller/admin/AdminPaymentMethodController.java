package com.badmintonshop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/payment-methods")
@RequiredArgsConstructor
public class AdminPaymentMethodController {

    @GetMapping
    public ResponseEntity<List<Object>> getAllMethods() {
        // Stub
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "name", "COD", "enabled", true),
                Map.of("id", 2, "name", "VNPAY", "enabled", true)));
    }

    @PostMapping
    public ResponseEntity<Void> createMethod(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMethod(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMethod(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
}
