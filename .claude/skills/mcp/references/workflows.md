# MCP Development Workflows

## When to use
Follow these workflows when standing up the MCP server for the first time, adding a new tool class, or verifying Claude Desktop connectivity.

## Workflow: Add a tool to an existing class
1. Write a private factory method in `ItemMcpTools.java` following the tool patterns.
2. Add a reference to it inside `all()`:
   ```java
   public List<McpServerFeatures.SyncToolSpecification> all() {
       return List.of(listItems(), getItem(), searchItems(),
                      createItem(), updateItem(), deleteItem(),
                      myNewTool()); // ← append here
   }
   ```
3. Start the server and confirm the tool appears in the SSE handshake:
   ```bash
   mvn spring-boot:run &
   curl -N http://localhost:8080/mcp/sse
   # Look for "tools/list" event containing your tool name
   ```
4. Send a test call via JSON-RPC:
   ```bash
   curl -X POST http://localhost:8080/mcp/message \
     -H 'Content-Type: application/json' \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"my_new_tool","arguments":{}}}'
   ```

## Workflow: Add a second tool class (e.g., PromptMcpTools)
1. Create `PromptMcpTools.java` annotated `@Component @RequiredArgsConstructor`; inject `PromptService` and `ObjectMapper`.
2. Expose `public List<SyncToolSpecification> all()` returning your tool specs.
3. Inject the new class into `McpConfig` and concatenate with `itemMcpTools.all()`:
   ```java
   .tools(Stream.of(itemMcpTools.all(), promptMcpTools.all())
                .flatMap(List::stream).toList())
   ```
4. Rebuild and re-verify via `curl -N http://localhost:8080/mcp/sse`.

## Workflow: Connect Claude Desktop
1. Ensure the server is running on port 8080.
2. Add to `claude_desktop_config.json`:
   ```json
   { "mcpServers": { "item-api": { "url": "http://localhost:8080/mcp/sse" } } }
   ```
3. Restart Claude Desktop — it subscribes on startup and lists available tools.
4. Test by asking Claude to call `list_items`; confirm inventory rows appear in the response.

## Pitfalls
- **Security bypass awareness**: `/mcp/**` is permitted without JWT in `SecurityConfig`. Adding auth at the transport level requires a custom `HandshakeInterceptor` on the SSE endpoint — do not rely on controller-level `@PreAuthorize` alone.
- **ObjectMapper bean conflict**: `McpConfig` declares its own `ObjectMapper` bean. If another `@Configuration` class also declares one without a qualifier, Spring will throw an `UnsatisfiedDependencyException` at startup — use `@Primary` or `@Qualifier` to disambiguate.
- **SSE connection drops on idle**: Long-running Claude Desktop sessions may drop the SSE stream if no keep-alive is sent; the MCP SDK handles ping frames, but corporate proxies with short idle timeouts will still disconnect — configure `server.tomcat.connection-timeout` if needed.