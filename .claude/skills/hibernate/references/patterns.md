# Hibernate Patterns

## When to use
Apply these patterns when adding entities, modifying schema, or tuning query performance in `src/main/java/com/example/api/entity/` or `src/main/java/com/example/api/repository/`.

## Entity Mapping
Declare all columns explicitly to avoid Hibernate guessing nullability and precision:
```java
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;
}
```

## Custom Search Query
Use `JPQL` with `LOWER/CONCAT` for case-insensitive name search — the pattern already used in `ItemRepository`:
```java
@Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%'))")
List<Item> searchByName(@Param("term") String term);
```

## Transaction Scoping
Default services to `readOnly = true`; override only on mutating methods:
```java
@Service
@Transactional(readOnly = true)
public class ItemService {
    public List<ItemResponse> findAll() { ... }          // read-only

    @Transactional
    public ItemResponse create(CreateItemRequest req) { ... }  // read-write
}
```

## Pitfalls
- **`create-drop` in production** — schema is wiped on shutdown. Always switch to `validate` before deploying.
- **Eager `@ManyToOne` + list queries** — default `EAGER` fetch on associations causes N+1 selects. Annotate with `fetch = FetchType.LAZY` and load with `JOIN FETCH` only when needed.
- **Missing `@Column(nullable = false)`** — Hibernate will infer nullable; schema may diverge from intent when using `validate` mode.