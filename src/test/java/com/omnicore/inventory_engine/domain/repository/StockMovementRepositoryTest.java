package com.omnicore.inventory_engine.domain.repository;

import com.omnicore.inventory_engine.TestcontainersConfiguration;
import com.omnicore.inventory_engine.domain.entity.StockMovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class StockMovementRepositoryTest {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
    }

    // ─── Test 1: Persistencia con todos los campos ─────────────────────────────

    @Test
    void shouldPersistMovementWithAllFields() {
        var movement = StockMovement.builder()
                .tenantId("tenant-a")
                .productSku("SKU-001")
                .delta(-10)
                .reason("OUT")
                .stockBefore(100)
                .stockAfter(90)
                .createdAt(Instant.now())
                .build();

        var saved = stockMovementRepository.save(movement);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDelta()).isEqualTo(-10);
        assertThat(saved.getStockBefore()).isEqualTo(100);
        assertThat(saved.getStockAfter()).isEqualTo(90);
        assertThat(saved.getReason()).isEqualTo("OUT");
    }

    // ─── Test 2: Buscar movimientos por tenant + SKU ───────────────────────────

    @Test
    void shouldFindAllMovementsByTenantAndSku() {
        stockMovementRepository.saveAll(List.of(
                buildMovement("tenant-a", "SKU-001", -10),
                buildMovement("tenant-a", "SKU-001",  50),
                buildMovement("tenant-a", "SKU-002", -5)   // otro SKU — no debe aparecer
        ));

        var result = stockMovementRepository.findAllByTenantIdAndProductSku("tenant-a", "SKU-001");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StockMovement::getProductSku).containsOnly("SKU-001");
    }

    // ─── Test 3: Aislamiento entre tenants ────────────────────────────────────

    @Test
    void shouldIsolateMovementsBetweenTenants() {
        stockMovementRepository.saveAll(List.of(
                buildMovement("tenant-a", "SKU-001", -10),
                buildMovement("tenant-b", "SKU-001",  20)  // otro tenant — no debe aparecer
        ));

        var result = stockMovementRepository.findAllByTenantIdAndProductSku("tenant-a", "SKU-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo("tenant-a");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private StockMovement buildMovement(String tenantId, String sku, int delta) {
        return StockMovement.builder()
                .tenantId(tenantId)
                .productSku(sku)
                .delta(delta)
                .reason("TEST")
                .stockBefore(100)
                .stockAfter(100 + delta)
                .createdAt(Instant.now())
                .build();
    }
}
