---
name: spring
description: Provides foundational Spring Framework patterns for dependency injection and configuration in this Spring Boot 3 project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Spring Skill

Assists with Spring Boot 3 patterns used throughout this project: bean wiring, configuration classes, service/controller/repository layering, and transaction management. All patterns follow the conventions established in `src/main/java/com/example/api/`.

## Quick Start

To add a new component, follow the existing layer structure:

1. Define a JPA entity in `entity/`
2. Create a `JpaRepository` extension in `repository/`
3. Add a `@Service` class in `service/` with `@Transactional(readOnly = true)` at class level
4. Expose via a `@RestController` in `controller/`, a GraphQL resolver in `graphql/`, or an MCP tool in `mcp/`

## Key Concepts

**Constructor Injection via Lombok**
All beans use `@RequiredArgsConstructor` — never `@Autowired` on fields.

```java
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
}
```

**Configuration Beans**
Infrastructure beans (security filter chain, MCP server, SSE transport) live in `@Configuration` classes under `config/`. Add new beans there rather than scattering them across application classes.

**Transaction Boundaries**
Services default to `@Transactional(readOnly = true)`. Override individual write methods with `@Transactional` (read-write).

**DTO Pattern**
Controllers never expose entities directly. Use Java records with a static `from(Entity)` factory in `dto/`.

## Common Patterns

**Adding a new service**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetService {
    private final WidgetRepository widgetRepository;

    public List<WidgetResponse> findAll() {
        return widgetRepository.findAll().stream()
            .map(WidgetResponse::from).toList();
    }

    @Transactional
    public WidgetResponse create(CreateWidgetRequest req) {
        Widget w = new Widget();
        w.setName(req.name());
        return WidgetResponse.from(widgetRepository.save(w));
    }
}
```

**Adding a configuration bean**
```java
@Configuration
@RequiredArgsConstructor
public class WidgetConfig {
    private final WidgetService widgetService;

    @Bean
    public SomeDependency someDependency() {
        return new SomeDependency(widgetService);
    }
}
```

**Exception handling**
Throw standard Jakarta/Spring exceptions (`EntityNotFoundException`, `ResponseStatusException`) from services. `GlobalExceptionHandler` maps them to HTTP responses — no try/catch needed in controllers.

**Import order** (enforced by project conventions):
1. `java.*` / `javax.*`
2. `jakarta.*`
3. `org.springframework.*`
4. Third-party (Lombok, JJWT)
5. `com.example.*`