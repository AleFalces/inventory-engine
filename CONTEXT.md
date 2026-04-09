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
| 4 | CRUD completo — `PUT /{sku}` y `DELETE /{sku}` | 🔲 Pendiente |
| 5 | Ajuste de stock (`POST /{sku}/adjust`) + optimistic locking | 🔲 Pendiente |
| 6 | Entidad `StockMovement` (trazabilidad de movimientos) | 🔲 Pendiente |
| 7 | Paginación en `GET /products` | 🔲 Pendiente |
| 8 | Spring Security + JWT (reemplazar header `X-Tenant-ID`) | 🔲 Pendiente |

---

## Fase 4 — CRUD Completo (próxima)

**Objetivo**: agregar `PUT /{sku}` (update) y `DELETE /{sku}` a las tres capas, TDD estricto.

### Tests a escribir (antes del código de producción)

#### ProductRepositoryTest
- `shouldUpdateProductFields` — persistir cambios de nombre/descripción/stock y verificar
- `shouldDeleteProductByTenantIdAndSku` — borrar y comprobar que no se encuentra

#### ProductServiceTest
- `shouldUpdateProductSuccessfully` — happy path update
- `shouldThrowExceptionWhenUpdatingNonExistentProduct` — `ProductNotFoundException`
- `shouldDeleteProductSuccessfully` — happy path delete
- `shouldThrowExceptionWhenDeletingNonExistentProduct` — `ProductNotFoundException`

#### ProductControllerTest
- `shouldUpdateProductAndReturn200` — `PUT /{sku}` → 200 + body
- `shouldReturn404WhenUpdatingNonExistentProduct` — 404
- `shouldDeleteProductAndReturn204` — `DELETE /{sku}` → 204
- `shouldReturn404WhenDeletingNonExistentProduct` — 404

### Artefactos nuevos
- DTO `UpdateProductRequest` (record): `name`, `description`, `stock`
- `ProductService.updateProduct(tenantId, sku, request)` → `ProductResponse`
- `ProductService.deleteProduct(tenantId, sku)` → `void`
- `ProductController` — endpoints `PUT /{sku}` y `DELETE /{sku}`
