# Maven Dependency Patterns

## When to use
Apply these patterns when adding, updating, or troubleshooting dependencies in `pom.xml`.

## Pattern 1: Version-managed dependencies (Spring ecosystem)
`spring-boot-starter-parent 3.4.1` manages versions for all Spring starters, H2, Lombok, and Jackson. Omit `<version>` for these — specifying one overrides the BOM and can cause subtle incompatibilities.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Pattern 2: Unmanaged versioned dependencies
JJWT (`0.12.6`) and MCP Java SDK (`0.9.0`) are not managed by the parent. Declare their versions as properties so all related artifacts stay in sync.

```xml
<properties>
    <jjwt.version>0.12.6</jjwt.version>
    <mcp-sdk.version>0.9.0</mcp-sdk.version>
</properties>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
```

## Pattern 3: Lombok exclusion from JAR
Lombok is compile-only. The Spring Boot Maven plugin must exclude it from the repackaged JAR or it ends up on the runtime classpath unnecessarily.

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

## Pitfalls
- **Do not duplicate the Spring Boot plugin** — it is inherited from `spring-boot-starter-parent`; adding it again causes double-repackaging errors.
- **Version conflicts**: if a dependency behaves unexpectedly, run `mvn dependency:tree -Dincludes=<groupId>` before overriding versions; the root cause is usually a transitive pull-in, not the direct declaration.