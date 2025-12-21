package com.badmintonshop.service;

import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.repository.OrderItemRepository;
import com.badmintonshop.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Cái này để tự động "nhúng" (autowire) mấy cái Repository bên dưới
public class StringingService {

    // Khai báo 2 cái này để Spring biết đường gọi database
    private final OrderItemRepository orderItemRepository;
    private final StaffRepository staffRepository;

    // Admin phân công thợ đan
    @Transactional
    public void assignStaffToItem(Long orderItemId, Long staffId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        item.assignStringingStaff(staff); // Gọi hàm helper trong Entity
        orderItemRepository.save(item);
    }

    // Thợ cập nhật trạng thái "Đã đan xong"
    @Transactional
    public void completeStringing(Long orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        item.completeStringing(); // Gọi hàm helper update timestamp
        orderItemRepository.save(item);

        // Logic phụ (nếu cần): Check xem tất cả item trong Order đó đã đan xong chưa
    }
}