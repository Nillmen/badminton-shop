package com.badmintonshop.service;

import com.badmintonshop.entity.Order;
import com.badmintonshop.entity.OrderStatusHistory;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.User;
import com.badmintonshop.entity.enums.ChangedByType;
import com.badmintonshop.entity.enums.OrderStatus;
import com.badmintonshop.exception.ResourceNotFoundException;
import com.badmintonshop.repository.OrderStatusHistoryRepository;
import com.badmintonshop.repository.StaffRepository;
import com.badmintonshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; // Thêm import này

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository historyRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

    /**
     * Lấy lịch sử thay đổi trạng thái của một đơn hàng - THÊM VÀO ĐÂY
     */
    public List<OrderStatusHistory> getHistoryByOrderId(Long orderId) {
        // Sử dụng historyRepository đã khai báo ở trên
        return historyRepository.findByOrder_OrderId(orderId);
    }

    /**
     * Ghi lại sự thay đổi trạng thái đơn hàng do User/Customer thực hiện
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(Order order, OrderStatus newStatus, String notes) {
        String currentStatusName = getFromStatus(order, newStatus);
        if (order.getOrderId() != null && currentStatusName != null && currentStatusName.equals(newStatus.name())) {
            return;
        }
        OrderStatus oldStatus = currentStatusName != null ? OrderStatus.valueOf(currentStatusName) : null;
        User userEntity = order.getUser();

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus.name())
                .notes(notes)
                .changedByType(ChangedByType.CUSTOMER)
                .user(userEntity)
                .changedAt(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }

    /**
     * Ghi lại sự thay đổi trạng thái đơn hàng do Admin/Staff thực hiện
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChangeByStaff(Order order, OrderStatus oldStatus, OrderStatus newStatus, String notes, Long staffId) {
        if (oldStatus == newStatus) {
            return;
        }
        Staff staffEntity = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff không tồn tại khi ghi log lịch sử."));

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .notes(notes)
                .changedByType(ChangedByType.STAFF)
                .staff(staffEntity)
                .changedAt(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }

    private String getFromStatus(Order order, OrderStatus newStatus) {
        if (order.getOrderId() == null) return null;
        String currentStatus = order.getStatus() != null ? order.getStatus().name() : null;
        if (currentStatus == null && newStatus == OrderStatus.PENDING) return null;
        return currentStatus;
    }

    // 🛑 XÓA BỎ TOÀN BỘ PHẦN KHAI BÁO CLASS LỒNG NHAU Ở ĐÂY 🛑
}