# Spring MVC Workflows

## When to use
Follow these workflows when adding a new resource endpoint or extending an existing one in this Spring Boot 3 project.

## Workflow 1 — Add a New Resource Endpoint
1. **Read the service** (`ItemService.java` or `PromptService.java`) to confirm which operations exist before writing controller methods.
2. **Create or update DTOs** in `src/main/java/com/example/api/dto/` — one record per direction (`CreateXRequest`, `UpdateXRequest`, `XResponse`).
3. **Write the controller** in `src/main/java/com/example/api/controller/` following the `@RestController` + `@RequiredArgsConstructor` skeleton.
4. **Map exceptions** in `GlobalExceptionHandler` (`EntityNotFoundException` → 404, `IllegalArgumentException` → 400) — do not catch in the controller.
5. **Run `mvn test`** to confirm no regressions before marking done.

## Workflow 2 — Add a Search / Filter Param
Use `@RequestParam(required = false)` and delegate the null check to the service layer:

```java
// Controller — no null logic here
@GetMapping
public List<ItemResponse> getAll(@RequestParam(required = false) String search) {
    return itemService.findAllOrSearch(search);
}

// Service — owns the branch
public List<ItemResponse> findAllOrSearch(String search) {
    if (search == null) return findAll();
    return itemRepository.findByNameContainingIgnoreCase(search)
                         .stream().map(ItemResponse::from).toList();
}
```

## Workflow 3 — Verify Security Coverage
After adding any new path prefix (e.g., `/api/widgets`):
1. Open `src/main/java/com/example/api/config/SecurityConfig.java`.
2. Confirm the new prefix is covered by the existing `.requestMatchers("/api/**").authenticated()` rule or add an explicit matcher.
3. Confirm `/auth/**` and `/mcp/**` remain permitted without a token — these are intentionally open.

## Pitfalls
- **Don't create a new service bean** just to satisfy a controller — check whether `ItemService` or `PromptService` already has the method you need.
- **Don't skip `mvn test` after adding endpoints** — `ApplicationTests` boots the full context and will catch wiring errors (missing bean, broken security config) that compilation misses.