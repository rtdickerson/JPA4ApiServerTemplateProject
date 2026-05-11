# JPA Entity Patterns

## When to use
Apply these patterns when adding or modifying JPA entities, repositories, or DTOs in `src/main/java/com/example/api/`.

## Pattern 1: Entity with Lombok and Jakarta imports
Use `jakarta.persistence.*` (not `javax.persistence.*`). Always pair with `@Data` and `@NoArgsConstructor` from Lombok.

```java
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "widgets")
@Data
@NoArgsConstructor
public class Widget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;
}
```

## Pattern 2: Repository with derived query and custom JPQL
Extend `JpaRepository<Entity, Long>`. Use derived method names for simple filters; `@Query` for anything more complex.

```java
public interface WidgetRepository extends JpaRepository<Widget, Long> {
    List<Widget> findByNameContainingIgnoreCase(String name);

    @Query("SELECT w FROM Widget w WHERE w.price < :max")
    List<Widget> findCheaperThan(@Param("max") BigDecimal max);
}
```

## Pattern 3: Record DTO with static factory
Entities never leave the service layer. Convert to a record DTO via a `static from()` factory.

```java
public record WidgetResponse(Long id, String name, BigDecimal price) {
    public static WidgetResponse from(Widget w) {
        return new WidgetResponse(w.getId(), w.getName(), w.getPrice());
    }
}
```

## Pitfalls
- **Wrong import namespace**: `javax.persistence.*` causes `ClassNotFoundException` at startup — always use `jakarta.persistence.*`.
- **Returning entities from controllers**: Triggers lazy-loading exceptions and leaks internal schema. Always map to a DTO before returning.
- **Missing `@NoArgsConstructor`**: JPA requires a no-arg constructor; `@Data` alone does not generate one.