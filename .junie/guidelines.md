# ByteChef Developer Guidelines

This document provides concise guidance for new developers working with the ByteChef codebase.

## Project Structure

ByteChef is an open-source, low-code, extendable API integration and workflow automation platform with the following structure:

### Backend (Java/Spring Boot)
- `/server`: Contains the backend Java/Spring Boot application
  - `/server/apps`: Application-specific code
  - `/server/libs`: Shared libraries and components
  - `/server/ee`: Enterprise edition features
  - Each module follows the standard Maven/Gradle structure:
    - `src/main/java`: Main source code
    - `src/test/java`: Test code
    - `src/main/resources`: Configuration files

### Frontend (TypeScript/React)
- `/client`: Contains the frontend application
  - `/client/src`: Source code
  - `/client/public`: Static assets
  - Uses modern frontend tools:
    - Vite for building
    - ESLint and Prettier for code formatting
    - Tailwind CSS for styling
    - Storybook for component documentation
    - Vitest for testing

### Other Important Directories
- `/docs`: Documentation
- `/sdks`: Software Development Kits
- `/cli`: Command-line interface tools
- `/buildSrc`: Custom Gradle build logic

## Building the Project

ByteChef uses Gradle for building the backend and npm for the frontend:

### Backend
```bash
# Build the entire project
./gradlew build

# Build a specific module
./gradlew :server:apps:app-name:build
```

### Frontend
```bash
# Navigate to client directory
cd client

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

## Running Tests

### Backend Tests
```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :server:apps:app-name:test
```

### Frontend Tests
```bash
# Navigate to client directory
cd client

# Run tests
npm test

# Run tests with coverage
npm run test:coverage
```

## Running the Application

### Using Docker (Recommended)
```bash
# Using docker-compose
docker compose -f docker-compose.yml up

# Or manually
docker network create -d bridge bytechef_network
docker run --name postgres -d -p 5432:5432 \
    --env POSTGRES_USER=postgres \
    --env POSTGRES_PASSWORD=postgres \
    --hostname postgres \
    --network bytechef_network \
    postgres:15-alpine
docker run --name bytechef -it -p 8080:8080 \
    --env BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef \
    --env BYTECHEF_DATASOURCE_USERNAME=postgres \
    --env BYTECHEF_DATASOURCE_PASSWORD=postgres \
    --env BYTECHEF_SECURITY_REMEMBER_ME_KEY=e48612ba1fd46fa7089fe9f5085d8d164b53ffb2 \
    --network bytechef_network \
    docker.bytechef.io/bytechef/bytechef:latest
```

Access the application at http://localhost:8080

## Best Practices

1. **Code Style**: Follow the established code style in each part of the codebase:
   - Java: Follow standard Java conventions and Spring Boot best practices
   - TypeScript/JavaScript: Follow ESLint and Prettier configurations

2. **Testing**: Write tests for all new features and bug fixes:
   - Backend: Use JUnit and Spring Boot Test
   - Frontend: Use Vitest

3. **Git Workflow**:
   - Create feature branches from `master`
   - Keep commits small and focused
   - Write clear commit messages
   - Submit pull requests for review

4. **Documentation**:
   - Document public APIs
   - Update README files when making significant changes
   - Add comments for complex logic

5. **Dependency Management**:
   - Backend: Use the version catalog in Gradle
   - Frontend: Keep dependencies updated using npm

## Spring Boot Guidelines

1. **Prefer Constructor Injection over Field/Setter Injection**
   - Declare all the mandatory dependencies as `final` fields and inject them through the constructor.
   - Spring will auto-detect if there is only one constructor, no need to add `@Autowired` on the constructor.
   - Avoid field/setter injection in production code.

   **Explanation:**
   - Making all the required dependencies as `final` fields and injecting them through constructor make sure that the object is always in a properly initialized state using the plain Java language feature itself. No need to rely on any framework-specific initialization mechanism.
   - You can write unit tests without relying on reflection-based initialization or mocking.
   - The constructor-based injection clearly communicates what are the dependencies of a class without having to look into the source code.
   - Spring Boot provides extension points as builders such as `RestClient.Builder`, `ChatClient.Builder`, etc. Using constructor-injection, we can do the customization and initialize the actual dependency.

   ```java
   @Service
   public class OrderService {
      private final OrderRepository orderRepository;
      private final RestClient restClient;

      public OrderService(OrderRepository orderRepository, 
                          RestClient.Builder builder) {
          this.orderRepository = orderRepository;
          this.restClient = builder
                  .baseUrl("http://catalog-service.com")
                  .requestInterceptor(new ClientCredentialTokenInterceptor())
                  .build();
      }

      //... methods
   }
   ```

2. **Prefer package-private over public for Spring components**
   - Declare Controllers, their request-handling methods, `@Configuration` classes and `@Bean` methods with default (package-private) visibility whenever possible. There's no obligation to make everything `public`.

   **Explanation:**
   - Keeping classes and methods package-private reinforces encapsulation and abstraction by hiding implementation details from the rest of your application.
   - Spring Boot's classpath scanning will still detect and invoke package-private components (for example, invoking your `@Bean` methods or controller handlers), so you can safely restrict visibility to only what clients truly need. This approach confines your internal APIs to a single package while still allowing the framework to wire up beans and handle HTTP requests.

3. **Organize Configuration with Typed Properties**
   - Group application-specific configuration properties with a common prefix in `application.properties` or `.yml`.
   - Bind them to `@ConfigurationProperties` classes with validation annotations so that the application will fail fast if the configuration is invalid.
   - Prefer environment variables instead of profiles for passing different configuration properties for different environments.

   **Explanation:**
   - By grouping and binding configuration in a single `@ConfigurationProperties` bean, you centralize both the property names and their validation rules. 
     In contrast, using `@Value("${…}")` across many components forces you to update each injection point whenever a key or validation requirement changes.
   - Overusing profiles to customize the application configuration may lead to unexpected issues due to the order of profiles specified. 
     As you can enable multiple profiles with different combinations, making sense of the effective application configuration becomes tricky.

4. **Define Clear Transaction Boundaries**
   - Define each Service-layer method as a transactional unit.
   - Annotate query-only methods with `@Transactional(readOnly = true)`.
   - Annotate data-modifying methods with `@Transactional`.
   - Limit the code inside each transaction to the smallest necessary scope.

   **Explanation:**
   - **Single Unit of Work:** Group all database operations for a given use case into one atomic unit, which in Spring Boot is typically a `@Service` annotated class method. This ensures that either all operations succeed or none do.
   - **Connection Reuse:** A `@Transactional` method runs on a single database connection for its entire scope, avoiding the overhead of acquiring and returning connections from the connection pool for each operation.
   - **Read-only Optimizations:** Marking methods as `readOnly = true` disables unnecessary dirty-checking and flushes, improving performance for pure reads.
   - **Reduced Contention:** Keeping transactions as brief as possible minimizes lock duration, lowering the chance of contention in high-traffic applications.

5. **Disable Open Session in View Pattern**
   - While using Spring Data JPA, disable the Open Session in View filter by setting `spring.jpa.open-in-view=false` in `application.properties/yml`.

   **Explanation:**
   - Open Session In View (OSIV) filter transparently enables loading the lazy associations while rendering the view or serializing JPA entities. This may lead to the N + 1 Select problem.
   - Disabling OSIV forces you to fetch exactly the associations you need via fetch joins, entity graphs, or explicit queries, and hence you can avoid unexpected N + 1 selects and `LazyInitializationExceptions`.

6. **Separate Web Layer from Persistence Layer**
   - Don't expose entities directly as responses in controllers.
   - Define explicit request and response record (DTO) classes instead.
   - Apply Jakarta Validation annotations on your request records to enforce input rules.

   **Explanation:**
   - Returning or binding directly to entities couples your public API to your database schema, making future changes riskier.
   - DTOs let you clearly declare exactly which fields clients can send or receive, improving clarity and security.
   - With dedicated DTOs per use case, you can annotate fields for validation without relying on complex validation groups.
   - Use Java bean mapper libraries to simplify DTO conversions. Prefer MapStruct library that can generate bean mapper implementation at compile time so that there won't be runtime reflection overhead.

7. **Follow REST API Design Principles**
   - **Versioned, resource-oriented URLs:** Structure your endpoints as `/api/v{version}/resources` (e.g. `/api/v1/orders`).
   - **Consistent patterns for collections and sub-resources:** Keep URL conventions uniform (for example, `/posts` for posts collection and `/posts/{slug}/comments` for comments of a specific post).
   - **Explicit HTTP status codes via ResponseEntity:** Use `ResponseEntity<T>` to return the correct status (e.g. 200 OK, 201 Created, 404 Not Found) along with the response body.
   - Use pagination for collection resources that may contain an unbounded number of items.
   - The JSON payload must use a JSON object as a top-level data structure to allow for future extension.
   - Use snake_case or camelCase for JSON property names consistently.

   **Explanation:**
   - **Predictability and discoverability:** Adhering to well-known REST conventions makes your API intuitive. Clients can guess URLs and behaviors without extensive documentation.
   - **Reliable client integrations:** Standardized URL structures, status codes, and headers enable consumers to build against your API with confidence, knowing exactly what each response will look like.
   - For more comprehensive REST API Guidelines, please refer [Zalando RESTful API and Event Guidelines](https://opensource.zalando.com/restful-api-guidelines/).

8. **Use Command Objects for Business Operations**
   - Create purpose-built command records (e.g., `CreateOrderCommand`) to wrap input data.
   - Accept these commands in your service methods to drive creation or update workflows.

   **Explanation:**
   - Using the use-case specific Command and Query objects clearly communicates what input data is expected from the caller. 
     Otherwise, the caller had to guess whether they should create and pass the unique key or created_date, or they will be generated by the server/database.

9. **Centralize Exception Handling**
   - Define a global handler class annotated with `@ControllerAdvice` (or `@RestControllerAdvice` for REST APIs) using `@ExceptionHandler` methods to handle specific exceptions.
   - Return consistent error responses. Consider using the ProblemDetails response format ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)).

   **Explanation:**
   - We should always handle all possible exceptions and return a standard error response instead of throwing exceptions.
   - It is better to centralize the exception handling in a `GlobalExceptionHandler` using `(Rest)ControllerAdvice` instead of duplicating the try/catch exception handling logic across the controllers.

10. **Actuator**
    - Expose only essential actuator endpoints (such as `/health`, `/info`, `/metrics`) without requiring authentication. All the other actuator endpoints must be secured.

    **Explanation:**
    - Endpoints like `/actuator/health` and `/actuator/metrics` are critical for external health checks and metric collection (e.g., by Prometheus). Allowing these to be accessed anonymously ensures monitoring tools can function without extra credentials. All the remaining endpoints should be secured.
    - In non-production environments (DEV, QA), you can expose additional actuator endpoints such as `/actuator/beans`, `/actuator/loggers` for debugging purpose.

11. **Internationalization with ResourceBundles**
    - Externalize all user-facing text such as labels, prompts, and messages into ResourceBundles rather than embedding them in code.

    **Explanation:**
    - Hardcoded strings make it difficult to support multiple languages. By placing your labels, error messages, and other text in locale-specific ResourceBundle files, you can maintain separate translations for each language.
    - At runtime, Spring can load the appropriate bundle based on the user's locale or a preference setting, making it simple to add new languages and switch between them dynamically.

12. **Use Testcontainers for integration tests**
    - Spin up real services (databases, message brokers, etc.) in your integration tests to mirror production environments.

    **Explanation:**
    - Most of the modern applications use a wide range of technologies such as SQL/NoSQL databases, key-value stores, message brokers, etc. Instead of using in-memory variants or mocks, Testcontainers can spin up those dependencies as Docker containers and allow you to test using the same type of dependencies that you will use in the production. This reduces environment inconsistencies and increases confidence in your integration tests.
    - Always use docker images with a specific version of the dependency that you are using in production instead of using the `latest` tag.

13. **Use random port for integration tests**
    - When writing integration tests, start the application on a random available port to avoid port conflicts by annotating the test class with:

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    ```

    **Explanation:**
    - **Avoid conflicts in CI/CD:** In your CI/CD environment, there can be multiple builds running in parallel on the same server/agent. In such cases, it is better to run the integration tests using a random available port rather than a fixed port to avoid port conflicts.

14. **Integration Test Class Naming Convention**
    - All integration test classes must end with the "IntTest" suffix (e.g., `WorkflowFacadeIntTest.java`, `ConnectionServiceIntTest.java`).

    **Explanation:**
    - **Consistency and clarity:** Using a standard suffix makes it immediately clear which tests are integration tests versus unit tests.
    - **Build configuration:** Many build tools and CI/CD pipelines can be configured to run specific test types based on naming patterns, making it easier to separate integration tests from unit tests during the build process.
    - **Developer experience:** Consistent naming helps developers quickly identify the purpose and scope of test classes.

15. **Logging**
    - **Use a proper logging framework.**  
      Never use `System.out.println()` for application logging. Rely on SLF4J (or a compatible abstraction) and your chosen backend (Logback, Log4j2, etc.).

    - **Protect sensitive data.**  
      Ensure that no credentials, personal information, or other confidential details ever appear in log output.

    - **Guard expensive log calls.**  
      When building verbose messages at `DEBUG` or `TRACE` level, especially those involving method calls or complex string concatenations, wrap them in a level check or use suppliers:

    ```java
    if (logger.isDebugEnabled()) {
        logger.debug("Detailed state: {}", computeExpensiveDetails());
    }

    // using Supplier/Lambda expression
    logger.atDebug()
        .setMessage("Detailed state: {}")
        .addArgument(() -> computeExpensiveDetails())
        .log();
    ```

    **Explanation:**
    - **Flexible verbosity control:** A logging framework lets you adjust what gets logged and where with the support for tuning log levels per environment (development, testing, production).
    - **Rich contextual metadata:** Beyond the message itself, you can capture class/method names, thread IDs, process IDs, and any custom context via MDC, aiding diagnosis.
    - **Multiple outputs and formats:** Direct logs to consoles, rolling files, databases, or remote systems, and choose formats like JSON for seamless ingestion into ELK, Loki, or other log-analysis tools.
    - **Better tooling and analysis:** Structured logs and controlled log levels make it easier to filter noise, automate alerts, and visualize application behavior in real time.

## Resources

- [Official Documentation](https://docs.bytechef.io)
- [GitHub Repository](https://github.com/bytechefhq/bytechef)
- [Contributing Guide](https://github.com/bytechefhq/bytechef/blob/master/CONTRIBUTING.md)

## Coding Conventions

- Do not use short or cryptic variable names on both the server and client sides; prefer clear, descriptive names that communicate intent.
- Blank line before control statements (Java)
  - Insert exactly one empty line before control statements to visually separate logic.
  - Applies to: `if`, `else if`, `else`, `for`, enhanced `for`, `while`, `do { ... } while (...)`, `switch`, `try`/`catch`/`finally`.
  - Exceptions (do not add a blank line):
    - At the very start of a file, class, or method/block (immediately after an opening `{`).
    - When continuing the previous block on the same line (e.g., `} else {`, `} catch (...) {`, `} finally {`).
    - Immediately after another required blank line (avoid double blank lines).
    - Very short top-of-method guard clauses may omit the blank line for brevity when placed directly after the method signature.
    - If the automatic formatter (Spotless/Google Java Format) enforces a different layout, the formatter wins.
  - Example:
    ```java
    void process(User user) {
        if (user == null) {
            return;
        }

        for (Order order : user.getOrders()) {
            // ...
        }

        try {
            doWork();
        } catch (IOException e) {
            handle(e);
        }
    }
    ```
- Method chaining
  - Do not chain method calls except where this is natural and idiomatic
  - Allowed exceptions (non-exhaustive):
    - Builder patterns (including Lombok `@Builder`)
    - Java Stream API (`stream()`, `map()`, `filter()`, `collect()`)
    - `Optional`
    - Query DSLs and criteria builders:
      - Spring Data JPA `Specification` (`where(...).and(...).or(...)`)
      - JPA Criteria API (fluent `CriteriaBuilder`/`Predicate` construction)
      - QueryDSL (`JPAQueryFactory.select(...).from(...).where(...).orderBy(...)`)
      - jOOQ (`dsl.select(...).from(...).where(...).orderBy(...)`)
    - Reactive operators: Project Reactor `Mono`/`Flux` (e.g., `map`, `flatMap`, `filter`, `onErrorResume`)
    - HTTP client builder/request DSLs: Spring `WebClient`, OkHttp
    - Testing/assertion DSLs: AssertJ, Mockito BDD APIs
    - Logging fluent APIs: SLF4J 2.x `logger.atXxx()` fluent logger
    - JSON builders and similar fluent APIs: Jackson `ObjectNode`/`ArrayNode`, JSON‑P `JsonObjectBuilder`
  - Formatting rules:
    - Break each chained step onto its own line when there are 3+ operations or line length would exceed limits
    - Keep declarative chains (queries, reactive pipelines) as one logical block; prefer one operation per line
    - Avoid chaining when side effects are involved or intermediate values deserve names for clarity/debugging
