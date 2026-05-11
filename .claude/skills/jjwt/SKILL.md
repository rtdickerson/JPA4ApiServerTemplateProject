---
name: jjwt
description: Generates and validates HS256 JWT tokens with JJWT 0.12.6 in the JPA4 API Server Template; covers JwtTokenProvider patterns, filter integration, and configuration.
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Jjwt Skill

This skill covers HS256 JWT token generation and validation using JJWT 0.12.6 as implemented in this project. The core logic lives in `src/main/java/com/example/api/security/JwtTokenProvider.java`, which is wired into Spring Security via `JwtAuthenticationFilter` and configured through `application.properties`. All endpoints except `/auth/login` require a valid `Bearer` token.

## Quick Start

1. Obtain a token via `POST /auth/login` with `{"username":"admin","password":"admin"}`.
2. Pass it as `Authorization: Bearer <token>` on every subsequent request.
3. The filter (`JwtAuthenticationFilter`) validates the token per-request; no session state is maintained.

Configuration in `src/main/resources/application.properties`:
```properties
jwt.secret=change-me-to-a-256-bit-secret-before-production-use!!
jwt.expiration-ms=86400000
```

## Key Concepts

- **`Keys.hmacShaKeyFor(secret.getBytes())`** — converts the raw string secret into a `SecretKey`; JJWT selects HS256/384/512 based on key length. A 32-byte secret yields HS256.
- **`Jwts.builder()`** — fluent builder; `.subject()`, `.issuedAt()`, `.expiration()`, and `.signWith(signingKey)` are the minimum required fields used here.
- **`Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)`** — single call that both verifies the signature and parses claims; throws `JwtException` on any failure.
- **`isValid()`** — wraps `parseSignedClaims` in a try/catch; returns `false` on expired, malformed, or tampered tokens without leaking details to callers.
- **Stateless auth** — `SecurityContextHolder` is populated per-request inside `JwtAuthenticationFilter.doFilterInternal()`; no `HttpSession` is created.

## Common Patterns

**Generate a token (from `JwtTokenProvider:25`):**
```java
public String generate(String username) {
    var now = new Date();
    return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(signingKey)
            .compact();
}
```

**Validate and extract subject (from `JwtTokenProvider:35`):**
```java
public String extractUsername(String token) {
    return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
}
```

**Filter wiring — extract Bearer token and set SecurityContext (from `JwtAuthenticationFilter:29`):**
```java
var token = extractToken(request);          // strips "Bearer " prefix
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

**Adding custom claims** — extend `generate()` with `.claim("role", role)` before `.signWith()`; retrieve via `.getPayload().get("role", String.class)`.

**Production secret rotation** — swap `jwt.secret` via environment variable (`JWT_SECRET`); re-issue tokens after rotation since old tokens signed with the previous key will fail `isValid()`.