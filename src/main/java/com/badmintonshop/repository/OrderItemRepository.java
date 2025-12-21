package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.enums.StringingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Tìm các item cần đan vợt nhưng chưa phân công thợ (để Admin assign)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.hasStringingService = true AND oi.stringingStatus = 'pending'")
    List<OrderItem> findItemsNeedingStringing();

    // Tìm các item đang được phân công cho thợ A (để thợ vào xem task của mình)
    List<OrderItem> findByAssignedStaff_StaffIdAndStringingStatus(Long staffId, StringingStatus status);
}