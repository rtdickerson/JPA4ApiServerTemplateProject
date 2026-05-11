# Spring Boot Workflows

## When to use
Follow these workflows when starting the dev server, running tests, building a production artifact, or switching from H2 to a real database.

## Dev cycle
```bash
# 1. Compile and resolve deps
mvn clean install

# 2. Start with live reload (IDE handles class reloading)
mvn spring-boot:run

# 3. Run all tests (JUnit 5 + MockMvc + Spring Test)
mvn test

# 4. Run a single test class
mvn test -Dtest=ApplicationTests
```
Server is available at `http://localhost:8080`; H2 console at `/h2-console` (JDBC URL `jdbc:h2:mem:itemdb`).

## Production build
```bash
mvn clean package -DskipTests
java -jar target/jpa4-api-server-*.jar
```
Before running in production: set `jwt.secret` to a 32+ byte random value, disable H2 console and GraphiQL, and point `spring.datasource.*` at a real database.

## Switching datasource (H2 → Postgres)
Replace the datasource block in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/itemdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=secret
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```
Add the Postgres driver dependency to `pom.xml` and remove the H2 dependency (or scope it `test`).

## Pitfalls
- **Test isolation**: `@SpringBootTest` spins up the full context including the real H2 database; tests that mutate data can interfere with each other unless each test rolls back via `@Transactional` or resets seed state.
- **Port conflicts**: `mvn spring-boot:run` binds port 8080; if a previous run did not shut down cleanly, kill the process before restarting (`lsof -i :8080`).