---
name: hibernate
description: Configures Hibernate ORM, entity relationships, and database DDL generation for the JPA4 API Server Template project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Hibernate Skill

Manages Hibernate 6 / Jakarta Persistence 3.x configuration in this Spring Boot 3 project, including entity modeling, relationship mapping, DDL strategy, and datasource switching between H2 (dev) and Postgres/MySQL (prod).

## Quick Start

Entities live in `src/main/java/com/example/api/entity/`. Configuration is in `src/main/resources/application.properties`. The active DDL strategy is `create-drop` (dev); switch to `validate` before production.

```bash
# Verify entity/schema alignment after changes
mvn test -Dtest=ApplicationTests

# Full build with schema validation
mvn clean install
```

## Key Concepts

**DDL strategies** (`spring.jpa.hibernate.ddl-auto`):
- `create-drop` — recreate schema on startup/shutdown (dev/test only)
- `update` — migrate schema in place (use with caution)
- `validate` — assert schema matches entities, fail fast (production)
- `none` — hands-off; manage schema externally

**Entity anatomy** (see `Item.java`, `Prompt.java`):
```java
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;
}
```

**Dialect auto-detection**: Spring Boot 3 + Hibernate 6 infer the dialect from the JDBC URL — explicit `spring.jpa.database-platform` is only needed for non-standard drivers.

## Common Patterns

**Switching to Postgres** (edit `application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=validate
```
Add the driver dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Read-only transactions** (already used in services):
```java
@Transactional(readOnly = true)   // optimises flush/dirty-check
public List<ItemResponse> findAll() { ... }

@Transactional                    // read-write for mutations
public ItemResponse create(CreateItemRequest req) { ... }
```

**Custom JPQL in repositories** (pattern used by `ItemRepository`):
```java
@Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%'))")
List<Item> searchByName(@Param("term") String term);
```

**Lazy vs eager loading**: default fetch for `@ManyToOne` is `EAGER`; default for `@OneToMany` is `LAZY`. Prefer `LAZY` and load explicitly to avoid N+1 queries.

**N+1 guard** — use JOIN FETCH for collections fetched in a list query:
```java
@Query("SELECT i FROM Item i LEFT JOIN FETCH i.tags WHERE i.id = :id")
Optional<Item> findByIdWithTags(@Param("id") Long id);
```

**Schema inspection (dev)**:
Open `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:itemdb`, no credentials required.