# Spring Security Patterns

## When to use
Apply these patterns when adding endpoints, changing authentication rules, rotating secrets, or wiring new users into this project's stateless JWT setup.

## Pattern 1: Permit-all vs authenticated endpoints

Open paths are declared in `SecurityConfig.java` before the catch-all `.anyRequest().authenticated()`. Add new public paths to the existing `requestMatchers` block; everything else is automatically locked.

```java
.requestMatchers(
    "/auth/login",
    "/mcp/sse", "/mcp/message",
    "/h2-console/**",
    "/graphiql",
    "/public/**"          // ← add new open paths here
).permitAll()
.anyRequest().authenticated()
```

## Pattern 2: Filter → context → controller flow

The filter sets the `SecurityContext`; controllers never touch tokens directly.

```java
// JwtAuthenticationFilter — runs before UsernamePasswordAuthenticationFilter
String token = resolveToken(request);          // strip "Bearer "
if (token != null && jwtTokenProvider.validateToken(token)) {
    Authentication auth = jwtTokenProvider.getAuthentication(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
}
// downstream controller reads: SecurityContextHolder.getContext().getAuthentication()
```

## Pattern 3: Role-based access on a new endpoint

Add `.requestMatchers` before the `.anyRequest()` line; order matters in Spring Security 6.

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
.anyRequest().authenticated()
```

## Pitfalls

- **Secret length**: `jwt.secret` must be ≥ 256 bits (32 bytes) for HS256; shorter values cause a JJWT `WeakKeyException` at startup.
- **CSRF is disabled** globally — correct for a stateless API, but never re-enable sessions without also re-enabling CSRF protection.
- **H2 frame options**: `headers.frameOptions().disable()` is dev-only; remove or tighten before production.