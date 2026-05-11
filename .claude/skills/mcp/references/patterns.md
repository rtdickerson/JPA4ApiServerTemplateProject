# MCP Tool Patterns

## When to use
Apply these patterns when adding, modifying, or debugging MCP tool definitions in `ItemMcpTools.java` or a new `*McpTools` component.

## Pattern: Single-argument tool with numeric coercion
JSON-RPC delivers numbers as `Double` or `Integer`; always coerce via `toLong`:
```java
private McpServerFeatures.SyncToolSpecification getItem() {
    var tool = new McpSchema.Tool(
        "get_item", "Get item by ID.",
        """
        {"type":"object","properties":{"id":{"type":"number","description":"Item ID"}},"required":["id"]}
        """
    );
    return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
        try {
            long id = toLong(args.get("id"));
            return ok(objectMapper.writeValueAsString(itemService.findById(id)));
        } catch (Exception e) {
            return err(e);
        }
    });
}
```

## Pattern: No-argument tool
Use the empty schema helper; omit `"required"`:
```java
private McpServerFeatures.SyncToolSpecification listItems() {
    var tool = new McpSchema.Tool("list_items", "Return all items.", emptySchema());
    return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
        try {
            return ok(objectMapper.writeValueAsString(itemService.findAll()));
        } catch (Exception e) {
            return err(e);
        }
    });
}
```

## Pattern: Multi-field create tool
Declare all optional fields in the schema but check for `null` before passing to the service:
```java
private McpServerFeatures.SyncToolSpecification createItem() {
    var tool = new McpSchema.Tool("create_item", "Create a new item.",
        """
        {
          "type":"object",
          "properties":{
            "name":       {"type":"string"},
            "description":{"type":"string"},
            "price":      {"type":"number"},
            "quantity":   {"type":"integer"}
          },
          "required":["name","price","quantity"]
        }
        """
    );
    return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
        try {
            var req = new CreateItemRequest(
                (String) args.get("name"),
                (String) args.getOrDefault("description", ""),
                new BigDecimal(args.get("price").toString()),
                ((Number) args.get("quantity")).intValue()
            );
            return ok(objectMapper.writeValueAsString(itemService.create(req)));
        } catch (Exception e) {
            return err(e);
        }
    });
}
```

## Pitfalls
- **Forgetting to add to `all()`**: A tool method that isn't returned from `all()` is never registered — the server starts silently without it.
- **Returning `isError=false` on exceptions**: Always use `err(e)` (sets `isError=true`) so Claude Desktop surfaces the failure instead of silently returning empty content.
- **Mismatched schema types**: Declaring `"type":"integer"` in the schema but calling `toLong` on a field that arrives as `String` when the client sends a quoted value — always call `.toString()` before parsing.