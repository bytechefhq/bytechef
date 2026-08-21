# MCP & A2A servers

Workflows-as-tools (fromAi mapping) and the Agent2Agent surface.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

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
- There is no `mcp_agent` subagent. `prompt_mcp_agent.txt` survives under its old name as the system
  prompt of the `configureMcpServer` intelligent tool (`McpServerSubAgentConfiguration`), narrowed to
  tool-mapping synthesis over exactly two tools (`listMcpProjectWorkflows`,
  `updateMcpProjectWorkflowParameters`). Creating, attaching and enabling servers are flat CRUD tools
  on whichever surface calls it.
- The management MCP server folds in `McpServerToolCallbackContributor` beans (SPI in
  `ai-mcp-server-api`, keeps the CE server free of EE imports). The intelligent delegates reach it
  through `IntelligentToolCatalog#getByNames`, each contributor over its OWN name partition rather
  than the whole catalog — CE `ToolCallbackContributorConfiguration` (seven: `buildWorkflow`,
  `importWorkflow`, `configureClusterElement`, `writeScript`, `authorSkill`,
  `debugWorkflowExecution`, `configureMcpServer`), EE `AutomationCopilotMcpContributorConfiguration`
  (`buildCustomComponent`, `buildCodeWorkflow`) and EE `EmbeddedCopilotMcpContributorConfiguration`
  (`buildIntegrationWorkflow`) — so the count is a property of those three sets, not a fixed number,
  and a definition whose `ChatClient` bean is absent is silently skipped. Each is wrapped in
  `WorkspaceScopedSubAgentToolCallback`: an optional `workspaceId` input is resolved and forwarded
  into the specialist's ToolContext under both `AutomationToolInvocationContext` and
  `AgentToolInvocationContext`'s workspace-id keys — the two key families the delegates' inner
  tools read; a sole workspace auto-selects; multiple return a `workspace_required` error listing
  candidates. `ProgressReportingToolCallback` is NOT applied on the MCP surface (no AG-UI stream).
  Spec: `docs/superpowers/specs/2026-07-18-management-mcp-manager-subagents-design.md`.

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
