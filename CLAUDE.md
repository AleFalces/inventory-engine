# OmniCore Inventory Engine — Claude Instructions

## Reglas de Oro

- **Concisión**: Mostrar solo el código afectado. Archivo completo solo si el contexto lo requiere. Sin explicaciones teóricas repetidas.
- **TDD Estricto**: Test primero en `src/test/java/`, luego código de producción. Nunca al revés.
- **tenantId obligatorio**: Toda entidad y consulta de repositorio debe incluir `tenantId`. Sin excepciones.

## Comandos

| Acción | Comando |
|--------|---------|
| Build (sin tests) | `./mvnw clean install -DskipTests` |
| Ejecutar tests | `./mvnw test` |
| Test clase específica | `./mvnw test -Dtest=ClassName` |
| Test método específico | `./mvnw test -Dtest=ClassName#methodName` |

## Estándares de Código

| Componente | Estándar |
|------------|---------|
| Entidades | Lombok: `@Getter @Setter @NoArgsConstructor @Builder` |
| DTOs | Java Records (inmutables) |
| Mappers | MapStruct (sin mapeo manual) |
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.4 |

## Estructura de Paquetes

```
com.omnicore.inventory_engine
├── domain/
│   ├── entity/       # @Entity JPA
│   ├── repository/   # Spring Data JPA
│   └── service/      # Lógica de negocio
├── api/
│   ├── controller/   # REST
│   ├── dto/          # Java Records
│   └── mapper/       # MapStruct
└── config/
```

## Notas del Proyecto

- Tests usan Testcontainers con PostgreSQL real — sin H2.
- Lombok y MapStruct ya están en `pom.xml` con sus annotation processors configurados.
