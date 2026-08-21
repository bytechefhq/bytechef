# AI Hub: Task Templates rename, Agents surfacing, and UX fixes — Design

Date: 2026-08-12
Branch: `claude/ai-hub-resource-selection-863527` (fast-forwarded to `0_732` tip)

## Scope

Six user-requested changes to the AI Hub surface:

1. Selecting a resource in the composer opens it in the right panel immediately (including on the home view).
2. Rename **Personal Agents → Task Templates** across client, server, and database (`ai_hub_task_template*`).
3. Change the Task Templates icon from the bot icon to `ClipboardListIcon`.
4. Rename the sidebar **Context** section to **More**, opening a right-side popup menu instead of inline expansion.
5. Add an **Agents** entry above Workflow Chats in the AI Hub sidebar, with a listing page; Workflow Chats and Agents are mutually exclusive lists.
6. `/automation/chats` sidebar also lists Agents.

## 1. Resource selection → right panel opens immediately

**Current behavior.** `AiHubComposer.handleSelect` already adds the chip AND opens a tab via
`aiHubTabsStore` (every `open*Tab` sets `rightPanelOpen: true`). But `AiHub.tsx` renders the
split view only when `hasActiveTask` (`showResourcePanel = hasActiveTask && rightPanelOpen`), so on
the home view tabs accumulate invisibly until the first message creates a task.

**Change.** Render the split view on the home view too:

- Gate becomes `showResourcePanel = rightPanelOpen && (hasActiveTask || openTabs.length > 0)`.
- The `!hasActiveTask` branch of `mainBody` reuses the same `ResizablePanelGroup` shell with
  `AiHubHomePanel` as the left pane (instead of `AiHubPanel`) and `AiHubResourcePanel` as the right
  pane. The existing open/close flex animation effect keys on `showResourcePanel` and is unchanged.
- The existing home→task snapshot carry-over in `useAiHubTabsStore.setActiveTaskId` already inherits
  home-view tabs into the auto-created task; no store change needed.

**Out of scope.** `mcpServer`, `apiCollection`, and previous-`task` picks have no right-panel tab
type/viewer; they keep chip-only behavior.

## 2 & 3. Personal Agents → Task Templates rename (incl. icon)

Confirmed: table name is `ai_hub_task_template` (user's "ai_hub_tak_template" was a typo).

### Naming map

| Current | New |
|---|---|
| `AiHubPersonalAgent*` (Java, ~40 classes) | `AiHubTaskTemplate*` |
| package `com.bytechef.ee.ai.hub.personalagent` | `com.bytechef.ee.ai.hub.tasktemplate` |
| `WorkspaceAiHubPersonalAgentService` | `WorkspaceAiHubTaskTemplateService` |
| `PersonalAgentSaveValidator` | `TaskTemplateSaveValidator` |
| tool callbacks `*AiHubPersonalAgent*ToolCallback` | `*AiHubTaskTemplate*ToolCallback` |
| MCP tool `personal_agent_manager` | `task_template_manager` |
| GraphQL types/queries/mutations `aiHubPersonalAgent*` | `aiHubTaskTemplate*` |
| task kind `PERSONAL_AGENT` | `TASK_TEMPLATE` (INT-ordinal storage — rename is free, `EnumOrdinalStabilityTest` updated) |
| client dir `ai-hub/personal-agents/` | `ai-hub/task-templates/` |
| client components/hooks `AiHubPersonalAgent*` / `useAiHubPersonalAgents` | `AiHubTaskTemplate*` / `useAiHubTaskTemplates` |
| route `/automation/ai-hub/personal-agents` | `/automation/ai-hub/task-templates` |
| UI strings "Personal Agents", "New Agent" | "Task Templates", "New Task Template" |
| DB `ai_hub_personal_agent`, `_tool`, `_resource`, `_schedule` | `ai_hub_task_template`, `_tool`, `_resource`, `_schedule` |
| DB `ai_hub_task.ai_hub_personal_agent_id` (+ index/FK names) | `ai_hub_task.ai_hub_task_template_id` |

### Database approach (decided: edit init changelogs)

The feature is unreleased (lives only on `0_732`), so the existing init changelog XMLs are edited in
place (table/column/index/FK names) and the changelog files renamed to match
(`*_ai_hub_task_template_*`). Per project practice: delete stale copies under `build/resources/` after
renaming changelog files (Liquibase sees both old and new on the classpath otherwise).

Consequence for existing dev DBs: Liquibase creates fresh empty `ai_hub_task_template*` tables; old
`ai_hub_personal_agent*` tables stay behind orphaned. A one-off SQL snippet is provided (in the PR
description / handoff) to copy local rows across and rewrite `ai_hub_task.ai_hub_personal_agent_id`
into the new column, for anyone who wants to keep local test data.

### Icon

`BotIcon` → `ClipboardListIcon` everywhere Task Templates render: sidebar nav item, task-list row
badge (kind `TASK_TEMPLATE`), listing page rows, tooltips. `BotIcon` remains in use for AI Agents
(resource picker Agents branch, and the new Agents nav below).

## 4. Context → More popup

In `AiHubTasksSidebar.tsx`, replace the `Collapsible` "Context" group with a **More** row that opens a
`DropdownMenu` (`side="right"`, `align="start"`) containing the same three links: Memories, Connectors,
Skills (icons kept). The row keeps the `menuItemClass` active treatment when the current route is one
of the three targets. The `contextExpanded` state and the indented `pl-8` sub-links go away.

## 5 & 6. Agents surfacing

**Why agents are invisible today.** An AI Agent (`ai_agent`, CE `automation-ai-agent`) owns a hidden
backing project; `ProjectDeploymentService`'s project-scoped listing paths filter agent-backed projects
out, so `workspaceChatWorkflows` never returns agent chat workflows — neither AI Hub's Workflow Chats
nor `/automation/chats` show them.

**Server.** New GraphQL query in `automation-ai-agent-graphql`
(`AiAgentGraphQlController`):

```graphql
workspaceChatAgents(workspaceId: ID!, environmentId: ID!): [ChatAgent!]!

type ChatAgent {
    aiAgentId: ID!
    agentName: String!
    agentTitle: String!
    projectDeploymentId: ID!
    workflowExecutionId: String!
    workflowLabel: String!
}
```

Implementation mirrors `workspaceChatWorkflows` but iterates workspace agents and uses the unfiltered
per-project deployment fetch (the same path `AiAgentFacade#getAgentDeployments` uses) to find enabled
deployments whose workflows carry a chat trigger.

**AI Hub sidebar + page.** New nav item **Agents** (icon `BotIcon`) directly above Workflow Chats →
new full-page route `/automation/ai-hub/agents` (component `AiHubAgents` + `AiHubAgentsList`,
mirroring `WorkflowChats`/`WorkflowChatsList`). Rows are agents (title + BotIcon, grouped flat — one
row per agent chat workflow; the common case is one). Clicking a row runs the same idempotent
`createWorkflowChatAiHubTask` flow with `title = agentTitle` and navigates to the task.

**Mutual exclusion.** Holds by construction: `workspaceChatWorkflows` already excludes agent-backed
projects; `workspaceChatAgents` returns only them.

**/automation/chats.** `ChatsSidebar` additionally queries `workspaceChatAgents` and renders an
"Agents" `LeftSidebarNav` group (same `LeftSidebarNavItem` rows, linking to
`/automation/chats/{workflowExecutionId}`) above the per-project groups.

## Testing / verification

- Client: rename-affected tests updated (`AiHubPersonalAgents*` test files, tasks sidebar tests,
  composer tests); new tests for the home-view split gate and `AiHubAgentsList`; `npm run check`.
- Server: `./gradlew spotlessApply`, compile + affected module `check`; `EnumOrdinalStabilityTest`
  updated for the kind rename; GraphQL codegen re-run (`npx graphql-codegen`) after schema changes.
- Manual: pick each resource kind from the home composer → panel opens instantly; create/edit/delete
  a Task Template end-to-end (fresh tables); More popup; Agents page → chat task; /automation/chats
  Agents group.

## Commits

Grouped per the repo convention (`<ticket> [client -] <description>`), separating: (1) home-view
resource panel, (2) server rename, (3) DB changelog rename, (4) client rename + icon, (5) More popup,
(6) workspaceChatAgents + AI Hub Agents page, (7) /automation/chats Agents group.
