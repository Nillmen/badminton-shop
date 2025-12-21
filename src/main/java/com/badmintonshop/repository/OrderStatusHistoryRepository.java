package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    // Hàm này dùng để lấy lịch sử cho API /history bro vừa thêm
    List<OrderStatusHistory> findByOrder_OrderId(Long orderId);
}