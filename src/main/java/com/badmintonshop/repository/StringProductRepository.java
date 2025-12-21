package com.badmintonshop.repository;

import com.badmintonshop.entity.StringProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StringProductRepository extends JpaRepository<StringProduct, Long> {
    // Nếu sau này Member 4 cần tìm cước theo hãng, bro có thể thêm:
    // List<StringProduct> findByBrand_BrandId(Long brandId);
}