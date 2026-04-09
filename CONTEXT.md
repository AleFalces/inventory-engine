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
| 5 | Ajuste de stock (`POST /{sku}/adjust`) + optimistic locking | 🔲 Pendiente |
| 6 | Entidad `StockMovement` (trazabilidad de movimientos) | 🔲 Pendiente |
| 7 | Paginación en `GET /products` | 🔲 Pendiente |
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

## Fase 5 — Ajuste de Stock (próxima)

**Objetivo**: operación atómica para incrementar o decrementar stock usando `@Version` (optimistic locking).

### Endpoint
`POST /api/v1/products/{sku}/adjust` con body `{ "delta": -10, "reason": "SALE" }`

### Tests a escribir (antes del código de producción)

#### ProductRepositoryTest
- `shouldIncrementStockAtomically` — delta positivo, verificar nuevo stock
- `shouldDecrementStockAtomically` — delta negativo, verificar nuevo stock

#### ProductServiceTest
- `shouldAdjustStockSuccessfully` — happy path
- `shouldThrowExceptionWhenProductNotFoundOnAdjust` — SKU inexistente
- `shouldThrowExceptionWhenStockWouldGoBelowZero` — stock insuficiente

#### ProductControllerTest
- `shouldAdjustStockAndReturn200` — `POST /{sku}/adjust` → 200 + body actualizado
- `shouldReturn404WhenAdjustingNonExistentProduct` — 404
- `shouldReturn422WhenStockWouldGoBelowZero` — 422 Unprocessable Entity

### Artefactos nuevos
- DTO `StockAdjustRequest` (record): `delta` (Integer, ≠ 0), `reason` (enum: `IN`, `OUT`, `ADJUSTMENT`)
- Excepción `InsufficientStockException` → HTTP 422
- `ProductService.adjustStock(tenantId, sku, request)` → `ProductResponse`
- Endpoint `POST /{sku}/adjust` en `ProductController`
