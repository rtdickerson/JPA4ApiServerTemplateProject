---
name: h2
description: Configures H2 in-memory database for development and testing in the JPA4 API Server Template project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# H2 Skill

Configures and troubleshoots the H2 in-memory database used for development and testing in this Spring Boot 3 project. H2 runs embedded within the JVM, requires no external database process, and resets on every restart via `create-drop` DDL. The H2 web console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:itemdb`).

## Quick Start

Start the server and open the console:

```bash
mvn spring-boot:run
# then navigate to http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:itemdb  |  user: sa  |  password: (blank)
```

Key properties in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:itemdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Key Concepts

- **In-memory only**: Database lives in JVM heap; all data is lost on restart. Schema and seed data are re-created via Hibernate `create-drop` and `Application.java` on every boot.
- **Named database (`mem:itemdb`)**: The `mem:itemdb` name allows multiple connections within the same JVM (e.g., application + H2 console) to share the same in-memory instance. Without a name, each connection gets a separate database.
- **H2 console security**: Spring Security must permit `/h2-console/**` and set `frameOptions` to `SAME_ORIGIN` (or disable) so the console iframe loads. See `SecurityConfig.java`.
- **`create-drop` vs `validate`**: Use `create-drop` in dev/test; switch to `validate` or `none` for production with a real database.
- **Switching to Postgres/MySQL**: Replace the four `spring.datasource.*` properties and set the appropriate `spring.jpa.database-platform` dialect — no Java code changes required.

## Common Patterns

**Permit H2 console in SecurityConfig:**

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/h2-console/**").permitAll()
        ...
    )
    .headers(headers -> headers
        .frameOptions(frame -> frame.sameOrigin())
    );
```

**Seed data in Application.java (runs after schema creation):**

```java
@Bean
CommandLineRunner seed(ItemRepository itemRepository) {
    return args -> {
        itemRepository.save(new Item(null, "Widget A", "desc", new BigDecimal("9.99"), 100));
        itemRepository.save(new Item(null, "Widget B", "desc", new BigDecimal("4.99"), 50));
    };
}
```

**Integration test — H2 auto-configures with `@SpringBootTest`:**

```java
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTests {
    // H2 starts automatically; no extra config needed
}
```

**Swap to Postgres for production (`application.properties`):**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.h2.console.enabled=false
```