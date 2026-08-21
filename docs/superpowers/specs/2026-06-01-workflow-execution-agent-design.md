# Workflow Execution Agent — Design Spec

**Date:** 2026-06-01
**Branch:** `0_732`
**Status:** Approved (pending spec review)
**Author:** Ivica Cardic

## 1. Goal

Introduce a dedicated **Workflow Execution** AI specialist that is the single owner of
"inspect / diagnose / fix a workflow execution" behaviour, reachable from **two** entry points:

1. The **Copilot panel** inside the `WorkflowExecutionDetails` sheet (client `Source.WORKFLOW_EXECUTION`).
2. **AI Hub**, which delegates execution-related turns to the same specialist as a nested tool.

Today there is no such specialist. The execution sheet's Copilot panel borrows
`Source.WORKFLOW_EDITOR`, and the only execution tool that exists (`listWorkflowExecutions`) lives
inside the AI Hub module and returns summaries only — there is no tool that inspects a single
execution's task-level detail. This spec consolidates execution tooling into the shared
`mcp-tool-automation` module and adds the specialist following the **existing** Copilot
"specialist-as-tool" pattern.

### Capability scope

- **ASK mode** — read-only diagnosis: list executions, inspect a single execution (job status,
  trigger execution, task executions with their inputs/outputs/errors), explain failures.
- **BUILD mode** — everything ASK does, plus the ability to propose/apply a fix to the workflow
  definition by reusing the existing workflow-editor write tools (`ProjectWorkflowTools`,
  `ScriptTools`).

### Out of scope

- `runChatWorkflow` / `listChatWorkflows` stay in AI Hub. They fire chat-eligible workflows and emit
  `WORKFLOW_EXECUTION_STARTED` artifacts into the AI Hub task timeline — chat-firing concerns, not
  execution diagnosis.
- `AiHubRoutingAgent` task-kind routing is **not** touched. That routing exists for `WORKFLOW_CHAT`
  (webhook bridge) and is unrelated to specialist delegation.

## 2. Background — the pattern we are mirroring

AI Hub already consumes every Copilot specialist (workflow editor, code editor, cluster element,
skills, converter). The mechanism, confirmed in code, decouples the two runtimes with **no
compile-time module edge** between `ai-copilot-service` and the AI Hub modules:

1. **`ai-copilot-service` / `CopilotConfiguration`** defines a *stateless* `ChatClient` bean per
   specialist (e.g. `workflowEditorAskSubAgentChatClient`, `CopilotConfiguration.java:334`) carrying
   the specialist's system prompt + tool catalog, **no `ChatMemory`**.
2. **`platform-ai-hub-service`** has a hand-rolled `*AgentToolCallback implements ToolCallback`
   (e.g. `WorkflowEditorAgentToolCallback`) that wraps a plain `ChatClient` and exposes a single tool
   (`workflow_editor_agent`) taking a `request` string. The specialist runs in an isolated context;
   the parent agent sees only the synthesised result. It forwards `ToolContext` and wraps the call in
   `CurrentAgentContext.callWith(Agent.<NAME>, parentAgent, ...)`.
3. **`automation-ai-hub-service` / `AiHubConfiguration.registerCopilotSubAgentToolCallbacks`** injects
   the ChatClient via `@Qualifier("...") ObjectProvider<ChatClient>` and `.ifAvailable(...)`. Because
   it is `ObjectProvider<ChatClient>` resolved by bean name, AI Hub references only Spring AI's
   `ChatClient` type — never an ai-copilot class. The monolith shares one Spring context, so the
   named bean resolves at runtime; if Copilot is disabled the provider is empty and the tool silently
   skips. `platform-ai-hub-service/build.gradle.kts` has no `copilot` dependency.

The new Workflow Execution specialist follows this template verbatim.

### Relevant existing files

| Concern | File |
| --- | --- |
| Copilot specialist base | `ag-ui/integrations/spring-ai/.../SpringAIAgent.java` |
| Template specialist | `server/ee/libs/ai/ai-copilot/ai-copilot-service/.../agent/CodeEditorSpringAIAgent.java` |
| Copilot bean wiring | `server/ee/libs/ai/ai-copilot/ai-copilot-service/.../config/CopilotConfiguration.java` |
| Copilot source enum | `server/ee/libs/ai/ai-copilot/ai-copilot-api/.../util/Source.java` |
| Copilot router | `server/ee/libs/ai/ai-copilot/ai-copilot-rest/.../web/rest/CopilotApiController.java` |
| Template agent-as-tool | `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/.../tool/WorkflowEditorAgentToolCallback.java` |
| AI Hub specialist wiring | `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/.../config/AiHubConfiguration.java` (`registerCopilotSubAgentToolCallbacks`) |
| Old execution tool (to remove) | `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/.../tool/ListWorkflowExecutionsToolCallback.java` |
| Execution facade | `server/libs/automation/automation-workflow/automation-workflow-execution/automation-workflow-execution-api/.../facade/ProjectWorkflowExecutionFacade.java` |
| MCP automation tools | `server/libs/ai/mcp/mcp-tool/mcp-tool-automation/.../ProjectTools.java` (and siblings) |
| Execution sheet | `client/src/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx` |
| Sheet copilot hook | `client/.../workflow-execution-sheet/hooks/useWorkflowExecutionSheet.ts` |
| Copilot store | `client/src/shared/components/copilot/stores/useCopilotStore.ts` |

## 3. Architecture — 5 layers

```
┌─ Client ────────────────────────────────────────────────────────────────┐
│  WorkflowExecutionSheet → CopilotPanel(source = WORKFLOW_EXECUTION)       │
│       │  POST /api/platform/internal/ai/chat/workflow_execution           │
│  AI Hub chat (unchanged client) ──► parent ai_hub agent                   │
└───────┼───────────────────────────────────────────────────────────┬──────┘
        ▼                                                             ▼
┌─ ai-copilot-service (EE) ───────────────┐         ┌─ platform-ai-hub-service (EE) ──────────┐
│ WorkflowExecutionSpringAIAgent          │         │ WorkflowExecutionAgentToolCallback       │
│   beans: workflow_execution_ask/build   │◄───────►│   tool: "workflow_execution_agent"       │
│   → localAgentMap (serves sheet)        │  wraps  │   wraps ChatClient (by bean name)        │
│ workflowExecution{Ask,Build}SubAgent-   │  bean   └──────────────────┬───────────────────────┘
│   ChatClient (stateless, for AI Hub)    │                            │ registered via
└───────────────────┬─────────────────────┘                            │ ObjectProvider in
                    │ tools                                            │ AiHubConfiguration
                    ▼                                                  ▼
┌─ mcp-tool-automation (CE) ──────────────────────────────────────────────┐
│ WorkflowExecutionTools (@Component, @Tool)                               │
│   getWorkflowExecution(id)  •  listWorkflowExecutions(...)               │
│        └─► ProjectWorkflowExecutionFacade (automation-workflow-execution)│
└──────────────────────────────────────────────────────────────────────────┘
```

### Layer 1 — Tools in `mcp-tool-automation` (CE)

New class `WorkflowExecutionTools` (`@Component`, `@Tool` methods), following the `ProjectTools`
convention (record DTOs, typed `*ToolErrorType`, SLF4J debug logging):

- **`getWorkflowExecution(long workflowExecutionId)`** — wraps
  `ProjectWorkflowExecutionFacade.getWorkflowExecution(id)`. Returns a record carrying:
  job status, start/end dates, error; trigger execution summary; and an ordered list of task
  executions each with name, component/type, status, input (where available), output, and error.
  **Context-free** — the id is sufficient; the facade method takes no workspace/env. This is the core
  capability the sheet needs and which does not exist today.
- **`listWorkflowExecutions(jobStatus?, workflowId?)`** — relocated from the EE
  `ListWorkflowExecutionsToolCallback`. Wraps `ProjectWorkflowExecutionFacade.getWorkflowExecutions(...)`
  (first page, summaries: id, workflowLabel, projectName, status, startDate, endDate). Requires
  workspace + environment (see §4 Workspace/environment scoping).

Build change: add `implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-api"))`
to `mcp-tool-automation/build.gradle.kts`.

Read-only variant: follow the existing `ReadProjectTools` delegation idiom only if a read-only
catalog split is needed; otherwise both methods are inherently read-only and a single class suffices.

Removal: delete `ListWorkflowExecutionsToolCallback` from `automation-ai-hub-service` and its two
direct registrations in `AiHubConfiguration` (the parent AI Hub agent now reaches execution data via
the `workflow_execution_agent` delegation tool — single source of truth).

### Layer 2 — Copilot specialist in `ai-copilot-service` (EE)

- **`Source.WORKFLOW_EXECUTION`** added to `ai-copilot-api/.../util/Source.java`.
- **`WorkflowExecutionSpringAIAgent extends SpringAIAgent`** modelled on `CodeEditorSpringAIAgent`:
  - `createSystemMessage` injects the in-context `workflowExecutionId` / `workflowId` (from agent
    `state`) so the specialist knows which run is on screen without an extra tool call.
  - `resolveChatClient` supports runtime LLM override via `OverrideChatClientResolver`
    (`CopilotChatClientResolver`), matching the other editor specialists.
- **`CopilotConfiguration` beans:**
  - `workflowExecutionAskSpringAIAgent` (agentId `workflow_execution_ask`) — tools:
    `WorkflowExecutionTools` + `ReadProjectWorkflowTools` (+ optional `FirecrawlTools`). Auto-discovered
    into `localAgentMap`; serves the sheet's ASK panel.
  - `workflowExecutionBuildSpringAIAgent` (agentId `workflow_execution_build`) — tools:
    `WorkflowExecutionTools` + `ProjectWorkflowTools` + `ScriptTools`. Serves the sheet's BUILD panel.
  - `workflowExecutionAskSubAgentChatClient` / `workflowExecutionBuildSubAgentChatClient` — stateless
    `ChatClient` beans (same prompt + tools, **no `ChatMemory`**) for AI Hub delegation.
  - System prompt resources: `prompt_workflow_execution_ask.txt`, `prompt_workflow_execution_build.txt`.

### Layer 3 — AI Hub delegation (EE)

- **`WorkflowExecutionAgentToolCallback implements ToolCallback`** in `platform-ai-hub-service`,
  copied from `WorkflowEditorAgentToolCallback`: tool name `workflow_execution_agent`, input `{request}`,
  forwards `ToolContext`, wraps in `CurrentAgentContext.callWith(Agent.WORKFLOW_EXECUTION_AGENT, ...)`.
- Add **`Agent.WORKFLOW_EXECUTION_AGENT`** to the `Agent` enum in `platform-ai-hub` usage.
- **`AiHubConfiguration.registerCopilotSubAgentToolCallbacks`**: add two `@Qualifier` +
  `ObjectProvider<ChatClient>` parameters (`workflowExecutionAskSubAgentChatClient` for the ASK agent,
  `workflowExecutionBuildSubAgentChatClient` for the BUILD agent) and register
  `new ProgressReportingToolCallback(new WorkflowExecutionAgentToolCallback(chatClient), "workflow_execution_agent")`
  via `.ifAvailable(...)`. No new module dependency edge.

### Layer 4 — Client

- Add `WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION'` to the `Source` enum in `useCopilotStore.ts`.
- `useWorkflowExecutionSheet.ts`: change `handleCopilotClick` to set
  `source: Source.WORKFLOW_EXECUTION` and `parameters: {workflowExecutionId, workflowId}` (replacing
  the borrowed `Source.WORKFLOW_EDITOR`). Register a state contributor so the current execution id is
  always present on the turn state. `CopilotRuntimeProvider` already posts to
  `/api/platform/internal/ai/chat/{source}` → `/workflow_execution`.
- AI Hub client is unchanged — delegation is entirely server-side.

### Layer 5 — Tests

- **Server**
  - `WorkflowExecutionToolsTest` — `@Tool` methods over a mocked `ProjectWorkflowExecutionFacade`
    (detail mapping, list mapping, error → typed `*ToolErrorType`). Class name ends `Test`, no `Impl`.
  - Copilot wiring smoke test: `workflow_execution_ask` / `workflow_execution_build` beans present and
    land in `localAgentMap`.
  - `AiHubConfiguration` gating test: `workflow_execution_agent` callback registered when the
    SubAgent `ChatClient` bean is present, skipped when absent (mirrors existing specialist gating).
  - `WorkflowExecutionAgentToolCallbackTest` — delegates to the wrapped `ChatClient`, forwards
    context, handles null/blank request (mirrors `WorkflowEditorAgentToolCallback` tests).
  - If `Source` / `Agent` enums are persisted as ordinals anywhere, append at the end and respect the
    existing ordinal-stability test convention.
- **Client**
  - Update `WorkflowExecutionDetail` / sheet tests for the new `Source`.
  - Test that opening the panel sets `Source.WORKFLOW_EXECUTION` and contributes the execution id.

## 4. Key decisions & rationale

- **Mirror the specialist-as-tool pattern, not an `AiHubRoutingAgent` edge.** The existing pattern
  already gives "one specialist, two entry points" with zero module coupling. Adding an
  `ai-hub → copilot` dependency and a new task-kind would duplicate a solved problem and conflate
  diagnosis with `WORKFLOW_CHAT` webhook routing.
- **Tools live in CE (`mcp-tool-automation`).** The execution facade is a `server/libs` (CE) type, so
  the tools have no EE dependency and are reusable by any agent. This is also where every other
  automation `@Tool` already lives.
- **Detail tool is workspace-scoped (IDOR guard).** Originally specced as "context-free"
  (`getWorkflowExecution(id)` taking only the id). An automated security review flagged this as a
  HIGH IDOR: the LLM supplies the id, so a hallucinated or prompt-injected id could read another
  workspace's run. Revised: the tool now also reads `workspaceId` from `ToolContext` and fails closed
  (returns a "not found" error) unless the execution's `project().getWorkspaceId()` matches. Both
  entry points already supply the workspace on `ToolContext` (AI Hub via `AiHubToolInvocationContext`;
  Copilot via the specialist's `toolContext()` override fed by the client `parameters.workspaceId`).
  Consequence: the client sheet MUST contribute `workspaceId` (Plan Task 15).

### Workspace/environment scoping (the one new bit of plumbing)

`listWorkflowExecutions` needs `workspaceId` + `environmentId`; `getWorkflowExecution(id)` does not.
The existing EE tool reads these from `AiHubToolInvocationContext` keys on `ToolContext`, but that
class is EE and must not be referenced from CE `mcp-tool-automation`.

**Decision:** `WorkflowExecutionTools.listWorkflowExecutions` reads workspace/env from `ToolContext`
using **CE-owned key constants** declared in `mcp-tool-automation` (or an existing CE context-key
holder). Both runtimes populate them:

- AI Hub already serialises workspace/env onto `ToolContext` (via `AiHubToolInvocationContext`); its
  keys are aligned to (or bridged into) the CE constants.
- The Copilot side populates the same keys from agent `state` in the specialist's `toolContext(...)`
  override (the Copilot runtime currently only forwards `allowedComponentNames`).

If aligning key names proves invasive, the fallback is to keep `listWorkflowExecutions` resolving
workspace/env from ambient `TenantContext`/current-user context; this is acceptable because the
high-value sheet path (`getWorkflowExecution`) is unaffected. Final choice to be locked in the
implementation plan.

## 5. Affected modules summary

| Module | Change |
| --- | --- |
| `server/libs/ai/mcp/mcp-tool/mcp-tool-automation` | New `WorkflowExecutionTools` + DTOs + error type; new build dep on `automation-workflow-execution-api` |
| `server/ee/libs/ai/ai-copilot/ai-copilot-api` | `Source.WORKFLOW_EXECUTION` |
| `server/ee/libs/ai/ai-copilot/ai-copilot-service` | `WorkflowExecutionSpringAIAgent`; 2 agent beans + 2 SubAgent ChatClient beans; 2 prompt resources |
| `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service` | `WorkflowExecutionAgentToolCallback`; `Agent.WORKFLOW_EXECUTION_AGENT` |
| `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service` | Register `workflow_execution_agent` in `registerCopilotSubAgentToolCallbacks`; remove `ListWorkflowExecutionsToolCallback` + its registrations |
| `client/src/shared/components/copilot` | `Source.WORKFLOW_EXECUTION` |
| `client/src/pages/automation/workflow-executions/.../workflow-execution-sheet` | Repoint Copilot panel to `WORKFLOW_EXECUTION`; contribute execution id |

## 6. Open questions for the plan

1. Final workspace/env resolution mechanism for `listWorkflowExecutions` (CE `ToolContext` key
   constants vs. ambient context) — see §4.
2. Whether the detail DTO should inline task outputs or expose a separate
   `getWorkflowExecutionTaskOutput(executionId, taskId)` tool to bound payload size for large runs.
3. Whether to keep a thin read-only catalog split (`ReadWorkflowExecutionTools`) for symmetry with
   `ReadProjectTools`, or rely on the methods being inherently read-only.
