# Module Map

## When to use
Use this to locate the right layer before adding or editing code.

## Request lifecycle
```
HTTP request
  → JwtAuthenticationFilter       (security/)
  → Controller                    (controller/ or graphql/)
  → Service                       (@Transactional boundary)
  → Repository                    (Spring Data JPA)
  → Entity / Database
```
MCP tools in `mcp/ItemMcpTools.java` call `ItemService` directly, bypassing controllers.

## Layer responsibilities
| Layer | Package | Responsibility |
|-------|---------|----------------|
| Controller | `controller/` | Validate input, delegate to service, map HTTP status |
| GraphQL resolver | `graphql/` | Map schema queries/mutations to service calls |
| MCP tools | `mcp/` | Expose service operations as JSON-RPC tools |
| Service | `service/` | Business logic, transactions, entity ↔ DTO conversion |
| Repository | `repository/` | Spring Data `JpaRepository`; custom JPQL queries |
| Entity | `entity/` | JPA-mapped domain objects; no business logic |
| DTO | `dto/` | Immutable records for API input/output |
| Security | `security/` | JWT creation (`JwtTokenProvider`) and filter (`JwtAuthenticationFilter`) |

## Adding a new resource (e.g. `Order`)
1. `entity/Order.java` — JPA entity
2. `repository/OrderRepository.java` — extends `JpaRepository<Order, Long>`
3. `dto/` — `CreateOrderRequest`, `UpdateOrderRequest`, `OrderResponse` records
4. `service/OrderService.java` — `@Transactional(readOnly = true)` class
5. `controller/OrderController.java` — REST endpoints at `/api/orders`
6. (Optional) Add GraphQL schema types and resolvers; add MCP tools.

## Pitfalls
- Never inject a `Repository` directly into a `Controller` — always go through a `Service`.
- `McpConfig.java` registers tools at startup; new `@McpTool` methods are picked up automatically only if the bean is in the Spring context.