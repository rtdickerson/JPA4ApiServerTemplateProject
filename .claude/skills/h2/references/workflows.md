# H2 Workflows

## When to use
Follow these workflows when starting the dev server, inspecting live data, running integration tests, or switching the project to a persistent database.

## Workflow 1 — Browse live data with the H2 console

1. Start the server: `mvn spring-boot:run`
2. Open `http://localhost:8080/h2-console` in a browser.
3. Set connection fields:
   - JDBC URL: `jdbc:h2:mem:itemdb`
   - User Name: `sa`
   - Password: *(leave blank)*
4. Click **Connect** — the `ITEM` and `PROMPT` tables will be visible.
5. Run ad-hoc SQL to inspect or mutate seed data while the application is running.

> The console is only available when `spring.h2.console.enabled=true` (set in `application.properties`; enabled by default in this project).

## Workflow 2 — Run integration tests against H2

Spring Boot auto-configures H2 for `@SpringBootTest` with no extra setup. The test context starts fresh for each test class and drops the schema on teardown.

```bash
mvn test                          # all tests
mvn test -Dtest=ApplicationTests  # one class
mvn verify                        # tests + coverage report
```

If a test needs a clean slate within a class, annotate the test method with `@Transactional` — Spring rolls back the transaction after each test, leaving seed data intact for the next.

## Workflow 3 — Swap H2 for Postgres

1. Add the Postgres driver to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
   </dependency>
   ```
2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.datasource.username=postgres
   spring.datasource.password=secret
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   spring.jpa.hibernate.ddl-auto=validate
   spring.h2.console.enabled=false
   ```
3. Create the schema manually (Flyway or Liquibase recommended) before starting the server — `validate` will fail if tables are missing.

## Pitfalls

- **H2 console unavailable after `permitAll` is removed**: If `SecurityConfig` no longer whitelists `/h2-console/**`, the console returns 403. Re-add the matcher before debugging database state.
- **Data disappears between restarts**: Expected behavior with `create-drop`. If you need data to survive restarts during development, switch to `jdbc:h2:file:./data/itemdb` and set `ddl-auto=update`.
- **Test isolation broken by shared state**: Avoid `@Commit` in tests; it persists rows that bleed into subsequent tests within the same context lifecycle.