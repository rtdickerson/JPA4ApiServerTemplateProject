---
name: spring-security
description: Implements authentication, authorization, and security configurations with Spring Security for the JPA4 API Server Template
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Spring Security Skill

Handles all security concerns in this Spring Boot 3 / JWT project: token generation and validation via JJWT 0.12, stateless filter wiring, `SecurityFilterChain` configuration, and per-endpoint authorization rules. The security layer spans `config/SecurityConfig.java`, `security/JwtTokenProvider.java`, and `security/JwtAuthenticationFilter.java`.

## Quick Start

1. Read the current security configuration: `src/main/java/com/example/api/config/SecurityConfig.java`
2. Read token logic: `src/main/java/com/example/api/security/JwtTokenProvider.java`
3. Read the filter: `src/main/java/com/example/api/security/JwtAuthenticationFilter.java`
4. Check `src/main/resources/application.properties` for `jwt.secret` and `jwt.expiration-ms`

## Key Concepts

- **Stateless JWT (HS256)**: No server-side sessions; every request carries a `Bearer` token validated by `JwtTokenProvider`
- **Filter chain**: `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`; sets `SecurityContextHolder` on valid tokens
- **Open paths**: `/auth/login`, `/mcp/sse`, `/mcp/message`, `/h2-console/**`, `/graphiql` are permit-all; everything else requires authentication
- **UserDetailsService**: Currently `InMemoryUserDetailsManager` (admin/admin); replace with a DB-backed implementation for production
- **CSRF**: Disabled (stateless API); frame options disabled for H2 console in dev

## Common Patterns

**Add a new permit-all path** (`SecurityConfig.java`):
```java
.requestMatchers("/public/**").permitAll()
```

**Require a role on an endpoint**:
```java
.requestMatchers("/admin/**").hasRole("ADMIN")
```

**Generate a token** (after successful login in `AuthController`):
```java
String token = jwtTokenProvider.generateToken(authentication);
return new AuthResponse(token);
```

**Validate a token** (inside `JwtAuthenticationFilter`):
```java
if (jwtTokenProvider.validateToken(token)) {
    Authentication auth = jwtTokenProvider.getAuthentication(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
}
```

**Rotate the JWT secret** — update `application.properties`:
```properties
jwt.secret=<random-32+-byte-base64-value>
jwt.expiration-ms=86400000
```

**Add a second user** (dev only — replace with DB-backed service in prod):
```java
UserDetails viewer = User.withUsername("viewer")
    .password(encoder.encode("viewer"))
    .roles("USER")
    .build();
manager.createUser(viewer);
```