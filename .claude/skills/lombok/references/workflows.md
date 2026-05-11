# Lombok Workflows

## When to use
Follow these workflows when creating a new entity/service pair, migrating an existing class to use Lombok, or verifying Lombok-generated code compiles after annotation changes.

## Adding a New Entity + Service Pair
1. Annotate the entity with `@Entity`, `@Data`, `@NoArgsConstructor` (and `@Table` if the table name differs).
2. Annotate the service with `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`.
3. Declare all injected repositories as `private final` fields — no explicit constructor needed.
4. Run `mvn compile` to confirm Lombok processes annotations without errors.

```bash
mvn compile
```

## Migrating a Class to Use Lombok
1. Identify boilerplate: explicit constructors, getters/setters, `toString`, `equals`/`hashCode`.
2. Remove the boilerplate methods.
3. Add the appropriate Lombok annotation (`@Data`, `@RequiredArgsConstructor`, etc.).
4. Add missing imports in the third-party block (after `org.springframework.*`, before `com.example.*`):
   ```java
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import lombok.RequiredArgsConstructor;
   ```
5. Run `mvn test` to confirm no regressions.

## Verifying Generated Code
Lombok generates code at compile time — IDEs and test runners must see the generated output.

```bash
# Full compile + test cycle
mvn clean test

# Inspect generated sources (optional)
ls target/generated-sources/
```

## Pitfalls
- **Missing `@NoArgsConstructor` on entities**: Hibernate will throw `InstantiationException` at startup if the no-arg constructor is absent. Always pair `@Data` with `@NoArgsConstructor` on `@Entity` classes.
- **Lombok version mismatch**: This project uses Lombok 1.18+. If `mvn compile` emits warnings about unsupported Java versions, verify the version in `pom.xml` and update before adding new annotations.