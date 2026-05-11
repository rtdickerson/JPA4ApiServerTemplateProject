# H2 Patterns

## When to use
Apply these patterns when configuring the H2 console, wiring security, seeding data, or writing integration tests against the in-memory database.

## Pattern 1 — Security: Permit console and enable iframes

`SecurityConfig.java` must whitelist `/h2-console/**` and relax frame options or the console iframe will be blocked by Spring Security's default CSP headers.

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/h2-console/**").permitAll()
        .anyRequest().authenticated()
    )
    .headers(headers -> headers
        .frameOptions(frame -> frame.sameOrigin())
    )
    .csrf(csrf -> csrf
        .ignoringRequestMatchers("/h2-console/**")
    );
```

## Pattern 2 — Seed data after schema creation

`CommandLineRunner` in `Application.java` runs after Hibernate applies `create-drop`, so it is the correct place for seed rows. Do not rely on `data.sql` with `create-drop` — ordering is fragile.

```java
@Bean
CommandLineRunner seed(ItemRepository items, PromptRepository prompts) {
    return args -> {
        items.save(new Item(null, "Widget A", "desc", new BigDecimal("9.99"), 100));
        prompts.save(new Prompt(null, "default", "You are a helpful assistant."));
    };
}
```

## Pattern 3 — Named in-memory database for shared connections

The JDBC URL `jdbc:h2:mem:itemdb` (named) lets both the application and the H2 web console connect to the same instance. An anonymous `jdbc:h2:mem:` URL creates a private database that the console cannot reach.

```properties
spring.datasource.url=jdbc:h2:mem:itemdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

`DB_CLOSE_DELAY=-1` keeps the database alive as long as the JVM runs; omitting it can cause the schema to vanish mid-session.

## Pitfalls

- **Forgetting CSRF exclusion**: H2 console POSTs form data; Spring Security's CSRF filter will reject it unless `/h2-console/**` is excluded.
- **`create-drop` in production**: This DDL setting drops all tables on shutdown. Always switch to `validate` or `none` before deploying against a real database.
- **Anonymous URL in tests**: If a test overrides the datasource URL without a name, it gets a separate empty database and will miss seed data from `Application.java`.