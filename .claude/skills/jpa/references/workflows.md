# JPA Development Workflows

## When to use
Follow these workflows when adding a new entity end-to-end, evolving an existing schema, or debugging persistence issues in this project.

## Workflow 1: Add a new entity end-to-end
1. Create the entity in `src/main/java/com/example/api/entity/` following the Lombok + Jakarta pattern.
2. Create the repository in `repository/` extending `JpaRepository<Entity, Long>`.
3. Add request/response DTOs in `dto/` as Java records with `static from()` factories.
4. Add a `@Service` with `@Transactional(readOnly = true)` at class level; override with `@Transactional` on write methods.
5. Wire the service into a `@RestController` at `/api/<resource>`.
6. Run `mvn spring-boot:run` and verify schema at `http://localhost:8080/h2-console` (JDBC: `jdbc:h2:mem:itemdb`).

## Workflow 2: Evolve an existing entity
1. Read the current entity (`Item.java` or `Prompt.java`) and its repository before changing anything.
2. Add/rename columns on the entity class; Hibernate regenerates the H2 schema on restart (`ddl-auto=create-drop`).
3. Update the matching DTO record and its `from()` factory to include the new field.
4. Run `mvn test` — `ApplicationTests` runs a full Spring context and will catch mapping failures.

## Workflow 3: Debug a persistence issue
1. Enable SQL logging: add `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true` to `application.properties`.
2. Check the H2 console for actual table structure and row data.
3. For `LazyInitializationException`, add `@Transactional` to the calling service method or switch the association fetch type to `EAGER` (only if the association is always needed).
4. Remove the SQL logging properties before committing.

## Pitfalls
- **`create-drop` in production**: The default H2 DDL mode drops all tables on shutdown. Set `ddl-auto=validate` and use a real database before deploying.
- **Bidirectional relationship ownership**: Always set the owning side (the side without `mappedBy`) when persisting; forgetting leaves the FK column null.
- **Transaction boundary mismatch**: Opening a transaction in a controller bypasses the service layer contract — keep `@Transactional` in the service only.