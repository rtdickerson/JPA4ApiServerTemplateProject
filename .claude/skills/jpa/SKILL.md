---
name: jpa
description: Designs JPA entity models and persistence mappings using Jakarta Persistence 3.x for this Spring Boot 3 project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Jpa Skill

Assists with designing, implementing, and evolving JPA entity models and persistence mappings in this Spring Boot 3 project using Jakarta Persistence 3.x with Hibernate 6. Entities live in `src/main/java/com/example/api/entity/`, repositories in `repository/`, and the schema is auto-generated from `spring.jpa.hibernate.ddl-auto=create-drop` (H2 dev) or validated in production.

## Quick Start

1. Read an existing entity to understand conventions:
   - `src/main/java/com/example/api/entity/Item.java`
   - `src/main/java/com/example/api/entity/Prompt.java`
2. Read the matching repository for query patterns:
   - `src/main/java/com/example/api/repository/ItemRepository.java`
3. Check `src/main/resources/application.properties` for datasource and DDL settings.
4. Run `mvn spring-boot:run` and verify via `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:itemdb`).

## Key Concepts

- **Jakarta Persistence 3.x**: Use `jakarta.persistence.*` imports (not `javax.persistence.*`).
- **Entity annotations**: `@Entity`, `@Table(name = "...")`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Lombok**: Use `@Data` (or `@Getter`/`@Setter`) and `@RequiredArgsConstructor` to reduce boilerplate; avoid manual getters/setters.
- **Spring Data JPA**: Extend `JpaRepository<Entity, Long>`; derived query methods and `@Query` for custom JPQL.
- **Transaction management**: Service layer owns transactions; `@Transactional(readOnly = true)` on class, `@Transactional` overrides mutations.
- **DTO separation**: Entities are never returned directly from controllers; use record-based DTOs with a `static from(Entity e)` factory.

## Common Patterns

**Define a new entity:**
```java
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

**Spring Data repository with custom query:**
```java
public interface WidgetRepository extends JpaRepository<Widget, Long> {
    List<Widget> findByNameContainingIgnoreCase(String name);

    @Query("SELECT w FROM Widget w WHERE w.price < :max")
    List<Widget> findCheaperThan(@Param("max") BigDecimal max);
}
```

**Record DTO with factory:**
```java
public record WidgetResponse(Long id, String name, BigDecimal price) {
    public static WidgetResponse from(Widget w) {
        return new WidgetResponse(w.getId(), w.getName(), w.getPrice());
    }
}
```

**One-to-many relationship:**
```java
@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Child> children = new ArrayList<>();
```

**Import order for entity files:**
```java
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
```