# JPA4 API Server Template

A minimal viable Spring Boot 3 server exposing an **Item** repository and **Prompt** templates over three access surfaces (REST, GraphQL, MCP), all secured with JWT authentication. Designed as both a production-ready template and reference implementation for multi-protocol API servers.

## Tech Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Runtime | Java | 17 | Modern LTS release; supports records, text blocks, sealed classes |
| Framework | Spring Boot | 3.4.1 | Auto-configuration, embedded Tomcat; Spring 6.1+ |
| Persistence | JPA / Hibernate | 6.x (Jakarta) | ORM with JPA 4 (Jakarta Persistence 3.x) |
| REST | Spring MVC | 3.x | Controller-based REST endpoints |
| GraphQL | Spring for GraphQL | 1.x | Schema-first GraphQL with `schema.graphqls` |
| MCP Server | MCP Java SDK | 0.9.0 | SSE transport; Claude Desktop integration |
| Security | Spring Security + JJWT | 6.x + 0.12.6 | HS256 JWT; stateless auth |
| Database | H2 in-memory | 2.x | Dev/test; swappable for Postgres/MySQL |
| Build | Maven | 3.8+ | Dependency management via `pom.xml` |
| Utilities | Lombok | 1.18+ | Boilerplate reduction (@Data, @RequiredArgsConstructor) |

## Quick Start

### Prerequisites

- **Java 17+** (OpenJDK or Eclipse Adoptium)
- **Maven 3.8+** (or use `mvn` wrapper)
- **Git** (for cloning)

### Installation & Development

```bash
# Clone the repository
git clone <repo-url>
cd JPA4ApiServerTemplateProject

# Build and run (Maven wrapper handles Maven auto-install)
mvn clean install
mvn spring-boot:run
```

Server starts on `http://localhost:8080` by default.

### Testing

```bash
# Run all unit and integration tests
mvn test

# Run a specific test class
mvn test -Dtest=ApplicationTests

# Generate test coverage report
mvn verify
```

### Build for Production

```bash
# Create a JAR executable
mvn clean package -DskipTests

# Run the JAR
java -jar target/jpa4-api-server-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
JPA4ApiServerTemplateProject/
├── src/
│   ├── main/
│   │   ├── java/com/example/api/
│   │   │   ├── Application.java                # Boot entry; seed data
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java         # Spring Security + JWT filter
│   │   │   │   └── McpConfig.java              # MCP server + SSE transport
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         # POST /auth/login
│   │   │   │   ├── ItemController.java         # REST CRUD /api/items
│   │   │   │   └── PromptController.java       # REST CRUD /api/prompts
│   │   │   ├── service/
│   │   │   │   ├── ItemService.java            # Item business logic
│   │   │   │   └── PromptService.java          # Prompt business logic
│   │   │   ├── repository/
│   │   │   │   ├── ItemRepository.java         # JPA repository (Item)
│   │   │   │   └── PromptRepository.java       # JPA repository (Prompt)
│   │   │   ├── entity/
│   │   │   │   ├── Item.java                   # Item JPA entity
│   │   │   │   └── Prompt.java                 # Prompt JPA entity
│   │   │   ├── dto/
│   │   │   │   ├── CreateItemRequest.java      # Request DTO
│   │   │   │   ├── UpdateItemRequest.java      # Request DTO
│   │   │   │   ├── ItemResponse.java           # Response DTO
│   │   │   │   ├── CreatePromptRequest.java    # Request DTO
│   │   │   │   ├── UpdatePromptRequest.java    # Request DTO
│   │   │   │   ├── PromptResponse.java         # Response DTO
│   │   │   │   ├── AuthRequest.java            # Login request
│   │   │   │   └── AuthResponse.java           # Login response
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java       # Token generation/validation
│   │   │   │   └── JwtAuthenticationFilter.java # Request filter
│   │   │   ├── mcp/
│   │   │   │   └── ItemMcpTools.java           # MCP tool definitions
│   │   │   ├── graphql/
│   │   │   │   └── ItemGraphQLController.java  # GraphQL resolvers
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java # Exception mapping
│   │   └── resources/
│   │       ├── application.properties          # Runtime config
│   │       └── graphql/
│   │           └── schema.graphqls             # GraphQL schema
│   ├── test/
│   │   └── java/com/example/api/
│   │       └── ApplicationTests.java           # Integration tests
│   └── pom.xml                                 # Maven configuration
├── .claude/
│   ├── CLAUDE.md                               # This file (alt location)
│   ├── settings.local.json                     # Claude Code local settings
│   └── commands/                               # Custom slash commands
├── target/                                     # Build output (ignored)
└── README.md                                   # Quick reference
```

## Architecture Overview

This project follows a **layered hexagonal architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│  HTTP (REST / GraphQL) | SSE (MCP)                      │
├─────────────────────────────────────────────────────────┤
│  Controllers (AuthController, ItemController, etc)      │
├─────────────────────────────────────────────────────────┤
│  Services (ItemService, PromptService)                  │
│  Security (JwtTokenProvider, JwtAuthenticationFilter)   │
├─────────────────────────────────────────────────────────┤
│  Repositories (ItemRepository, PromptRepository)        │
├─────────────────────────────────────────────────────────┤
│  JPA / Hibernate (EntityManager, Session)               │
├─────────────────────────────────────────────────────────┤
│  Database (H2 in-memory or Postgres/MySQL)              │
└─────────────────────────────────────────────────────────┘
```

**Key Architectural Decisions:**

- **Stateless JWT Authentication**: No sessions; all endpoints verify token freshness per-request
- **DTO Pattern**: Controllers accept/return DTOs; services work with entities; repositories deal with persistence
- **Service Layer**: Handles business logic, transactions, and orchestration
- **Exception Handling**: Centralized via `GlobalExceptionHandler`; maps domain exceptions to HTTP responses
- **Transaction Management**: Service methods marked `@Transactional`; read-only queries optimized
- **Multi-Protocol Parity**: Same business logic (ItemService) exposed via REST, GraphQL, and MCP

### Key Modules

| Module | Location | Purpose |
|--------|----------|---------|
| **Authentication** | `security/*`, `config/SecurityConfig.java` | JWT token generation, validation, and filter integration; stateless authentication |
| **Item CRUD** | `service/ItemService.java`, `repository/ItemRepository.java` | Item lifecycle (create, read, update, delete, search); business rules |
| **Prompt Management** | `service/PromptService.java`, `repository/PromptRepository.java` | Prompt template storage and retrieval; used by Claude via MCP |
| **REST API** | `controller/ItemController.java`, `controller/PromptController.java` | HTTP/JSON endpoints at `/api/items` and `/api/prompts` |
| **GraphQL** | `graphql/ItemGraphQLController.java`, `resources/graphql/schema.graphqls` | Schema-first GraphQL at `/graphql`; queries and mutations |
| **MCP Server** | `mcp/ItemMcpTools.java`, `config/McpConfig.java` | SSE-based MCP endpoint for Claude Desktop at `/mcp/sse` and `/mcp/message` |
| **Data Persistence** | `entity/*`, `repository/*` | JPA entities and Spring Data repositories; automatic schema generation |

## Development Guidelines

### Code Naming Conventions

**Files (PascalCase — Java convention):**
```
Controllers: ItemController.java, AuthController.java
Services:   ItemService.java, PromptService.java
Entities:   Item.java, Prompt.java
DTOs:       ItemResponse.java, CreateItemRequest.java
```

**Code Identifiers (camelCase for methods/vars, PascalCase for classes):**
```java
// Class names — PascalCase
public class ItemService { }
public class CreateItemRequest { }

// Methods — camelCase (verb prefix)
public List<ItemResponse> findAll() { }
public Optional<ItemResponse> findById(Long id) { }

// Variables — camelCase
String itemName = "Widget";
BigDecimal price = new BigDecimal("9.99");

// Constants — SCREAMING_SNAKE_CASE (if any)
private static final long SERIAL_VERSION_UID = 1L;
```

### Import Order

1. `java.*` and `javax.*` imports
2. `jakarta.*` imports (Jakarta EE)
3. `org.springframework.*` imports
4. Third-party libraries (Lombok, JJWT, etc)
5. Local `com.example.*` imports

### Annotations & Patterns

**Common Spring annotations:**
```java
@SpringBootApplication     // Main entry point
@Service                   // Business logic class
@Repository                // Data access (Spring Data does this)
@Component                 // Generic bean
@Configuration             // Config class
@Bean                      // Factory method for bean
@RequiredArgsConstructor   // Lombok constructor injection
@Data                      // Lombok: @Getter @Setter @ToString @EqualsAndHashCode
@Entity                    // JPA entity
@Table(name = "...")       // Database table mapping
@Id                        // Primary key
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
```

**Constructor Injection (via Lombok `@RequiredArgsConstructor`):**
```java
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    // Lombok generates: public ItemService(ItemRepository itemRepository) { ... }
}
```

**DTOs as records (Java 16+):**
```java
public record ItemResponse(Long id, String name, String description, 
                           BigDecimal price, Integer quantity) {
    // Factory method for entity → DTO conversion
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), ...);
    }
}
```

### Transaction Management

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Read-only by default (optimized)
public class ItemService {
    
    public List<ItemResponse> findAll() {  // Uses read-only transaction
        return itemRepository.findAll().stream()...
    }
    
    @Transactional  // Override to read-write for mutations
    public ItemResponse create(CreateItemRequest req) {
        return ItemResponse.from(itemRepository.save(...));
    }
}
```

### Error Handling

Centralized exception mapping via `GlobalExceptionHandler`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(...) {
        return ResponseEntity.status(404).body(new ErrorResponse(...));
    }
}
```

### REST API Design

- **Base path**: `/api/items`, `/api/prompts`
- **Idempotent operations**: GET (safe), PUT (update), DELETE
- **Resource creation**: POST returns 201 with `Location` header
- **Partial updates**: PUT with optional fields (service merges)
- **Search**: Query parameter `?search=widget`
- **Authentication**: `Authorization: Bearer <jwt-token>` header

### GraphQL Design

- **Schema location**: `src/main/resources/graphql/schema.graphqls`
- **Schema-first**: Define `.graphqls` files first; resolvers are generated
- **Resolver mapping**: Methods in `ItemGraphQLController` match schema queries/mutations
- **Endpoint**: POST `/graphql` with `Content-Type: application/json`
- **InteractiveIDE**: GraphiQL at `/graphiql` (dev only)

### MCP Tool Implementation

```java
@Component
@RequiredArgsConstructor
public class ItemMcpTools {
    private final ItemService itemService;
    
    @McpTool  // Register with MCP server
    public List<ItemResponse> listItems() {
        return itemService.findAll();
    }
}
```

- **Endpoint**: GET `/mcp/sse` (client subscribes)
- **Message endpoint**: POST `/mcp/message` (client sends JSON-RPC)
- **Tools**: Exposed as JSON-RPC methods; Claude Desktop calls them

## Available Commands

| Command | Description |
|---------|-------------|
| `mvn clean install` | Clean build artifacts; resolve dependencies; compile and test |
| `mvn spring-boot:run` | Start dev server (hot reload via IDE/IDEA) |
| `mvn test` | Run all unit/integration tests via Maven |
| `mvn test -Dtest=ApplicationTests` | Run a specific test class |
| `mvn verify` | Run tests + code coverage report |
| `mvn clean package -DskipTests` | Build production JAR without tests |
| `java -jar target/jpa4-api-server-*.jar` | Run the built JAR locally |

## Environment Variables & Configuration

**Configuration file**: `src/main/resources/application.properties`

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `server.port` | No | Server port (default 8080) | `8080` |
| `jwt.secret` | **Yes** | 32+ character secret for HS256 | `your-256-bit-secret-here` |
| `jwt.expiration-ms` | No | Token expiry (default 24h) | `86400000` |
| `spring.datasource.url` | No | Database connection string | `jdbc:h2:mem:itemdb` |
| `spring.jpa.hibernate.ddl-auto` | No | Schema generation (create-drop, validate, none) | `create-drop` |
| `spring.h2.console.enabled` | No | H2 web console (dev only) | `true` |
| `spring.graphql.graphiql.enabled` | No | GraphiQL IDE (dev only) | `true` |

**Switching databases** (swap H2 for Postgres):
```properties
# Postgres example
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

## Testing

**Test structure:**
- **Unit tests**: Test individual services/repositories with mocks
- **Integration tests**: Test full request-response cycle (controllers + services + database)
- **Location**: `src/test/java/com/example/api/`
- **Framework**: JUnit 5, Spring Test, Spring Security Test, Spring GraphQL Test

**Example integration test:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTests {
    @Autowired MockMvc mockMvc;
    
    @Test
    void testGetAll() throws Exception {
        mockMvc.perform(get("/api/items")
                .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));  // 4 seed items
    }
}
```

## Deployment

### Prerequisites for Production

- Replace `jwt.secret` with a randomly generated 32+ byte value
- Set `spring.jpa.hibernate.ddl-auto=validate` (never auto-generate schema)
- Replace H2 with a real database (Postgres, MySQL, etc)
- Replace `InMemoryUserDetailsManager` with a database-backed `UserDetailsService`
- Disable H2 console: `spring.h2.console.enabled=false`
- Disable GraphiQL: `spring.graphql.graphiql.enabled=false`
- Enable HTTPS and configure secure cookie flags
- Implement rate limiting and DDoS protection

### Docker Deployment (Optional)

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/jpa4-api-server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t jpa4-api-server .
docker run -p 8080:8080 \
  -e JWT_SECRET="your-secret-here" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/itemdb" \
  jpa4-api-server
```

## Additional Resources

- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/docs/3.4.1/reference/html/
- **Spring Data JPA**: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- **Spring Security**: https://docs.spring.io/spring-security/docs/6.1.x/reference/html/
- **Spring for GraphQL**: https://docs.spring.io/spring-graphql/docs/current/reference/html/
- **MCP Specification**: https://spec.modelcontextprotocol.io/
- **MCP Java SDK**: https://github.com/modelcontextprotocol/java-sdk
- **JJWT Documentation**: https://github.com/jwtk/jjwt
- **Project README**: @README.md (quick reference and curl examples)


## Skill Usage Guide

When working on tasks involving these technologies, invoke the corresponding skill:

| Skill | Invoke When |
|-------|-------------|
| spring-boot | Configures Spring Boot auto-configuration, embedded servers, and application startup |
| spring | Provides foundational Spring Framework patterns for dependency injection and configuration |
| spring-mvc | Builds REST APIs with Spring MVC controllers, request mappings, and HTTP response handling |
| jpa | Designs JPA entity models and persistence mappings using Jakarta Persistence 3.x |
| java | Enforces Java type safety and modern Java 17+ patterns like records and text blocks |
| spring-security | Implements authentication, authorization, and security configurations with Spring Security |
| hibernate | Configures Hibernate ORM, entity relationships, and database DDL generation |
| lombok | Uses Lombok annotations to reduce boilerplate with @Data, @RequiredArgsConstructor |
| maven | Manages dependencies and build lifecycle with Maven and pom.xml configuration |
| jjwt | Generates and validates HS256 JWT tokens with JJWT library |
| mcp | Implements MCP server tools and SSE transport for Claude Desktop integration |
| spring-graphql | Implements GraphQL schemas and resolvers with Spring for GraphQL |
| h2 | Configures H2 in-memory database for development and testing |
