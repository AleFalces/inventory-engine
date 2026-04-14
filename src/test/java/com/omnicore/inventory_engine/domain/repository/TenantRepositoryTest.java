package com.omnicore.inventory_engine.domain.repository;

import com.omnicore.inventory_engine.TestcontainersConfiguration;
import com.omnicore.inventory_engine.domain.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        tenantRepository.deleteAll();
    }

    // ─── Test 1: Persistencia básica ──────────────────────────────────────────

    @Test
    void shouldPersistTenant() {
        var tenant = Tenant.builder()
                .tenantId("acme")
                .passwordHash("$2a$10$hashedpassword")
                .build();

        var saved = tenantRepository.save(tenant);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo("acme");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedpassword");
    }

    // ─── Test 2: Búsqueda por tenantId ────────────────────────────────────────

    @Test
    void shouldFindTenantByTenantId() {
        tenantRepository.save(Tenant.builder()
                .tenantId("acme")
                .passwordHash("$2a$10$hashedpassword")
                .build());

        var result = tenantRepository.findByTenantId("acme");

        assertThat(result).isPresent();
        assertThat(result.get().getTenantId()).isEqualTo("acme");
    }

    // ─── Test 3: Tenant inexistente retorna vacío ─────────────────────────────

    @Test
    void shouldReturnEmptyWhenTenantNotFound() {
        var result = tenantRepository.findByTenantId("unknown");

        assertThat(result).isEmpty();
    }
}
