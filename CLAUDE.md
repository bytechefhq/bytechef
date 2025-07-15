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
- **Backend**: Java 21 with Spring Boot 3.5.3
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
- **Java version**: Ensure Java 21+ is installed and JAVA_HOME is set
- **Docker**: Make sure Docker is running for infrastructure services
- **Database schema**: Use `docker compose down -v` to reset database

### Performance
- Gradle JVM is configured with 4GB heap in `gradle.properties`
- Parallel builds are disabled by default but can be enabled
- Use `./gradlew --build-cache` for faster builds

This architecture provides a solid foundation for building scalable integration workflows while maintaining flexibility for both embedded and standalone deployment scenarios.
