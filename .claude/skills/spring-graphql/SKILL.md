---
name: spring-graphql
description: Implements GraphQL schemas and resolvers with Spring for GraphQL in the JPA4 API Server Template project
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Spring GraphQL Skill

This skill guides implementation of GraphQL schemas and resolvers using Spring for GraphQL 1.x (schema-first) within the JPA4 API Server Template. It covers the full cycle: defining types in `schema.graphqls`, wiring resolver methods in a `@Controller` class, mapping arguments, and integrating with the service layer — matching the patterns already in use in `ItemGraphQLController.java`.

## Quick Start

Schema file lives at `src/main/resources/graphql/schema.graphqls`. Spring for GraphQL auto-discovers it. Add a controller in `src/main/java/com/example/api/graphql/` and annotate methods with `@QueryMapping` or `@MutationMapping`.

```bash
# Verify schema parses and app starts cleanly
mvn spring-boot:run

# Hit the interactive IDE (dev only)
open http://localhost:8080/graphiql
```

## Key Concepts

**Schema-first**: define types and inputs in `.graphqls` before writing resolver code. Method names in the controller must match query/mutation names in the schema exactly.

**Resolver controller**: annotate with `@Controller` (not `@RestController`). Use `@RequiredArgsConstructor` for injection. Class-level `@PreAuthorize("isAuthenticated()")` enforces JWT auth on every resolver.

**Argument binding**: `@Argument` binds a named schema argument to a method parameter. For `input` objects (e.g. `CreateItemInput`), receive as `Map<String, Object>` and construct the DTO manually, or use a matching Java record/class that Spring can coerce automatically.

**Return types**: return the DTO or entity directly; Spring for GraphQL serializes it. Return `null` for optional fields rather than throwing.

## Common Patterns

**Adding a new query**

1. Add the field to `schema.graphqls`:
   ```graphql
   type Query {
       myEntity(id: ID!): MyEntity
   }
   type MyEntity { id: ID!, name: String! }
   ```
2. Add a method in the controller:
   ```java
   @QueryMapping
   public MyEntityResponse myEntity(@Argument Long id) {
       return myEntityService.findById(id).orElse(null);
   }
   ```

**Adding a mutation with an input type**

1. Define in schema:
   ```graphql
   input CreateMyEntityInput { name: String! }
   type Mutation { createMyEntity(input: CreateMyEntityInput!): MyEntity! }
   ```
2. Resolve in controller:
   ```java
   @MutationMapping
   public MyEntityResponse createMyEntity(@Argument Map<String, Object> input) {
       var req = new CreateMyEntityRequest((String) input.get("name"));
       return myEntityService.create(req);
   }
   ```

**Testing a resolver**

Use `@GraphQlTest` or `@SpringBootTest` with `HttpGraphQlTester`:
```java
@SpringBootTest
@AutoConfigureHttpGraphQlTester
class ItemGraphQLControllerTests {
    @Autowired HttpGraphQlTester tester;

    @Test
    void items() {
        tester.document("{ items { id name price } }")
              .execute()
              .path("items").entityList(ItemResponse.class).hasSizeGreaterThan(0);
    }
}
```

**Security**: the class-level `@PreAuthorize("isAuthenticated()")` in `ItemGraphQLController` covers all resolvers. Mirror this on new GraphQL controllers; do not rely on Spring Security path rules for `/graphql` alone.

**Schema location**: place all `.graphqls` files under `src/main/resources/graphql/`. Spring for GraphQL merges them automatically — split by domain if the schema grows.