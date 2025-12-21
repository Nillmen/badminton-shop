package com.badmintonshop.repository;

import com.badmintonshop.entity.StringingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StringingServiceRepository extends JpaRepository<StringingService, Long> {
    // JpaRepository đã có sẵn findById, save, delete... cho bro dùng rồi
}