# GraphQL Input Validation

## When to use

Refer to this when adding required-field constraints, null checks on optional inputs, or error responses for invalid arguments in a resolver.

## Patterns

**Schema-level nullability as the first gate**

Mark required fields with `!` in the schema. Spring for GraphQL rejects the request before the resolver runs if a required argument is absent, returning a standard GraphQL error automatically.

```graphql
input CreateItemInput {
    name: String!   # required — enforced by schema
    price: Float!   # required
    description: String  # optional — may be null
    quantity: Int!
}
```

**Null-safe partial update pattern**

For update inputs where every field is optional, check for `null` before applying. This mirrors `UpdateItemRequest` in `ItemService.update()`.

```java
@MutationMapping
public ItemResponse updateItem(@Argument Long id, @Argument Map<String, Object> input) {
    var priceRaw = input.get("price");
    var req = new UpdateItemRequest(
        (String) input.get("name"),           // null if omitted
        (String) input.get("description"),
        priceRaw != null ? new BigDecimal(priceRaw.toString()) : null,
        (Integer) input.get("quantity")
    );
    return itemService.update(id, req).orElse(null);
}
```

**Propagating domain errors as GraphQL errors**

Throw a `RuntimeException` (or a custom subclass) in the service layer; `GlobalExceptionHandler` maps it to an HTTP 4xx, but Spring for GraphQL wraps unhandled runtime exceptions into a `INTERNAL_ERROR` GraphQL response. For user-visible messages, throw `graphql.GraphQLException` with a descriptive message.

```java
// In service
if (price.compareTo(BigDecimal.ZERO) <= 0)
    throw new IllegalArgumentException("price must be positive");
```

## Pitfalls

- Spring for GraphQL does not run `@Valid` / Bean Validation on `@Argument Map<String, Object>` parameters — schema nullability is your only declarative gate. Add explicit null/range checks in the resolver or service for business rules.
- Missing keys in the `Map` return `null`, not an empty string. A cast like `(String) input.get("name")` on a missing key is safe; arithmetic on a missing numeric key will NPE.