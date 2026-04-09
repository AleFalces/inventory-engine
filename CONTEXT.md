# OmniCore Inventory Engine — Contexto de Arquitectura

## Misión

Motor de inventario de alta concurrencia para SaaS Multi-tenant. El producto gestiona stock de múltiples clientes (tenants) en una base de datos compartida, garantizando **aislamiento total entre tenants**.

## Aislamiento Multi-Tenant

**Estrategia**: Shared schema con columna discriminadora `tenant_id`.

### Reglas de Aislamiento (obligatorias en cada capa)

| Capa | Regla |
|------|-------|
| Entidad | `tenantId` con `@Column(nullable = false)` en toda `@Entity` |
| Repositorio | Toda consulta incluye `WHERE tenant_id = :tenantId` |
| Servicio | `tenantId` se obtiene del security context, **nunca del request body** |
| Test | Todo test de repositorio debe incluir un test de aislamiento entre tenants |

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de Datos | PostgreSQL |
| Utilidades de entidad | Lombok |
| Mapeo DTO | MapStruct |
| Tests | JUnit 5 + Testcontainers (PostgreSQL real) |

## Flujo TDD

```
1. Escribir test en src/test/java/  →  RED (falla, compila)
2. Escribir código mínimo          →  GREEN (pasa)
3. Refactorizar                    →  REFACTOR (sigue verde)
```

## Modelo de Dominio

```
Product
├── id: Long (PK, auto-generado)
├── tenantId: String (NOT NULL, indexed)
├── sku: String (NOT NULL, único por tenant)
├── name: String (NOT NULL)
├── description: String
├── stock: Integer (NOT NULL, >= 0)
└── version: Long (@Version — bloqueo optimista para concurrencia)
```

## Infraestructura y CI/CD

| Componente | Detalle |
|------------|---------|
| CI | GitHub Actions — corre en cada push a `main` y en PRs |
| Runner | `ubuntu-latest` (Docker incluido — soporta Testcontainers) |
| Pipeline | `./mvnw test` completo: unitarios + integración con PostgreSQL real |
| Artefactos | Surefire reports subidos automáticamente si hay fallo |
| Base local | `docker compose up -d` → `omnicore-postgres` en puerto 5433 |

**Garantía**: el pipeline ejecuta los tests de aislamiento multi-tenant en cada integración, asegurando que ningún cambio pueda romper la separación de datos entre tenants.

---

## Decisiones de Diseño

| Decisión | Razón |
|----------|-------|
| `tenantId` obligatorio en toda consulta | Multi-tenancy estricto, sin fugas entre tenants |
| `@Version` en `Product` | Preparado para optimistic locking en ajuste de stock |
| `X-Tenant-ID` header (temporal) | Se reemplazará por extracción desde JWT en Fase 8 |
| Testcontainers (sin H2) | Evitar divergencia entre test y producción |
| `@Transactional(readOnly = true)` en service | Optimización de lecturas por defecto |

---

## Fases de Desarrollo

| # | Fase | Estado |
|---|------|--------|
| 0 | Proyecto base + CLAUDE.md + CONTEXT.md | ✅ Completo |
| 1 | `Product` entity + `ProductRepository` + tests de persistencia | ✅ Completo |
| 2 | `ProductService` + tests de servicio | ✅ Completo |
| 3 | `ProductController` + DTOs + MapStruct + tests de controller | ✅ Completo |
| 4 | CRUD completo — `PUT /{sku}` y `DELETE /{sku}` | ✅ Completo |
| 5 | Ajuste de stock (`POST /{sku}/adjust`) + optimistic locking | ✅ Completo |
| 6 | Entidad `StockMovement` (trazabilidad de movimientos) | ✅ Completo |
| 7 | Paginación en `GET /products` | ✅ Completo |
| 8 | Spring Security + JWT (reemplazar header `X-Tenant-ID`) | 🔲 Pendiente |

---

## Fase 4 — CRUD Completo ✅

**Completado**: `PUT /{sku}` y `DELETE /{sku}` implementados con TDD. 25/25 tests en verde.

### Artefactos entregados
- DTO `UpdateProductRequest` (record): `name`, `description`, `stock`
- `ProductService.updateProduct(tenantId, sku, request)` → `ProductResponse`
- `ProductService.deleteProduct(tenantId, sku)` → `void`
- `ProductController` — endpoints `PUT /{sku}` y `DELETE /{sku}`

---

## Fase 5 — Ajuste de Stock ✅

**Completado**: `POST /{sku}/adjust` implementado con TDD. 33/33 tests en verde.

### Artefactos entregados
- DTO `StockAdjustRequest` (record): `delta` (Integer), `reason` (String)
- Excepción `InsufficientStockException` → HTTP 422
- `ProductService.adjustStock(tenantId, sku, request)` → `ProductResponse`
- `GlobalExceptionHandler` actualizado con handler 422
- Endpoint `POST /{sku}/adjust` en `ProductController`

### Lógica implementada
```
newStock = product.stock + delta
if newStock < 0 → InsufficientStockException (422)
else → product.stock = newStock → save() con @Version
```

---

## Fase 6 — StockMovement ✅

**Completado**: trazabilidad de movimientos implementada con TDD. 39/39 tests en verde.

### Artefactos entregados
- Entidad `StockMovement` (tenantId, productSku, delta, reason, stockBefore, stockAfter, createdAt)
- `StockMovementRepository.findAllByTenantIdAndProductSku()`
- DTO `StockMovementResponse` (record)
- `ProductService.adjustStock()` ampliado: persiste movimiento en cada ajuste
- `ProductService.findMovements(tenantId, sku)` → lista de movimientos
- `GET /api/v1/products/{sku}/movements` en `ProductController`

---

## Fase 7 — Paginación ✅

**Completado**: `GET /products` soporta paginación. 43/43 tests en verde.

### Endpoint actualizado
`GET /api/v1/products?page=0&size=20&sort=name,asc` → `Page<ProductResponse>`

### Artefactos entregados
- `ProductRepository` — `Page<Product> findAllByTenantId(String tenantId, Pageable pageable)`
- `ProductService.findAllByTenant(String tenantId, Pageable pageable)` → `Page<ProductResponse>`
- `ProductController.findAll()` — acepta `@PageableDefault(size=20, sort="name") Pageable`
- Default: página 0, tamaño 20, ordenado por nombre

---

## Fase 8 — Spring Security + JWT (próxima)

**Objetivo**: reemplazar el header `X-Tenant-ID` por extracción del `tenantId` desde un JWT válido.

### Decisiones de diseño
- El `tenantId` se extrae del claim del JWT en un filtro de seguridad
- Se elimina `@RequestHeader("X-Tenant-ID")` de todos los controllers
- El `SecurityContext` expone el `tenantId` al servicio

### Artefactos a crear
- Dependencia `spring-boot-starter-security` + `jjwt` en `pom.xml`
- `JwtAuthenticationFilter` — valida token y puebla `SecurityContext`
- `SecurityConfig` — configura la cadena de filtros (stateless, sin CSRF)
- `TenantContext` — extrae `tenantId` del `Authentication` en el servicio
