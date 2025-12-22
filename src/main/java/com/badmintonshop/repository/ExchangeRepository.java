package com.badmintonshop.repository;

import com.badmintonshop.entity.Exchange;
import com.badmintonshop.entity.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    List<Exchange> findByStatus(ExchangeStatus status);

    boolean existsByOrderItem_OrderItemId(Long orderItemId);

    List<Exchange> findByOrder_OrderNumber(String orderNumber);

    Optional<Exchange> findByExchangeNumber(String exchangeNumber);

    List<Exchange> findByOrder_User_UserIdOrderByCreatedAtDesc(Long userId);
}
