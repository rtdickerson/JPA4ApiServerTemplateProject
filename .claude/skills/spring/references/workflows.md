# Spring Workflows Reference

## When to use
Follow this file when adding a new domain object end-to-end (entity → repository → service → controller/GraphQL/MCP) or when wiring a new infrastructure bean.

## Workflows

### Adding a New Domain Object
1. Create `entity/Widget.java` — `@Entity`, `@Data`, `@Id`, `@GeneratedValue`.
2. Create `repository/WidgetRepository.java` extending `JpaRepository<Widget, Long>`.
3. Create `service/WidgetService.java` — `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`.
4. Create DTOs in `dto/` (`CreateWidgetRequest`, `WidgetResponse` with `from` factory).
5. Expose via one or more surfaces:
   - **REST**: `controller/WidgetController.java` (`@RestController`, `@RequestMapping("/api/widgets")`)
   - **GraphQL**: add types/queries to `schema.graphqls`; add resolver in `graphql/`
   - **MCP**: register tool methods in `mcp/ItemMcpTools.java` or a new tools class

### Adding an Infrastructure Bean
Add a `@Bean` method inside an existing `@Configuration` class under `config/` (e.g., `McpConfig`, `SecurityConfig`). Inject dependencies via the config class constructor — do not create `new` instances inside `@Service` classes.
```java
@Configuration
@RequiredArgsConstructor
public class WidgetConfig {
    private final WidgetService widgetService;

    @Bean
    public WidgetIntegration widgetIntegration() {
        return new WidgetIntegration(widgetService);
    }
}
```

### Exception Flow
Throw standard exceptions from the service layer; never catch-and-swallow in controllers.
```
Service throws EntityNotFoundException / ResponseStatusException
  → JVM unwinds to GlobalExceptionHandler
  → GlobalExceptionHandler maps to HTTP 404 / appropriate status
```

## Pitfalls
- **Skipping the service layer**: controllers that call repositories directly bypass transaction boundaries and break the DTO contract — always go through a service.
- **Adding beans inside `@Service` classes**: `new SomeDependency()` in a service bypasses the container; the dependency won't be managed, proxied, or testable. Declare it as a `@Bean` in a `@Configuration` class instead.