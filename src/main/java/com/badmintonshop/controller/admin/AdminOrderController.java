package com.badmintonshop.controller.admin;

import com.badmintonshop.dto.DTOMapper;
import com.badmintonshop.dto.order.OrderResponse;
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final DTOMapper dtoMapper;
    private final com.badmintonshop.service.OrderService orderService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestParam(required = false) OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findAllByStatusWithDetails(status);
        } else {
            orders = orderRepository.findAllWithDetails();
        }
        return ResponseEntity.ok(orders.stream()
                .map(dtoMapper::toOrderResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(dtoMapper::toOrderResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        // Assuming admin ID is retrieved from security context, here passing null or
        // placeholder for now
        // In real app: Long staffId = ((StaffUserDetails)
        // SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        Long staffId = 1L; // Fallback to system/admin ID
        orderService.updateOrderStatus(id, status, "Admin updated status to " + status, staffId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/history")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Object>> getOrderHistory(@PathVariable Long id) {
        // Stub: Return OrderStatusHistory list. Assuming logic exists or empty list.
        // In real app: return
        // orderRepository.findById(id).get().getStatusHistories()...
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{id}/tracking")
    @Transactional
    public ResponseEntity<Void> updateTracking(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> trackingInfo) {
        orderRepository.findById(id).orElseThrow();
        // Stub: Update tracking info
        // order.setTrackingNumber(trackingInfo.get("number"));
        // orderRepository.save(order);
        return ResponseEntity.ok().build();
    }
}
