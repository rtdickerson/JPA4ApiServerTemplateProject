# Spring Boot Patterns

## When to use
Reference these patterns when adding new beans, startup hooks, property bindings, or switching datasource configuration in this project.

## Startup seed data
The project seeds items via `ApplicationRunner` in `Application.java`. Follow the same guard to stay idempotent:
```java
@Bean
ApplicationRunner seedData(ItemRepository repo) {
    return args -> { if (repo.count() == 0) repo.saveAll(defaultItems()); };
}
```

## Property binding with defaults
Use `@Value` with a colon-separated default so the app starts without every property set:
```java
@Value("${jwt.expiration-ms:86400000}")
private long expirationMs;
```
For grouped config, prefer `@ConfigurationProperties(prefix = "jwt")` bound to a record or class.

## Constructor injection via Lombok
Never use `@Autowired` field injection. All Spring-managed classes use `@RequiredArgsConstructor` so the compiler enforces required dependencies:
```java
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;  // injected by constructor
}
```

## Pitfalls
- **DDL in production**: `spring.jpa.hibernate.ddl-auto=create-drop` drops the schema on shutdown. Always switch to `validate` before pointing at a real database.
- **Dev-only surfaces**: `spring.h2.console.enabled` and `spring.graphql.graphiql.enabled` must both be `false` in prod — they are not secured by the JWT filter.