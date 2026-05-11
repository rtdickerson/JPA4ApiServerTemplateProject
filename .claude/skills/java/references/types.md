# Java Types Reference

## When to use
Consult this when adding new entities, DTOs, or service return types in this codebase.

## Entity types
JPA entities in `entity/` use `@Data` (Lombok) and Jakarta annotations:
```java
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
```
Entities are never returned from service methods — always map to a DTO.

## Optional for nullable finders
```java
// Service
public Optional<ItemResponse> findById(Long id) {
    return itemRepository.findById(id).map(ItemResponse::from);
}

// Controller — unwrap with 404 fallback
return service.findById(id)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```
Service finders always return `Optional<T>`. Never return `null`.

## Request/response DTO naming
| Purpose | Naming convention | Example |
|---------|------------------|---------|
| Create payload | `Create{Entity}Request` | `CreateItemRequest` |
| Update payload | `Update{Entity}Request` | `UpdateItemRequest` |
| Response body | `{Entity}Response` | `ItemResponse` |
| Auth pair | `AuthRequest` / `AuthResponse` | — |

## Pitfalls
- `BigDecimal` for all monetary values — never `double` or `float`.
- Update request fields should be `@Nullable` or boxed types so absent fields can be detected and skipped during partial updates.