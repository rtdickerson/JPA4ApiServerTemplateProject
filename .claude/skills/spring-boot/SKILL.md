---
name: spring-boot
description: Configures Spring Boot auto-configuration, embedded servers, and application startup for the JPA4 API Server Template project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Spring Boot Skill

Assists with Spring Boot 3.4.1 configuration, auto-configuration tuning, embedded Tomcat setup, and application startup concerns for this multi-protocol API server (REST, GraphQL, MCP) running on Java 17.

## Quick Start

```bash
# Start the dev server
mvn spring-boot:run

# Build production JAR
mvn clean package -DskipTests

# Run the built JAR
java -jar target/jpa4-api-server-*.jar
```

## Key Concepts

**Auto-configuration entry point**: `src/main/java/com/example/api/Application.java` — annotated `@SpringBootApplication`; also seeds initial data via `ApplicationRunner` or `CommandLineRunner`.

**Runtime configuration**: `src/main/resources/application.properties` — controls port, JWT secret, datasource URL, H2 console, GraphiQL, and JPA DDL strategy.

**Embedded server**: Tomcat is included via `spring-boot-starter-web`; no external servlet container needed.

**Bean lifecycle**: Spring manages all `@Service`, `@Repository`, `@Configuration`, and `@Component` beans; use `@RequiredArgsConstructor` (Lombok) for constructor injection rather than field injection.

**Profile-based config**: Use `application-prod.properties` (or env vars) to override H2 with Postgres and tighten security settings before deploying.

## Common Patterns

**Disabling auto-configuration** for a specific concern:
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

**Registering a startup hook** (seed data pattern used in this project):
```java
@Bean
ApplicationRunner seedData(ItemRepository repo) {
    return args -> { if (repo.count() == 0) repo.saveAll(defaultItems()); };
}
```

**Property injection** via `@Value`:
```java
@Value("${jwt.secret}")
private String jwtSecret;

@Value("${jwt.expiration-ms:86400000}")
private long expirationMs;
```

**Switching datasource** (H2 → Postgres in `application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

**Production hardening checklist** (disable dev-only surfaces):
```properties
spring.h2.console.enabled=false
spring.graphql.graphiql.enabled=false
spring.jpa.hibernate.ddl-auto=validate
```

**Key auto-configured starters** declared in `pom.xml`:

| Starter | Activates |
|---------|-----------|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, Jackson |
| `spring-boot-starter-data-jpa` | Hibernate, Spring Data repositories |
| `spring-boot-starter-security` | Spring Security filter chain |
| `spring-boot-starter-graphql` | Spring for GraphQL, schema parsing |
| `spring-boot-starter-test` | JUnit 5, MockMvc, Spring Test |