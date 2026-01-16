package com.workflow.hrms.repository;

import com.workflow.hrms.entity.RefProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefProductRepository extends JpaRepository<RefProduct, Long> {
    java.util.Optional<RefProduct> findByProductName(String productName);
}
