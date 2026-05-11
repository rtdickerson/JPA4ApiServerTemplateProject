---
name: lombok
description: Uses Lombok annotations to reduce boilerplate with @Data, @RequiredArgsConstructor and related annotations in this Spring Boot 3 / Java 17 codebase
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Lombok Skill

Applies Lombok annotations to eliminate boilerplate constructors, getters, setters, and `toString`/`equals`/`hashCode` implementations across the JPA4 API Server codebase (Java 17, Lombok 1.18+).

## Quick Start

Lombok is already on the classpath via `pom.xml`. Add annotations directly to classes — no additional configuration is needed. Use `mvn compile` to verify generated code compiles cleanly.

## Key Concepts

| Annotation | Generates | Use on |
|---|---|---|
| `@Data` | getters, setters, `toString`, `equals`, `hashCode` | Mutable entities and request DTOs |
| `@RequiredArgsConstructor` | Constructor for all `final` fields | `@Service`, `@Component`, `@Configuration` beans |
| `@NoArgsConstructor` | Zero-arg constructor | JPA entities (required by Hibernate) |
| `@AllArgsConstructor` | All-fields constructor | Value objects, test builders |
| `@Builder` | Builder pattern | Complex object construction |
| `@Slf4j` | `private static final Logger log` | Any class needing logging |
| `@Value` | Immutable class (all fields `final`, no setters) | Immutable value objects |

**JPA entities need both `@NoArgsConstructor` and `@Data` (or explicit `@Getter`/`@Setter`)** because Hibernate requires a no-arg constructor at runtime.

## Common Patterns

### Service bean (constructor injection)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    private final ItemRepository itemRepository;  // injected via generated constructor
}
```

### JPA entity
```java
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
}
```

### Controller bean
```java
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
}
```

### Avoid `@Data` on JPA entities with bidirectional relationships — use `@Getter`/`@Setter` instead to prevent infinite `toString`/`hashCode` loops.

### Import
```java
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
```

Imports follow project convention: Lombok imports belong in the third-party block, after `org.springframework.*` and before `com.example.*`.