# JJWT Workflows

## When to use
Follow these workflows when tracing the full request authentication cycle, debugging 401 responses, or wiring a new secured endpoint.

## Login → Token Flow
1. Client `POST /auth/login` with `{"username":"admin","password":"admin"}`.
2. `AuthController` delegates to `AuthenticationManager.authenticate()`.
3. On success, `JwtTokenProvider.generate(username)` returns a compact token string.
4. Response body: `{"token":"<jwt>"}`.

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .token
```

## Per-Request Validation Flow
`JwtAuthenticationFilter.doFilterInternal()` runs on every request:

1. Extract `Authorization: Bearer <token>` header.
2. Call `jwtTokenProvider.isValid(token)` — returns `false` on expiry, bad signature, or malformed input.
3. If valid, load `UserDetails` and set `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken`.
4. Call `chain.doFilter()` regardless — Spring Security's access rules decide 401/403 downstream.

```java
var token = extractToken(request);
if (token != null && jwtTokenProvider.isValid(token)) {
    var username = jwtTokenProvider.extractUsername(token);
    var userDetails = userDetailsService.loadUserByUsername(username);
    var auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);
}
chain.doFilter(request, response);
```

## Adding a New Secured Endpoint
1. Add controller method and mapping under `/api/**`.
2. `SecurityConfig` already permits only `/auth/login` and MCP paths without a token — no config change needed for standard `/api/**` routes.
3. Verify with `curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/your-endpoint`.

## Pitfalls
- **MCP endpoints are unauthenticated by default**: `/mcp/sse` and `/mcp/message` bypass the JWT filter per `SecurityConfig`. If you need auth there, add token validation inside the tool handlers in `ItemMcpTools` or tighten the `SecurityConfig` permit list.
- **Filter order**: `JwtAuthenticationFilter` must be registered `before(UsernamePasswordAuthenticationFilter.class)`. Changing `SecurityConfig` bean ordering can silently break auth for all endpoints.