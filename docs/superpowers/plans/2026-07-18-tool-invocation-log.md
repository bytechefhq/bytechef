# Plan: tool invocation log for direct action/tool executions

Spec: `docs/superpowers/specs/2026-07-18-tool-invocation-log-design.md`

## Status (2026-07-18)

Shipped and verified (local Gradle 8.14.3 / JDK 21 → Java 25 toolchain; client `npm`):

- **Phase 1** — `platform-tool-execution` (api + service); `ToolExecutionRecorder` wired into all five
  choke points (automation + embedded MCP component/workflow closures, embedded
  `executeAction`/`executeTool`). Metrics + logs + `ToolExecutionEvent` publish.
- **Phase 2** — `platform-tool-invocation-log` (api + service); `tool_invocation_log` table + async
  best-effort listener + paged/filtered read + retention job; wired into `server-app` + `execution-app`.
- **Phase 3** — `platform-tool-invocation-log-graphql` (`toolInvocationLogs` query) + client data
  layer (`useToolInvocationLogsQuery`); **Tool Invocations tab on both the automation and embedded
  Executions pages** (nav renamed "Workflow Executions" → "Executions", route
  `executions/tool-invocations`, reusable `ExecutionsTabs` + `ToolInvocations` page via `basePath`,
  `ToolInvocationsTable` + filters + a vitest test); and a **scoped Tool Invocations tab on the
  embedded connected-user sheet** (connectedUserId pre-filter).

Agreed placement: the Executions page Tool Invocations tab is the single home for tool invocations
on each surface (done). Per-server / per-user surfacing is achieved by filtering that tab, not by
separate sheets — so no MCP-server detail-sheet tab is planned. (The connected-user sheet already
got a convenience Invocations tab; that is a bonus, not required.)

Remaining (optional):

- An mcpServerId filter on the Executions Tool Invocations tab (so a single server's invocations are
  reachable from the one home) — the only "scoped view" still worth adding.
- Public per-connected-user history REST endpoint.
- Populate `workspace_id` on automation MCP events (resolve from mcpServer) for workspace-scoped reads.

Delivered in phases; each phase is independently shippable. Phase 1 alone closes the
"completely blind" gap (metrics + structured logs) with no new table and no EE code.

## Phase 1 — CE emission seam (metrics + logs, no persistence)

1. **Module `platform-tool-execution-api`** (`server/libs/platform/platform-tool-execution/`)
   - `ToolExecutionEvent` record (fields per spec; metadata only — no inputs/outputs).
   - `ToolExecutionSurface`, `ToolExecutionKind`, `ToolExecutionOutcome` enums.
   - `ToolExecutionEventPublisher` interface + `ToolExecutionRecorder` helper that
     stamps `durationMs` around a supplier and emits SUCCESS / maps a thrown
     `ExecutionException` (or `JobCompletionAwaiter` timeout) to the matching outcome.
   - `EnumOrdinalStabilityTest` pinning the three enums (append-only).

2. **Module `platform-tool-execution-service`**
   - `DefaultToolExecutionEventPublisher`: `ObjectProvider<MeterRegistry>` counter
     `bytechef_tool_invocation{surface,outcome}`, one structured SLF4J line, and a Spring
     `ApplicationEventPublisher` publish of `ToolExecutionEvent` (for the Phase-2 EE
     listener; a no-op when no listener is registered).
   - `@AutoConfiguration` registered in
     `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`;
     `spring-boot-autoconfigure` dependency.

3. **Wire the choke points** (wrap each so success + failure both emit; capture
   `tenantId` and identity from the request-scoped context already present):
   - `AutomationMcpToolFacade.getClusterElementToolCallbackFunction` — MCP_AUTOMATION /
     COMPONENT (mcpServerId from the callback closure).
   - `AutomationMcpToolFacade.getWorkflowToolCallbackFunction` — MCP_AUTOMATION /
     WORKFLOW, `jobId` after `createJob`, TIMEOUT on awaiter expiry.
   - `EmbeddedMcpToolFacade` component + workflow closures — MCP_EMBEDDED,
     CONNECTION_REQUIRED on the setup-URL branch.
   - `ActionFacadeImpl.executeAction` — EMBEDDED_API_ACTION (externalUserId,
     connectedUserId, integrationInstanceId, connectionId, environment from the facade
     args / `ConnectionIdHelper`).
   - `ToolFacadeImpl.executeTool` — EMBEDDED_API_TOOL.
   - Add `platform-tool-execution-api` as a dependency of the three facade modules.

4. **Tests**: recorder maps success/error/timeout/connection-required correctly; publisher
   increments the counter and publishes the event; each facade emits with the right
   surface/kind and no payload fields. Verify each wrapped call still returns/propagates
   exactly as before (no behavioural change on the hot path).

## Phase 2 — EE persistence

5. **Module `platform-tool-invocation-log-api`**
   - `ToolInvocationLog` domain (Spring Data JDBC), `ToolInvocationLogService` interface,
     read DTO + `Page`-returning query method (filters: surface, outcome, date range,
     mcpServerId, connectedUserId, integrationInstanceId).

6. **Module `platform-tool-invocation-log-service`**
   - Liquibase `tool_invocation_log` changelog under
     `config/liquibase/changelog/platform/tool_invocation/`; `includeAll` line in
     `liquibase-config/.../master.xml` with context filter
     `mono or configuration or multitenant`. Indexes per spec.
   - `ToolInvocationLogRepository` + `ToolInvocationLogServiceImpl`.
   - `ToolInvocationLogEventListener`: `@Async` on `TenantThreadPoolTaskExecutor`,
     `@Transactional(REQUIRES_NEW)`, best-effort (catch + log-warn, never rethrow), maps
     `ToolExecutionEvent` → row. `@ConditionalOnEEVersion`.
   - `@AutoConfiguration` + `@EnableJdbcRepositories` +
     `@ConditionalOnBean(AbstractJdbcConfiguration.class)`, registered in
     `AutoConfiguration.imports`.
   - Retention job modelled on the `persistent_audit_event` cleanup (configurable TTL
     property, default e.g. 90 days).

7. **Tests**: `IntTest` (Testcontainers) — event published → row persisted with expected
   columns, tenant-scoped; listener failure does not propagate; retention deletes rows
   older than the TTL. Enum ordinal ↔ INT column mapping pinned.

## Phase 3 — read APIs & UI

8. **Public read (optional)**: embedded per-connected-user history endpoint mirroring
   `WorkflowExecutionApiController` (this branch), reusing the connected-user scoping so
   A cannot read B's rows. Spec-first (openapi.yaml → generated) like the execution API.

9. **Client — tabbed Executions page (both surfaces)**
   - Promote `pages/automation/workflow-executions/WorkflowExecutions.tsx` and
     `ee/pages/embedded/workflow-executions/WorkflowExecutions.tsx` into tabbed pages:
     *Workflow Executions* (existing table) + *Tool Invocations* (new
     `ToolInvocationsTable` + filter sidebar + detail sheet, built from the
     `workflow-execution-sheet` components).
   - Subroutes `…/executions/workflows`, `…/executions/tool-invocations` in `routes.tsx`
     with legacy redirects from the old paths.
   - Rename nav entries `App.tsx:92` and `App.tsx:143` "Workflow Executions" →
     "Executions".
   - `WORKFLOW`-kind rows link into the existing execution sheet rather than duplicating
     detail.

10. **Client — scoped views**
    - MCP server sheet: *Invocations* tab (list with `mcpServerId` pre-filtered) on
      `pages/automation/mcp-servers` and `ee/pages/embedded/mcp-servers`.
    - `ConnectedUserSheetPanel`: *Invocations* tab scoped to that connected user.
    - Follow client conventions: sort-keys, `Icon`-suffixed lucide imports, interface
      names ending `I`/`Props`, `twMerge`, hook ordering.

11. **Client tests**: `ToolInvocationsTable` renders/paginates; tab routing + legacy
    redirects; scoped filters pass the right id; `WORKFLOW`-row link opens the execution
    sheet.

## Phase 4 (deferred, tracked not built)

- Management MCP server emission (`@Tool` beans + `ManagerSubAgentToolCallback`).
- Opt-in payload capture (inputs/outputs → `TaskFileStorage`, per-server/integration
  toggle, TTL).
- Licence metering of ad-hoc calls (pricing decision; transactional path).

## Verification

- Server: `./gradlew spotlessApply` then module `:check` (compile + Spotless + Checkstyle
  + PMD + SpotBugs + unit tests) on each new module and each touched facade module; the
  Phase-2 `IntTest`s via `:testIntegration` (Testcontainers). Local runs use Gradle
  8.14.3 with the daemon on JDK 21 driving the Java 25 toolchain; CI runs 9.4.1.
- Client: `cd client && npm run check` (lint + typecheck + vitest).
- Sequencing: land Phase 1 first (operator visibility, low blast radius), then 2, then 3.
