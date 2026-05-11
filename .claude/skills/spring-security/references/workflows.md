# Spring Security Workflows

## When to use
Follow these workflows when rotating credentials, adding users, or debugging a 401/403 in this project's request lifecycle.

## Workflow 1: Rotate the JWT secret

1. Generate a new secret (≥ 32 random bytes, base64-encoded).
2. Update `src/main/resources/application.properties`:
   ```properties
   jwt.secret=<new-base64-value>
   jwt.expiration-ms=86400000
   ```
3. Restart the server — all existing tokens are immediately invalidated (stateless; no revocation list needed).
4. Re-issue tokens via `POST /auth/login`.

## Workflow 2: Add a user (dev) or replace UserDetailsService (prod)

**Dev** — add to `SecurityConfig.java` `InMemoryUserDetailsManager` bean:
```java
UserDetails viewer = User.withUsername("viewer")
    .password(encoder.encode("viewer"))
    .roles("USER")
    .build();
return new InMemoryUserDetailsManager(admin, viewer);
```

**Prod** — implement `UserDetailsService` backed by a `UserRepository` and register it as a `@Bean`; Spring Security picks it up automatically once `InMemoryUserDetailsManager` is removed.

## Workflow 3: Trace a 401 / 403

1. Confirm the token is present and prefixed with `Bearer ` in the `Authorization` header.
2. Decode the JWT (e.g., `jwt.io`) — check `exp` (expiry) and `sub` (username).
3. Set a breakpoint or add a log in `JwtAuthenticationFilter` at `validateToken()` — returns `false` on bad signature or expiry.
4. Check `SecurityConfig` permit-all list — the path may be missing, causing a 401 before the filter even runs.
5. A 403 (not 401) means the token is valid but the principal lacks the required role — review `.hasRole()` / `.hasAnyRole()` rules.

## Pitfalls

- **Filter registration order**: `JwtAuthenticationFilter` must be added with `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`; inserting it at the wrong position silently skips token validation.
- **`@PreAuthorize` requires method security**: Add `@EnableMethodSecurity` to `SecurityConfig` before using `@PreAuthorize` or `@Secured` on service methods — it is not on by default.