# GraphQL Pagination

## When to use

Refer to this when a query returns a list that may grow large, or when a client needs cursor- or offset-based paging over a GraphQL collection.

## Patterns

**Current state: unpaged list queries**

The existing schema returns full collections. This is appropriate for small, bounded datasets (e.g., inventory items in a template). For larger datasets, add offset pagination as shown below.

```graphql
# Current — returns all items
type Query {
    items: [Item!]!
}
```

**Offset pagination (limit/offset)**

Add `limit` and `offset` arguments to the schema field, pass them through to the repository, and use Spring Data's `PageRequest`.

```graphql
type Query {
    items(limit: Int = 20, offset: Int = 0): [Item!]!
}
```

```java
@QueryMapping
public List<ItemResponse> items(@Argument int limit, @Argument int offset) {
    return itemService.findAll(PageRequest.of(offset / limit, limit));
}
```

```java
// ItemService — add an overload
public List<ItemResponse> findAll(Pageable pageable) {
    return itemRepository.findAll(pageable).stream()
            .map(ItemResponse::from)
            .toList();
}
```

**Returning total count alongside results**

Add a wrapper type to the schema so clients can compute total pages without a second request.

```graphql
type ItemPage {
    items: [Item!]!
    totalCount: Int!
}
type Query {
    itemsPage(limit: Int = 20, offset: Int = 0): ItemPage!
}
```

```java
public record ItemPage(List<ItemResponse> items, long totalCount) {}
```

## Pitfalls

- `@Argument int limit` defaults to `0` if the client omits it — always supply a schema default (`limit: Int = 20`) or guard against division-by-zero in `PageRequest.of(offset / limit, limit)`.
- Spring Data's `findAll(Pageable)` is defined on `PagingAndSortingRepository`; `JpaRepository` extends it, so `ItemRepository` already supports it without changes.