package com.badmintonshop.api.controller.admin;

import com.badmintonshop.dto.OrderStatusUpdateRequest;
import com.badmintonshop.dto.OrderTrackingUpdateRequest; // CẦN TẠO DTO NÀY
import com.badmintonshop.dto.OrderListResponse;
import com.badmintonshop.dto.OrderDetailResponse; // Sử dụng DTO này cho detail/response
import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.badmintonshop.service.OrderStatusHistoryService;
import java.util.List;

@RestController
@RequestMapping("/admin/api/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    private final OrderStatusHistoryService orderStatusHistoryService;

    // --- Giả lập Admin ID (Tạm thời) ---
    private Long getCurrentAdminId() {
        return 1L; // Staff/Admin ID 1
    }
    // -----------------------------------

    // 1. GET /admin/api/orders (List Orders with Filtering)
    @GetMapping
    public ResponseEntity<List<OrderListResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword) {

        // FIX: Sử dụng DTO được trả về từ OrderService.findAllOrders() hoặc hàm tương
        // tự
        // Trong thực tế cần update Service để hỗ trợ filter trả về DTO.
        // Tạm thời dùng findAllOrders() đã convert DTO nếu chưa có filter logic deep.

        List<OrderListResponse> ordersDto = orderService.findAllOrders();

        // TODO: Nếu muốn filter, cần implement method findOrdersByStatusAndKeyword
        // trong Service trả về List<Order>
        // sau đó stream map sang DTO ở đây hoặc trong Service.
        // Hiện tại findAllOrders() trong Service đã trả về List<OrderListResponse>.

        return ResponseEntity.ok(ordersDto);
    }

    // 2. PUT /admin/api/orders/{id}/status (Update Status) - [FIX]
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) { // 🛑 DÙNG REQUEST DTO

        Long adminId = getCurrentAdminId();

        // 1. Chuyển String status từ Request thành Enum
        OrderStatus newStatus = OrderStatus.valueOf(request.getNewStatus().toUpperCase());

        // 2. Gọi Service
        Order updatedOrder = orderService.updateOrderStatus(
                orderId,
                newStatus,
                adminId,
                request.getStaffNote() // Lấy notes từ DTO
        );

        // 3. Trả về DTO chi tiết
        OrderDetailResponse responseDto = orderService.getOrderDetailForAdmin(updatedOrder.getOrderId());
        return ResponseEntity.ok(responseDto);
    }

    // 3. PUT /admin/api/orders/{id}/tracking (Update Tracking Info) - [FIX]
    @PutMapping("/{orderId}/tracking")
    public ResponseEntity<String> updateTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderTrackingUpdateRequest request) { // 🛑 DÙNG REQUEST DTO

        // Bro cần sửa lại OrderService.updateOrderTracking để nhận 3 tham số này
        orderService.updateOrderTracking(orderId, request.getTrackingNumber(), request.getCarrier());

        return ResponseEntity.ok("Tracking information updated successfully.");
    }

    // 4. GET /admin/api/orders/{id} (Order Detail) - GIỮ NGUYÊN
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        // Hàm này đã hoạt động
        OrderDetailResponse dto = orderService.getOrderDetailForAdmin(orderId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getOrderHistory(@PathVariable Long id) {
        // Trả về danh sách từ bảng OrderStatusHistory
        return ResponseEntity.ok(orderStatusHistoryService.getHistoryByOrderId(id));
    }
}