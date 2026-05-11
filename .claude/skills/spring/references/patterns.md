# Spring Patterns Reference

## When to use
Consult this file when adding new beans, wiring dependencies, or deciding where logic belongs in the `controller → service → repository` stack.

## Patterns

### Constructor Injection (Lombok)
Never use `@Autowired` on fields. All injectable classes use `@RequiredArgsConstructor`; Lombok generates the constructor.
```java
@Service
@RequiredArgsConstructor
public class WidgetService {
    private final WidgetRepository widgetRepository;
}
```

### Service Transaction Defaults
Annotate the class `@Transactional(readOnly = true)` and override individual write methods with `@Transactional`.
```java
@Service
@Transactional(readOnly = true)
public class WidgetService {
    public List<WidgetResponse> findAll() { ... }

    @Transactional
    public WidgetResponse create(CreateWidgetRequest req) { ... }
}
```

### DTO Records with Factory Method
Controllers accept and return records, never entities. Conversion belongs in a static `from` factory on the record.
```java
public record WidgetResponse(Long id, String name) {
    public static WidgetResponse from(Widget w) {
        return new WidgetResponse(w.getId(), w.getName());
    }
}
```

## Pitfalls
- **Circular dependencies**: if two `@Configuration` classes inject each other via constructors, Spring fails at startup. Extract the shared dependency into a third bean.
- **Missing `@Transactional` on writes**: the class-level `readOnly = true` causes silent rollbacks on inserts/updates if the method override is forgotten.
- **Exposing entities from controllers**: returning an entity directly can trigger lazy-load exceptions and leaks internal schema; always map through a DTO.