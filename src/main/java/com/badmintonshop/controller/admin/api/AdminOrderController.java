package com.badmintonshop.controller.admin.api;

import com.badmintonshop.dto.order.*;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.entity.enums.PaymentStatus;
import com.badmintonshop.security.StaffUserDetails;
import com.badmintonshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Admin Order Management
 * SALE_STAFF: Full access, WAREHOUSE_STAFF: View only
 */
@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SALE_STAFF', 'WAREHOUSE_STAFF')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Get all orders with optional filters
     * GET /admin/api/orders
     */
    @GetMapping
    public ResponseEntity<Page<OrderListDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Page<OrderListDTO> orders;
        if (paymentStatus != null || fromDate != null || toDate != null || search != null) {
            // Complex filter
            orders = orderService.getAllOrdersWithFilters(
                    status, paymentStatus, fromDate, toDate, search,
                    PageRequest.of(page, size, sort));
        } else {
            // Simple filter
            orders = orderService.getAllOrders(status, PageRequest.of(page, size, sort));
        }

        return ResponseEntity.ok(orders);
    }

    /**
     * Get order details by ID
     * GET /admin/api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            OrderResponse order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Update order status
     * PUT /admin/api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal StaffUserDetails staffDetails) {

        try {
            Long staffId = staffDetails != null ? staffDetails.getStaffId() : null;
            OrderResponse order = orderService.updateOrderStatus(id, request, staffId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Get order status history
     * GET /admin/api/orders/{id}/history
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getOrderHistory(@PathVariable Long id) {
        try {
            List<OrderStatusHistoryDTO> history = orderService.getOrderHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Update order tracking info
     * PUT /admin/api/orders/{id}/tracking
     */
    @PutMapping("/{id}/tracking")
    public ResponseEntity<?> updateOrderTracking(
            @PathVariable Long id,
            @RequestBody OrderTrackingDTO tracking) {

        try {
            OrderResponse order = orderService.updateOrderTracking(id, tracking);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error updating order tracking: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Get order statistics (for dashboard)
     * GET /admin/api/orders/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats() {
        // Basic stats - can be expanded
        return ResponseEntity.ok(Map.of(
                "pending", orderService.getAllOrders(OrderStatus.PENDING, PageRequest.of(0, 1)).getTotalElements(),
                "confirmed", orderService.getAllOrders(OrderStatus.CONFIRMED, PageRequest.of(0, 1)).getTotalElements(),
                "processing", orderService.getAllOrders(OrderStatus.PROCESSING, PageRequest.of(0, 1)).getTotalElements(),
                "shipped", orderService.getAllOrders(OrderStatus.SHIPPED, PageRequest.of(0, 1)).getTotalElements()
        ));
    }
}
