---
name: mcp
description: Implements MCP server tools and SSE transport for Claude Desktop integration in this Spring Boot 3 project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# MCP Skill

Guides implementation of MCP server tools and SSE transport configuration for this Spring Boot 3 project. Tools are defined in `src/main/java/com/example/api/mcp/` using MCP Java SDK 0.9.0 and wired into Spring WebMVC via `McpConfig.java`. Each tool delegates directly to a service method so REST, GraphQL, and MCP surfaces share identical business logic.

## Quick Start

**Add a new MCP tool** by appending a `SyncToolSpecification` factory method to `ItemMcpTools.all()`:

```java
// In ItemMcpTools.java — add method, then include in all()
private McpServerFeatures.SyncToolSpecification myTool() {
    var tool = new McpSchema.Tool(
        "my_tool",
        "What this tool does.",
        """
        {"type":"object","properties":{"id":{"type":"number"}},"required":["id"]}
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

**Verify the server starts** and lists tools:
```bash
mvn spring-boot:run
curl http://localhost:8080/mcp/sse   # should stream an SSE handshake event
```

## Key Concepts

| Concept | Detail |
|---------|--------|
| Transport | `WebMvcSseServerTransportProvider` — registers `GET /mcp/sse` and `POST /mcp/message` via Spring WebMVC router |
| Tool registration | `McpServer.sync(...).tools(itemMcpTools.all())` in `McpConfig.java` |
| Tool spec | `McpSchema.Tool(name, description, jsonSchemaString)` + `SyncToolSpecification` handler lambda |
| Input schema | Inline JSON Schema string passed to `McpSchema.Tool` constructor; use text blocks for multi-field schemas |
| Result | `McpSchema.CallToolResult` — use `ok(String)` for success, `err(Exception)` for errors; `isError=true` signals tool failure to the client |
| Auth | MCP endpoints are currently unauthenticated — `SecurityConfig` permits `/mcp/**` without a JWT token |
| Server capabilities | Declared in `McpConfig.mcpSyncServer()` — `tools(false)` disables list-changed notifications; `prompts(true)` enables prompt notifications |

## Common Patterns

**Empty input schema** (tool takes no arguments):
```java
private static String emptySchema() {
    return "{\"type\":\"object\",\"properties\":{}}";
}
```

**Numeric argument coercion** (JSON numbers arrive as `Double` or `Integer`):
```java
private static long toLong(Object value) {
    if (value instanceof Number n) return n.longValue();
    return Long.parseLong(value.toString());
}
```

**Standard result helpers** (copy from `ItemMcpTools`):
```java
private static McpSchema.CallToolResult ok(String text) {
    return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
}
private static McpSchema.CallToolResult err(Exception e) {
    log.error("MCP tool error", e);
    return new McpSchema.CallToolResult(
        List.of(new McpSchema.TextContent("Error: " + e.getMessage())), true);
}
```

**Adding a second tool class** (e.g., `PromptMcpTools`): declare it `@Component`, inject the relevant service, expose an `all()` method returning `List<SyncToolSpecification>`, then concatenate in `McpConfig`:
```java
.tools(Stream.of(itemMcpTools.all(), promptMcpTools.all())
             .flatMap(List::stream).toList())
```

**Claude Desktop config** — point at the running server's SSE endpoint:
```json
{
  "mcpServers": {
    "item-api": { "url": "http://localhost:8080/mcp/sse" }
  }
}
```