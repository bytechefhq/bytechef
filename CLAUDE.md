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
- **Backend**: Java 25 with Spring Boot 3.5.6
- **Frontend**: React 19 with TypeScript, Vite, TailwindCSS
- **Database**: PostgreSQL with Liquibase migrations
- **Message Broker**: Redis (default), supports RabbitMQ, Kafka, JMS, AMQP
- **Build System**: Gradle with Kotlin DSL
- **Code Execution**: GraalVM Polyglot (Java, JavaScript, Python, Ruby)

### Main Server Module Structure

#### Core Modules (`server/libs/`)
- **`atlas/`** - Workflow engine core
  - `atlas-coordinator/` - Orchestrates workflow execution
  - `atlas-execution/` - Manages workflow execution lifecycle
  - `atlas-worker/` - Task execution workers
  - `atlas-configuration/` - Workflow configuration management

- **`automation/`** - iPaaS automation implementation
  - `automation-configuration/` - Project and workflow configuration
  - `automation-connection/` - Connection management
  - `automation-workflow/` - Workflow coordination and execution
  - `automation-task/` - Task management services

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
- `configuration-app/` - Configuration management service
- `execution-app/` - Workflow execution service
- `worker-app/` - Task execution workers
- `runtime-job-app/` - Runtime job execution

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

### Method Chaining
- Avoid method chaining except when the builder pattern is applicable

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

## Troubleshooting

### Common Issues
- **Port conflicts**: Check if ports 5432, 6379, 1025 are in use
- **Java version**: Ensure Java 25+ is installed and JAVA_HOME is set
- **Docker**: Make sure Docker is running for infrastructure services
- **Database schema**: Use `docker compose down -v` to reset database

### Performance
- Gradle JVM is configured with 4GB heap in `gradle.properties`
- Parallel builds are disabled by default but can be enabled
- Use `./gradlew --build-cache` for faster builds

This architecture provides a solid foundation for building scalable integration workflows while maintaining flexibility for both embedded and standalone deployment scenarios.
