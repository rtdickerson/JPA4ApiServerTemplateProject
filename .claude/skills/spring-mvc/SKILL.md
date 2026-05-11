---
name: spring-mvc
description: Builds REST APIs with Spring MVC controllers, request mappings, and HTTP response handling
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Spring Mvc Skill

Implements REST endpoints using Spring MVC controllers in this Spring Boot 3 project. Controllers live in `src/main/java/com/example/api/controller/`, accept/return DTOs (not entities), delegate business logic to services, and are secured with JWT via `JwtAuthenticationFilter`.

## Quick Start

1. Identify the resource and its base path (e.g., `/api/items`, `/api/prompts`)
2. Read the existing service (`ItemService.java`, `PromptService.java`) to understand available operations
3. Add or modify a controller in `src/main/java/com/example/api/controller/`
4. Add or update DTOs in `src/main/java/com/example/api/dto/`
5. Verify exception cases are handled by `GlobalExceptionHandler`
6. Run `mvn test` to confirm no regressions

## Key Concepts

- **Controller layer**: `@RestController` + `@RequestMapping` on the class; method-level `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- **DTO pattern**: Controllers never expose JPA entities; use records (`ItemResponse`, `CreateItemRequest`, `UpdateItemRequest`) with a static `from(Entity)` factory
- **Constructor injection**: `@RequiredArgsConstructor` (Lombok) injects `final` service fields; no `@Autowired`
- **Response codes**: POST returns `ResponseEntity` with `201 Created` and `Location` header; GET returns `200 OK`; DELETE returns `204 No Content`
- **Search**: Query params via `@RequestParam(required = false)` — null means "return all"
- **Security**: All `/api/**` routes require a valid `Authorization: Bearer <jwt>` header; no controller-level auth code needed

## Common Patterns

**Read-only endpoint**
```java
@GetMapping
public List<ItemResponse> getAll(@RequestParam(required = false) String search) {
    return (search == null) ? itemService.findAll() : itemService.search(search);
}
```

**Create with 201 + Location**
```java
@PostMapping
public ResponseEntity<ItemResponse> create(@RequestBody @Valid CreateItemRequest req,
                                            UriComponentsBuilder ucb) {
    ItemResponse created = itemService.create(req);
    URI location = ucb.path("/api/items/{id}").buildAndExpand(created.id()).toUri();
    return ResponseEntity.created(location).body(created);
}
```

**Partial update**
```java
@PutMapping("/{id}")
public ItemResponse update(@PathVariable Long id, @RequestBody UpdateItemRequest req) {
    return itemService.update(id, req);
}
```

**Delete with 204**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    itemService.delete(id);
    return ResponseEntity.noContent().build();
}
```

**Exception mapping** (in `GlobalExceptionHandler`, not the controller):
```java
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
}
```