---
name: maven
description: Manages dependencies and build lifecycle with Maven and pom.xml configuration for the JPA4 API Server Template
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Maven Skill

Manages the Maven build lifecycle, dependency configuration, and `pom.xml` for this Spring Boot 3 project. Handles adding/updating dependencies, running builds, executing tests, and packaging the application for deployment.

## Quick Start

```bash
mvn clean install          # full build with tests
mvn spring-boot:run        # start dev server on :8080
mvn test                   # run all tests
mvn test -Dtest=ClassName  # run a specific test class
mvn verify                 # tests + coverage report
mvn clean package -DskipTests  # production JAR
```

## Key Concepts

- **pom.xml** lives at the project root; defines `groupId: com.example`, `artifactId: jpa4-api-server`, `version: 0.0.1-SNAPSHOT`
- **Parent**: `spring-boot-starter-parent 3.4.1` — manages Spring dependency versions; do not override managed versions without good reason
- **Java version**: property `<java.version>17</java.version>` controls source/target compilation
- **Key managed versions**: JJWT `0.12.6`, MCP Java SDK `0.9.0`, Lombok `1.18+`
- **Build output**: `target/jpa4-api-server-0.0.1-SNAPSHOT.jar`

## Common Patterns

**Adding a dependency** — always check if the version is managed by `spring-boot-starter-parent` first; omit `<version>` if so:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <!-- no version — managed by parent -->
</dependency>
```

**Adding a versioned dependency** (not managed by parent):
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
```
Declare the version as a property in `<properties>` so it stays in one place.

**Test-scoped dependency**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Checking effective dependency tree** (useful for version conflict diagnosis):
```bash
mvn dependency:tree
mvn dependency:tree -Dincludes=io.jsonwebtoken
```

**Excluding a transitive dependency**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**Spring Boot plugin** (already configured via parent — do not duplicate):
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
Lombok must be excluded from the repackaged JAR because it is compile-only.