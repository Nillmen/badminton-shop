package com.badmintonshop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/api/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    @GetMapping
    public ResponseEntity<List<Object>> getAllPayments() {
        // Stub
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<Void> verifyPayment(@PathVariable Long id) {
        // Stub: Verify bank transfer
        return ResponseEntity.ok().build();
    }
}
