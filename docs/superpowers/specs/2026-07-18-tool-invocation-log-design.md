# Tool invocation log: record direct action/tool executions

Date: 2026-07-18
Status: Draft

## Problem

ByteChef serves two structurally different tool kinds from its MCP servers, and only
one leaves a durable trace.

- **Workflow-as-MCP-tool** (rows in `mcp_project` / `mcp_project_workflow`, or embedded
  `mcp_integration_instance_configuration_workflow`) runs through
  `PrincipalJobFacade.createJob` → `JobFacadeImpl.createJob` → the atlas engine, writing
  `job`, `context`, `task_execution`, and `principal_job` rows. These appear in the
  Workflow Executions UI and the public workflow-execution APIs, and they consume licence
  quota via `LicenceJobUsageService`.

- **Direct component-action tools** (rows in `mcp_server` / `mcp_component` / `mcp_tool`)
  terminate at `ClusterElementDefinitionFacade.executeTool` →
  `ClusterElementDefinitionServiceImpl.doExecuteTool` → the component function, a purely
  in-process call. The embedded public REST `executeAction`
  (`ActionFacadeImpl.executeAction` → `ActionDefinitionFacade.executePerform`, all
  job/taskExecution ids null) and `executeTool` (`ToolFacadeImpl.executeTool`) are the
  same shape. These paths write **no `job`, no `task_execution`, no audit event, no
  Micrometer counter, and no info-level log line** — the only trace is an
  `ExecutionException(EXECUTE_PERFORM)` on failure.

The result: an operator or customer cannot answer "what did this MCP client / connected
user actually call, when, and did it fail?" for any non-workflow tool. The management MCP
server (manager subagents) is equally silent.

## Decision

Add a **tool invocation log** — an append-only, access-log-style record of every direct
action/tool execution — fed by a single canonical event emitted at the (few) choke
points every direct call funnels through. Do **not** route direct executions through the
atlas `Job` model.

### Why not synthetic Jobs

Routing ad-hoc calls through `PrincipalJobFacade.createJob` (so they appear in the
existing executions surfaces with zero new tables) was rejected: it redefines `job` from
"a workflow run" to "a run of something," makes `job.workflow_id` name workflows nobody
authored, forces new `PlatformType` ordinals through a pervasive enum, and entangles the
feature with licence metering. Workflow executions and ad-hoc invocations are genuinely
different shapes (hierarchical, task-backed vs. flat, single-step); forcing one model
corrupts both. The log keeps them separate and correctly-shaped.

### Emission seam (CE)

New CE module pair `platform-tool-execution-api` / `platform-tool-execution-service`
(CE because the automation MCP server is CE). It defines:

- **`ToolExecutionEvent`** — an immutable record carrying identity + outcome only:
  `tenantId` (captured explicitly at the call site), `surface`
  (`MCP_AUTOMATION | MCP_EMBEDDED | MCP_MANAGEMENT | EMBEDDED_API_ACTION |
  EMBEDDED_API_TOOL`), `kind` (`COMPONENT | WORKFLOW | CONTRIBUTED | MANAGEMENT_TOOL`),
  `toolName`, `componentName?`, `componentVersion?`, `operationName?`
  (actionName / clusterElementName), `connectionId?`, `environment?`, `workspaceId?`,
  `externalUserId?`, `connectedUserId?`, `integrationInstanceId?`, `mcpServerId?`,
  `jobId?` (workflow-as-tool link), `outcome`
  (`SUCCESS | ERROR | CONNECTION_REQUIRED | TIMEOUT`), `errorType?`, truncated
  `errorMessage?` (512 chars), `durationMs`. **No input parameters, no outputs — ever.**
  All enum ordinals pinned append-only by an `EnumOrdinalStabilityTest`.

- **`ToolExecutionEventPublisher`** — on each event: (a) records Micrometer meters via
  `ObjectProvider<MeterRegistry>`, (b) writes one structured SLF4J line, (c) publishes a
  Spring `ApplicationEvent` for the optional EE persistence listener. This mirrors the
  proven CE-publishes / EE-persists split of the audit bus
  (`*AuditPublisher` → `CustomAuditEventRepository`).

Every direct execution passes exactly one call site; each is wrapped so success and
failure both emit:

1. `AutomationMcpToolFacade.getClusterElementToolCallbackFunction` — `MCP_AUTOMATION` /
   `COMPONENT`.
2. `AutomationMcpToolFacade.getWorkflowToolCallbackFunction` — `MCP_AUTOMATION` /
   `WORKFLOW`, `jobId` set after `createJob`; `JobCompletionAwaiter` timeout →
   `outcome = TIMEOUT`.
3. `EmbeddedMcpToolFacade` component + workflow closures — `MCP_EMBEDDED`, including the
   `connection_required` branch → `outcome = CONNECTION_REQUIRED`.
4. `ActionFacadeImpl.executeAction` — `EMBEDDED_API_ACTION`.
5. `ToolFacadeImpl.executeTool` — `EMBEDDED_API_TOOL`.
6. Phase-later: the management MCP server (`@Tool` beans + `ManagerSubAgentToolCallback`).

### Persistence (EE)

New EE module pair `platform-tool-invocation-log-api` / `-service`, following the repo's
"New Spring Data JDBC Modules" convention (`@AutoConfiguration` +
`@EnableJdbcRepositories` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)`,
registered in `AutoConfiguration.imports`). It listens for the CE `ApplicationEvent` and
writes one `tool_invocation_log` row.

- Table `tool_invocation_log`, Liquibase under
  `config/liquibase/changelog/platform/tool_invocation/`, `includeAll`-wired into
  `liquibase-config/.../master.xml` with a context filter. Columns mirror
  `ToolExecutionEvent`: `id` identity PK, `created_date`, `duration_ms`, `surface` INT,
  `kind` INT, `tool_name`, `component_name`, `component_version`, `operation_name`,
  `connection_id`, `environment` INT, `workspace_id`, `external_user_id`,
  `connected_user_id`, `integration_instance_id`, `mcp_server_id`, `job_id`,
  `outcome` INT, `error_type`, `error_message` VARCHAR(512), `tenant_id`. Indexed on
  `(tenant_id, surface, created_date)`, `(mcp_server_id, created_date)`,
  `(connected_user_id, created_date)`.
- The write is **async, best-effort, tenant-safe**: a `@TransactionalEventListener`-style
  `@Async` handler on `TenantThreadPoolTaskExecutor`, `REQUIRES_NEW`, so a log failure
  never fails the tool call — the exact pattern `AiToolUsageRecorderImpl` /
  `MeteredToolCallback` already use for "ad-hoc tool runs."
- Retention reuses the `persistent_audit_event` retention-job pattern (configurable TTL).

### Payload safety

Action input maps routinely contain connection credentials and customer PII, so the log
stores **metadata only** by default. Optional payload capture is an explicit opt-in that
offloads inputs/outputs to `TaskFileStorage` with a TTL and a per-server /
per-integration toggle — off by default, never on the default path.

### Read surfaces

- **Public API** (optional, follow-up): an embedded per-connected-user history endpoint
  mirroring the `WorkflowExecutionApiController` shipped on this branch, reusing its
  connected-user scoping (`getConnectedUser*` facade methods) so end-user A can never read
  end-user B's invocations.
- **Metrics**: `bytechef_tool_invocation{surface,outcome}` (+ optional workspace-tagged
  companion, opt-in, matching the `bytechef_workflow_chat_turn` cardinality pattern).

### UI

Surface the log where operators already look for "what ran," without polluting the
job-backed tables.

- **Primary — tabbed Executions page, both surfaces.** Promote the two existing
  job-backed pages (`pages/automation/workflow-executions`,
  `ee/pages/embedded/workflow-executions`) into a tabbed **Executions** page:
  *Workflow Executions* (existing table, untouched) + *Tool Invocations* (new flat table,
  its own columns/filters/detail). Subroutes `…/executions/workflows` and
  `…/executions/tool-invocations`, matching the Components tabbed-page pattern. Rename the
  two nav entries (`App.tsx:92`, `App.tsx:143`) from "Workflow Executions" to
  "Executions." Reuse `workflow-execution-sheet` components for the row→detail split so a
  `WORKFLOW`-kind invocation row links into the existing execution sheet instead of
  duplicating it.
- **Secondary — scoped views.** The MCP server sheet gets an *Invocations* tab (same
  list, `mcpServerId` pre-filtered); the embedded connected-user sheet
  (`ConnectedUserSheetPanel`, already tabbed Integrations / MCP Servers / Workflows) gets
  an *Invocations* tab scoped to that end user.

## Non-goals

- **Licence metering of ad-hoc calls.** Direct executions bypass `licence_job_usage`
  today; the log makes that visible but does not change it — metering is a pricing
  decision needing the transactional licence path.
- **Editor test runs.** `WorkflowNodeTestOutputFacadeImpl` (editorEnvironment=true)
  already persists to `workflow_node_test_output`; not an invocation.
- **AI-Hub / copilot agent tool calls.** Covered by `SPRING_AI_CHAT_MEMORY` transcripts +
  the `ai_tool_usage` / `workspace_ai_tool_usage` metering tables; the log does not
  double-record them.
- **AI-agent-in-workflow inner tool calls.** A parent `task_execution` row already exists.

## Consequences

Every direct action/tool execution — the surfaces that are invisible today — becomes
observable to operators (metrics + structured logs from day one, via the CE seam alone)
and, once the EE table lands, queryable per caller and surfaceable as a product feature.
Workflow runs keep their existing home; the two executions kinds sit as sibling tabs
under one destination. The atlas `Job` model is untouched. The default path stores no
sensitive payloads.
