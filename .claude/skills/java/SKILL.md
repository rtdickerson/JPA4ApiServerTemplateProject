---
name: java
description: Enforces Java type safety and modern Java 17+ patterns like records and text blocks in this Spring Boot 3 codebase
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Java Skill

Enforces modern Java 17+ idioms and type safety across this Spring Boot 3 project. Audits and rewrites code to use records for DTOs, text blocks for multi-line strings, `var` for local type inference, sealed classes where appropriate, and proper use of `Optional` instead of null returns. Applies project conventions: Lombok `@RequiredArgsConstructor` for injection, `@Transactional(readOnly = true)` defaults on services, and Jakarta (not `javax`) imports throughout.

## Quick Start

Invoke `/java` to scan Java source files for anti-patterns and upgrade them to Java 17+ equivalents. Point it at a specific file or package, or let it sweep `src/main/java/com/example/api/` and `src/test/`.

```bash
# Compile-check after edits
mvn compile

# Full test pass to verify nothing regressed
mvn test
```

## Key Concepts

| Concept | Rule |
|---------|------|
| DTOs | Use `record` with a static `from(Entity)` factory; never plain classes with getters |
| Injection | `@RequiredArgsConstructor` + `private final` fields; no `@Autowired` on fields |
| Nullability | Return `Optional<T>` from service finders; never return `null` |
| Transactions | Class-level `@Transactional(readOnly = true)`; override with `@Transactional` on mutations |
| Imports | `jakarta.*` not `javax.*`; import order: `java` → `jakarta` → `org.springframework` → third-party → `com.example` |
| Strings | Text blocks (`"""..."""`) for multi-line JSON, SQL, or GraphQL literals |

## Common Patterns

**Record DTO with factory:**
```java
public record ItemResponse(Long id, String name, BigDecimal price, Integer quantity) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getPrice(), item.getQuantity());
    }
}
```

**Service with Lombok injection and transaction defaults:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    private final ItemRepository itemRepository;

    public Optional<ItemResponse> findById(Long id) {
        return itemRepository.findById(id).map(ItemResponse::from);
    }

    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        var item = new Item(req.name(), req.description(), req.price(), req.quantity());
        return ItemResponse.from(itemRepository.save(item));
    }
}
```

**Text block for multi-line content:**
```java
String query = """
        SELECT i FROM Item i
        WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """;
```

**Pattern matching (instanceof):**
```java
if (ex instanceof EntityNotFoundException nfe) {
    return ResponseEntity.status(404).body(new ErrorResponse(nfe.getMessage()));
}
```