# Hibernate Workflows

## When to use
Follow these workflows when creating a new entity, migrating the schema, or switching from H2 to a production database.

## Adding a New Entity
1. Create the entity class in `src/main/java/com/example/api/entity/` following the `Item.java` pattern.
2. Create a `JpaRepository` subinterface in `src/main/java/com/example/api/repository/`.
3. Add seed data in `Application.java` (existing `CommandLineRunner` bean).
4. Run `mvn test -Dtest=ApplicationTests` — `create-drop` will generate the table and the test will verify the context loads cleanly.

## Switching to Postgres
1. Replace H2 properties in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=validate
```
2. Add the driver to `pom.xml` (remove the H2 dependency or scope it to `test`):
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
3. Create the schema externally (Flyway, Liquibase, or DDL script), then start the app — `validate` will assert alignment without mutating the schema.

## Schema Inspection (Dev)
Open `http://localhost:8080/h2-console` while the server is running. JDBC URL: `jdbc:h2:mem:itemdb`, no credentials. Use `SHOW TABLES` and `SHOW COLUMNS FROM items` to confirm generated DDL matches entity definitions before switching DDL strategy.

## Pitfalls
- **`update` mode** silently leaves orphaned columns when you rename a field — never use in shared environments.
- **`validate` with H2** will fail on startup if the in-memory DB is empty (no prior migration ran). Use `create-drop` for local H2 and `validate` only against a pre-migrated Postgres instance.
- **Running `mvn clean`** drops `target/` but does not reset the in-memory H2 schema — restart the Spring Boot process to get a clean slate.