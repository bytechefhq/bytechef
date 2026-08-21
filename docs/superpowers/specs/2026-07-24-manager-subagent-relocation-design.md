# Prep A — Relocate management manager-subagents out of AI Hub

**Date:** 2026-07-24
**Status:** Design — approved for planning
**Author:** Ivica Cardic

## Context

Three of the four "manager" subagents contributed to the management MCP server manage
**automation** resources, not AI-Hub state, yet they live under `ai-hub-service` (EE) only for
historical reasons:

- `mcp_manager` — MCP servers/projects and their `fromAi` tool mapping.
- `deployment_manager` — project deployments (create/promote/rollback/toggle).
- `api_collection_manager` — API collections.

Only `personal_agent_manager` is genuinely AI-Hub-specific (personal agents are an AI-Hub
feature).

Today all four are contributed by a single EE bean,
`AiHubManagerMcpContributorConfiguration` (`ai-hub-service`), through the
`McpServerToolCallbackContributor` SPI, and each is also instantiated inline by the ai_hub BUILD
agent via a static `create*ManagerToolCallback` factory. Each manager is a subagent `ChatClient`
wrapped in `ManagerSubAgentToolCallback` and, on the MCP surface, in
`WorkspaceScopedManagerToolCallback` (which adds an explicit `workspaceId` argument because the
MCP surface carries no AI-Hub chat state).

This is the first of three sequenced pieces:

1. **Prep A (this spec)** — relocate the three management managers out of `ai-hub`.
2. **Prep B** — consolidate the viewer-backing read tools (`getAssetFileContent`,
   `queryDataTable`, `getCodeWorkflowSource`, `getCustomComponentSource`) into `automation-ai-tool`
   by edition, dropping `ai-hub-service` duplicates.
3. **Feature** — four read-only MCP App viewers (data table, code workflow, custom component,
   file) rendered in MCP Apps hosts, plus SPI-driven `_meta.ui` / `structuredContent` wiring.

Prep A is a pure relocation refactor: no new capability, no behavior change. Same tools, same
`workspaceId` scoping, same `bytechef.ai.hub.enabled` gating.

## Goal

- Move `mcp_manager`, `deployment_manager`, `api_collection_manager` (configs, prompts, and their
  constituent tool callbacks) out of `ai-hub-service`.
- Place each in **CE `automation-ai-tool`** when its facade permits, otherwise **EE
  `automation-ai-tool`**.
- Keep `personal_agent_manager` in `ai-hub-service`.
- Preserve every existing contribution to the management MCP server and every use by the ai_hub
  BUILD agent.

## Non-goals

- No change to what any manager does, which tools it binds, or its prompt content.
- No change to `personal_agent_manager`.
- No viewer, read-tool, or `structuredContent` work (Prep B and the Feature).
- No change to the `McpServerToolCallbackContributor` SPI itself (the Feature extends it later).

## Edition analysis

Edition is dictated by the facade each manager's tools depend on:

| Manager | Backing facade | Facade edition | Target home |
|---|---|---|---|
| `mcp_manager` | `McpServerService` (`com.bytechef.platform.mcp.service`) | **CE** | CE `automation-ai-tool` |
| `deployment_manager` | `ProjectDeploymentFacade` (`com.bytechef.automation.configuration.facade`) | **CE** | CE `automation-ai-tool` |
| `api_collection_manager` | `ApiCollectionFacade` (`com.bytechef.ee.automation.apiplatform...`) | **EE** | EE `automation-ai-tool` |
| `personal_agent_manager` | AI-Hub personal agents | **EE** | `ai-hub-service` (unchanged) |

The facades for `mcp_manager` and `deployment_manager` are CE, so both can land in CE — but only
after their constituent tool callbacks and the shared scaffolding they lean on are made CE too
(they are currently authored under `com.bytechef.ee.ai.hub.tool`).

Both CE modules exist already:

- CE: `server/libs/automation/automation-ai/automation-ai-tool`
- EE: `server/ee/libs/automation/automation-ai/automation-ai-tool`

## What moves

### To CE `automation-ai-tool`

- `McpManagerConfiguration` + `prompt_mcp_manager.txt` and its seven MCP tool callbacks:
  `CreateMcpServerToolCallback`, `ListMcpServersToolCallback`, `UpdateMcpServerToolCallback`,
  `CreateMcpProjectToolCallback`, `CloneMcpProjectToolCallback`,
  `ListMcpProjectWorkflowsToolCallback`, `UpdateMcpProjectWorkflowParametersToolCallback`.
- `DeploymentManagerConfiguration` + `prompt_deployment_manager.txt` and its deployment tool
  callbacks: `CreateProjectDeploymentToolCallback`, `UpdateProjectDeploymentToolCallback`,
  `DeleteProjectDeploymentToolCallback`, `ListProjectDeploymentsToolCallback`,
  `PromoteWorkflowToolCallback`, `RollbackProjectDeploymentToolCallback`,
  `ToggleProjectDeploymentToolCallback`.
- Shared scaffolding used by CE and EE managers alike: `ManagerSubAgentToolCallback`,
  `WorkspaceScopedManagerToolCallback`.

Each relocated tool callback must be verified to import only CE types before it moves; any that
transitively touches an EE facade blocks its manager from CE and is called out during planning
(the split-per-manager fallback below).

### To EE `automation-ai-tool`

- `ApiCollectionManagerConfiguration` + `prompt_api_collection_manager.txt` and its tool callbacks:
  `CreateApiCollectionToolCallback`, `CloneApiCollectionToolCallback`,
  `ListApiCollectionsToolCallback` (facade `ApiCollectionFacade` is EE).

### Stays in `ai-hub-service`

- `PersonalAgentManagerConfiguration`, `prompt_personal_agent_manager.txt`, and its tools.

## Scaffolding decouple

The relocated CE managers currently reference `AiHubAgentType` (EE) as a subagent type label.
CE code cannot import it. Replace that reference in the CE managers with a CE-side label — a small
enum (or constant) introduced in CE `automation-ai-tool` — carrying the same values the managers
need. `personal_agent_manager` keeps `AiHubAgentType`.

`ManagerSubAgentToolCallback` and `WorkspaceScopedManagerToolCallback` move to CE
`automation-ai-tool` unchanged in behavior; EE managers (`api_collection_manager`,
`personal_agent_manager`) import them from CE (EE may depend on CE).

## Contribution fan-out

The single EE contributor bean splits into three, each contributing through the existing
`McpServerToolCallbackContributor` SPI with no SPI change:

- **New CE contributor** in CE `automation-ai-tool` → contributes `mcp_manager` +
  `deployment_manager`.
- **New EE contributor** in EE `automation-ai-tool` → contributes `api_collection_manager`.
- **`AiHubManagerMcpContributorConfiguration` (slimmed)** → contributes only
  `personal_agent_manager`.

All three keep:

- `WorkspaceScopedManagerToolCallback` wrapping on the MCP surface.
- The `bytechef.ai.hub.enabled=true` gate. (Rationale: these managers drive AI-Hub-style
  management flows and remain gated by the same property so their MCP-surface exposure is unchanged
  from today. If a manager should be independently gated in a later phase, that is out of scope
  here.)
- `ObjectProvider.ifAvailable` silent-skip when a manager `ChatClient` bean is absent.

## ai_hub BUILD agent

The ai_hub BUILD agent instantiates each manager inline via the static
`create*ManagerToolCallback` factory. Those factories move with their configs; the BUILD agent
bean re-imports them from the new homes. `ai-hub-service` (EE) may import from CE and EE
`automation-ai-tool`, so all four factories remain reachable. Behavior is identical — the BUILD
agent registers the same four manager tools.

## Module dependencies

- CE `automation-ai-tool` gains dependencies on: `platform-mcp` service API (for
  `McpServerService`), `automation-configuration` facade (for `ProjectDeploymentFacade`),
  `ai-mcp-server-api` (the SPI), and Spring AI ChatClient — verify each is not already present.
- EE `automation-ai-tool` gains a dependency on `ai-mcp-server-api` if not already present (it
  already depends on the EE apiplatform facade).
- `ai-hub-service` keeps its dependency on `automation-ai-tool` (CE + EE) so the BUILD agent can
  reach the relocated factories; it drops the now-removed manager classes.

## Testing

- Relocate each manager's existing tests to the new module and package; rename per the
  `Impl`-drop / `IntTest` conventions where applicable.
- CE contributor test: asserts `mcp_manager` + `deployment_manager` are contributed, each
  workspace-scoped, and skipped when the `ChatClient` bean is absent.
- EE contributor test: asserts `api_collection_manager` is contributed and workspace-scoped.
- Slimmed `AiHubManagerMcpContributorConfigurationTest`: asserts only `personal_agent_manager`
  remains.
- ai_hub BUILD agent test (existing): still registers all four manager tools after the relocation.
- No new behavioral assertions — this is a move; existing assertions must pass unchanged against
  the new locations.

## Risks

- **Hidden EE coupling.** A CE-targeted tool callback may transitively pull an EE type not visible
  in its imports (e.g. via a shared DTO). Mitigation: compile CE `automation-ai-tool` after each
  manager moves; if a callback won't compile CE, fall back to EE `automation-ai-tool` for that
  manager and record the blocking dependency (this is the "split: CE-easy, EE-rest" outcome for
  that one manager, applied only when forced).
- **Spring bean qualifier drift.** The manager `ChatClient` beans are resolved by qualifier
  (`mcpManagerChatClient`, etc.). Keep the qualifier names identical across the move so injection
  points elsewhere continue to resolve.
- **Spotless EE headers.** Files landing under `server/ee/` need the ByteChef Enterprise header +
  `@version ee`; files landing in CE need the Apache header. Apply per destination, not origin.

## Rollout

Single PR, no data migration, no flag. The change is transparent at runtime: the same four manager
tools appear on the management MCP server and in the ai_hub BUILD agent, gated and scoped exactly
as before.
