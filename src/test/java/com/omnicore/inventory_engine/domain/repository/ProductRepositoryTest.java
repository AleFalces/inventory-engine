package com.omnicore.inventory_engine.domain.repository;

import com.omnicore.inventory_engine.TestcontainersConfiguration;
import com.omnicore.inventory_engine.domain.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    // ─── Test 5: Actualizar campos de un producto ──────────────────────────────

    @Test
    void shouldUpdateProductFields() {
        var saved = productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Original").stock(10).build()
        );

        saved.setName("Updated");
        saved.setDescription("New desc");
        saved.setStock(99);
        productRepository.save(saved);

        var result = productRepository.findByTenantIdAndSku("tenant-a", "SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Updated");
        assertThat(result.get().getDescription()).isEqualTo("New desc");
        assertThat(result.get().getStock()).isEqualTo(99);
    }

    // ─── Test 6: Eliminar producto por tenant + SKU ────────────────────────────

    @Test
    void shouldDeleteProductByTenantIdAndSku() {
        productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-DEL").name("ToDelete").stock(5).build()
        );

        var toDelete = productRepository.findByTenantIdAndSku("tenant-a", "SKU-DEL");
        toDelete.ifPresent(productRepository::delete);

        var result = productRepository.findByTenantIdAndSku("tenant-a", "SKU-DEL");
        assertThat(result).isEmpty();
    }

    // ─── Test 7: Incrementar stock ─────────────────────────────────────────────

    @Test
    void shouldIncrementStockAtomically() {
        var saved = productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Widget").stock(50).build()
        );

        saved.setStock(saved.getStock() + 30);
        productRepository.save(saved);

        var result = productRepository.findByTenantIdAndSku("tenant-a", "SKU-001");
        assertThat(result).isPresent();
        assertThat(result.get().getStock()).isEqualTo(80);
    }

    // ─── Test 9: Paginación — primera página ──────────────────────────────────

    @Test
    void shouldReturnPagedProducts() {
        productRepository.saveAll(List.of(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Alpha").stock(10).build(),
                Product.builder().tenantId("tenant-a").sku("SKU-002").name("Beta").stock(20).build(),
                Product.builder().tenantId("tenant-a").sku("SKU-003").name("Gamma").stock(30).build()
        ));

        Page<Product> page = productRepository.findAllByTenantId("tenant-a", PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    // ─── Test 10: Paginación — segunda página ─────────────────────────────────

    @Test
    void shouldReturnSecondPage() {
        productRepository.saveAll(List.of(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Alpha").stock(10).build(),
                Product.builder().tenantId("tenant-a").sku("SKU-002").name("Beta").stock(20).build(),
                Product.builder().tenantId("tenant-a").sku("SKU-003").name("Gamma").stock(30).build()
        ));

        Page<Product> page = productRepository.findAllByTenantId("tenant-a", PageRequest.of(1, 2));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    // ─── Test 8: Decrementar stock ─────────────────────────────────────────────

    @Test
    void shouldDecrementStockAtomically() {
        var saved = productRepository.save(
                Product.builder().tenantId("tenant-a").sku("SKU-001").name("Widget").stock(50).build()
        );

        saved.setStock(saved.getStock() - 20);
        productRepository.save(saved);

        var result = productRepository.findByTenantIdAndSku("tenant-a", "SKU-001");
        assertThat(result).isPresent();
        assertThat(result.get().getStock()).isEqualTo(30);
    }
}
