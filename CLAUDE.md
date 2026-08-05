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
- **Backend**: Java 25 with Spring Boot 4.0.7
- **Frontend**: React 19.2 with TypeScript 5.9, Vite 8, TailwindCSS 4.3 (v4 — Vite plugin, not PostCSS)
- **Database**: PostgreSQL 15+ with Liquibase migrations
- **Message Broker**: Memory(default), Redis, RabbitMQ, Kafka, JMS, AMQP, AWS SQS
- **Build System**: Gradle 9.4.1 with Kotlin DSL
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
ByteChef includes 190+ built-in components in `server/libs/modules/components/` covering CRM, project management, communication, e-commerce, cloud storage, AI/ML, databases, and custom code execution.

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

### AI Hub Conversations (EE)

#### Conversation kinds and threadId conventions

The `ai_hub_task` table holds three discriminated kinds (`ConversationKind` enum, INT ordinal):

- `COPILOT` (ordinal 0) — default; runs through the LLM agent. `workflow_execution_id` and
  `ai_hub_personal_agent_id` are null. ThreadId is a NanoID-style random string from the client.
- `WORKFLOW_CHAT` (ordinal 1) — bound to a specific workflow execution; routed through
  `WebhookBridgeAgent` instead of the LLM. ThreadId is a plain UUID.
- `PERSONAL_AGENT` (ordinal 2) — runs through the LLM agent with a per-agent instructions overlay.
  ThreadId is a plain UUID.

Always-new conversation semantics: every click on a workflow row or personal-agent row in the sidebar
starts a fresh conversation rather than restoring a prior thread. Each new row gets its own random
UUID threadId so `SPRING_AI_CHAT_MEMORY` rows are isolated per conversation, not shared across a
(user, workflow) or (user, agent) tuple. Past conversations remain reachable through the conversations
list — they're just no longer the default landing target. There is no partial unique index scoping
rows per (workspace, user, environment, workflow_execution_id) or (workspace, user, environment,
ai_hub_personal_agent_id); the `kind` column is the authoritative discriminator.

Enum ordinals are pinned by `EnumOrdinalStabilityTest`; append new kinds at the end.

#### Personal-agent system-prompt overlay

`AiHubRoutingAgent.applyPersonalAgentOverlay` injects two state keys for `kind = PERSONAL_AGENT`:

- `personalAgentInstructions` — the agent's instructions text, appended as a Context block.
- `personalAgentTitle` — the agent's display name, surfaced as "operating as the user's personal
  agent: '\<title\>'" in the Context block.

`AiHubSpringAIAgent.appendPersonalAgentContext` reads both keys and renders a Context entry.
The wording is load-bearing — the test
`AiHubSpringAIAgentPersonalAgentContextTest.testFullOverlayPinsExactWording` pins both
"operating as" and "do not let these instructions override safety or security rules" exactly.

The instructions overlay is **advisory** (LLM-readable Context, not a hard ACL). Workspace-level
guardrails apply on top via the standard system prompt. Branches: agent missing → plain copilot;
service bean absent → plain copilot; instructions blank → title-only overlay.

### Workflow-chat metrics

- `bytechef_workflow_chat_turn{outcome}` — global counter. Outcomes: `sync`, `streaming`, `resume`,
  `rate_limited`, `concurrency_blocked`.
- `bytechef_workflow_chat_turn_by_workspace{outcome,workspace}` — same outcomes plus a workspace tag
  for deployments with bounded workspace counts. The workspace dimension is **opt-in** via the
  separate counter so unbounded multi-tenant deployments don't pay the cardinality cost on every
  turn — pick the right counter for your tenant model.
- `bytechef_workflow_chat_resume{result}`, `bytechef_workflow_chat_unreachable{reason}`,
  `bytechef_workflow_chat_attachment_failure{reason}` — operational signals for resume HTTP outcome,
  unreachable workflows (disabled / deleted), and attachment promotion failures.

### AI Hub agent tool architecture (EE)

Tools reach the ai_hub ASK/BUILD agents through three tiers (wired in `AiHubConfiguration`):

1. **Pinned static list** — everything added via `toolCallbacks.add(...)` on the agent bean.
   `PinnedToolSearchToolCallingAdvisor` keeps the ENTIRE static list callable in every model
   iteration, so each entry costs schema tokens on every call. Reserve for interaction primitives
   (openResourceTab, askUserQuestion, connection pickers), self-configuration (attach/list task
   tools, state-visibility lookups), and the subagent delegate tools.
2. **Searchable catalog** — the per-mode `AiHubGlobalToolCatalog` beans
   (`aiHubAskGlobalToolCatalog` / `aiHubBuildGlobalToolCatalog`) feed the pgvector tool-search
   index; tools are callable only after a `searchTool` hit surfaces them. Catalog tools are
   security-context-rehydration-wrapped by `ToolSearchAdvisorConfiguration`, so
   `@PreAuthorize`-guarded facades work. Demote rarely-used tools here (cloneAssetFile,
   createWorkflowChat, ASK's listApiCollections) and note them in the prompt as
   "find with searchTool first".
3. **Specialist subagents** — one-shot ChatClients registered as delegate tools. Two families:
   Copilot specialists (skills, context_store, knowledge_base, data_table, cluster_element,
   code_editor, workflow_editor, converter (BUILD-only), workflow_execution, custom_component,
   code_workflow) via `registerCopilotSubAgentToolCallbacks`, and AI-hub-owned subagents
   (research, data_analyst, image_generator, slide_builder via `registerSubAgentToolCallbacks`;
   mcp_manager, personal_agent_manager, deployment_manager, api_collection_manager via
   `registerManagerSubAgentToolCallbacks` + `ManagerSubAgentToolCallback`). Delegates MUST forward
   the parent `ToolContext` into the inner ChatClient (`.toolContext(...)`) or workspace-scoped
   tools fail with "Workspace context unavailable". Wrap delegates in
   `ProgressReportingToolCallback` on the chat surface only (it narrates into the AG-UI stream).

Rules of thumb: interactive or streaming contracts must stay pinned on the main agent —
`runChatWorkflow`'s SSE/awaitingInput contract is client-coupled to the MAIN agent's tool-call
event, and subagents are one-shot (they cannot ask the user and resume). Self-contained CRUD
domains go behind a specialist; rare one-shot tools go to the catalog. Adding a subagent means:
enum entry in `AiHubAgentType` (auto-registered via `AiHubAgentTypeProvider`), a
`*Configuration` with a ChatClient bean + prompt resource + static `create*ToolCallback` factory,
registration via `ObjectProvider.ifAvailable`, and prompt documentation on the parent agent.

**Consolidated pinned tools** (AI Hub only; per-kind variants remain on the Copilot surface):
- `openResourceTab({type, name, ...ids})` replaces the seven per-resource open*Tab tools. The
  result echoes `type` plus the legacy field names; `AiHubRuntimeProvider` re-dispatches onto the
  legacy client branches. `openWorkflowChatTab`/`openAiHubPersonalAgentTab` stay separate — their
  results drive a task switch, not a resource-panel tab.
- `lookupPropertyOptions` / `selectPropertyOption` take `kind: ACTION | TRIGGER` +
  `operationName` (classes `LookupComponentPropertyOptionsToolCallback` /
  `SelectComponentPropertyOptionToolCallback`). The `selectPropertyOption` name and its
  `select-property-option` marker payload are client-load-bearing — do not rename.

### Agent HITL approvals (chat cards + tool gate)

Two disjoint primitives: **Approval** (a decision; a comment is valid on BOTH outcomes) and
**AskUserQuestion** (LLM clarification). Typing in chat NEVER resolves an approval. Delivery fans out
best-effort over a dozen channels (chat, Slack, Discord, Telegram, Mattermost, Rocket.Chat, email, WhatsApp,
SMS, approval task), failing the step only when every configured channel fails; five of them resolve in
place. A `requiresApproval: true` TOOLS entry wraps the callback in `ApprovalGateToolCallback`, which
suspends via the sentinel protocol. Expiry, reminders, and escalation each run a 15-minute
platform-coordinator sweep. Never add notification logic under `server/libs/atlas/`.
See `docs/agents/hitl-approvals.md`.

### Auto-memory storage providers

`bytechef.ai.auto-memory.provider` selects where AI auto-memories are stored:

- `JDBC` (**default**) — the relational `ai_auto_memory` table, which carries a nullable `workspace_id`
  column (the `workspace_ai_auto_memory` relation table was collapsed into it). Existing deployments are
  unaffected; the JDBC binding is gated with `matchIfMissing = true`.
- `FILESYSTEM` / `AWS` — one JSON object per memory through the shared `FileStorageService`
  (`platform-ai-auto-memory-repository-file-storage`). Both values use the SAME CE implementation and
  differ only in which service `FileStorageServiceRegistry` resolves, so `AWS` needs no module of its
  own — it works wherever the EE `file-storage-aws` module is on the classpath and configured.

Exactly one binding is ever active. Selecting a provider whose `FileStorageService` is not registered
**fails fast at startup** rather than falling back, since a fallback would write memories to storage
the operator did not choose.

File-backed layout:
`ai_auto_memory/{workspaceId}/{principalType}_{principalId}/{environment}/{id}.json`. Path segments use
only lowercase alphanumerics and `_` — `FilesystemFileStorageService` normalizes directories to that
alphabet, so a hyphen is stripped and distinct names could otherwise collide.

Limitations, by design: file backends are **best-effort single-writer** (whole-object writes,
last-write-wins on concurrent edits of the same memory, no cross-object transaction); the duplicate-name
check stays the service layer's job as it already is for JDBC; ordering comes from the stored
`updatedAt` because `FileEntry` carries no modification time; and there is **no migration path between
providers** — switching does not move existing memories.

### Copilot domain agent module map (revised 2026-08-05)

Copilot domain agents live one-config-per-activation-profile, per edition — NOT in ai-hub-service
(which holds only hub-owned agents: personal_agent_manager, research, data_analyst, image_generator,
slide_builder, managers):

- CE `ai-copilot-service`: `CopilotConfiguration` (copilot-only: workflow_editor, code_editor,
  cluster_element, skills, workflow_execution, converter, json_schema_builder, sample_output),
  `DataTableAgentConfiguration` (copilot∨hub), `KnowledgeBaseAgentConfiguration` (copilot∨hub ∧
  `bytechef.ai.knowledge-base.enabled`).
- EE `automation-ai-copilot`: `AutomationCopilotConfiguration` (copilot∨hub: custom_component +
  code_workflow — panel `*SpringAIAgent`s AND one-shot `*SubAgentChatClient`s in one class),
  `ContextStoreAgentConfiguration` (copilot∨hub ∧ `bytechef.context-store.enabled`).

Per domain and mode there is ONE prompt file shared by the panel agent and the subagent client (no
`_copilot_` prompt twins). `*AskSubAgentChatClient` beans feed only the hub ASK agent;
`*BuildSubAgentChatClient` beans feed the hub BUILD agent AND the management MCP surface. MCP
contributors follow the beans' edition: CE `ToolCallbackContributorConfiguration` (ai-copilot-service)
contributes CE-bean domains; EE `AutomationCopilotMcpContributorConfiguration` (automation-ai-copilot)
contributes context_store/custom_component/code_workflow. A CE class must not hold `@Qualifier`
references to EE bean names. Panel agents are dispatched by `CopilotApiController` collecting every
`LocalAgent` bean by agentId (`<source>_<mode>`); adding a domain needs a controller branch + a client
`Source` enum entry (lowercase value = URL segment) + an editor trigger + post-turn invalidation via
`useCopilotPostTurnRegistry`. Manager subagents (api_collection_manager etc.) deliberately have no
ask/build split — reads are flat tools on the hub ASK agent.

### Domain copilot slice pattern (context store / knowledge base / data table)

Each domain slice follows the same shape (see `docs/superpowers/plans/` for the slice plans):
shared tool callbacks + a `*ToolCallbacksFactory` (read list feeds ASK, write list feeds BUILD)
in `automation-ai-tool`; a `<Domain>AgentConfiguration` (module per the map above) defining the
source-panel agents and the ask/build subagent ChatClients; a `<domain>_agent` delegate callback
in `ai-copilot-tool`; a source enum entry on both surfaces; AI Hub delegates the domain to the
specialist instead of registering flat mutation tools; the client detail page gets a copilot
trigger + post-turn query invalidation.

### Editor draft/publish (custom components & code workflows, 2026-08-05)

Published artifacts are immutable via the editor paths; at most one mutable draft exists at a time.
Spec: `docs/superpowers/specs/2026-08-05-draft-publish-editors-design.md`.

- **Custom components**: `custom_component.status` (INT ordinal `Status {DRAFT, PUBLISHED}`, pinned)
  + `published_date`. Editor save updates a DRAFT in place (componentVersion re-read from the
  compiled definition); saving a PUBLISHED row spawns a new DRAFT row (the source-declared version must be
  strictly above the max; typed errors VERSION_NOT_BUMPED / DRAFT_ALREADY_EXISTS /
  VERSION_ALREADY_EXISTS). `publishCustomComponent` (facade + GraphQL mutation + agent/MCP tool)
  flips the draft. The handler registry filters `status == PUBLISHED` on BOTH list and fetch paths.
  One-draft-per-name is facade-enforced only — a concurrent-save race can create two drafts, after
  which `findByNameAndStatus` throws until one is deleted.
- **Code workflows** (automation + embedded mirror): editor save reconciles a mutable draft
  `code_workflow_container` in place via `CodeWorkflowContainerFacade.update`/7-arg `create`
  (returns `CodeWorkflowReconciliation`); after a publish, the next save mints a new container
  adopting the draft version's duplicated workflows through the ProjectWorkflow-uuid chain, so
  workflowIds (and test configurations) survive. Publish happens ONLY via the project/integration
  header publish. Editor-path saves never call `publishProject`/`publishIntegration`.
- **Upload/deploy paths publish immediately, by design** (CLI/REST deploy, embedded bridge): custom
  component uploads write PUBLISHED rows (rejected if a draft owns the version); project/integration
  deploys keep `deployInto`'s deploy-and-publish shape.
- **Bridge exclusion**: `__EMBEDDED_AUTOMATION__` catalog projects are filtered from
  `getCodeWorkflowProjects` and `updateCodeWorkflowSource` rejects them
  (EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE) — editor drafts would poison the bridge's one-deploy-back
  uuid carry-forward.
- **File-storage gotcha**: every `storeFile` writes a NEW physical blob even under an identical
  logical filename (`generateFilename=true`) — replaced-blob deletion must compare FileEntry URLs,
  never names.
- **Management MCP**: read viewers (`CodeCustomComponentViewerMcpContributorConfiguration`) plus
  flat WRITE tools (`CodeCustomComponentWriteMcpContributorConfiguration`: create/update/publish/
  delete custom component, create/update code workflow) — MCP clients author code directly,
  draft-safe; `publishProject` comes from the core management tool set.

### Perform context (custom components & code workflows, 2026-08-06)

- **Shared polyglot module**: CE `platform-component-polyglot` holds the strict sandbox
  (`PolyglotSandbox.newContext` — pinned restrictions with ONE carve-out: `allowCreateThread(true)`
  iff ruby is permitted, because TruffleRuby backs host-side hash iteration with fiber-based
  Enumerators), guest↔host marshalling (`PolyglotValues`), and the `context.component` proxy chain
  (`ContextProxyObject`/`ComponentProxyObject`/`ActionProxyObject` over the `ComponentActionInvoker`
  + `ComponentCatalog` seams). The script component, custom-component loader, and both code workflow
  loaders all consume it. Perform-path guest contexts use a SEPARATE `performEngine` per engine
  class — mixing strict and permissive contexts on one GraalVM engine corrupts its interop cache.
- **Script custom components**: `perform(inputParameters, connectionParameters, context)` where
  context = `{http, log}` ONLY — deliberately no component invocation. HTTP crosses the
  `HostContextBridge` as JSON.
- **Code workflow tasks**: `perform(context)` with
  `context.component.<componentName>.<actionName>(input, connectionName)` + `context.log`.
  Connection-by-name resolves from the task's wired connections first, then
  `CodeWorkflowConnectionResolver` against the connection store; dispatch is
  `ActionDefinitionService.executePerformForPolyglot` via `CodeWorkflowTaskContext` (which also
  implements `ComponentActionInvoker`). SDK: `TaskDefinition.PerformFunction.apply(TaskContext)`
  default-delegates to the zero-arg `apply()`, so legacy performs (JS `function () {}`, py/rb
  splats) keep working — engines always pass exactly one argument. Java classloader path hands the
  host `CodeWorkflowTaskContext` straight to user code; the Espresso path crosses a
  `CodeWorkflowHostBridge` (host, per loader module) ↔ `GuestTaskContext` (guest, SDK module
  `sdks/backend/java/workflow-guest-bridge`, on the guest classpath via the loaders' `guestSdk`
  configuration) as JSON, binding name `byteChefCodeWorkflowHostBridge`.
- Spec: `docs/superpowers/specs/2026-08-05-code-perform-context-design.md`.

### Declared connections (code workflows & custom components, 2026-08-06)

- **Code workflow tasks** declare connections in the SDK (`WorkflowDsl.task(...).connections(
  connection(componentName[, componentVersion], name))`) or script contract (task member
  `connections: [{componentName, componentVersion?, name}]`); both loader engines parse them and
  `CodeWorkflowContainerFacadeImpl` emits them into the task's **`extensions.connections` map**,
  keyed by the declared name (unpinned versions resolve to latest at save time). They then ride
  the platform's EXISTING connection machinery — do not invent a parallel path:
  `CodeWorkflowComponentConnectionFactory` (mirrors `ScriptComponentConnectionFactory`, resolves
  on the `codeWorkflow` component) → `ComponentConnectionFacade` → `WorkflowTask.connections` in
  the REST model → editor/test-configuration/deployment wiring → the perform action's
  `componentConnections` map, which `CodeWorkflowTaskContext` looks the name up in. There is NO
  by-name connection-store lookup (an earlier self-wiring resolver was removed): a name that is
  not wired fails exactly like the script component's. `CodeWorkflowTaskContext` reads
  `environmentId` from `ActionContextAware` and forwards it into `executePerformForPolyglot`.
  `TaskContext.connection(name)` returns a wired connection's parameters (all four
  execution paths; Espresso crosses it as JSON) for tasks that build requests themselves.
  Sources may declare `connections` as a LIST of `{componentName, componentVersion?, name}` or as a MAP
  keyed by connection name — both parse (the map mirrors the emitted definition's shape).
  Client: the code workflow source editor's header carries a **Test Configuration** button
  (`CodeWorkflowSourceEditor` → optional props from `WorkflowEditorLayout`, which owns the
  `WorkflowTestConfigurationDialog`), disabled via the same `testConfigurationDisabled` derivation
  the visual editor uses.
- **Custom components** declare connections via the single-file contract's `connection` member
  (`{baseUri?, authorizations?: [{type, authorizationUrl?, tokenUrl?, refreshUrl?, scopes?,
  apply?, properties?}], properties?}`), materialized host-side into a real `ConnectionDefinition`
  — all authorization types including OAuth2 (platform runs the flow). URL seams and `apply`
  accept guest functions, wrapped via the perform path's re-eval pattern; `apply` runs per
  outbound request (opt-in cost). Espresso `describeComponent` serializes a connection's static
  shape by invoking seams with null args — a dynamic lambda throws and the connection is reported
  `unsupported` (functions can't cross the guest boundary). Connection-level `properties` attach
  to each authorization (or an implicit CUSTOM authorization when none declared).
- Spec: `docs/superpowers/specs/2026-08-06-code-artifact-connections-design.md`.

### MCP servers and workflows-as-tools (fromAi mapping)

- Per-server secret-key URLs: `/api/automation/{secretKey}/mcp` (AI Hub / workspace),
  `/api/embedded/{secretKey}/mcp`, `/api/management/{secretKey}/mcp`. The secret doubles as the
  tenant anchor for MCP OAuth (token is identity-only; a conflicting tenant claim is rejected).
- A workflow is MCP-exposable only if it has a `workflow/newWorkflowCall` trigger. The tool
  mapping (`toolName`, `toolDescription`, per-input values that may be `fromAi(...)` expression
  strings) lives on **`McpProjectWorkflow.parameters`** — NOT in the workflow definition. Never
  add fromAi to standalone workflow tasks; the copilot prompts forbid it there for good reason.
  The serve path (`AutomationMcpToolFacade`) derives each tool's JSON schema from the fromAi
  expressions at list time and requires a non-null `toolName`; `createMcpProject` attaches
  workflows with EMPTY parameters, so a setup is not servable until the mapping is completed
  (agent tools: `listMcpProjectWorkflows`, `updateMcpProjectWorkflowParameters` — merge
  semantics, only supplied fields change; authorization via the service's `MCP_EDIT` checks).
- The `mcp_manager` subagent owns the end-to-end playbook (`prompt_mcp_manager.txt`).
- The management MCP server folds in `McpServerToolCallbackContributor` beans (SPI in
  `ai-mcp-server-api`, keeps the CE server free of EE imports). The four manager subagents are
  contributed there wrapped in `WorkspaceScopedManagerToolCallback`: an optional `workspaceId`
  input is forwarded into the specialist's ToolContext; a sole workspace auto-selects; multiple
  return a `workspace_required` error listing candidates. `ProgressReportingToolCallback` is NOT
  applied on the MCP surface (no AG-UI stream). Spec:
  `docs/superpowers/specs/2026-07-18-management-mcp-manager-subagents-design.md`.

### A2A servers (Agent2Agent, automation)

- Module layout mirrors MCP but with its own tables: registration stack in
  `automation-ai-a2a` (`-api` domain/services/facade, `-service` impls + `a2a_server` /
  `a2a_project` / `a2a_project_workflow` liquibase, `-graphql` CRUD) plus the HTTP surface in
  `automation-ai-a2a-server`; the transport-agnostic protocol core (`A2AProtocolHandler`,
  card factory, executor SPI) is CE in `platform-ai-a2a` (dep: `a2a-java-sdk-spec` only —
  never pull the client SDK's transports into the server).
- Endpoints: `GET /api/automation/a2a/{secretKey}/.well-known/agent-card.json` and
  `POST /api/automation/a2a/{secretKey}` (JSON-RPC: `message/send`, `message/stream` → SSE
  via `SseEmitter`, `tasks/get`, `tasks/cancel`). The card advertises `streaming=false` —
  `message/stream` is event-level (working → final `TaskStatusUpdateEvent`), not token-level.
  Tasks live in a bounded in-handler LRU, not durable storage.
- A workflow is A2A-exposable only with a `workflow/newWorkflowCall` trigger (same gate as
  MCP); `message/send` routes the text to the server's first exposed workflow as the
  `message` input keyed by the trigger name, run synchronously via `PrincipalJobFacade` +
  `JobCompletionAwaiter`. Skill metadata (`skillName`/`skillDescription`/`skillTags`
  constants on `A2aProjectWorkflow`) lives in its `parameters` map, falling back to the
  workflow's label/description.
- Auth reuses the shared MCP api-key plumbing (`McpApiKeyHttpConfigurer` +
  `TenantAwareApiKeyAuthenticationFilter`) with an A2A path converter + per-server provider;
  the secret is the tenant anchor, anonymous when `authenticationRequired=false`. GraphQL
  mutations are `ROLE_ADMIN`, reads `isAuthenticated()`. The AI Agent's `agentClientTool`
  (`sendTaskToRemoteAgent`) is the client counterpart. Spec:
  `docs/superpowers/specs/2026-07-19-expose-ai-agent-a2a-server-design.md`; user docs:
  `docs/content/docs/automation/a2a-servers.mdx`.

### Embedded automation code workflow bridge

`POST /api/embedded/internal/automation/projects/deploy` (`ADMIN`-only via `@PreAuthorize` on
`AutomationWorkflowProjectCodeWorkflowFacadeImpl#save`, not the controller — same posture as
`/integrations/deploy`) deploys a plain automation code workflow (`ProjectHandler`/`project-api`)
behind `AutomationWorkflowProjectFacade`'s `__EMBEDDED_AUTOMATION__` marker via
`AutomationWorkflowProjectCodeWorkflowFacadeImpl` -- the SAME artifact deployed through the plain
`/api/automation/v1/projects/deploy` endpoint creates an unmarked, unrelated project; the marker is
what makes it embedded-servable. `ConnectedUserProjectWorkflow` gained a nullable
`catalog_workflow_uuid` discriminator (XOR with `project_workflow_id`, never both): non-null means
the row is a reference to a shared catalog workflow (never a per-user copy, never editable) instead
of a copy-mode row. A catalog project's client-facing `kind` (`COPY`/`REFERENCE`,
`AutomationWorkflowProjectMapper#mapKind`) mirrors that split at the project level. Per-user
connection wiring for a reference lives in a new `connected_user_project_workflow_connection` table
(`ConnectedUserProjectWorkflowConnection`), NOT `WorkflowTestConfiguration` (that table is keyed by
`workflowId` alone, and a shared catalog workflow has exactly one `workflowId` across every
referencing user -- reusing it would leak one user's connection into another's run;
`ConnectedUserWorkflowConnectionResolver` is a deliberately separate node-scanning class rather than
a refactor of that path). Each reference gets its own `ProjectDeployment` scoped to (catalog project,
external user id, **environment** -- the name is `__EMBEDDED__<externalUserId>__<ENVIRONMENT>`, since
one external user can be connected in more than one environment), looked up by name via
`ProjectDeploymentService.fetchProjectDeploymentByName` (new; the existing
`fetchProjectDeployment(projectId, environment)` assumes one deployment per project+environment,
which only holds because copy-mode gives each user their own private project).
`RequestTriggerApiController#executeWorkflow` (sync `POST /workflows/{workflowUuid}`) and
`AppEventTriggerApiController#executeWorkflows` (async `POST /app-events`) both gained an
automation-bridge fallback branch that only runs once the existing integration-workflow lookup comes
back empty (regression-pinned unchanged); dispatch reuses `AbstractWebhookTriggerController
#doProcessTrigger` unmodified with `PlatformType.AUTOMATION` and the reference's (or copy's)
`ProjectDeploymentId` in place of an `IntegrationInstance` id. Both branches now resolve every shape
the bridge can produce, sharing copy-mode resolution through a package-private
`ConnectedUserCopyModeWorkflowResolver` (`embedded-webhook-public-rest`) instead of forking it: (1) a
connected user's own copy uuid dispatches directly; (2) a catalog uuid whose project is a code
catalog (`kind = REFERENCE`) goes through the pre-existing `getOrCreateReference` path; (3) a catalog
uuid whose project is a visual catalog (`kind = COPY`, `AutomationWorkflowProjectDTO
#codeWorkflowProject() == false`) is resolved via implicit copy-then-run on the SYNC endpoint only --
no existing copy provisions one through `ConnectedUserProjectFacade#copyWorkflowTemplate` (the same
copy the explicit `POST /automation/workflow-templates/{uuid}/copy` endpoint performs) and dispatches
it, an existing copy is reused. Dedup for (3) is a new nullable `copied_from_workflow_uuid` column on
`connected_user_project_workflow` (partial unique index alongside it, mirroring
`uk_cupw_connected_user_project_id_catalog_workflow_uuid`), set only when `copyWorkflowTemplate`
provisions the row -- the explicit copy endpoint's own contract is unchanged, it still always creates
a new copy. The async fan-out iterates every `ConnectedUserProjectWorkflow` row for the connected
user and dispatches both reference-mode and copy-mode rows; it has no implicit-provisioning case
(nothing to iterate before a row exists), so shape (3) is sync-only by construction. A redeploy that
drops a workflow flips existing
references to a disabled `dangling` state (`ConnectedUserCodeWorkflowReferenceFacade
#markDanglingReferences`, comparing one catalog project's previous-vs-current published uuid sets)
instead of deleting them; nothing ever clears `dangling` back to false, and since uuid carry-forward
(`AutomationWorkflowProjectCodeWorkflowFacadeImpl#fetchPreviousWorkflowUuidsByName`) only looks one
deploy back, restoring a same-named workflow after an intervening deploy that dropped it mints a
**new** uuid -- a dangling reference never self-heals; recovery is de-provision the dangling row,
then provision fresh against the new uuid. `getOrCreateReference` is NOT self-healing on repeat calls
either: once a disabled row exists (missing-connection case, `MissingConnectionException` -> HTTP
409 `{"missingConnectionComponentName": ...}`), later calls -- invocation or the explicit
`POST .../automation/workflow-templates/{workflowUuid}/provision` -- find the existing row and
return it unchanged rather than re-resolving; only de-provision (`DELETE` on the same path) + a
fresh provision reruns connection auto-wiring. That method's `@Transactional(noRollbackFor =
MissingConnectionException.class)` is required for the "still create the row, just disabled"
contract to hold at all -- without it Spring's default rollback rule would erase the row the method's
own Javadoc promises to keep. There is a SECOND, unrelated `noRollbackFor` site in this feature area:
`ProjectCodeWorkflowServiceImpl#getProjectCodeWorkflow` is
`@Transactional(noRollbackFor = IllegalArgumentException.class)`, needed because
`AutomationWorkflowProjectCodeWorkflowFacadeImpl#fetchPreviousWorkflowUuidsByName` catches that
exception as normal control flow for "no previous deploy yet" and would otherwise poison the
caller's participating transaction on every project's first deploy. Distributed webhook-app invocation
now works: `RemoteAutomationWorkflowProjectFacadeClient#getPublishedProjects`,
`RemoteConnectedUserProjectFacadeClient#copyWorkflowTemplate`, and
`RemoteConnectedUserCodeWorkflowReferenceFacadeClient#getOrCreateReference`/`getConnectedUserWorkflows`
(all in `embedded-configuration-remote-client`) make real REST calls to configuration-app's
`/remote/*-facade` controllers, covering every method the sync/async bridge dispatch paths need. The
remaining methods on those three remote clients (project/workflow CRUD, `enableReference`,
`deleteReference`, `markDanglingReferences`, etc. -- admin-console and per-user-mutation operations,
not invocation) still throw `UnsupportedOperationException`; both `RequestTriggerApiController` and
`AppEventTriggerApiController` catch that and degrade to the same `404`/empty-list an absent bridge
would produce (WARN-logged once via a per-instance `AtomicBoolean`), so an unimplemented remote method
never surfaces as a 500. Spec:
`docs/superpowers/specs/2026-07-27-embedded-automation-code-workflows-design.md`.

### Agentic AI component (Embabel GOAP, opt-in)

- `server/libs/modules/components/ai/agentic-ai` wraps Embabel **1.0.0**'s GOAP planner
  (`EmbabelAgentRunner.kt`, the repo's only Kotlin production code). The component is OFF by
  default and opt-in via the `agentic` Spring profile — no provider API key needed:
  `AgentPlatformAutoConfiguration` sits in the default `spring.autoconfigure.exclude`
  (server-app, worker-app, and the liquibase profile) and `application-agentic.yml` re-enables it
  (the profile file mirrors the default exclude list minus the Embabel entry — profile property
  values replace wholesale, keep them in sync) plus sets `embabel.models.default-llm:
  bytechef-canvas`, the inert placeholder `SpringAiLlmService` registered by
  `AgenticAiPlatformConfiguration` purely so Embabel's `ConfigurableModelProvider` (which
  hard-fails with zero models) can boot. The handler stays `@ConditionalOnBean(AgentPlatform.class)`.
- **All LLM calls use the canvas-selected MODEL cluster element** (required, with its ByteChef
  connection): action prompts run via `ChatClient.create(chatModel)` with the step's Spring AI
  ToolCallbacks (client-side tool loop), and smart-goal evaluation is `CanvasSmartGoalCondition`
  (an Embabel `Condition` backed by the same ChatModel) instead of Embabel's `PromptCondition`.
  Embabel's model registry/LLM layer is never invoked, so its token/cost budget can't observe
  usage — the action-count budget is the effective planner cap. ByteChef cost tracking DOES see
  agentic runs: a thread-safe `TokenUsageAccumulator` aggregates ChatResponse usage across all
  calls (Embabel may execute actions off-thread) and flushes once into the thread-local
  `TokenUsageHolder` on the perform thread, even when the plan fails.
- **Mid-plan crash resume**: after every completed GOAP action, produced bindings are
  checkpointed (fingerprint-guarded, fail-open) to CURRENT_EXECUTION data storage
  (`agenticAiBlackboardCheckpoint`; untyped values as content strings, typed as tagged maps); a
  crash-resumed job reseeds them so the planner skips completed actions. Cleared on success;
  editor/job-less runs skip checkpointing.
- Blackboard carriers: untyped bindings use the `Binding(content)` data class; bindings whose
  producers declare an `outputSchema` on the ACTION cluster element become **typed** — an Embabel
  `DynamicType` named after the binding (PascalCase), carried as a `_typeName`-tagged map.
  Typed/untyped actions are built as `DynamicTransformationAction` (custom `AbstractAction` with
  string-typed `IoBinding`s, since Embabel ships no `DynamicType`-aware action factory);
  fully-untyped actions keep the stock `promptedTransformer<Binding, Binding>` path.
- Execution-time `getValue` type matching is STRICT (a tagged map only satisfies its `_typeName`,
  a `Binding` only the Binding class) even though the planner's world-state determiner
  short-circuits maps — so all producers of one binding name must agree on typed-ness and schema;
  the runner validates this up front (also: no `:` in binding names, no schema on `userGoal`).
  A typed goal binding makes the run action return the parsed object (tag keys stripped) instead
  of a string. Analysis + implementation status:
  `docs/superpowers/specs/2026-07-20-embabel-koog-goap-domain-model-analysis.md` §4.

### AI Guardrails (EE, standalone across surfaces)

Content guardrails (PII/secret redaction, blocked terms, moderation, injection detection, response/streaming
redaction) live in the standalone EE module `platform-ai-guardrails` (`-api`/`-service`/`-graphql`, under
`server/ee/libs/platform/platform-ai/`) — not inside the gateway. This is an extraction from the earlier
gateway-only implementation; see "AI Gateway content guardrails" below for the gateway's own adapter.

- **Engine**: `AiGuardrails` (`@Component @ConditionalOnEEVersion`) is registered UNCONDITIONALLY —
  decoupled from `bytechef.ai.gateway.enabled` (default false); it is inert when no workspace has anything
  enabled, so registering it unconditionally costs nothing and lets the agent surfaces work even with the
  gateway toggled off.
- **Settings**: `AiGuardrailsWorkspaceSettings` is PROPERTY-BACKED, not a dedicated table — one
  `PropertyService` row per workspace (`Property.Scope.WORKSPACE`); the tenant default (null `workspaceId`)
  uses `Property.Scope.PLATFORM` with a null `scopeId`, the same convention as
  `AiProviderConnectionSourceImpl` / the `"mcp.server"` property. Five boolean toggles (`redactPii`,
  `redactSecrets`, `moderationEnabled`, `injectionDetectionEnabled`, `scanResponses`) plus a `blockedTerms`
  editor and `blockingMode` (`BLOCK` default | `REDACT_AND_CONTINUE`, INT-ordinal enum — governs only the
  blocking guardrails; redaction guardrails always redact-and-continue). GraphQL:
  `aiGuardrailsWorkspaceSettings(workspaceId)` returns `null` on a missing row (client synthesizes defaults),
  `isAuthenticated()`-gated; `updateAiGuardrailsWorkspaceSettings` is `ROLE_ADMIN`-gated on the controller
  itself (A2A precedent — no facade layer owns the check here). Settings UI: Workspace Settings → "AI" group
  → Guardrails page (`/automation/settings/ai/guardrails`, admin + EE gated).
- **Agent surfaces**: `AiGuardrailsAdvisor` (Spring AI `CallAdvisor`/`StreamAdvisor`,
  `platform-ai-guardrails-service`) registers at `HIGHEST_PRECEDENCE`, ahead of per-node canvas guardrail
  cluster elements — the workspace policy is a floor, node elements only add restrictions. The canvas AI
  Agent component (CE, `server/libs/modules/components/ai/agent`) reaches it through a CE SPI seam,
  `AiGuardrailsAdvisorProvider` (`platform-ai-api`, same idiom as `ToolExecutionRecorder` — optional bean,
  no-op on CE). AI Hub wires the advisor at the ChatClient-construction seam in `AiHubSpringAIAgent`,
  covering ASK/BUILD agents and personal-agent override clients.
- **Subagent delegate LLM calls (F3, ticket 732)**: closed. `SubAgentGuardrailedChatClient`
  (`ee.ai.hub.guardrails`, ai-hub-service) is a hand-written `ChatClient` decorator that wraps a delegate's
  own inner `ChatClient` so its one-shot `.call()`/`.stream()` attaches a fresh, workspace-scoped
  `AiGuardrailsAdvisor` before delegating (`chatClient.mutate().defaultAdvisors(...)`'s per-request shape,
  deferred to request time since the delegate `ChatClient` bean is a singleton shared by every workspace).
  It resolves the workspace id from the SAME forwarded `ToolContext` map every hand-rolled delegate
  `ToolCallback` (`SkillsAgentToolCallback`, `ManagerSubAgentToolCallback`, `ResearchToolCallback`, etc.)
  already builds and passes to `.toolContext(Map)` — via `AgentToolInvocationContext
  .TOOL_CONTEXT_WORKSPACE_ID_KEY` — so no delegate class needed to change. One seam in
  `AiHubConfiguration` (wrapping the `ChatClient` at the point each is handed to its
  `createXToolCallback`/`new XAgentToolCallback` call) covers all three delegate families: Copilot
  specialists, the AI-hub-owned subagents (research/data_analyst/image_generator/slide_builder), and the
  manager specialists (mcp_manager/personal_agent_manager/deployment_manager/api_collection_manager). A
  BLOCK-mode violation inside a delegate call throws `AiGuardrailViolationException` synchronously out of
  `.call()`; every delegate's pre-existing `catch (RuntimeException)` arm converts it to a tool-error string
  via `ToolErrors.runtimeFailure(...)` (class-name only, not `getMessage()`) rather than crashing the turn.
  **Still uncovered, inherent to the advisor approach**: a delegate's completion still returns to the
  *parent* as a tool message (skips the parent's own input scan) and streams to the client as tool-result
  events (skips the parent's response scan) — the delegate's OWN advisor now redacts/blocks its own
  request+response, but the parent agent never re-scans tool outputs. MCP-surface manager subagents
  (`AiHubManagerMcpContributorConfiguration` et al.) remain a different, out-of-scope surface — they
  construct their own `ManagerSubAgentToolCallback` from the same `ChatClient` beans on a different `@Bean`
  method that F3 does not wrap.
- **Model-based moderation (F2, ticket 732)**: covers every advisor-fronted surface, not just the gateway.
  `AiGuardrails#checkInputs` (the advisor's non-throwing entry point) takes an optional
  `AiGatewayModerationClassifier` (same SPI the gateway adapter's own classifier already implements — no
  cycle, the module already depended on `platform-ai-gateway-api`) and checks it when `moderation-enabled` /
  workspace `moderationEnabled` is active, fail-open on a classifier error like injection. The throwing
  `AiGuardrails#applyToInputs` (the gateway adapter's own path) deliberately never moderates — the gateway
  already moderates its own DTO pipeline with its own classifier + project overlay, so running it a second
  time here would double-moderate. `isActive` now also counts moderation as an activation reason. Because a
  moderation verdict has no locatable span (unlike a blocked term), a `REDACT_AND_CONTINUE` downgrade
  replaces the WHOLE message with `[REDACTED_MODERATED]` (category/metric `moderation_flagged`) rather than
  masking a substring — a deliberate departure from injection's existing downgrade (which still forwards
  only the pii/secret-redacted original text, unchanged by this work). The settings UI's old "currently
  applies to AI Gateway traffic only" caveat on the moderation toggle is gone; it now names the
  `bytechef.ai.gateway.guardrails.moderation-model` property required for the toggle to take effect. See
  the design spec's decisions log for why REDACT_AND_CONTINUE was implemented as whole-message masking
  rather than "moderation never downgrades."
- **Metric**: `bytechef_ai_guardrail{event, surface}` (`surface = gateway | ai_agent | ai_hub`), generalized
  from the old gateway-only counter; wired via `ObjectProvider<MeterRegistry>`. The per-surface split is
  clean for EVERY event on the advisor path, including request-direction ones: `AiGuardrails#checkInputs`
  (the advisor's non-throwing entry point) takes an `AiGuardrailMetrics` parameter and records
  `pii_redacted`/`secret_redacted`/`blocked_term`/`injection_flagged`/`moderation_flagged` through whatever instance
  `AiGuardrailsAdvisor` passes in — its own per-request, surface-tagged instance — not through the engine's
  own bean. Only `AiGuardrails#applyToInputs` (the gateway adapter's throwing entry point) still records
  through the engine's own internal `AiGuardrailMetrics` bean, gated on `bytechef.ai.gateway.enabled` and
  tagged `surface=gateway`. See `docs/agents/ai-guardrails.md` for the full breakdown.
- Spec: `docs/superpowers/specs/2026-07-31-ai-guardrails-standalone-design.md`. Agent docs:
  `docs/agents/ai-guardrails.md` (engine, advisor, surfaces) and `docs/agents/ai-gateway-guardrails.md`
  (gateway adapter specifics, project overlay).

### AI Gateway content guardrails (EE)

`AiGatewayGuardrails` (`automation-ai-gateway-service`) is now a thin adapter over the shared
`platform-ai-guardrails` engine described above, gated by its OWN `bytechef.ai.gateway.enabled` toggle
(unchanged pre-extraction behavior — the engine's unconditional registration does not change when the
gateway itself runs). It layers the gateway's project-level overlay (`AiGatewayProjectSettings`,
additive-only — a project can enable a guardrail or add blocked terms, never turn one off) and
its own moderation/injection classifier instances (same SPIs the shared engine also takes, wired separately
for the adapter's own DTO/project-overlay checks — moderation is no longer gateway-exclusive overall, see
"Model-based moderation" above) on top of the engine's global+workspace union, and preserves
the exact pre-extraction public surface (chat/embedding DTO methods, `projectId` overloads). The gateway
does not read `blockingMode` — a block always throws `AiGatewayGuardrailException` → HTTP 422, never
echoing the offending content. The gateway's own trace-privacy check
(`AiGatewayFacadeImpl.isPiiRedactionEnabled`) reads the workspace-level `AiGuardrailsWorkspaceSettingsService`
directly rather than through this adapter — deliberately workspace-scoped only, no project overlay, matching
pre-extraction semantics. See `docs/agents/ai-gateway-guardrails.md`.

### Workspace scoping for platform entities

**New platform-package entities get a nullable `workspace_id BIGINT` column — not a
`workspace_<entity>` relation table.**

- The column is **nullable**, and the entity field is `Long` (never primitive `long`). Null is a real
  state: embedded has no workspace concept, and some entities use it for "global" scope (a
  `notification` with no workspace applies everywhere).
- If the entity later needs to be shared beyond its owning workspace, add a `visibility` column typed
  `ResourceVisibility` (`PRIVATE < WORKSPACE < ORGANIZATION`, in `platform-api`) and contribute a
  `ResourceVisibilityPolicy` declaring which rungs the resource supports and which one it is created
  with. Reach is **additive to a column** — it does not need a relation table, and it expresses
  ownership + reach, which membership rows cannot.
- **Exception — named-user grants.** A column expresses *reach* and is the right shape for it. It
  cannot express "these three specific people" without an unbounded array in a cell. Grants to
  individual users therefore live in the polymorphic `resource_grant` table (EE, see
  `platform-resource-grant`). That is the one sanctioned relation table for sharing; the rule above
  still governs reach. See `docs/superpowers/specs/2026-08-10-resource-visibility-design.md`.
- Create a `workspace_<entity>` relation table **only** for a genuinely many-to-many relationship with
  no owner concept — the `workspace_user` shape. The bar: *can the same row legitimately belong to two
  workspaces with equal standing, today, with an API that does it?*

**Six relation tables deliberately remain** (they are in release `v0.31.2`, so collapsing them would
mean data migrations against customer data for no user-facing benefit): `workspace_api_key`,
`workspace_connection`, `workspace_data_table`, `workspace_knowledge_base`, `workspace_mcp_server`, and
`workspace_user` (which is genuinely many-to-many and correct as-is). The resulting mixed state is
intentional, not drift.

Background and evidence: `docs/superpowers/specs/2026-07-25-workspace-relation-table-convention-revision.md`.
This revises the earlier `2026-05-06-workspace-relation-tables-design.md` for new work only.

### Sidebar navigation groups (Client)

`AppSidebarNavItemI` has an optional `group` field; `AppSidebar` folds CONSECUTIVE items sharing
a `group` into one labeled `SidebarGroup` at the position of their first item (non-adjacent items
with the same group form separate sections — keep group members adjacent in the nav arrays in
`App.tsx`). Current groups: automation "Deployments" (Project Deployments, API Collections, MCP
Servers, Context Store) and "Data" (Data Tables, Knowledge Base, Files); embedded
"Configurations" (Integration Configurations, MCP Servers). Feature-flag filtering runs before
grouping, so a group renders with whatever members survive their flags.

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

This pattern shows up in `AiHubPersonalAgentsList.test.tsx` and any test that mocks Zustand
stores or router hooks via factory-injected mocks.

### Resource Visibility & Sharing

Connections carry a `visibility` column typed `ResourceVisibility` (`PRIVATE < WORKSPACE <
ORGANIZATION`, in `platform-api`). Every resource is created **WORKSPACE-visible** — shared with its
workspace unless its owner withholds it. The model is resource-agnostic; connections are the only
resource wired to it so far.

- **CE**: `ConnectionFacadeImpl.create()` force-writes `WORKSPACE`. No picker, no grants — CE has no
  authorization boundary between workspace members, so everything is workspace-public.
- **EE**: the picker offers Shared with workspace / Private / Specific people. No `ROLE_ADMIN` gate on
  `WORKSPACE` — it is the default, so gating it would fail every ordinary create. `ORGANIZATION` is
  **not** offered here: it is reached through `createOrganizationConnection`, and
  `setConnectionVisibility` rejects it. (`ConnectionVisibilityPicker` can render an Organization
  option behind `showOrganizationOption`, but no caller passes it today.)
- **Embedded**: force-written `PRIVATE`, unchanged. An embedded connection belongs to a connected user,
  not a workspace member, so workspace reach would be wrong in a way that crosses customers.

**What sharing exposes.** `WORKSPACE` grants *use plus existence*, not *read plus write*: both REST
controllers obfuscate `authorizationParameters` and null `parameters`, and no `ConnectionFacade` method
mutates authorization parameters after creation. A member can run a workflow against a colleague's
account; they cannot extract or repoint the credential.

**"Specific people"** is not a fourth stored value — it is `PRIVATE` plus rows in `resource_grant`
(EE, `platform-resource-grant`). A grant conveys visibility only; what the recipient may then do is
decided by the usual `PermissionScope`/`WorkspaceRole` machinery. Grants survive promotion so demoting
restores the previous audience, and are deleted with the connection because `resource_id` is
polymorphic and has no foreign key.

**Visibility is a precondition of `hasResourceScope`**, in both editions — not a filter running beside
it. Without that, a member holding `CONNECTION_EDIT` would pass the by-id check for a connection the
list correctly hides. In CE this replaces owner-isolation *only* for resource types that registered a
`ResourceVisibilityProvider`; API keys and other user-owned resources keep it.
`PermissionServiceVisibilityTest` is the regression guard.

**GraphQL mutations** (owner-or-admin, annotated on the facade so they protect every caller):
- `setConnectionVisibility(workspaceId, connectionId, visibility)` — rejects `ORGANIZATION` (set
  through `createOrganizationConnection`) and refuses to narrow to `PRIVATE` while an active
  deployment uses the connection.
- `grantConnectionAccess` / `revokeConnectionAccess(workspaceId, connectionId, userId)` — grantee must
  be a member of the owning workspace; rejection reuses the unknown-connection error so user ids
  cannot be enumerated. Grant is idempotent via `ON CONFLICT DO NOTHING`, not a caught
  `DuplicateKeyException` — PostgreSQL aborts the transaction on a constraint violation, so catching it
  still fails at commit.
- `connectionGrants(workspaceId, connectionId)` — owner-or-admin; a plain viewer must not learn who
  else a connection was handed to.

**Audit**: `CONNECTION_VISIBILITY_CHANGED`, `CONNECTION_ACCESS_GRANTED`, `CONNECTION_ACCESS_REVOKED`.
The first and last are `strictAudit` — both can remove access.

**Metrics**: `bytechef_connection_create` (Counter), tagged
`visibility=PRIVATE|WORKSPACE|ORGANIZATION`, wired via `ObjectProvider<MeterRegistry>` so lightweight
app variants without actuator start cleanly.


### Spring Boot Project Conventions

- **Integration Test Naming**: All integration test classes must end with "IntTest" suffix (e.g., `WorkflowFacadeIntTest.java`)
- **Spring 7 Programmatic Bean Registration**:
    - Use `BeanRegistrar` + `@Import` instead of `BeanFactoryPostProcessor` for programmatic bean registration
    - Resolve collection dependencies via `context.beanProvider(Class).orderedStream().toList()` (replaces `beanFactory.getBeansOfType()`)
    - Resolve named beans via `context.bean("beanName", Class)` in supplier
    - Test `BeanRegistrar` specs by capturing `Consumer<Spec<T>>` with `ArgumentCaptor`, applying to mock `Spec`, and verifying fluent calls

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
- **Client**: 3000 (development server)
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

### Resolving PR Review Comments
- Use `gh api graphql` with `resolveReviewThread` mutation to close threads programmatically
- Get thread IDs via: `gh api graphql -f query='{ repository(owner: "X", name: "Y") { pullRequest(number: N) { reviewThreads(first: 20) { nodes { id isResolved path } } } }'`

## Plan limits (placeholders)

- `server/libs/platform/platform-plan` (`-api`/`-service`, CE) holds the plan-tier policy layer:
  `PlanTier` (SELF_HOSTED default + FREE/PRO/TEAM/ENTERPRISE), `PlanLimits` record (every limit
  nullable, **null = unlimited — never zero**), and the `PlanLimitsProvider` SPI. The default
  `PropertiesPlanLimitsProvider` resolves `bytechef.plan.tier` (unset = SELF_HOSTED = unlimited,
  the pre-plan behavior) with per-field `bytechef.plan.limits.*` overrides; a billing integration
  replaces the bean (`@ConditionalOnMissingBean`). Tier tables in `DefaultPlanLimits` are
  Sim-modeled placeholders pinned by `DefaultPlanLimitsTest`.
  Design + phased plan (cost calculation, alert rules, Bucket4j rate limiting, Atlas admission
  gate): `docs/superpowers/specs/2026-07-20-plan-limits-cost-alerts-design.md`.
- **Enforcement** lives in CE `server/libs/platform/platform-rate-limit`
  (`bytechef.plan.enforcement.enabled`, default on — SELF_HOSTED's all-null limits make it a
  no-op): `Bucket4jRateLimiter` (local buckets in Caffeine; per-node default — set
  `bytechef.plan.enforcement.provider=redis` for strict global limits via `RedisRateLimiter`
  (Lua token bucket) + `RedisConcurrentExecutionGate` (bounded INCR/DECR, 24h self-healing
  TTL); both Redis impls fail open on Redis outages — pinned against a real Redis by
  `RedisPlanEnforcementIntTest` (Testcontainers)), `PlanRateLimitFilter`
  (order 0, after the security chain: login 10/min/IP, webhooks + the MCP/A2A secret-key
  endpoints (`/{secretKey}/mcp|sse|message`, `/api/automation/a2a/**`) → sync tier/tenant, public
  APIs → api tier/tenant, anonymous `/api/**` → per-IP; reject = 429 + Retry-After), and
  the two async-admission gates in `PrincipalJobFacadeImpl.createJob` plus the monthly-cost cap
  (`PlanSpendProvider` SPI, EE impl over cost rows, 60s memo, fail-open; over-cap submissions can
  be admitted under the tenant's on-demand overage terms via the stub `PlanOveragePolicyProvider`
  SPI — `PlanOveragePolicy(enabled, unbilledLimitUsd)`, Sim's opt-in overage model — no default
  bean, so the cap hard-stops until the billing integration contributes one) (async only; sync
  `createJobWithoutDispatch` is deliberately ungated to avoid slot leaks): the
  `async:<tenant>` submissions-per-minute bucket (checked FIRST so a rate reject never
  leaks a slot) then `ConcurrentExecutionGate` slots, released by platform-coordinator's
  `ConcurrencySlotReleaseApplicationEventListener`
  on terminal job status (floors at zero; restart over-admits, never wrongly blocks). Never
  gate inside `server/libs/atlas/` — admission and release both live outside the engine. Every
  rejection increments
  `bytechef_plan_limit_rejection{limit=login|sync|api|preauth|async|concurrency|cost|timeout|workspace|member|storage}`
  (`PlanLimitRejectionCounter`, no-op without a MeterRegistry).
- **Quota fields** are enforced at their natural creation points, each via an optional
  `ObjectProvider<PlanLimitsProvider>` (null limit / no bean = unlimited): `maxWorkspaces` in EE
  `WorkspaceServiceImpl.create`, `maxMembers` in `UserServiceImpl.create`/`registerUser` (counts ALL
  user rows — pending invites hold a seat; checked after the non-activated-user cleanup),
  `maxStorageBytes` in `AssetFileFacadeImpl` (tenant-wide `sumSizeBytes()` alongside the existing
  per-workspace property quota), `syncRunTimeout` caps the `JobCompletionAwaiter` wait on ALL
  three sync surfaces — `WebhookWorkflowExecutorImpl`, `AutomationMcpToolFacade`, and
  `AutomationA2AServerFacade` (plan can only tighten the configured default, never extend), and
  `logRetentionDays` drives `JobRetentionMonitor` (platform-coordinator, 6h per-tenant sweep,
  `getEndedJobs(endDateBefore)` finder — endDate exists only on terminal jobs — deleting through
  `JobFacade.deleteJob`'s cascade and skipping subflow children; works distributed via the remote
  job service/facade endpoints; operator fallback
  `bytechef.workflow.execution.retention.default-retention-days`, disable with
  `bytechef.workflow.execution.retention.enabled=false`). `JobFacadeImpl.deleteJob` also releases
  file-storage blobs (task outputs, job outputs, context values via `TaskFileStorage.delete*`) and
  context rows (`ContextService.getStackFileEntries`/`deleteStackContexts`) best-effort — a storage
  failure never blocks the row delete; in-memory repos throw `UnsupportedOperationException` for
  context enumeration and the facade skips that portion. The retention monitor additionally drops
  the purged job's `data_storage` CURRENT_EXECUTION rows via `DataStorage.deleteScopeData(scope,
  scopeId)` (jdbc provider + remote client implement it; the file-storage provider throws and the
  monitor skips). Quota rejections throw
  `QuotaLimitExceededException` (core exception-api) → HTTP 403 without Retry-After — a capacity
  ceiling, not a retryable rate limit (`RateLimitExceededException` stays 429) — and count into the
  rejection metric with tags `workspace`/`member`/`storage`.

## Crash recovery (orphaned jobs)

- Workers publish `TaskHeartbeatApplicationEvent` every 30s per in-flight task (scheduler inside
  `TaskWorker`, tenant captured at task receipt); the coordinator-side
  `TaskHeartbeatApplicationEventListener` re-saves the STARTED row, bumping `lastModifiedDate`.
  `OrphanedJobRecoveryMonitor` (platform-coordinator, every minute) then treats a job as orphaned
  only when the job row AND all its non-terminal task executions are stale
  (`bytechef.workflow.execution.recovery.staleness-threshold`, default PT5M) — children's
  heartbeats keep control-flow parent tasks alive transitively. Recovery marks tasks + job FAILED
  (normal job-status fan-out fires) making the job resumable via the existing
  `resumeToStatusStarted` path; `bytechef.workflow.execution.recovery.auto-resume=true` (default
  false) also publishes `ResumeJobEvent` — at-least-once semantics, the interrupted task re-runs
  from the last completed node — capped by `max-auto-resume-attempts` (default 3) tracked in job
  metadata. Disable the whole monitor with `bytechef.workflow.execution.recovery.enabled=false`.
  Stale-row finders are `getStaleTaskExecutions`/`getStaleJobs` (EE remote clients throw
  `UnsupportedOperationException`; the monitor warn-skips, so orphan detection is monolith-only
  for now). Detection lives OUTSIDE `server/libs/atlas/` except the engine-owned heartbeat
  primitives; semantics pinned by `OrphanedJobRecoveryMonitorTest`, and the underlying
  stale/long-running SQL finders by `StaleExecutionFinderIntTest` (Testcontainers PG).
- **Per-run timeouts**: `JobTimeoutMonitor` (platform-coordinator, every minute,
  `bytechef.workflow.execution.timeout.enabled` default on) fails STARTED jobs whose runtime
  exceeds the plan's `asyncRunTimeout` (per tenant) or the operator fallback
  `bytechef.workflow.execution.timeout.default-timeout`; with neither set it is a no-op. Uses the
  startDate-based finder `getLongRunningJobs` (remote clients throw, monitor skips). No
  auto-resume — a timed-out run would immediately exceed again. Pinned by `JobTimeoutMonitorTest`.
- **Mockito gotcha**: unstubbed wrapper-returning methods (Long/Integer) return 0, NOT null — stub
  `thenReturn(null)` explicitly when a null-means-absent field (e.g. `Job.getParentTaskExecutionId`)
  drives branching.
- **Redis broker redelivery**: `RedisListenerEndpointRegistrar` reclaims consumer-group pending
  entries left by crashed consumers (XPENDING + XCLAIM sweep every 10s, min idle 60s) and
  redelivers them through the normal invoke-then-ack path — at-least-once semantics like amqp.
- **Transactional completion**: `DefaultTaskCompletionHandler` takes an optional
  `TransactionTemplate` (coordinator config wires it from `ObjectProvider<PlatformTransactionManager>`)
  and runs update-task + push-context + advance-job (+ next-task create/dispatch) in ONE
  transaction — closing the half-advanced coordinator-crash window. Dispatch stays correct because
  `TaskExecutionEvent`/`JobStatusApplicationEvent` are `MessageEvent`s and `MessageEventListener`
  is `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` — deferred under a
  transaction, immediate without one. `JobSyncExecutor` passes null (in-memory sync path,
  unchanged). Pinned by `DefaultTaskCompletionHandlerTest`.
- **Agent-loop checkpoints**: `SuspendableToolCallingManager` takes an optional per-tool-round
  checkpointer; the AI Agent writes `AiAgentConversationCheckpoint` (SHA-256 input-parameter
  fingerprint + `ConversationState`) to `Data.Scope.CURRENT_EXECUTION` after each completed
  round, `AiAgentChatAction.perform` restores it on a crash-resumed job (fingerprint must match
  — protects against a different agent node in the same job) and clears it on success. Editor /
  job-less runs skip it; all checkpoint I/O is fail-open (a storage failure never fails the
  turn). Fingerprint computation is deliberately lazy (inside the write lambda).

## Notification delivery (central point)

- **`platform-notification` is THE central registry for notifications AND channels.** All channel
  types are first-class on `Notification.Type` — `EMAIL, WEBHOOK, SLACK` (INT ordinal, append-only) —
  with settings keys `email` / `webhook` + `webhookSecret` / `slackWebhookUrl` and a sender + handler
  pair per type (`Email|Webhook|SlackNotificationSender`, `JobStatus*NotificationHandler`). New
  notification surfaces and alert rules must reference `Notification` rows for delivery targets
  instead of defining their own channel entities. Workspace scoping is a **nullable
  `notification.workspace_id` column**, where `NULL` means global — i.e. the notification applies to
  every workspace. (It was previously the `workspace_notification` membership table, where the ABSENCE
  of a row meant global; that table was collapsed into the column, so "no row" became "null".) Writes go
  through `NotificationWorkspaceRepository` in `platform-notification-workspace`. The former EE
  `AiObservabilityNotificationChannel` table is GONE, along with its `workspace_*` relation table.
  There is no migration to run: the whole `platform-ai-observability` module is unreleased (absent
  from `master` and every release tag), so no database ever held a channel row. The observability init
  changelog therefore creates `ai_observability_alert_rule_channel` with a `notification_id` column
  FK'd straight to `notification(id)` — the conversion migration (`20260720000004`) that used to
  create, convert, then drop the channel tables was deleted rather than kept as a permanent no-op.
  That FK sits in its own changeset behind a `tableExists` precondition, because module-scoped test
  contexts load only a subset of changelogs and may not have `notification`.

- Webhook + Slack transports live in CE `server/libs/platform/platform-notification/platform-notification-delivery`:
  `WebhookNotificationClient` is THE single outbound-webhook transport (one `RestTemplate`, one Spring
  core `RetryTemplate`/`ExponentialBackOff` retry mechanism). Two entry points: `deliver(request[, retry])`
  for admin-configured notification webhooks — SSRF-validated via commons-util `UrlValidator`
  (loopback/private hosts rejected, so tests can't use a local HTTP server), standard
  `X-ByteChef-Event/Timestamp/Delivery` headers, optional HMAC
  `X-ByteChef-Signature: t=<ts>,v1=hex(HMAC-SHA256(secret, "<ts>.<body>"))` — and
  `deliverEvent(url, payload, retry)` for Atlas per-job callback webhooks (`Job.getWebhooks()`), which
  keeps the pre-existing contract: NO SSRF validation (authenticated API callers may target internal
  hosts) and message-converter payload serialization. Non-2xx / exhausted retries →
  `WebhookDeliveryException`. `SlackNotificationClient` (incoming-webhook transport) owns the
  `{"text": ...}` payload shape and delegates to the webhook client.
- Email: there is NO separate email transport — `MailService` (platform-mail, `@Async`, warn-skips when
  no mail host configured) is the single email path for everything, user-account mail and notification
  email alike. `EmailNotificationSender` and the EE
  `AiObservabilityNotificationDispatcher` both call `mailService.sendEmail(...)` — no inline
  `JavaMailSender` remains anywhere in notification delivery.
- Consumers: all three CE senders (`Email|Webhook|SlackNotificationSender`) live in
  platform-notification-delivery (so coordinator-app carries them). `EmailNotificationSender`
  reaches mail through the `NotificationEmailGateway` port (platform-notification-api):
  monolith/configuration-app bind it to MailService (`MailServiceNotificationEmailGateway`),
  coordinator/webhook apps bind it to `RemoteNotificationEmailGatewayClient` which proxies to
  configuration-app's `/remote/notification-email-gateway/send-email` — SMTP credentials stay in
  one app; no gateway bean at all = the EMAIL channel warn-skips. In the distributed deployment
  the coordinator resolves delivery targets through `configuration-app`'s
  `/remote/notification-service` read endpoints (platform-notification-remote-rest + the
  implemented `RemoteNotificationServiceClient` reads).
  `WebhookNotificationSender` (job-status webhook channel; settings keys `webhook` +
  optional `webhookSecret`, `@Async`), payload shaped by `JobStatusWebhookNotificationHandler` in
  platform-coordinator; platform-coordinator's `WebhookJobStatusApplicationEventListener` delegates the
  Atlas job-callback delivery to `deliverEvent` with the `Job.Retry` schedule (defaults: 5 attempts,
  2s initial interval, 2.0 multiplier); EE `AiObservabilityNotificationDispatcher` (post-migration: reads `Notification` rows,
  delivers via MailService + the shared clients; per-channel lastError bookkeeping is gone with the
  channel entity).
- The job-status trigger path is unchanged: `JobStatusApplicationEvent` → platform-coordinator
  `NotificationJobStatusApplicationEventListener` → sender/handler registries. Never add notification
  logic under `server/libs/atlas/` — the engine stays notification-agnostic (hard requirement).
- The listener warn-skips event/channel combos with no sender or handler (don't NPE the fan-out).
  JOB_CANCELLED fires when a job is stopped while still CREATED (never started) — `Job.Status.CANCELLED`
  is appended at the enum end (INT-ordinal persisted); STOPPED remains the mid-run interruption status.
- **Workflow alert rules (EE, Sim model)**: `server/ee/libs/automation/automation-workflow-alert` —
  workspace-scoped `workflow_alert_rule` rows (7 `WorkflowAlertRuleType`s, INT ordinal append-only)
  whose delivery targets are `Notification` ids (join table, FK CASCADE — rules own WHEN, the
  notification registry owns WHERE/HOW). Evaluation state lives ON the rule row (consecutive counter,
  tumbling-window counters, EWMA latency, lastActivity) — updated per terminal job event by
  `WorkflowAlertApplicationEventListener` (`@Order(200)`, after the cost listener's `@Order(100)` so
  COST_THRESHOLD sees the cost row); NO_ACTIVITY fires from a 5-min scheduled monitor; fixed cooldown
  (default 60 min). `WorkflowAlertDispatcher` delivers via MailService / WebhookNotificationClient
  (`workflow.alert` eventType) / SlackNotificationClient. Semantics pinned by
  `WorkflowAlertEvaluatorTest`.

### Workflow error handler

When an automation run ends `FAILED`, `ErrorWorkflowJobStatusApplicationEventListener`
(platform-coordinator, `@Order(300)`, after cost and workflow alerts) dispatches the configured
error workflow through `PrincipalJobFacade.createJob`. Config is a nullable
`project.error_project_workflow_id` (set via the `updateProjectErrorWorkflow` GraphQL mutation, and
in the client via the project header's Settings menu → Project tab → Error Workflow dialog) with a
per-workflow override + a separate `error_workflow_disabled` flag on `project_workflow` (null
already means inherit) — set together via `updateProjectWorkflowErrorWorkflow` (client: Settings
menu → Workflow tab → Error Handling dialog's three-state Inherit/Override/Disabled radio). The
handler must live in the same project and carry a `workflow/newWorkflowError` trigger; both are
validated when configured
(`ErrorWorkflowConfigurationValidator`), not at failure time. `errorHandlerFor` job metadata caps
recursion at depth 1 — a failing handler does not spawn another; a subflow child job is also
skipped (only the top-level failed run dispatches). Admission gates are deliberately not bypassed,
so a failure storm is bounded by plan limits, not deduped — N failures can produce up to N handler
runs. This layers on the `on-error` task dispatcher rather than competing with it: `on-error` is an
intra-workflow catch (a handled error ends the job `COMPLETED`), so an error workflow only fires on
a genuinely uncaught, inter-workflow failure. **Monolith only** — resolution needs
`ProjectWorkflowService` lookups, and `RemoteProjectWorkflowServiceClient` is all
`UnsupportedOperationException` stubs, so distributed EE can't resolve the handler at all (same
root cause as orphaned-job recovery); the listener detects this, logs once, and records the
`skipped_unsupported` outcome instead of warning on every failed job. The payload carries
`execution.autoRecoveryAttempts` rather than n8n's `retryOf`: ByteChef resumes a job IN PLACE
(`resumeToStatusStarted` reuses the same id), so there is no prior job to point at — what a handler
can use is how many times this run was already auto-recovered. Metric:
`bytechef_error_workflow_dispatch{outcome=dispatched|rejected|
skipped_recursion|skipped_subflow_child|skipped_no_config|skipped_unsupported|failed}`.

## Public URL Signing

- `/file-entries/{id}/content` is intentionally unauthenticated (serves webhook outputs to anonymous callers). As of the 2026-05-18 signing rollout, the preferred form is an HMAC-SHA256 signed token (`v1.<exp>.<payload>.<sig>`) minted via `FileEntryTokens.toSignedToken`. Legacy unsigned `FileEntry.toId()` IDs are still accepted while `bytechef.file-storage.signed-url.required=false` (default).
- **Use `FileEntry.toId()` for**: DB persistence, intra-process passing. No security claim, deterministic forever.
- **Use `FileEntryTokens.toSignedToken(fileEntry)` for**: anything that leaves the server as part of a URL (webhook response body, etc.). TTL applies.
- The signer lives in `file-storage-token-service` (not in `file-storage-api`, which stays interface-only). Consumers depend on `file-storage-api` for the `FileEntryTokens` interface and pull `file-storage-token-service` at runtime so the autoconfig fires.
- **Signing key resolution order**: (1) explicit `bytechef.file-storage.signed-url.secret` property — power-user override for independent key rotation; (2) `EncryptionKey` bean present (standard ByteChef setup) — derived automatically via `HMAC-SHA256(decode(encryptionKey), "bytechef-file-storage-signed-url-v1")`; (3) neither present — unconfigured mode (mint throws, verify accepts legacy only). In practice, signed URLs work out of the box on every deployment because `EncryptionKey` is always configured. Setting `bytechef.file-storage.signed-url.secret` explicitly is not required for normal deployments.
- The domain-separation label `"bytechef-file-storage-signed-url-v1"` ensures the derived signing key is mathematically independent from the AES master key (key-separation principle). The `-v1` suffix allows rolling forward to a new derivation scheme without rotating the encryption key.
- Spec: `docs/superpowers/specs/2026-05-18-hmac-signed-file-entry-tokens-design.md`. Plan: `docs/superpowers/plans/2026-05-18-hmac-signed-file-entry-tokens.md`.

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

## Agent skills

### Issue tracker

Issues and PRDs are tracked on GitHub at `bytechefhq/bytechef` (pinned explicitly because this clone has multiple remotes). See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles mapped to this repo's labels — `needs triage` and `wontfix` reuse existing labels; `needs-info` / `ready-for-agent` / `ready-for-human` are new. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
