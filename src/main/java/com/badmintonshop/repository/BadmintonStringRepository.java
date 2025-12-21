package com.badmintonshop.repository;

import com.badmintonshop.entity.BadmintonString;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadmintonStringRepository extends JpaRepository<BadmintonString, Long> {
}