# Lombok Patterns

## When to use
Apply these patterns when adding or refactoring classes in this codebase to eliminate boilerplate while preserving JPA and Spring compatibility.

## Service / Component (Constructor Injection)
Use `@RequiredArgsConstructor` on any Spring-managed bean. All `final` fields become constructor parameters — Spring injects them automatically.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptService {
    private final PromptRepository promptRepository;

    @Transactional
    public PromptResponse create(CreatePromptRequest req) { ... }
}
```

## JPA Entity
JPA entities require a no-arg constructor (Hibernate) but also benefit from generated accessors. Combine `@Data` + `@NoArgsConstructor`.

```java
@Entity
@Table(name = "prompts")
@Data
@NoArgsConstructor
public class Prompt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String content;
}
```

## Controller
Same pattern as services — `@RequiredArgsConstructor` only; no `@Data` needed on controllers.

```java
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {
    private final PromptService promptService;
}
```

## Pitfalls
- **Bidirectional JPA relationships**: Never use `@Data` when an entity has a `@OneToMany`/`@ManyToOne` back-reference — generated `toString` and `hashCode` will recurse infinitely. Use `@Getter`/`@Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` instead.
- **DTOs as records**: Response DTOs in this project (`ItemResponse`, `PromptResponse`) are Java records — Lombok is not needed and must not be applied to them.