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
| 8 | Spring Security + JWT (reemplazar header `X-Tenant-ID`) | ✅ Completo |
| 9 | Endpoint de login (`POST /auth/login`) | ✅ Completo |
| 10 | Registro de tenants (`POST /auth/register`) | ✅ Completo |
| 11 | Validación de entrada (Bean Validation) | ✅ Completo |
| 12 | RBAC — roles por tenant (ADMIN / VIEWER) | ✅ Completo |

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

## Fase 8 — Spring Security + JWT ✅

**Completado**: autenticación JWT implementada con TDD. 49/49 tests en verde.

### Artefactos entregados
- Dependencias `spring-boot-starter-security` + `jjwt-api/impl/jackson 0.12.6` en `pom.xml`
- `JwtUtil` — genera tokens, extrae `tenantId` del subject, valida firma y expiración
- `JwtAuthenticationFilter` — valida Bearer token y puebla `SecurityContextHolder` con `tenantId` como principal
- `SecurityConfig` — cadena stateless, sin CSRF, todo endpoint requiere autenticación
- `ProductController` — usa `@AuthenticationPrincipal String tenantId` (eliminado `X-Tenant-ID` header)
- `JwtUtilTest` — 4 tests unitarios para `JwtUtil`
- `SecurityIntegrationTest` — tests 401 sin token y con token inválido
- `src/test/resources/application.properties` — añadido `jwt.secret` para tests de integración

### Decisiones de diseño
- `tenantId` se extrae del `subject` del JWT (no de un claim custom)
- Token firmado con HMAC-SHA (HS256), secreto configurable vía `jwt.secret`
- Sin refresh tokens ni revocación en esta fase

---

## Fase 9 — Endpoint de Login 🔄

**Objetivo**: exponer `POST /auth/login` para que los clientes puedan obtener un JWT real. Actualmente los tokens solo se generan en tests.

### Contrato del endpoint

```
POST /auth/login
Body: { "tenantId": "acme", "password": "secret" }
Response 200: { "token": "<jwt>" }
Response 401: credenciales inválidas
```

### Artefactos a entregar
- Entidad / tabla `Tenant` (id, tenantId, passwordHash) con datos de prueba vía `data.sql`
- `TenantRepository` — `findByTenantId(String tenantId)`
- `AuthService.login(tenantId, password)` — valida credenciales y genera JWT con `JwtUtil`
- DTO `LoginRequest` (record): `tenantId`, `password`
- DTO `LoginResponse` (record): `token`
- `AuthController` — `POST /api/v1/auth/login`, público (excluido de `SecurityConfig`)
- `AuthControllerTest` — casos: login OK → 200 + token, password incorrecta → 401, tenant inexistente → 401

### Decisiones de diseño
- Contraseñas almacenadas con BCrypt (`PasswordEncoder`)
- El endpoint `/api/v1/auth/login` se añade a la lista blanca en `SecurityConfig`
- Sin gestión de usuarios completa — solo credenciales de tenant para obtener JWT

---

## Fase 10 — Registro de Tenants 🔄

**Objetivo**: exponer `POST /auth/register` para que nuevos tenants puedan registrarse sin acceso directo a la BD. Cierra el ciclo de autenticación self-service.

### Contrato del endpoint

```
POST /api/v1/auth/register
Body: { "tenantId": "acme", "password": "secret" }
Response 201: { "tenantId": "acme" }
Response 409: tenantId ya registrado
```

### Artefactos a entregar
- DTO `RegisterRequest` (record): `tenantId`, `password`
- DTO `RegisterResponse` (record): `tenantId`
- Excepción `TenantAlreadyExistsException` → HTTP 409
- `AuthService.register(tenantId, password)` → `RegisterResponse`
- `AuthServiceImpl.register()` — hashea password con BCrypt, persiste `Tenant`, lanza excepción si ya existe
- `AuthController` — `POST /api/v1/auth/register` (público)
- `GlobalExceptionHandler` — handler 409 para `TenantAlreadyExistsException`
- Tests unitarios: `AuthControllerTest` + `AuthServiceTest` — casos OK y conflicto
- Test de integración en `LoginIntegrationTest` — registro + login en secuencia

### Decisiones de diseño
- El endpoint es público (sin JWT) — se añade a la lista blanca en `SecurityConfig`
- `tenantId` debe ser único — validado a nivel de BD (`unique = true`) y de servicio
- No se devuelve token en el registro — el cliente debe hacer login después

---

## Fase 11 — Validación de Entrada (Bean Validation) 🔲

**Objetivo**: rechazar requests malformados antes de que lleguen a la capa de servicio. Actualmente se pueden persistir `tenantId` vacío, SKU nulo, stock negativo, etc.

### Campos a validar

| DTO | Campo | Restricción |
|-----|-------|-------------|
| `CreateProductRequest` | `sku` | `@NotBlank` |
| `CreateProductRequest` | `name` | `@NotBlank` |
| `CreateProductRequest` | `stock` | `@NotNull @Min(0)` |
| `UpdateProductRequest` | `name` | `@NotBlank` |
| `UpdateProductRequest` | `stock` | `@NotNull @Min(0)` |
| `StockAdjustRequest` | `delta` | `@NotNull` |
| `StockAdjustRequest` | `reason` | `@NotBlank` |
| `LoginRequest` | `tenantId` | `@NotBlank` |
| `LoginRequest` | `password` | `@NotBlank` |
| `RegisterRequest` | `tenantId` | `@NotBlank` |
| `RegisterRequest` | `password` | `@NotBlank @Size(min=8)` |

### Artefactos a entregar
- Dependencia `spring-boot-starter-validation` en `pom.xml`
- Anotaciones `@Valid` en todos los `@RequestBody` de controllers
- Anotaciones de constraint en todos los DTOs afectados
- `GlobalExceptionHandler` ya tiene handler 400 para `MethodArgumentNotValidException` — verificar que devuelve el campo con el error
- Tests 400 en `AuthControllerTest`, `ProductControllerTest` para inputs inválidos

### Decisiones de diseño
- Records de Java admiten anotaciones de Bean Validation en los componentes del constructor
- Respuesta 400 unificada vía `GlobalExceptionHandler` existente — sin cambios en el handler

---

## Fase 12 — RBAC: Roles por Tenant 🔲

**Objetivo**: introducir roles `ADMIN` y `VIEWER` por tenant. `ADMIN` puede crear/editar/eliminar productos y ajustar stock. `VIEWER` solo puede leer.

### Modelo de roles

```
ADMIN  → GET, POST, PUT, DELETE, POST /adjust
VIEWER → GET únicamente
```

### Contrato

| Endpoint | ADMIN | VIEWER |
|----------|-------|--------|
| `GET /products` | ✅ | ✅ |
| `GET /products/{sku}` | ✅ | ✅ |
| `POST /products` | ✅ | 403 |
| `PUT /products/{sku}` | ✅ | 403 |
| `DELETE /products/{sku}` | ✅ | 403 |
| `POST /products/{sku}/adjust` | ✅ | 403 |
| `GET /products/{sku}/movements` | ✅ | ✅ |

### Artefactos a entregar
- Campo `role` en entidad `Tenant` (enum: `ADMIN`, `VIEWER`)
- Campo `role` en `RegisterRequest` (opcional, default `VIEWER`)
- `role` incluido en el JWT como claim — `JwtUtil` actualizado
- `JwtAuthenticationFilter` extrae el rol y lo añade como `GrantedAuthority`
- `SecurityConfig` — `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` en endpoints de escritura
- Tests de autorización: `VIEWER` recibe 403 en endpoints de escritura

### Decisiones de diseño
- El rol se almacena en BD y se embebe en el JWT — sin consulta a BD por request
- `ADMIN` es el único rol que puede registrar otros tenants con rol `ADMIN` (fase futura)
- Sin jerarquía de roles compleja — solo `ADMIN` y `VIEWER`
