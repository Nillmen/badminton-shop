package com.badmintonshop.repository;

import com.badmintonshop.entity.Warranty;
import com.badmintonshop.entity.enums.WarrantyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    List<Warranty> findByStatus(WarrantyStatus status);

    boolean existsByOrderItem_OrderItemId(Long orderItemId);

    List<Warranty> findByOrder_OrderNumber(String orderNumber);

    Optional<Warranty> findByWarrantyNumber(String warrantyNumber);

    List<Warranty> findByOrder_User_UserIdOrderByCreatedAtDesc(Long userId);
}
