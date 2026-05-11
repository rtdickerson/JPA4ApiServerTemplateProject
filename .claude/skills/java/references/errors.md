# Error Handling

## When to use
Reference this when throwing, catching, or mapping exceptions anywhere in the codebase.

## GlobalExceptionHandler mappings
All exception-to-HTTP mappings live in `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`). Add new mappings there rather than catching exceptions in controllers.

```java
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
}
```

## Pattern matching on exception type (Java 16+)
```java
if (ex instanceof EntityNotFoundException nfe) {
    return ResponseEntity.status(404).body(new ErrorResponse(nfe.getMessage()));
}
```
Prefer pattern matching over `(EntityNotFoundException) ex` casts.

## Service-layer conventions
- Throw `jakarta.persistence.EntityNotFoundException` (not Spring's) when a resource is not found by ID.
- Let constraint violations (`DataIntegrityViolationException`) propagate to `GlobalExceptionHandler`.
- Never swallow exceptions silently; never return `null` to signal absence — use `Optional`.

## Pitfalls
- Do not add `try/catch` in controllers for exceptions already handled by `GlobalExceptionHandler` — double-handling produces inconsistent responses.
- `@Transactional` rolls back on unchecked exceptions by default. Checked exceptions do **not** trigger rollback unless `rollbackFor` is specified.