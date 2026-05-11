# JJWT Patterns

## When to use
Apply these patterns when modifying `JwtTokenProvider`, adding custom claims, or extending token validation logic in this project.

## Token Generation
Build the token with the minimum required claims. The `signingKey` is initialized once via `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))` in `@PostConstruct`.

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

## Token Validation
Wrap `parseSignedClaims` in a try/catch so callers receive a clean boolean — never let `JwtException` propagate to the filter chain.

```java
public boolean isValid(String token) {
    try {
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}
```

## Adding Custom Claims
Extend `generate()` with `.claim()` before `.signWith()`; read back with typed `get()` on the payload.

```java
.claim("role", user.getRole())   // set
...
.getPayload().get("role", String.class)  // read
```

## Pitfalls
- **Secret length**: A secret shorter than 32 bytes causes JJWT to throw at startup. Keep `jwt.secret` at least 32 characters; use an env var (`JWT_SECRET`) in production rather than a literal in `application.properties`.
- **Key reuse after rotation**: Tokens signed with an old key will immediately fail `isValid()` after the secret is swapped. Coordinate rotation with a grace-period re-login flow.