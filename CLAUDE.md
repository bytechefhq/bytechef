# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

ByteChef is an open-source, low-code API integration and workflow automation platform built on Spring Boot. It serves as both an automation solution and an embedded iPaaS (Integration Platform as a Service) for SaaS products.

## Essential Development Commands

### Server Development

All server commands should be run from the project root directory:

```bash
# Build and compile the project
./gradlew clean compileJava

# Run the server locally (requires Docker infrastructure)
cd server
docker compose -f docker-compose.dev.infra.yml up -d
cd ..
./gradlew -p server/apps/server-app bootRun

# Code formatting (must run before commits)
./gradlew spotlessApply

# Run checks and tests
./gradlew check
./gradlew test && ./gradlew testIntegration

# Generate component documentation
./gradlew generateDocumentation
```

### Client Development

Client commands should be run from the `client/` directory:

```bash
# Install dependencies
npm install

# Development server
npm run dev

# Code formatting
npm run format

# Linting and type checking
npm run lint
npm run typecheck

# Full check (lint + typecheck)
npm run check

# Build for production
npm run build

# Run tests
npm run test
```

### Infrastructure Setup

```bash
# Start PostgreSQL, Redis, and other services
cd server
docker compose -f docker-compose.dev.infra.yml up -d

# Or run everything in Docker
docker compose -f docker-compose.dev.server.yml up -d
```

## Architecture Overview

### Core Technology Stack
- **Backend**: Java 25 with Spring Boot 4.0.2
- **Frontend**: React 19.2 with TypeScript 5.9, Vite 7.3, TailwindCSS 3.4
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Message Broker**: Memory(default), Redis, RabbitMQ, Kafka, JMS, AMQP, AWS SQS
- **Build System**: Gradle 8+ with Kotlin DSL
- **Code Execution**: GraalVM Polyglot 25.0.1 (Java, JavaScript, Python, Ruby)
- **Testing**: JUnit 5, Vitest 4, Testcontainers
- **Node.js**: Version 20.19+ required for client development
- **Additional Tools**: MapStruct 1.6.3, Jackson 2.19.2, SpringDoc OpenAPI 3.0.0

### Main Server Module Structure

#### Core Modules (`server/libs/`)
- **`atlas/`** - Workflow engine core
    - `atlas-coordinator/` - Orchestrates workflow execution
    - `atlas-execution/` - Manages workflow execution lifecycle
    - `atlas-worker/` - Task execution workers
    - `atlas-configuration/` - Workflow configuration management

- **`automation/`** - iPaaS automation implementation
    - `automation-ai/` - AI-powered automation features
    - `automation-configuration/` - Project and workflow configuration
    - `automation-data-table/` - Data table management
    - `automation-execution/` - Workflow execution services
    - `automation-knowledge-base/` - Knowledge base integration
    - `automation-mcp/` - MCP (Model Context Protocol) integration
    - `automation-task/` - Task management services
    - `automation-workflow/` - Workflow coordination and execution

- **`platform/`** - Core infrastructure services
    - `platform-component/` - Component definition and management
    - `platform-connection/` - Connection handling
    - `platform-workflow/` - Workflow management
    - `platform-scheduler/` - Scheduling services
    - `platform-oauth2/` - OAuth2 authentication
    - `platform-webhook/` - Webhook handling
    - `platform-ai/` - AI integration services

- **`core/`** - Foundational utilities
    - `evaluator/` - Expression evaluation
    - `file-storage/` - File storage abstraction
    - `encryption/` - Encryption services
    - `message/` - Message broker abstraction

#### Component System
Components are located in `server/libs/modules/components/` and follow this pattern:
- Each component has a `ComponentHandler` class with `@AutoService` annotation
- Components define actions (operations) and triggers (event initiators)
- Connection definitions handle authentication and configuration
- OpenAPI specifications are often included for API-based components

### Enterprise Edition (EE) Microservices
The `server/ee/` directory contains microservices for distributed deployment:
- `api-gateway-app/` - API Gateway with routing
- `ai-copilot-app/` - AI Copilot service for workflow assistance
- `config-server-app/` - Spring Cloud Config server
- `configuration-app/` - Configuration management service
- `connection-app/` - Connection management service
- `coordinator-app/` - Workflow coordination service
- `execution-app/` - Workflow execution service
- `scheduler-app/` - Scheduling service
- `webhook-app/` - Webhook handling service
- `worker-app/` - Task execution workers
- `runtime-job-app/` - Runtime job execution

### Available Components
ByteChef includes 160+ built-in components for integrating with various services. Examples include:
- **CRM**: Salesforce, HubSpot, Pipedrive, Zoho CRM, Affinity, Agile CRM, Attio
- **Project Management**: Asana, Jira, Monday, Trello, ClickUp, Airtable
- **Communication**: Slack, Discord, Microsoft Teams, Telegram, WhatsApp
- **Email Marketing**: Mailchimp, SendGrid, Brevo, ActiveCampaign
- **E-commerce**: Shopify, WooCommerce, BigCommerce, Stripe, Square
- **Cloud Storage**: Google Drive, Dropbox, Box, OneDrive, AWS S3
- **Developer Tools**: GitHub, GitLab, Bitbucket, Docker, AWS
- **AI/ML**: OpenAI, Google Gemini, Anthropic Claude, Stability AI
- **Databases**: PostgreSQL, MySQL, MongoDB, Airtable, Baserow
- **Productivity**: Google Workspace, Microsoft 365, Notion, Confluence
- **Custom**: Bash scripting, HTTP requests, Custom code execution

## Development Patterns

### Component Development
When working on components in `server/libs/modules/components/`:

1. **Component Definition Pattern**:
```java
@AutoService(ComponentHandler.class)
public class ExampleComponentHandler implements ComponentHandler {
    private static final ComponentDefinition COMPONENT_DEFINITION = component("example")
        .title("Example Component")
        .connection(CONNECTION_DEFINITION)
        .actions(/* actions */)
        .triggers(/* triggers */);
}
```

2. **Testing Pattern**:
    - Component tests are in `./src/test/java/com/bytechef/component/`
    - Running tests auto-generates `.json` definition files in `./src/test/resources/definition/`
    - Delete existing `.json` files before running tests to regenerate them

3. **Documentation**:
    - Component documentation goes in `./src/main/resources/README.md`
    - Run `./gradlew generateDocumentation` to update docs

### Code Quality Requirements
- **Spotless**: Code formatting is enforced. Run `./gradlew spotlessApply` before commits
- **Checkstyle, PMD, SpotBugs**: Static analysis tools are configured
- **Tests**: All new code should include appropriate tests
- **Documentation**: Update component documentation when adding features

### File Structure Guidelines
- Configuration files are in `server/libs/config/`
- SDK components are in `sdks/backend/java/`
- CLI tools are in `cli/`
- Documentation source is in `docs/`

## Code Style and Best Practices

### Variable Naming
- Do not use short or cryptic variable names on both the server and client sides; prefer clear, descriptive names that communicate intent.
- This applies everywhere, including arrow function parameters and loop variables.
- Examples:
  ```typescript
  // Bad
  const current = users.find((u) => u?.login === login);

  // Good
  const current = users.find((user) => user?.login === login);
  ```
  ```java
  // Bad
  for (Order o : orders) { ... }

  // Good
  for (Order order : orders) { ... }
  ```

### Blank Line Before Control Statements (Java)
- Insert exactly one empty line before control statements to improve visual separation of logic:
  - Applies to: `if`, `else if`, `else`, `for`, enhanced `for`, `while`, `do { ... } while (...)`, `switch`, `try`/`catch`/`finally`.
- Exceptions (do not add a blank line):
  - At the very start of a file, class, or method/block (immediately after an opening `{`).
  - When the control keyword directly continues the previous block on the same line (e.g., `} else {`, `} catch (...) {`, `} finally {`).
  - Immediately after another required blank line (avoid double blank lines).
  - Very short top-of-method guard clauses may omit the blank line for brevity when they appear immediately after the method signature.
  - If the automatic formatter (Spotless/Google Java Format) enforces a different layout, the formatter’s output wins.
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

### Blank Line After Variable Modification (Java)
- Insert exactly one empty line between a variable modification and a subsequent statement that uses that variable
- This improves readability by visually separating the setup from the usage
- Example:
  ```java
  // Bad
  document.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);
  knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document);

  // Good
  document.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

  knowledgeBaseDocumentService.saveKnowledgeBaseDocument(document);
  ```

### Method Chaining
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
  - Break each chained step onto its own line for readability when there are 3+ operations or lines exceed the limit
  - Keep declarative chains (queries, reactive pipelines) as one logical block; prefer one operation per line
  - Avoid chaining when side effects are involved or intermediate values deserve names for clarity/debugging

### Spring Boot Best Practices

1. **Prefer Constructor Injection over Field/Setter Injection**
    - Declare all mandatory dependencies as `final` fields and inject them through the constructor
    - Spring will auto-detect if there is only one constructor, no need to add `@Autowired`
    - Constructor injection ensures proper initialization and enables easier unit testing

2. **Prefer package-private over public for Spring components**
    - Declare Controllers, `@Configuration` classes and `@Bean` methods with default (package-private) visibility whenever possible
    - Reinforces encapsulation while still allowing Spring's classpath scanning to work

3. **Organize Configuration with Typed Properties**
    - Group application-specific configuration properties with a common prefix
    - Bind them to `@ConfigurationProperties` classes with validation annotations
    - Prefer environment variables instead of profiles for different environments

4. **Define Clear Transaction Boundaries**
    - Define each Service-layer method as a transactional unit
    - Annotate query-only methods with `@Transactional(readOnly = true)`
    - Annotate data-modifying methods with `@Transactional`
    - Keep transactions as brief as possible

5. **Disable Open Session in View Pattern**
    - Set `spring.jpa.open-in-view=false` in application properties
    - Prevents N+1 select problems and forces explicit fetching strategies

6. **Separate Web Layer from Persistence Layer**
    - Don't expose entities directly as responses in controllers
    - Define explicit request and response record (DTO) classes
    - Apply Jakarta Validation annotations on request records

7. **Follow REST API Design Principles**
    - Use versioned, resource-oriented URLs: `/api/v{version}/resources`
    - Consistent patterns for collections and sub-resources
    - Use `ResponseEntity<T>` for explicit HTTP status codes
    - Use pagination for unbounded collections
    - Use snake_case or camelCase consistently in JSON

8. **Use Command Objects for Business Operations**
    - Create purpose-built command records (e.g., `CreateOrderCommand`) to wrap input data
    - Clearly communicates expected input data to callers

9. **Centralize Exception Handling**
    - Use `@ControllerAdvice` or `@RestControllerAdvice` with `@ExceptionHandler` methods
    - Return consistent error responses using ProblemDetails format (RFC 9457)

10. **Actuator Security**
    - Expose only essential actuator endpoints (`/health`, `/info`, `/metrics`) without authentication
    - Secure all other actuator endpoints

11. **Internationalization with ResourceBundles**
    - Externalize all user-facing text into ResourceBundles rather than embedding in code
    - Enables proper localization support

12. **Use Testcontainers for Integration Tests**
    - Spin up real services (databases, message brokers) in integration tests
    - Use specific Docker image versions, not `latest` tag

13. **Use Random Port for Integration Tests**
    - Annotate test classes with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
    - Avoids port conflicts in CI/CD environments

14. **Integration Test Naming Convention**
    - All integration test classes must end with "IntTest" suffix (e.g., `WorkflowFacadeIntTest.java`)
    - Ensures consistency and clarity between unit tests and integration tests

15. **Logging Best Practices**
    - Use SLF4J logging framework, never `System.out.println()`
    - Protect sensitive data - no credentials or personal information in logs
    - Guard expensive log calls with level checks or suppliers:
    ```java
    if (logger.isDebugEnabled()) {
        logger.debug("Detailed state: {}", computeExpensiveDetails());
    }
    ```

## Access and Authentication

### Development Login Credentials
- **Admin**: admin@localhost.com / admin
- **User**: user@localhost.com / user

### Default Ports
- **Server**: 8080 (main application)
- **API**: 9555 (backend API server)
- **Client**: 3000 (development server)
- **PostgreSQL**: 5432
- **Redis**: 6379
- **Mailhog**: 1025

## Common Development Workflows

### Adding a New Component
1. Create component directory in `server/libs/modules/components/`
2. Add component to `settings.gradle.kts`
3. Implement `ComponentHandler` with actions/triggers
4. Add tests and run to generate JSON definition
5. Add documentation in README.md
6. Run `./gradlew generateDocumentation`

### Working with Workflows
- Workflows are defined in JSON format
- Visual editor is available in the client application
- Workflow execution is handled by the Atlas engine
- Test workflows through the UI or API endpoints

### Database Changes
- Use Liquibase for schema migrations
- Migration files are in `server/libs/config/liquibase-config/`
- Database changes are applied automatically on startup

### Running Integration Tests
Integration tests use Testcontainers to spin up real services:
```bash
# Run all integration tests
./gradlew testIntegration

# Run integration tests for a specific module
./gradlew :server:libs:platform:platform-workflow:platform-workflow-service:testIntegration

# Run with specific Docker image versions (recommended)
# Integration tests automatically use Testcontainers with PostgreSQL 15
```

### Working with the Client
```bash
cd client

# Development with hot reload
npm run dev

# Run full quality checks before committing
npm run check  # Runs lint, typecheck, and tests with coverage

# Format code
npm run format

# Build for production
npm run build

# Run Storybook for component development
npm run storybook
```

### Debugging Workflows
1. **Enable Debug Logging**: Set logging level to DEBUG for specific packages in `application.yml`
2. **Use Workflow Test Mode**: Test workflows in the UI with step-by-step execution
3. **Check Execution Logs**: View workflow execution logs in the UI or database
4. **Inspect Variables**: Use the workflow editor to inspect variable values at each step
5. **Test Actions Individually**: Use the component test feature to test individual actions

### Code Quality Workflow
Before committing code, ensure:
```bash
# Server-side
./gradlew spotlessApply  # Format code
./gradlew check          # Run all checks

# Client-side
cd client
npm run format           # Format code
npm run check            # Run lint, typecheck, and tests
```

### Creating Custom Components via CLI
ByteChef includes a CLI tool for scaffolding components:
```bash
cd cli
./gradlew :cli-app:bootRun --args="component init openapi --name=my-component --openapi-path=/path/to/openapi.yaml"
```

## Build and Deployment

### Docker
- `Dockerfile` for server application
- `docker-compose.yml` for full stack
- `docker-compose.dev.infra.yml` for development infrastructure
- `docker-compose.dev.server.yml` for server-only development

### Kubernetes
- Helm charts are in `kubernetes/helm/bytechef-monolith/`
- Supports both monolith and microservices deployments

### CI/CD
- GitHub Actions workflows for build and test
- Automated component documentation generation
- Code quality checks are enforced

## Testing Strategies

### Unit Testing
- Write unit tests for all business logic in service classes
- Mock external dependencies using Mockito
- Test component actions and triggers in isolation
- Aim for high code coverage (target: 80%+)

### Integration Testing
- All integration test classes must end with `IntTest` suffix
- Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
- Leverage Testcontainers for real service dependencies
- Test configuration: `src/test/resources/config/application-testint.yml`
- Example integration test structure:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
class WorkflowServiceIntTest {
    @Autowired
    private WorkflowService workflowService;

    @Test
    void testWorkflowExecution() {
        // Test implementation
    }
}
```

### Component Testing
- Component tests auto-generate JSON definition files in `src/test/resources/definition/`
- Delete existing `.json` files before running tests to regenerate them
- Test both actions and triggers
- Verify connection configurations
- Test error handling and edge cases

### Client-Side Testing
```bash
cd client

# Run tests in watch mode during development
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Run all quality checks (includes tests)
npm run check
```

### End-to-End Testing
- Test complete workflows through the UI
- Verify trigger activation and workflow execution
- Test with real connections to external services (in staging)
- Validate data transformations and error handling

### CI/CD Testing
GitHub Actions workflows automatically run:
- Server tests: `./gradlew check jacocoTestReport sonar`
- Client tests: `npm run check` (lint + typecheck + tests)
- SonarCloud analysis for code quality
- Integration tests with Testcontainers

## Debugging Tips

### Server-Side Debugging

#### Enable Debug Logging
Edit `application.yml` or set environment variables:
```yaml
logging:
  level:
    com.bytechef: DEBUG
    com.bytechef.platform.workflow: TRACE
    org.springframework.web: DEBUG
```

#### Remote Debugging
Start the server with debug enabled:
```bash
./gradlew -p server/apps/server-app bootRun --debug-jvm
# Connects on port 5005 by default
```

In IntelliJ IDEA:
1. Run → Edit Configurations
2. Add New Configuration → Remote JVM Debug
3. Set port to 5005
4. Start debugging

#### Common Debugging Scenarios

**Workflow Execution Issues:**
- Check `atlas-execution` logs for execution details
- Inspect workflow JSON definition for syntax errors
- Verify component connections are properly configured
- Check if triggers are enabled and properly configured

**Component Action Failures:**
- Enable DEBUG logging for `com.bytechef.component`
- Verify input parameters match action definitions
- Check connection credentials and permissions
- Review component-specific logs in the execution logs

**Database Issues:**
- Check Liquibase changelog execution: `SELECT * FROM databasechangelog`
- Verify connection pool settings if seeing connection timeouts
- Check for transaction rollbacks in logs
- Use `spring.jpa.show-sql=true` to see SQL queries (dev only)

**Authentication/Authorization:**
- Check JWT token validity and expiration
- Verify user roles and permissions in database
- Review Spring Security filter chain execution
- Check CORS configuration for client-server communication

### Client-Side Debugging

#### Browser DevTools
- Use React DevTools extension for component inspection
- Monitor Network tab for API calls and responses
- Check Console for error messages and warnings
- Use Redux DevTools for state management debugging (if applicable)

#### Debug Mode
Run client in development mode with source maps:
```bash
npm run dev
# Source maps enabled by default for debugging
```

#### Common Client Issues

**API Call Failures:**
- Check Network tab for failed requests
- Verify API endpoint URLs are correct
- Check request/response headers and payloads
- Ensure authentication tokens are included

**State Management:**
- Use Zustand DevTools to inspect state changes
- Check React Query DevTools for cache state
- Verify state updates are triggering re-renders

**Workflow Editor Issues:**
- Check console for React Flow errors
- Verify workflow JSON structure
- Clear browser cache if seeing stale data
- Check for JavaScript errors in component definitions

### Performance Debugging

#### Server Performance
```bash
# Enable JVM metrics
./gradlew -p server/apps/server-app bootRun \
  -Dmanagement.endpoints.web.exposure.include=metrics,health,prometheus

# Access metrics at http://localhost:8080/actuator/metrics
```

#### Database Query Performance
- Enable query logging: `logging.level.org.hibernate.SQL=DEBUG`
- Monitor slow queries in PostgreSQL logs
- Use EXPLAIN ANALYZE for query optimization
- Check for N+1 query problems

#### Memory Issues
- Monitor heap usage: `/actuator/metrics/jvm.memory.used`
- Generate heap dump: `jmap -dump:format=b,file=heap.bin <PID>`
- Analyze with tools like Eclipse MAT or VisualVM

### Docker Debugging

```bash
# View logs for specific container
docker compose -f docker-compose.dev.infra.yml logs -f postgres

# Access container shell
docker exec -it bytechef-postgres bash

# Check container resource usage
docker stats

# Restart services
docker compose -f docker-compose.dev.infra.yml restart
```

### Useful Debugging Commands

```bash
# Check database connectivity
psql -h localhost -U postgres -d bytechef

# Monitor Redis
redis-cli
> MONITOR

# Check port usage
lsof -i :8080
netstat -an | grep 8080

# Tail application logs
tail -f server/apps/server-app/build/logs/application.log
```

## Troubleshooting

### Common Issues

**Port conflicts**
- Check if ports 5432, 6379, 1025, 8080 are in use: `lsof -i :<port>`
- Stop conflicting services or change port in configuration
- On macOS: `sudo lsof -i -P | grep LISTEN | grep :<port>`

**Java version mismatch**
- Ensure Java 25+ is installed: `java -version`
- Set JAVA_HOME environment variable
- Use GraalVM distribution for full functionality
- Check Gradle is using correct Java: `./gradlew -v`

**Docker issues**
- Make sure Docker Desktop is running
- Check Docker daemon status: `docker info`
- Verify Docker Compose version: `docker compose version`
- Clear Docker cache: `docker system prune -a`

**Database schema issues**
- Reset database: `docker compose -f server/docker-compose.dev.infra.yml down -v`
- Check Liquibase logs for migration errors
- Manually run migrations: `./gradlew liquibaseUpdate`
- Verify PostgreSQL version compatibility (15+)

**Build failures**
- Clean build: `./gradlew clean build`
- Clear Gradle cache: `rm -rf ~/.gradle/caches`
- Check for dependency conflicts: `./gradlew dependencies`
- Ensure Spotless formatting: `./gradlew spotlessApply`

**Client build issues**
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Clear npm cache: `npm cache clean --force`
- Check Node.js version: `node -v` (should be 20.19+)
- Verify package-lock.json is in sync: `npm ci`

**Workflow execution failures**
- Check Atlas worker logs for task execution errors
- Verify component connections are active
- Check Redis connectivity for message broker
- Ensure workflow JSON is valid
- Review component-specific documentation for required parameters

**Integration test failures**
- Ensure Docker is running (required for Testcontainers)
- Check if ports are available for test containers
- Verify test database initialization
- Review test logs in `build/test-results/`
- Increase test timeout if needed

### Performance

**Gradle Build Optimization**
- Gradle JVM is configured with 4GB heap in `gradle.properties`
- Parallel builds are enabled by default
- Build cache is enabled by default
- Use configuration cache: `./gradlew --configuration-cache build`
- Gradle daemon runs by default for faster subsequent builds

**Runtime Performance**
- Monitor JVM metrics via `/actuator/metrics`
- Adjust heap size via `JAVA_OPTS` environment variable
- Use connection pooling for database (HikariCP configured)
- Enable Redis for distributed caching in production
- Scale workers horizontally for increased throughput

**Client Performance**
- Use production build: `npm run build`
- Enable code splitting (configured in Vite)
- Lazy load components where appropriate
- Monitor bundle size: `npm run build` shows bundle analysis

This architecture provides a solid foundation for building scalable integration workflows while maintaining flexibility for both embedded and standalone deployment scenarios.
