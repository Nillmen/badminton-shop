package com.badmintonshop.repository;

import com.badmintonshop.entity.OrderItem;
import com.badmintonshop.entity.Staff;
import com.badmintonshop.entity.enums.StringingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

        // Tìm các item có dịch vụ đan
        // Tìm các item có dịch vụ đan (Eager load Order & Staff)
        @Query(value = "SELECT oi FROM OrderItem oi JOIN FETCH oi.order LEFT JOIN FETCH oi.assignedStaff WHERE oi.hasStringingService = true", countQuery = "SELECT count(oi) FROM OrderItem oi WHERE oi.hasStringingService = true")
        Page<OrderItem> findByHasStringingServiceTrue(Pageable pageable);

        // Tìm các item theo trạng thái đan
        Page<OrderItem> findByHasStringingServiceTrueAndStringingStatus(StringingStatus status, Pageable pageable);

        // Tìm các item được gán cho staff cụ thể
        Page<OrderItem> findByAssignedStaff(Staff staff, Pageable pageable);

        // Tìm các item được gán cho staff theo trạng thái
        // Tìm các item được gán cho staff theo trạng thái (Eager load Order & Staff)
        @Query(value = "SELECT oi FROM OrderItem oi JOIN FETCH oi.order LEFT JOIN FETCH oi.assignedStaff WHERE oi.assignedStaff = :staff AND oi.stringingStatus = :status", countQuery = "SELECT COUNT(oi) FROM OrderItem oi WHERE oi.assignedStaff = :staff AND oi.stringingStatus = :status")
        Page<OrderItem> findByAssignedStaffAndStringingStatus(
                        @org.springframework.data.repository.query.Param("staff") Staff staff,
                        @org.springframework.data.repository.query.Param("status") StringingStatus status,
                        Pageable pageable);

        @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.assignedStaff = :staff AND oi.stringingStatus IN :statuses")
        long countByAssignedStaffAndStringingStatusIn(
                        @org.springframework.data.repository.query.Param("staff") Staff staff,
                        @org.springframework.data.repository.query.Param("statuses") java.util.Collection<StringingStatus> statuses);
}
