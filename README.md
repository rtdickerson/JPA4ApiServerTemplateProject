# JPA 4 API Server Template

Minimal viable Spring Boot 3 server exposing an **Item** repository over three access surfaces, all secured with JWT.

| Surface | Protocol | Base path |
|---------|----------|-----------|
| REST | HTTP/JSON | `/api/items` |
| GraphQL | HTTP/GraphQL | `/graphql` |
| MCP | SSE + JSON-RPC | `/mcp/sse` |

---

## Stack

| Concern | Library |
|---------|---------|
| Persistence | JPA 4 (Jakarta Persistence 3.x) + Hibernate 6 |
| Serialisation | Jackson (via Spring Boot autoconfigure) |
| REST | Spring MVC |
| GraphQL | Spring for GraphQL |
| MCP server | MCP Java SDK 0.9.0 — WebMVC SSE transport |
| Security | Spring Security + JJWT 0.12 (HS256) |
| Database | H2 in-memory (swap `spring.datasource.*` for Postgres/MySQL) |

---

## Quick start

```bash
mvn spring-boot:run
```

### 1. Get a JWT token

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq .
```

Copy the `token` value into `$TOKEN` for the examples below.

---

### 2. REST API

```bash
# List all items
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/items

# Get by id
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/items/1

# Search
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/items?search=widget"

# Create
curl -X POST http://localhost:8080/api/items \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"New Item","description":"desc","price":14.99,"quantity":5}'

# Update (partial)
curl -X PUT http://localhost:8080/api/items/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"price":7.99}'

# Delete
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/items/1
```

---

### 3. GraphQL

Interactive GraphiQL UI: <http://localhost:8080/graphiql>

Example query via curl:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ items { id name price quantity } }"}'
```

Example mutation:

```graphql
mutation {
  createItem(input: { name: "Widget C", price: 5.99, quantity: 200 }) {
    id name price
  }
}
```

---

### 4. MCP

The server exposes an SSE-based MCP endpoint compatible with Claude Desktop and any MCP client.

**Endpoints**

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/mcp/sse` | Client subscribes — server streams events |
| `POST` | `/mcp/message` | Client sends JSON-RPC requests |

**Claude Desktop config** (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "item-api": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

**Available MCP tools**

| Tool | Description |
|------|-------------|
| `list_items` | Return all inventory items |
| `get_item` | Get item by numeric ID |
| `search_items` | Search by name fragment |
| `create_item` | Create a new item |
| `update_item` | Partially update an item |
| `delete_item` | Delete an item |

> MCP endpoints are currently open (no JWT required) so AI clients can connect without headers. Add token validation inside the tool handlers or at the transport level if you need stricter auth.

---

## Dev tooling

| URL | Purpose |
|-----|---------|
| <http://localhost:8080/h2-console> | H2 web console (JDBC URL: `jdbc:h2:mem:itemdb`) |
| <http://localhost:8080/graphiql> | GraphiQL IDE |

---

## Configuration

Key properties in `src/main/resources/application.properties`:

```properties
jwt.secret=change-me-to-a-256-bit-secret-before-production-use!!
jwt.expiration-ms=86400000   # 24 h

spring.datasource.url=jdbc:h2:mem:itemdb   # swap for real DB
spring.jpa.hibernate.ddl-auto=create-drop  # use validate/none in prod
```

**Production checklist**
- Replace the JWT secret with a randomly generated 32+ byte value.
- Replace H2 with a real database and set `ddl-auto=validate`.
- Replace `InMemoryUserDetailsManager` with a database-backed `UserDetailsService`.
- Set `spring.h2.console.enabled=false`.
- Set `spring.graphql.graphiql.enabled=false`.

---

## Project layout

```
src/main/java/com/example/api/
├── Application.java              # Boot entry point + seed data
├── config/
│   ├── SecurityConfig.java       # Spring Security + JWT filter wiring
│   └── McpConfig.java            # MCP server + SSE transport
├── controller/
│   ├── AuthController.java       # POST /auth/login
│   └── ItemController.java       # REST CRUD /api/items
├── graphql/
│   └── ItemGraphQLController.java
├── mcp/
│   └── ItemMcpTools.java         # MCP tool definitions
├── entity/Item.java
├── repository/ItemRepository.java
├── service/ItemService.java
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
├── dto/                          # Records: AuthRequest/Response, ItemResponse, ...
└── exception/GlobalExceptionHandler.java
```
