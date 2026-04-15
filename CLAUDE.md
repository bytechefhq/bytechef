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
- **Backend**: Java 25 with Spring Boot 4.0.5
- **Frontend**: React 19.2 with TypeScript 5.9, Vite 8, TailwindCSS 3.4
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Message Broker**: Memory(default), Redis, RabbitMQ, Kafka, JMS, AMQP, AWS SQS
- **Build System**: Gradle 8+ with Kotlin DSL
- **Code Execution**: GraalVM Polyglot 25.0.2 (Java, JavaScript, Python, Ruby)
- **Testing**: JUnit 5, Vitest 4, Testcontainers
- **Node.js**: Version 20.19+ required for client development
- **Additional Tools**: MapStruct 1.6.3, Jackson 3.x (`tools.jackson` package), SpringDoc OpenAPI 3.0.0

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
    - `automation-knowledge-base/` - Knowledge base integration
    - `automation-mcp/` - MCP (Model Context Protocol) integration
    - `automation-search/` - Search services for automation entities
    - `automation-swagger/` - OpenAPI/Swagger surface for automation APIs
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

**EE Code Conventions:**
- Use ByteChef Enterprise license header (not Apache 2.0) for all files under `server/ee/`
- Add `@version ee` Javadoc tag to all classes under `server/ee/`
- `api-gateway-app/` - API Gateway with routing
- `ai-gateway-app/` - AI gateway service for routing model traffic
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
ByteChef includes 180+ built-in components in `server/libs/modules/components/` covering CRM, project management, communication, e-commerce, cloud storage, AI/ML, databases, and custom code execution.

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
    - Delete existing `.json` files AND `build/resources/test/definition/` before running tests to regenerate them

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

### Client ESLint sort-keys Rule
- Object keys must be in natural ascending (alphabetical) order in client code
- Applies to mock objects, hoisted state, test data, and component props
- ESLint `--fix` does NOT auto-fix sort-keys - must be fixed manually
- Example: `{content: 'x', id: 'y'}` not `{id: 'y', content: 'x'}`

### Client Interface Naming Convention
- Interface names must end with `I` or `Props` (enforced by `@typescript-eslint/naming-convention`)
- Example: `EnvironmentConfigI`, `BadgePropsType` — not `EnvironmentConfig`

### Client Import Destructure Sort Order
- Named imports must be sorted alphabetically within `{}` (enforced by `bytechef/sort-import-destructures`)
- `type` keyword imports sort by their name, not grouped separately
- Example: `import {BoxIcon, CheckIcon, type LucideIcon, WrenchIcon} from 'lucide-react'`

### Non-null Assertion on Optional Chain (Client)
- `@typescript-eslint/no-non-null-asserted-optional-chain` forbids `obj?.prop!`
- Instead, filter nulls first, then assert: `.filter((item) => item?.id != null).map((item) => { const id = item!.id!; ... })`

### Ref Name Suffix (Client)
- `useRef` variables must end with `Ref` suffix (enforced by `bytechef/ref-name-suffix`) — e.g., `fileInputRef`, `totalToUploadRef`

### Variable Naming
- Do not use short or cryptic variable names on both the server and client sides; prefer clear, descriptive names that communicate intent.
- Do not prefix private methods with `_` — use plain method names (e.g., `extractFrontmatter` not `_extractFrontmatter`)
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

### Lucide Icon Imports (Client)
- Always import icons with the `Icon` suffix: `SearchIcon`, `DatabaseIcon`, `Loader2Icon`
- Not: `Search`, `Database`, `Loader2`

### CSS Class Merging (Client)
- Use `twMerge` from `tailwind-merge` for conditional class merging
- Do not use `cn()` utility

### React Patterns (Client)
- Use `fieldset` (with `border-0`) for semantic form grouping instead of `div`
- Use `useMemo` for computed values instead of IIFEs in JSX
- Prefer `||` over `??` for JSX fallbacks (e.g., `trigger || defaultTrigger`)

### React Hook Ordering (Client)
- Order hooks in components/custom hooks: `useState` → `useRef` → custom store hooks → other custom hooks → derived values/`useMemo`/`useCallback` → `useEffect` → `return`
- All `useEffect` calls go last, immediately before the `return` statement
- Group multiple declarations of the same hook type consecutively (e.g., all `useRef` calls together, then `.current` assignments in a separate block)

### Client Error Handling
- `useFetchInterceptor.ts` provides centralized error handling for all fetch requests including GraphQL
- GraphQL errors are automatically parsed and displayed as toast notifications
- Individual `onError` handlers on mutations are therefore not necessary for basic error display
- Only add per-mutation `onError` if you need custom behavior beyond the global toast (e.g., resetting form state)

### GraphQL Conventions
- Enum values must use SCREAMING_SNAKE_CASE (e.g., `DELETE`, `GET`, `QUERY`, `PATH`)
- Consistent with HttpMethod and other enums in `*.graphqls` files

### ID Generation
- Avoid `hashCode()` for generating unique identifiers (collision risk)
- Prefer SHA-256 with first 8 bytes for deterministic long IDs, or UUID for true uniqueness

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

### No Trailing Blank Line in Class Body (Java)
- Do not add an empty line between the last method (or field) and the closing `}` of a class

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

### Temporal Dead Zone (TDZ) with Synchronous Callbacks
- `const x = fn(callback)` — if `fn` calls `callback` synchronously, `x` is not yet assigned inside `callback`
- Accessing `x` inside such a callback throws `ReferenceError: Cannot access 'x' before initialization`
- Fix: defer access to `x` via `setTimeout` or store in a mutable ref before the call

### Code Quality Tool Patterns

**SpotBugs**:
- Don't use rough approximations of known constants (e.g., use `Math.PI` instead of `3.14`)
- Always check return values of methods like `CountDownLatch.await(long, TimeUnit)` - returns boolean
- Use try-with-resources for `Connection` objects to avoid resource leaks
- Catch specific exceptions (`SQLException`) instead of generic `Exception` when possible

**PMD**:
- Use `@SuppressWarnings("PMD.UnusedFormalParameter")` for interface-required but unused parameters
- Don't qualify static method calls with the class name when already inside that class (e.g., `builder()` not `ClassName.builder()`)

**Checkstyle**:
- Test method names must be camelCase without underscores (e.g., `testExecuteSuccess` not `testExecute_Success`)
- Naming rule applies to ALL methods in test sources (including private helpers), not just `@Test` methods
- Empty blocks are forbidden — a comment alone doesn't satisfy the `EmptyBlock` rule; add an executable statement
- `TODO:` comments are forbidden (`TodoComment` rule) — rewrite as plain comments describing intent, or implement the work

### Connection Visibility (EE-only feature)

Connections in automation can be `PRIVATE` (creator only), `WORKSPACE` (all workspace members),
`PROJECT` (members of the projects the connection is shared with), or `ORGANIZATION` (cross-workspace).

- **CE**: only `PRIVATE` is reachable. `ConnectionFacadeImpl.create()` forces visibility to PRIVATE
  whenever the running edition is not `EE`. The visibility selector, scope badge, promote/demote/share
  menu items, project-share dialog, and demote-confirm dialog are all hidden in CE.
- **EE**: visibility selector appears in the connection create dialog (only `PRIVATE` for non-admins,
  `WORKSPACE` requires `ROLE_ADMIN`). The connection list ellipsis menu and the scope badge expose
  promote/demote/share-with-projects actions to admins; the connection's creator can also demote a
  WORKSPACE-promoted connection back to PRIVATE (orphan-recovery path if all admins lose role).
- **Embedded**: visibility is server-side forced to PRIVATE in `ConnectionFacadeImpl.create()`
  regardless of the incoming request body.
- **Workflow editor connection dropdown**: filters by `connectionDefinition.version` (NOT
  `componentConnection.componentVersion`); the two often differ.

**GraphQL mutations** (admin-only via `@PreAuthorize` unless noted):
- `promoteConnectionToWorkspace(workspaceId, connectionId)` — PRIVATE/PROJECT → WORKSPACE.
- `demoteConnectionToPrivate(workspaceId, connectionId)` — any → PRIVATE. Admin OR creator
  (orphan-recovery; no `@PreAuthorize`, check is in the facade).
- `shareConnectionToProject(workspaceId, connectionId, projectId)` — adds project share, sets
  visibility to PROJECT.
- `revokeConnectionFromProject(workspaceId, connectionId, projectId)` — removes share; auto-demotes
  to PRIVATE when the last project is removed.
- `setConnectionProjects(workspaceId, connectionId, projectIds: [ID!]!)` — diff-based bulk replace;
  emits `CONNECTION_SHARES_REPLACED` audit event with a correlation ID for grouping the
  per-row share/revoke events.
- `promoteAllPrivateConnectionsToWorkspace(workspaceId)` — CE→EE migration helper; returns
  `BulkPromoteResult { promoted, skipped, failed, failures: [{connectionId, message}] }` so partial
  failures surface to the caller instead of bailing on the first error. `skipped` counts benign
  races where a connection was already promoted to the target visibility by a concurrent actor
  (`CONNECTION_ALREADY_AT_TARGET_VISIBILITY`), so the UI can report "N promoted, M skipped" rather
  than treating those as failures.

**Metrics**:
- `bytechef_connection_create` (Counter) — incremented on every successful workspace connection
  creation. Tag: `visibility=PRIVATE|WORKSPACE`. Wired via `ObjectProvider<MeterRegistry>` so
  lightweight app variants without actuator start cleanly.

### Spring Boot Project Conventions

- **Integration Test Naming**: All integration test classes must end with "IntTest" suffix (e.g., `WorkflowFacadeIntTest.java`)
- **Spring 7 Programmatic Bean Registration**:
    - Use `BeanRegistrar` + `@Import` instead of `BeanFactoryPostProcessor` for programmatic bean registration
    - Resolve collection dependencies via `context.beanProvider(Class).orderedStream().toList()` (replaces `beanFactory.getBeansOfType()`)
    - Resolve named beans via `context.bean("beanName", Class)` in supplier
    - Test `BeanRegistrar` specs by capturing `Consumer<Spec<T>>` with `ArgumentCaptor`, applying to mock `Spec`, and verifying fluent calls

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
- After renaming migration files, delete stale copies from `build/resources/` — Liquibase sees both old and new on classpath

### New Spring Data JDBC Modules
- Create `@AutoConfiguration` class with `@EnableJdbcRepositories(basePackages = "...")` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)`
- Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Add `spring-boot-autoconfigure` dependency to `build.gradle.kts`

### Agent Component Handlers
- `@AutoService(ComponentHandler.class)` — ServiceLoader discovery, no Spring DI available
- `@Component("name_v1_ComponentHandler")` — Spring discovery, supports constructor injection (used by guardrails, RAG, chat-memory, and agent utils when Spring beans are needed)

### GraphQL Development Workflow
- Add schema path to `client/codegen.ts` `schema` array
- Create operation `.graphql` files in `client/src/graphql/<domain>/`
- Run `cd client && npx graphql-codegen` to regenerate `src/shared/middleware/graphql.ts`
- Commit operations and generated file separately

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

### Debugging Workflows
1. **Enable Debug Logging**: Set logging level to DEBUG for specific packages in `application.yml`
2. **Use Workflow Test Mode**: Test workflows in the UI with step-by-step execution
3. **Check Execution Logs**: View workflow execution logs in the UI or database
4. **Inspect Variables**: Use the workflow editor to inspect variable values at each step
5. **Test Actions Individually**: Use the component test feature to test individual actions

### Commit Message Convention
- Client-side changes: `<ticket_number> client - <description>`
- Server-side changes: `<ticket_number> <description>`
- Example client: `2898 client - Add EnvironmentSelect dropdown to automation page headers`
- Example server: `2898 Add environment selection endpoint`

### Code Quality Workflow
- When committing, only stage files directly modified by the current task — do not include pre-existing unstaged changes that are unrelated

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

### Resolving PR Review Comments
- Use `gh api graphql` with `resolveReviewThread` mutation to close threads programmatically
- Get thread IDs via: `gh api graphql -f query='{ repository(owner: "X", name: "Y") { pullRequest(number: N) { reviewThreads(first: 20) { nodes { id isResolved path } } } }'`

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
- Unit test class names must end with `Test` suffix only (NOT `IntTest`) — e.g., `KnowledgeBaseFileStorageTest`
- Drop `Impl` from test class names — test the interface contract, not the implementation detail

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
- Delete existing `.json` files AND `build/resources/test/definition/` before running tests to regenerate them (classpath serves from build output)
- Test both actions and triggers
- Verify connection configurations
- Test error handling and edge cases

### Test ObjectMapper Setup
- Use `@ExtendWith(ObjectMapperSetupExtension.class)` for tests that use `JsonUtils`, `MapUtils`, or `ConvertUtils` — do NOT manually call `setObjectMapper()` in test configurations

### Task Dispatcher Definition Snapshot Tests
- `DefinitionFactoryTest` classes use `JsonFileAssert` (snapshot pattern): if the JSON file is missing, it's auto-generated; if present, it's compared
- When task dispatcher definition models change (new fields), delete snapshot JSON files from BOTH `src/test/resources/definition/` and `build/resources/test/definition/`, then rerun tests

### EE Microservice Remote Client Pattern
- EE apps (`server/ee/apps/`) use remote client stubs instead of local service implementations
- When adding new SPI interfaces to platform modules, create corresponding `@Component @ConditionalOnEEVersion` stub classes in the relevant `*-remote-client` module (e.g., `automation-configuration-remote-client`)
- Stubs throw `UnsupportedOperationException` — they satisfy Spring DI; actual work is done via REST calls
- `@ConditionalOnEEVersion` requires `bytechef.edition=ee` in the app's config
- For lightweight EE apps (e.g., `runtime-job-app`) that can't pull in full remote client modules, use `@TestConfiguration` with mock/stub beans in the integration test

### Component Integration Test Configuration
- Component integration tests use `@ComponentIntTest` → `ComponentTestIntConfiguration` in `platform-component-test-int-support`
- `ComponentTestIntConfiguration` only scans `com.bytechef.platform.component` — beans from other packages (e.g., `com.bytechef.file.storage`) must be manually registered
- `Base64FileStorageService.getType()` returns `"JDBC"`, so test property `bytechef.file-storage.provider=jdbc` must be set to match

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

#### Zustand Store Testing
- Reset store state in `beforeEach` via `store.setState({...initial...})` — avoids cross-test leakage
- Access store imperatively via `store.getState()` for assertions (no hook needed)
- Export stores (e.g., `export const featureFlagsStore`) to enable direct state manipulation in tests
- Use `renderHook` from `@testing-library/react` for hooks that wrap stores
- Flush async store updates with `await act(async () => { await new Promise(r => setTimeout(r, 10)); })`

#### PostHog Mock
- Global mock in `.vitest/setup.ts` — `onFeatureFlags: vi.fn()`, `isFeatureEnabled: vi.fn().mockReturnValue(false)`
- `onFeatureFlags` returns `() => void` (unsubscribe); mock overrides must return a function: `return () => {}`
- `import('posthog-js')` dynamic imports resolve to the same mock; multiple synchronous calls share one Promise

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


## Troubleshooting

**Database schema issues**
- Reset database: `docker compose -f server/docker-compose.dev.infra.yml down -v`
- Check Liquibase logs for migration errors
- Manually run migrations: `./gradlew liquibaseUpdate`

**Workflow execution failures**
- Check Atlas worker logs for task execution errors
- Verify component connections are active
- Check Redis connectivity for message broker
- Review component-specific documentation for required parameters

**Integration test failures**
- Ensure Docker is running (required for Testcontainers)
- Review test logs in `build/test-results/`
