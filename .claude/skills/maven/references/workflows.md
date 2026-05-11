# Maven Build Workflows

## When to use
Use these workflows for the full development cycle: local development, running tests, diagnosing build failures, and producing a production artifact.

## Workflow 1: Daily development loop
Start the server with live-reload support via your IDE (IntelliJ IDEA detects changed classes automatically when `spring-boot:run` is active).

```bash
mvn clean install          # resolve deps + compile + test once at start of day
mvn spring-boot:run        # start server on :8080; leave running
```

For a faster restart without cleaning the full build:
```bash
mvn spring-boot:run -pl .  # re-run from project root without clean
```

## Workflow 2: Test execution
Run the full suite before committing; target a single class during active development.

```bash
mvn test                              # all tests (JUnit 5 + Spring Test)
mvn test -Dtest=ApplicationTests      # single class
mvn verify                            # tests + JaCoCo coverage report → target/site/jacoco/
```

The integration tests in `ApplicationTests` spin up a full Spring context with H2 — they are slow but authoritative. Do not mock the database for these tests; H2 is already in-memory and fast enough.

## Workflow 3: Diagnosing dependency problems
Before adding a new dependency or chasing a `ClassNotFoundException` at runtime:

```bash
mvn dependency:tree                              # full tree
mvn dependency:tree -Dincludes=io.jsonwebtoken   # filter to one group
mvn dependency:analyze                           # unused declared / used undeclared
```

Then produce and inspect the final artifact:
```bash
mvn clean package -DskipTests                    # build JAR
jar tf target/jpa4-api-server-0.0.1-SNAPSHOT.jar | grep <suspect>  # verify contents
java -jar target/jpa4-api-server-0.0.1-SNAPSHOT.jar                # smoke-test locally
```

## Pitfalls
- **Skipping tests in CI**: `-DskipTests` is acceptable for a packaging step only; never skip tests as a workaround for a failing suite — fix the test.
- **ddl-auto=create-drop in production**: the default in `application.properties` destroys the schema on shutdown; always override to `validate` or `none` when pointing at a real database.