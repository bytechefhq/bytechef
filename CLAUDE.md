# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

ByteChef is an open-source, low-code API integration and workflow automation platform built on Spring Boot. It serves as both an automation solution and an embedded iPaaS (Integration Platform as a Service) for SaaS products.

**Maintaining this file:** it is loaded into every session, so it stays a working guide — commands,
conventions, and cross-cutting gotchas. Feature deep-dives belong in `.agents/` with a one-line
pointer in the deep-dive index below; design rationale belongs in `docs/superpowers/specs/`.
When a landed feature produces a page of narrative detail, write it to `.agents/`, not here.

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
- **Backend**: Java 25 with Spring Boot 4.0.7
- **Frontend**: React 19.2 with TypeScript 6.0, Vite 8, TailwindCSS 4.3 (v4 — Vite plugin, not PostCSS)
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Message Broker**: Memory(default), Redis, RabbitMQ, Kafka, JMS, AMQP, AWS SQS
- **Build System**: Gradle 9.7 with Kotlin DSL
- **Code Execution**: GraalVM Polyglot 25.0.3 (Java, JavaScript, Python, Ruby)
- **Testing**: JUnit 5, Vitest 4, Testcontainers
- **Node.js**: Version 20.19+ required for client development
- **Additional Tools**: MapStruct 1.6.3, Jackson 2.19 (transitioning to 3.x — both `com.fasterxml.jackson` and `tools.jackson` packages currently appear in deps), SpringDoc OpenAPI 3.0.3

### Main Server Module Structure

#### Core Modules (`server/libs/`)
- **`atlas/`** - Workflow engine core
    - `atlas-coordinator/` - Orchestrates workflow execution
    - `atlas-execution/` - Manages workflow execution lifecycle
    - `atlas-worker/` - Task execution workers
    - `atlas-configuration/` - Workflow configuration management

- **`automation/`** - iPaaS automation implementation
    - `automation-ai/` - AI-powered automation features, including `automation-ai-mcp-*` (MCP / Model Context Protocol integration)
    - `automation-configuration/` - Project and workflow configuration
    - `automation-data-table/` - Data table management
    - `automation-knowledge-base/` - Knowledge base integration
    - `automation-search/` - Search services for automation entities
    - `automation-openapi/` - OpenAPI (Scalar) surface for automation APIs
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
ByteChef ships built-in components in `server/libs/modules/components/` covering CRM, project
management, communication, e-commerce, cloud storage, AI/ML, databases, and custom code execution.
Count them by **leaf module**, not top-level directory — several top-level entries are grouping
folders holding many components each (`google` holds 18, `microsoft` 7) and a few are not
components at all (`build`, `libs`). For the current number, count the generated reference pages:
`ls docs/content/docs/reference/components | wc -l`.

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

### List row rhythm (Client)

Every `*ListItem` row is two columns (title/tags on the left, version-or-switch + date on the right)
centred against each other with `items-center`. The two columns only line up if they are the **same
height**, so all 16 of them share one rhythm: first row `min-h-8`, an 8px gap (`gap-y-2` on the right
column, the left column's long-standing `mt-2`), second row `min-h-7`. Changing one column's gap or
dropping a `min-h` silently offsets that list's second row against its neighbour — the failure is
invisible in the file you edited and only shows on screen.

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
  - Logging fluent APIs: SLF4J 2.x `log.atXxx()` fluent logger
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

### Agent HITL approvals (chat cards + tool gate)

Two disjoint primitives: **Approval** (a decision; a comment is valid on BOTH outcomes) and
**AskUserQuestion** (LLM clarification). Typing in chat NEVER resolves an approval. Delivery fans out
best-effort over a dozen channels (chat, Slack, Discord, Telegram, Mattermost, Rocket.Chat, email, WhatsApp,
SMS, approval task), failing the step only when every configured channel fails; five of them resolve in
place. A `requiresApproval: true` TOOLS entry wraps the callback in `ApprovalGateToolCallback`, which
suspends via the sentinel protocol. Expiry, reminders, and escalation each run a 15-minute
platform-coordinator sweep. Never add notification logic under `server/libs/atlas/`.
See `.agents/hitl-approvals.md`.

### Variables (workspace / embedded organization, EE)

`server/ee/libs/platform/platform-variable/` (`-api`/`-service`/`-graphql`). One `Property` row per variable:
key `variable.<NAME>`, value `{"value": …}`, `Scope.WORKSPACE`/workspaceId (automation) or `Scope.EMBEDDED`/null
(embedded — its first use), `environment` always set; listed via `PropertyService.getPropertiesByKeyPrefix`. No
new table — variables reuse the existing `property` table. One changelog exists:
`20260825000001_platform_configuration_property_unique_null_scope_id.xml` adds a partial unique index on
`property (key, scope, environment) WHERE scope_id IS NULL`, because `uk_property_key_scope_scope_id_environment`
(non-partial, includes `scope_id`) never fires for `Scope.EMBEDDED` rows — Postgres treats every NULL `scope_id`
as distinct, so two concurrent embedded creates of the same name both inserted before this index existed.
`VariableServiceImpl.create`/`update` catch the resulting `DataIntegrityViolationException` and translate it to
`VARIABLE_NAME_ALREADY_EXISTS`. `VariableServiceImpl` re-lists the scope on every by-id operation rather than
trusting the id alone, so a variable id from another scope is indistinguishable from a missing one — ids never
leak across scopes.
Name/value validation (`^[A-Za-z_][A-Za-z0-9_]{0,49}$`, 4096-char value cap) is a static, unconditional
`VariableNameValidator`, deliberately not a Spring bean so it can't be silently disabled by a conditional.

Runtime: `PrincipalJobFacadeImpl` (all four create methods) and `TestWorkflowExecutorImpl` seed the resolved map
into `Job.inputs` under `JobInputConstants.VARIABLES_INPUT` (`"vars"`) through the CE `WorkflowVariablesResolver`
seam (`platform-api`, resolved via `ObjectProvider#getIfAvailable`), implemented by `WorkflowVariablesResolverImpl`
— fail-open, WARN once per JVM on a genuine resolution failure. That WARN is real defence-in-depth (it fires if the
resolver bean exists and the store it reaches misbehaves), but it does **not** cover the actual distributed-EE gap:
only `server-app` and `configuration-app` carry `platform-variable-*`, and of the distributed apps only
`execution-app` runs `PrincipalJobFacadeImpl` at all (`coordinator-app`/`webhook-app`/`connection-app`/
`scheduler-app` depend on `platform-workflow-execution-remote-client`, not the `-service` module that hosts the
facade). `execution-app` has no `platform-variable-*` on its classpath, so `WorkflowVariablesResolverImpl` never
registers there; `getIfAvailable()` simply returns null, `Job.inputs` is left untouched, and nothing is ever
attempted — so no WARN is ever logged for this case. Because `Job.inputs` IS the initial workflow context, this
gives snapshot semantics for free at the top level: a top-level run resolves `vars` once at job creation and keeps
that same map for its whole lifetime, so editing a variable mid-run never affects an already-created job's own
inputs. `createChildJob` re-runs the seeding for subflows, though, so a subflow started mid-run resolves its own
`vars` snapshot afresh and can observe a value edited after the parent run started. Scope comes from per-`PlatformType`
`VariableScopeProvider`s (`ProjectVariableScopeProvider` in EE automation-configuration-service,
`IntegrationVariableScopeProvider` in embedded-configuration-instance-impl).
Editor previews get `vars` through `WorkflowEvaluationInputsFacade` — the ONE place that merges test-configuration
inputs with `vars`; never call `getWorkflowTestConfigurationInputs` directly from a preview facade. `vars` is
reserved as a workflow input name and a node name (`WorkflowValidatorFacade` + client) — this guards the top-level
`vars.*` namespace in the flat execution context, not individual variable names; a variable literally named `vars`
is fine and resolves as `${vars.vars}`.

Variables are **not secrets**: values are shown in clear on the settings page, in the editor's data-pill panel, and
(via `Job.inputs`) on the execution detail page of any run that used them. Storage is encrypted at rest only because
`Property.value` always is — that's incidental, not a security boundary. There is no `set` action; variables are
read-only from workflows.

Client: CE editor reads variables through `shared/edition/variables/variablesApi.ts`; pages
`ee/pages/settings/{automation,embedded}/variables` (routes `/automation/settings/variables` under the "Current
Workspace" nav group, member-or-admin-reachable but mutation controls gated on the `VARIABLE_MANAGE` scope; and
`/embedded/settings/variables`, admin-only), shared `ee/shared/components/variables`. The workflow editor surfaces
variables as their own **Variables** section in the Data Pill Panel (`DataPillPanelBodyVariablesItem`), fed by
`getWorkflowInputAndVariableDataPills`/`useWorkflowVariables`.

Spec: `docs/superpowers/specs/2026-08-17-custom-variables-design.md`.

### Environment promotion (EE)

`server/ee/libs/automation/automation-promotion` promotes an API collection, MCP server, A2A server or
plain project deployment to its counterpart in another environment, matched by a lineage `uuid` rather
than by name. **Monolith only** (`server-app`). Re-promotion syncs ONLY the exposed surface; name, tags,
enabled flags, auth settings, secret keys and existing connection bindings are environment-local. A
created counterpart is always disabled. See `.agents/environment-promotion.md`.

### Sidebar navigation groups (Client)

`AppSidebarNavItemI` has an optional `group` field; `AppSidebar` folds CONSECUTIVE items sharing
a `group` into one labeled `SidebarGroup` at the position of their first item (non-adjacent items
with the same group form separate sections — keep group members adjacent in the nav arrays in
`App.tsx`). Current groups: automation "Deployments" (Project Deployments, API Collections, MCP
Servers, Context Store) and "Data" (Data Tables, Knowledge Base, Files); embedded
"Configurations" (Integration Configurations, MCP Servers). Feature-flag filtering runs before
grouping, so a group renders with whatever members survive their flags.

### Graph dispatcher canvas (Client)

A `graph/v1` dispatcher renders as a free-form `graphFrame` box, not as a chain. `layoutGraphFrames`
is a PRE-PASS: it lays each frame's members out first, hands the outer engine one sized leaf node,
and re-appends members and their routes afterwards — so nothing the outer layout does can disturb
them, and a nested graph must be processed innermost-first. Member positions are FRAME-relative
(`metadata.ui.nodePosition`); `toFrameChildPosition`/`fromFrameChildPosition` in `graphFrameGeometry`
are the only sanctioned crossing to canvas coordinates — open-coding the header offset flings the
group. Routing is an explicit `parameters.transitions: [{from, to, condition?}]` list, never a
per-node `next`: declaration order within a `from` IS conditional priority, so a transition's index
is its identity. `graphTransition`/`graphStart` edges are free-form routes, not chain links — a
cyclic pair would corrupt dagre's and ELK's ranking, so both engines strip them, and chain walkers
(`collectChainSuccessorNodes`) must skip them. `onConnect` is scoped by handle suffix
(`resolveGraphConnection`): only graph transition handles connect, and only within one graph.

### Vitest mock factory hoisting (Client)

`vi.mock(...)` calls hoist to the top of the file, so module-scope `const` declarations are NOT yet
initialised when the factory runs. Referencing `setStateMock` (or any module-scope ref) inside a
`vi.mock` factory crashes with `Cannot access X before initialization`.

Use `vi.hoisted(() => ({...}))` to declare the refs alongside the mocks. Example:

```ts
const {navigateMock, setStateMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    setStateMock: vi.fn(),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', () => ({
    useCopilotStore: {setState: setStateMock},
}));
```

This pattern shows up in `Agents.test.tsx` and any test that mocks Zustand
stores or router hooks via factory-injected mocks.

### Spring Boot Project Conventions

- **Integration Test Naming**: All integration test classes must end with "IntTest" suffix (e.g., `WorkflowFacadeIntTest.java`)
- **Spring 7 Programmatic Bean Registration**:
    - Use `BeanRegistrar` + `@Import` instead of `BeanFactoryPostProcessor` for programmatic bean registration
    - Resolve collection dependencies via `context.beanProvider(Class).orderedStream().toList()` (replaces `beanFactory.getBeansOfType()`)
    - Resolve named beans via `context.bean("beanName", Class)` in supplier
    - Test `BeanRegistrar` specs by capturing `Consumer<Spec<T>>` with `ArgumentCaptor`, applying to mock `Spec`, and verifying fluent calls

## Feature deep-dives

These areas have enough hard-won detail that it lives in its own file rather than here. **Read the
relevant file before working in that area** — each records invariants whose violation is silent.

| Area | Read before touching |
|------|----------------------|
| `.agents/ai-hub.md` | AI Hub chats, agent tool tiers, subagent memory + interactive questions, auto-memory, Copilot module map |
| `.agents/agents.md` | Agents (automation): channels, generated workflows, elements, publishing |
| `.agents/hitl-approvals.md` | Approval cards, the tool gate, delivery fan-out |
| `.agents/ai-guardrails.md` | Guardrails engine, advisor, per-surface coverage |
| `.agents/ai-gateway-guardrails.md` | Gateway adapter + project overlay |
| `.agents/ai-model-catalog.md` | CE snapshot vs EE persisted twin |
| `.agents/agentic-ai-component.md` | Embabel GOAP component (opt-in) |
| `.agents/code-artifacts.md` | Custom components & code workflows: draft/publish, perform context, declared connections |
| `.agents/mcp-a2a-servers.md` | MCP workflows-as-tools (fromAi), A2A servers |
| `.agents/environment-promotion.md` | Promoting API collections / MCP / A2A / deployments across environments |
| `.agents/embedded-bridge.md` | Embedded automation code workflow bridge |
| `.agents/api-connectors.md` | API connectors (spec-as-source-of-truth) |
| `.agents/resource-visibility.md` | Workspace scoping, visibility/sharing, per-environment roles |
| `.agents/execution-reliability.md` | Plan limits, crash recovery, notifications, error workflow, URL signing |
| `.agents/component-wrappers.md` | Component wrapper patterns |

Design rationale for most of the above lives in `docs/superpowers/specs/`.

### Cross-cutting rules from those docs

These are the parts that apply outside their own area, so they stay here:

- **New platform-package entities get a nullable `workspace_id BIGINT` column** — not a
  `workspace_<entity>` relation table. The field is `Long`, never primitive; null is a real state.
  Create a relation table only for a genuinely many-to-many relationship with no owner concept
  (the `workspace_user` shape). Six pre-existing relation tables deliberately remain.
- **Every resource is created WORKSPACE-visible.** Visibility is a *precondition* of
  `hasResourceScope`, not a filter beside it. "Specific people" is `PRIVATE` + `resource_grant` rows.
- **Never add notification, admission, or approval logic under `server/libs/atlas/`** — the engine
  stays agnostic; those concerns live in `platform-coordinator` and the platform modules.
- **Enum ordinals are persisted as INT** — append new values at the end, never reorder.
- **A new task dispatcher must be registered in three places, not one.** Its own `@Bean`s cover the
  production coordinator, which autowires `List<TaskDispatcherResolverFactory>` — but
  `WorkflowTestConfiguration` (the editor's Test button) and `WebhookConfiguration` each build their
  chain from a literal `List.of(...)`. An unregistered type matches no resolver, falls through to the
  worker, and surfaces as `Component definition with name '<type>' ... not found`. Adding the Gradle
  dependency is not enough — `WorkflowTestConfigurationTest`/`WebhookConfigurationTest` scan
  `com.bytechef.task.dispatcher` and fail when a dispatcher or completion handler on the classpath is
  missing from either list.
- **A specialist subagent is for multi-step reasoning over a domain**, not for hiding the number of
  CRUD tools in one. Self-contained CRUD goes flat (on the AI Hub, pinned or catalog-demoted; on Copilot and MCP, just registered — tool search exists only on the hub).

## Access and Authentication

### API facade vs shared facade
Where a domain has both (e.g. `AiSkillApiFacade` / `AiSkillFacade`), the **API facade is the HTTP surface and
owns authorization** (`checkOwnerOrAdmin(id)` on every by-id operation). The shared facade is deliberately
unguarded because runtime agent tools call it with no security context. Controllers and GraphQL mappings must
go through the API facade — wiring one to the shared facade compiles fine and silently removes the ownership
check. When adding a method, add it to BOTH interfaces, not just the shared one.

### Development Login Credentials
- **Admin**: admin@localhost.com / admin
- **User**: user@localhost.com / user

### Default Ports
- **Server**: 9555 in the `dev` profile, 8080 in `prod` — one app, not two services
- **Client**: 5173 (Vite dev server — `vite.config.ts` sets no `server.port`, so it takes Vite's default)
- **PostgreSQL**: 5432
- **Redis**: 6379
- **Mailpit**: 1025 (`axllent/mailpit`; the compose service is `mailpit`, not mailhog)

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
- Before editing an init changelog **in place**, prove the schema is unreleased:
  `git ls-tree -r --name-only <latest-tag> | grep <module>` plus `git merge-base --is-ancestor <introducing-commit> master`.
  Unreleased ⇒ edit init directly; released ⇒ add a new changeset (never rewrite what customers have run)
- In-place init edits break local dev DBs two ways (schema drift + stale md5sums);
  `scripts/dev/sync-local-schema-after-collapse.sh` patches both, idempotently
- The `liquibase` Spring profile does **not** apply migrations via `bootRun` — it exits 0 having created nothing.
  Verify changelog edits with an existing `*IntTest` instead; Testcontainers builds the schema from scratch,
  which is stronger evidence anyway
- `--spring.profiles.active=X` *replaces* the value baked into `build/resources/main/config/application.yml`
  at processResources time (`profiles.active: dev`, `contexts: mono, dev`), silently dropping the dev
  `bytechef.*` defaults. Layer it instead: `--spring.profiles.active=dev,X`

### New Spring Data JDBC Modules
- Create `@AutoConfiguration` class with `@EnableJdbcRepositories(basePackages = "...")` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)`
- Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Add `spring-boot-autoconfigure` dependency to `build.gradle.kts`

### Agent Component Handlers
- `@AutoService(ComponentHandler.class)` — ServiceLoader discovery, no Spring DI available
- `@Component("name_v1_ComponentHandler")` — Spring discovery, supports constructor injection (used by guardrails, RAG, chat-memory, and agent utils when Spring beans are needed)

### Lazy Component Loading & Build-time Index
- `ComponentDefinitionRegistry` loads NO ServiceLoader components at Spring startup — all registry beans are `lazyInit`, and the registry constructor only captures suppliers
- server-app's `generateComponentIndex` Gradle task writes `META-INF/bytechef/component-index.json` at build time; the components-list view is served from index stubs (zero handlers loaded), single components load on demand via recorded provider class names
- **When an index is present it is authoritative**: a ServiceLoader component missing from the index is invisible (both in the list and per-name resolution). Apps that assemble their classpath differently (EE apps) must either run the generator or ship without an index — absent/corrupt index falls back transparently to full loading on first registry access
- The first deep read (`getComponentDefinitions()` consumers, first task execution) still triggers a one-time full load; stubs never reach detail/execution paths
- **A derived flag is false on a stub unless the index carries its source data.** A plain `ModifiableComponentDefinition` cannot implement a platform interface (it is an SDK class), so `clusterRoot` read false for every component in list views until the index recorded each component's `clusterElementTypes` and `ComponentIndex` started wrapping stubs in `StubClusterRootComponentDefinition`. Any new list-visible flag computed from a capability interface needs the same treatment — check the stub path, not just the loaded one

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
- **Never judge a Gradle run piped into `tail`/`grep`** — the pipeline exit code is the filter's, not Gradle's,
  and `${PIPESTATUS[0]}` is already clobbered if another command ran. Redirect to a file, check `$?` on its own
  line, then grep the file. Use `--continue` so one failing task doesn't hide the rest, and grep
  `^> Task .* FAILED` — not `error:`, which matches module paths like `:server:libs:core:error:`
- After a rebase, always run `./gradlew compileJava compileTestJava --continue` **and** `cd client && npm run check`.
  Git merges non-overlapping lines cleanly even when one side renamed a field or replaced `useState` setters with
  a reducer — a clean merge is not a correct one

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

### CLI
ByteChef includes a CLI (`cli/`, Spring Boot + Spring Shell) that scaffolds custom components and
calls the public REST API (automation and embedded). It uses the `application` plugin. Build a
`bytechef` binary with `installDist` — this is the normal way to use it, since the binary runs from
your current working directory so relative paths behave as expected:
```bash
# Build the distribution; binary lands at cli/cli-app/build/install/bytechef/bin/bytechef
./gradlew :cli:cli-app:installDist

# Scaffold a component from an OpenAPI spec
bytechef component init --name my-component --open-api-path ./openapi.yaml --output-path .

# Public API commands (store a profile first)
bytechef configure --host https://app.bytechef.io --token <public-api-token> --environment PRODUCTION --workspace-id 1
bytechef automation execution list --output table
bytechef embedded integration list --external-user-id user-42
```
For a quick dev invocation without building the distribution, use the `run` task — but note its
working directory is `cli/cli-app`, so pass **absolute** paths:
```bash
./gradlew :cli:cli-app:run --args="component init --name my-component --open-api-path /abs/openapi.yaml --output-path /abs/out"
```
See `cli/README.md` for the full command reference.

Regenerate the CLI's OpenAPI clients with each project's **`generateClient`** task — not the plugin's
default `openApiGenerate`, which is unconfigured and fails with "generator name must be specified".
Generated Java sources are committed and deliberately not wired to `compileJava`; regenerate manually
when an `openapi.yaml` changes. The surrounding `docs/`, `gradlew`, `pom.xml` scaffolding is gitignored.

**Upgrading openapi-generator is a two-file change.** `platform-configuration-rest-impl` vendors one
template, `openapi-templates/pojo.mustache`, to keep 7.24.0's unconditional `@JsonInclude(NON_NULL)`
off the REST models (there is no generator flag for it). The copy is pinned to 7.24.0's shape, so a
bump must re-diff it against the new generator's own `JavaSpring/pojo.mustache` and re-apply the two
deletions. The `verifyOpenApiPojoTemplate` task in that module fails the build on a stale copy and
prints the procedure; see `openapi-templates/README.md`.

### Resolving PR Review Comments
- Use `gh api graphql` with `resolveReviewThread` mutation to close threads programmatically
- Get thread IDs via: `gh api graphql -f query='{ repository(owner: "X", name: "Y") { pullRequest(number: N) { reviewThreads(first: 20) { nodes { id isResolved path } } } }'`

## Build and Deployment

### Fast IntelliJ Dev Startup (`fastStartup`)
- `server-app` startup from IntelliJ is slow because IntelliJ puts every module dependency on the
  classpath as an exploded output directory (hundreds of dirs → slow ServiceLoader/DevTools scans).
- Set `fastStartup=true` in `gradle.properties` (alias: the deprecated `useComponentJars`) to present
  **all** bytechef library + component modules to IntelliJ as pre-built JARs instead. Everything then
  loads in a single base classloader — fast startup, and no Spring DevTools base/restart classloader
  split (the split is what made the old component-only jar swap fail with `NoClassDefFoundError`).
- Mechanism (`server/apps/server-app/build.gradle.kts`): a resolvable `fastStartupRuntime` configuration
  forces the runtime-JAR variant and carries each module's **full transitive closure**, then feeds it
  back onto `implementation` as one `files(...)` entry. Plain `files(jarTask)` would drop transitives —
  that was the old breakage.
- Workflow: `fastStartup=true` → `./gradlew -PfastStartup=true :server:apps:server-app:buildModuleJars --parallel`
  → refresh Gradle in IntelliJ → run. Trade-off: DevTools still hot-reloads server-app's own code, but
  library modules (now JARs) don't hot-reload — edit a lib, re-run `buildModuleJars` + restart (or use
  IntelliJ HotSwap for method bodies). Zero-prebuild alternative: IntelliJ "Build and run using: Gradle"
  (bootRun's `runtimeClasspath` already yields JARs). `fastStartup=false` (default) keeps full library
  hot-reload and is byte-for-byte the previous behavior.

### Docker
- `Dockerfile` for server application
- `docker-compose.yml` for full stack
- `docker-compose.dev.infra.yml` for development infrastructure
- `docker-compose.dev.server.yml` for server-only development

### Kubernetes
- The Helm chart is in `kubernetes/helm/bytechef/` — one chart, deploying the single-node application
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
- **Regenerating takes two runs, and the first one crashes.** `JsonFileAssert` writes the snapshot to
  `src/test/resources/…` but reads it back off the classpath (`build/resources/test/…`). After deleting both,
  run once — the file is written to `src` and the test then throws `NullPointerException: url` on the
  still-missing classpath copy. Run again; `processTestResources` copies it across and it passes. That NPE is
  the expected midpoint, not a bug to debug
- **`anthropic`, `gemini` and `mistral` snapshots drift on their own.** Their model options are read off SDK
  enums (`AnthropicApi.Model`, `GoogleGenAiChatModel.ChatModel`, `MistralAiApi.ChatModel`), so an SDK bump
  stales them with no repo change. Treat the three as one class — regenerating only the one that failed leaves
  the others to fail later looking unrelated. `mistral` also filters SDK-deprecated constants, so a bump can
  silently remove a model from the picker
- Test both actions and triggers
- Verify connection configurations
- Test error handling and edge cases

### Test ObjectMapper Setup
- Use `@ExtendWith(ObjectMapperSetupExtension.class)` for tests that use `JsonUtils`, `MapUtils`, or `ConvertUtils` — do NOT manually call `setObjectMapper()` in test configurations

### Task Dispatcher Definition Snapshot Tests
- `DefinitionFactoryTest` classes use `JsonFileAssert` (snapshot pattern): if the JSON file is present it's compared; if missing it's written to `src/test/resources/` and the run then fails on the missing classpath copy — see the two-run note under Component Testing
- When task dispatcher definition models change (new fields), delete snapshot JSON files from BOTH `src/test/resources/definition/` and `build/resources/test/definition/`, then rerun tests

### EE Microservice Remote Client Pattern
- EE apps (`server/ee/apps/`) use remote client stubs instead of local service implementations
- When adding new SPI interfaces to platform modules, create corresponding `@Component @ConditionalOnEEVersion` stub classes in the relevant `*-remote-client` module (e.g., `automation-configuration-remote-client`)
- Stubs throw `UnsupportedOperationException` — they satisfy Spring DI; actual work is done via REST calls
- `@ConditionalOnEEVersion` requires `bytechef.edition=ee` in the app's config
- For lightweight EE apps (e.g., `runtime-job-app`) that can't pull in full remote client modules, use `@TestConfiguration` with mock/stub beans in the integration test
- `@Bean` declarations consumed across deployments (registries, SPI wiring) must live in the `*-api` module, not `*-service` — distributed EE apps (e.g., `configuration-app`) carry `*-api` + `*-remote-client` WITHOUT `*-service`, so a bean declared there fails their context at boot (bit once with `ResourceVisibilityPolicyRegistry`)
- Adding a constructor collaborator to a scanned `@Service` impl breaks OTHER modules' hand-assembled `@SpringBootTest(classes=...)` IntTest contexts with missing-bean errors — grep for `*IntTestConfiguration`/`@TestConfiguration` classes that assemble the impl and add mock `@Bean`s there

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
- **Never flush async store updates with a fixed sleep** (`await act(async () => { await new Promise(r => setTimeout(r, 10)); })`).
  A fixed delay races an async chain whose latency is unbounded under parallel vitest workers: it passes in
  isolation and fails intermittently in full runs, on a different test each time. Wait for the condition instead:
  `await waitFor(() => { expect(store.getState().someFlag).toBe(expected); }, {interval: 5, timeout: 5000})`.
  The timeout is a ceiling paid only on genuine failure, not a delay every test waits out.
- A test that kicks off an async store update **must wait for it to settle before it ends**, even when the
  assertion only covers intermediate state — otherwise its pending `setTimeout` callbacks fire during a later
  test and mutate the shared store there (`beforeEach` reset cannot help; the timer outlives the reset).

#### PostHog Mock
- Global mock in `.vitest/setup.ts` — `onFeatureFlags: vi.fn()`, `isFeatureEnabled: vi.fn().mockReturnValue(false)`
- `onFeatureFlags` returns `() => void` (unsubscribe); mock overrides must return a function: `return () => {}`
- `import('posthog-js')` dynamic imports resolve to the same mock; multiple synchronous calls share one Promise
- `useFeatureFlagsStore` resolves a flag through a four-hop chain: `import('posthog-js')` → promise microtask →
  `onFeatureFlags` callback → `setTimeout(…, 0)`. Tests must wait on the resulting store state (or on the
  `onFeatureFlags` subscription), never on elapsed time — see the Zustand note above.
- Test files are isolated (vitest `forks` pool, `isolate: true`), so `featureFlagsStore` and the PostHog mock
  **cannot** leak between files; contamination is always within a single file.

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

## Agent skills

### Issue tracker

Issues and PRDs are tracked on GitHub at `bytechefhq/bytechef` (pinned explicitly because this clone has multiple remotes). See `.agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles mapped to this repo's labels — `needs triage` and `wontfix` reuse existing labels; `needs-info` / `ready-for-agent` / `ready-for-human` are new. See `.agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `.agents/domain.md`.
