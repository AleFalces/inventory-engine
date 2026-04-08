package com.omnicore.inventory_engine.domain.repository;

import com.omnicore.inventory_engine.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByTenantId(String tenantId);

    Optional<Product> findByTenantIdAndSku(String tenantId, String sku);

    Optional<Product> findByTenantIdAndId(String tenantId, Long id);
}
