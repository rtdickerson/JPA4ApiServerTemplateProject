# Spring MVC Patterns

## When to use
Apply these patterns when adding or modifying REST endpoints in `src/main/java/com/example/api/controller/`.

## Controller Skeleton
Every controller in this project follows the same structure — `@RestController` + `@RequestMapping` on the class, `@RequiredArgsConstructor` for injection, no `@Autowired`:

```java
@RestController
@RequestMapping("/api/widgets")
@RequiredArgsConstructor
public class WidgetController {
    private final WidgetService widgetService;
}
```

## DTO Record with Factory Method
DTOs are Java records. Response DTOs carry a static `from(Entity)` factory so the controller never touches entity internals:

```java
public record WidgetResponse(Long id, String name, BigDecimal price) {
    public static WidgetResponse from(Widget w) {
        return new WidgetResponse(w.getId(), w.getName(), w.getPrice());
    }
}
```

Request DTOs are plain records with no factory:

```java
public record CreateWidgetRequest(String name, BigDecimal price, Integer quantity) {}
public record UpdateWidgetRequest(String name, BigDecimal price, Integer quantity) {}
```

## POST Returns 201 + Location
Creation endpoints must return `201 Created` with a `Location` header pointing to the new resource:

```java
@PostMapping
public ResponseEntity<WidgetResponse> create(@RequestBody @Valid CreateWidgetRequest req,
                                              UriComponentsBuilder ucb) {
    WidgetResponse created = widgetService.create(req);
    URI location = ucb.path("/api/widgets/{id}").buildAndExpand(created.id()).toUri();
    return ResponseEntity.created(location).body(created);
}
```

## Pitfalls
- **Never return JPA entities from controllers.** Lazy-loaded associations cause `LazyInitializationException` outside a transaction and leak internal schema.
- **Do not add `@PreAuthorize` or token checks in controllers.** `JwtAuthenticationFilter` + `SecurityConfig` handle all `/api/**` auth — duplicating logic here creates drift.
- **`@RequestParam(required = false)` for optional search** — pass `null` to the service and let the service decide "no filter = return all."