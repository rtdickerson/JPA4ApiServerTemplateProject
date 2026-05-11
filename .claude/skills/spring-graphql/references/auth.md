# GraphQL Authentication & Authorization

## When to use

Refer to this when securing a new GraphQL controller or adding role-based access to individual resolvers.

## Patterns

**Class-level authentication guard**

Place `@PreAuthorize("isAuthenticated()")` on the controller class to require a valid JWT on every resolver. This is the established pattern in `ItemGraphQLController`. Spring Security's method security (`@EnableMethodSecurity` in `SecurityConfig`) must be active, which it already is.

```java
@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ItemGraphQLController {
    // all @QueryMapping and @MutationMapping methods inherit this guard
}
```

**Role-based access on individual resolvers**

Override at the method level for finer-grained control. Methods inherit the class annotation; a stricter method annotation takes precedence.

```java
@MutationMapping
@PreAuthorize("hasRole('ADMIN')")
public boolean deleteItem(@Argument Long id) {
    return itemService.delete(id);
}
```

**How the JWT reaches the resolver**

`JwtAuthenticationFilter` extracts the `Authorization: Bearer <token>` header on every request and populates `SecurityContextHolder`. The `/graphql` path is covered by `.anyRequest().authenticated()` in `SecurityConfig` — but that path-level rule fires after the filter; method security fires inside the resolver, giving finer granularity. Both must pass.

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ items { id name } }"}'
```

## Pitfalls

- Do not rely solely on the path-level rule in `SecurityConfig` to protect GraphQL. All mutations and sensitive queries must also carry `@PreAuthorize` at the class or method level — path rules alone are bypassed if a future config change permits `/graphql` for a subset of callers.
- `@EnableMethodSecurity` is required for `@PreAuthorize` to fire. It is present in `SecurityConfig`; do not remove it.