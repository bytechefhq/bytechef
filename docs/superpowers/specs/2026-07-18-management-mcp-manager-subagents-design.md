# Management MCP server: expose AI Hub manager subagents

Date: 2026-07-18
Status: Approved

## Problem

The management MCP server (`/api/management/{secretKey}/mcp`) exposes the CE automation
tools (projects, workflows, components, scripts, cluster elements) plus the Copilot
specialist agent-tools contributed by `ToolCallbackContributorConfiguration`. The four
AI-hub-owned manager subagents introduced on this branch — `mcp_manager`,
`personal_agent_manager`, `deployment_manager`, `api_collection_manager` — are only
reachable from the AI Hub BUILD chat. An external MCP client (Claude, Cursor) connected
to the management server cannot create MCP servers, complete fromAi tool mappings,
manage personal agents, drive deployments, or manage API collections.

## Decision

Contribute the four manager subagent delegates to the management MCP server through the
existing `McpServerToolCallbackContributor` SPI — the same extension point the Copilot
specialists use, keeping the CE server free of EE imports.

### Workspace scoping

The manager delegates forward their `ToolContext` into the specialist ChatClient, whose
workspace-scoped tools resolve `AiHubToolInvocationContext` (workspaceId). On the AI Hub
chat surface that context is injected by the agent state; on the management MCP surface
there is no chat state, so the contributor wraps each delegate in
`WorkspaceScopedManagerToolCallback`, which:

- extends the delegate's input schema with an optional `workspaceId`;
- when `workspaceId` is supplied, injects it into the forwarded `ToolContext` under
  `AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY`;
- when absent, resolves via `WorkspaceService.getWorkspaces()`: exactly one workspace
  auto-selects; zero or multiple returns a typed error listing `{id, name}` candidates so
  the calling agent can retry with an explicit id.

`ProgressReportingToolCallback` is NOT applied on this surface — it narrates progress
into the AG-UI stream, which does not exist for MCP clients.

### Security

The wrapper adds no authorization of its own. Management-MCP requests are already
authenticated (API key / OAuth per the MCP security configurers), and every mutation the
subagents perform goes through facades/services carrying `@PreAuthorize` checks
(`MCP_EDIT`, workspace membership, etc.). Workspace selection only scopes lookups; it
grants nothing the authenticated principal does not already hold.

### Non-goals

- Exposing the raw domain tools (listMcpServers, createProjectDeployment, ...) directly
  on the management server. The agent-tool granularity matches the Copilot specialists
  already contributed there; raw tools remain reachable through the subagents.
- Environment selection. `AiHubToolInvocationContext.resolveEnvironmentOrDefault`
  falls back to DEVELOPMENT, matching the AI Hub chat surface before the environment
  selector is wired through; a follow-up can add an `environmentId` input alongside
  `workspaceId`.

## Consequences

Everything the AI Hub BUILD agent can do in these four domains — including the fromAi
tool-mapping playbook (`listMcpProjectWorkflows` / `updateMcpProjectWorkflowParameters`)
— becomes reachable from any MCP client connected to the management server, with
workspace scoping explicit at the tool boundary.
