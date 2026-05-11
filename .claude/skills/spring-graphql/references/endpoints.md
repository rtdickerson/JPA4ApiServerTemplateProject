# GraphQL Endpoints

## When to use

Refer to this when wiring a new query or mutation, or when debugging a resolver that isn't being invoked.

## Patterns

**Schema-first field declaration**

Every resolver method must have a matching field in `schema.graphqls`. The method name in the controller must exactly match the query or mutation name.

```graphql
# src/main/resources/graphql/schema.graphqls
type Query {
    item(id: ID!): Item
    items: [Item!]!
    searchItems(name: String!): [Item!]!
}
type Mutation {
    createItem(input: CreateItemInput!): Item!
    deleteItem(id: ID!): Boolean!
}
```

**Resolver method mapping**

Use `@QueryMapping` for `Query` fields and `@MutationMapping` for `Mutation` fields. Arguments bind by name via `@Argument`. Scalar `ID` maps to `Long` automatically.

```java
@QueryMapping
public ItemResponse item(@Argument Long id) {
    return itemService.findById(id).orElse(null);
}

@MutationMapping
public boolean deleteItem(@Argument Long id) {
    return itemService.delete(id);
}
```

**Input object binding**

Spring for GraphQL coerces `input` arguments to `Map<String, Object>`. Construct DTOs manually; numeric types from the map arrive as `Integer` or `Double`, never `BigDecimal` — convert via `.toString()`.

```java
@MutationMapping
public ItemResponse createItem(@Argument Map<String, Object> input) {
    var req = new CreateItemRequest(
        (String) input.get("name"),
        (String) input.get("description"),
        new BigDecimal(input.get("price").toString()),
        (Integer) input.get("quantity")
    );
    return itemService.create(req);
}
```

## Pitfalls

- A method name mismatch between controller and schema causes a silent no-op at startup — the resolver is never registered and callers receive `null`. Always verify names match exactly.
- `Float` in GraphQL arrives as `Double` in Java, not `Float`. Use `new BigDecimal(value.toString())` rather than casting directly to `BigDecimal`.