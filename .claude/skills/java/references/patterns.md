# Java Patterns

## When to use
Apply these patterns when writing or reviewing any class in `src/main/java/com/example/api/`.

## Record DTOs with factory methods
```java
public record ItemResponse(Long id, String name, BigDecimal price, Integer quantity) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getPrice(), item.getQuantity());
    }
}
```
Never use plain classes with getters for DTOs. Both request and response DTOs live in `dto/`.

## Lombok constructor injection
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        var item = new Item(req.name(), req.description(), req.price(), req.quantity());
        return ItemResponse.from(itemRepository.save(item));
    }
}
```
`@RequiredArgsConstructor` + `private final` is the only injection style used here. No `@Autowired` on fields.

## Text blocks for multi-line literals
```java
String jpql = """
        SELECT i FROM Item i
        WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """;
```
Use text blocks for any multi-line JPQL, JSON, or GraphQL string literals.

## Pitfalls
- Do not mix `javax.*` imports with `jakarta.*` — this project uses Jakarta EE throughout.
- `var` is fine for local variables but never use it for field declarations or method parameters.