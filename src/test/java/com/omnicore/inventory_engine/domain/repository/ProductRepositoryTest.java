package com.omnicore.inventory_engine.domain.repository;

import com.omnicore.inventory_engine.TestcontainersConfiguration;
import com.omnicore.inventory_engine.domain.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // ─── Test 1: Persistencia básica ───────────────────────────────────────────

    @Test
    void shouldPersistProductWithAllFields() {
        var product = Product.builder()
                .tenantId("tenant-alpha")
                .sku("SKU-001")
                .name("Widget Pro")
                .description("High-quality widget")
                .stock(100)
                .build();

        var saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo("tenant-alpha");
        assertThat(saved.getSku()).isEqualTo("SKU-001");
        assertThat(saved.getStock()).isEqualTo(100);
    }

    // ─── Test 2: Aislamiento entre tenants ─────────────────────────────────────

    @Test
    void shouldIsolateDataBetweenTenants() {
        productRepository.saveAll(List.of(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Widget").stock(50).build(),
                Product.builder().tenantId("tenant-b").sku("SKU-001").name("Widget").stock(75).build()
        ));

        List<Product> tenantAProducts = productRepository.findAllByTenantId("tenant-a");

        assertThat(tenantAProducts).hasSize(1);
        assertThat(tenantAProducts.get(0).getTenantId()).isEqualTo("tenant-a");
    }

    // ─── Test 3: Búsqueda por tenant + SKU ─────────────────────────────────────

    @Test
    void shouldFindProductByTenantIdAndSku() {
        productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-999").name("Gadget").stock(10).build()
        );

        var result = productRepository.findByTenantIdAndSku("tenant-a", "SKU-999");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Gadget");
    }

    // ─── Test 4: Un tenant no puede acceder al ID de otro tenant ───────────────

    @Test
    void shouldNotFindProductByIdFromDifferentTenant() {
        var saved = productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Widget").stock(5).build()
        );

        // tenant-b intenta acceder al ID de tenant-a → debe retornar vacío
        var result = productRepository.findByTenantIdAndId("tenant-b", saved.getId());

        assertThat(result).isEmpty();
    }
}
