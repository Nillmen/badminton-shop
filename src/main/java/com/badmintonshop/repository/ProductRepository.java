package com.badmintonshop.repository;

import com.badmintonshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants")
    java.util.List<Product> findAllWithVariants();
}