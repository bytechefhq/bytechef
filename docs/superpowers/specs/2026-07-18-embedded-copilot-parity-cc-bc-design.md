# Embedded Copilot Parity + Code-Workflow In-Editor Copilot (CC-B/C) — Design

**Date:** 2026-07-18
**Status:** Approved (design)
**Initiative:** CW-C copilot coverage. CC-A (AI Hub code workflows) is done. This spec covers the user's three embedded copilot asks: (1) copilot for embedded code workflows, (2) embedded integration workflow-editor copilot on par with the automation project editor, (3) copilot for embedded workflow executions — plus, as a natural completion, an in-editor copilot for **automation** code workflows (the same mechanism, and without it the code-editor surface would route to the wrong agent on both products).
**Area:** new EE `embedded-ai` copilot modules, EE `automation-ai-tool`, EE `CopilotApiController` (ai-copilot-rest), client copilot store/layout/executions sheet.

## Verified architecture (the linchpin)

- The in-editor copilot posts to `/api/platform/internal/ai/chat/{agentId}` served by the EE `CopilotApiController`, which collects **`List<LocalAgent>`** into a map keyed by `agentId` and dispatches `{source}` → `{source}_ask` / `{source}_build` via explicit branches. Any EE-defined `SpringAIAgent` bean joins the registry automatically (precedent: the AI Hub agents).
- The client `Source` enum (`useCopilotStore.ts`) supplies the source key; `useWorkflowLayout.handleCopilotClick` currently hard-codes `Source.WORKFLOW_EDITOR`.
- The embedded Integration editor **already renders the copilot button + global panel** (shared `WorkflowEditorLayout`/`WorkflowRightSidebar`, gated `ai.copilot.enabled && ff_1570`) — but routes to the automation-project-scoped `workflow_editor` agent. The server side is the real gap.
- The CE `CopilotConfiguration` cannot carry EE tools — so every embedded (and automation-EE code-workflow) agent pair is EE-defined, with its own Source key.

## New Sources and agents (all ask+build pairs)

| Source (client + controller branch) | Agent ids | Tools | Module |
|---|---|---|---|
| `WORKFLOW_EDITOR_EMBEDDED` = `workflow_editor_embedded` | `workflow_editor_embedded_ask/build` | new `IntegrationWorkflowTools`/`ReadIntegrationWorkflowTools` (+ shared CE `TaskTools`, `WorkflowValidatorTools`, etc. mirroring the automation workflow-editor agent's set) | EE `embedded-ai-copilot` |
| `CODE_WORKFLOW` = `code_workflow` | `code_workflow_ask/build` | CC-A's `CodeWorkflowTools`/`ReadCodeWorkflowTools` (automation) | EE `automation-ai-tool` (config class added there, gated `bytechef.ai.copilot.enabled`) |
| `CODE_WORKFLOW_EMBEDDED` = `code_workflow_embedded` | `code_workflow_embedded_ask/build` | new `IntegrationCodeWorkflowTools`/`ReadIntegrationCodeWorkflowTools` (wrap CW-B's `IntegrationCodeWorkflowFacade`) | EE `embedded-ai-copilot` |
| `WORKFLOW_EXECUTION_EMBEDDED` = `workflow_execution_embedded` | `workflow_execution_embedded_ask/build` | new `IntegrationWorkflowExecutionTools` (mirror `WorkflowExecutionTools`, wrapping `IntegrationWorkflowExecutionFacade` — identical method surface, platform-generic DTOs) | EE `embedded-ai-copilot` |

`CopilotAgentType` (CE) gains the corresponding ask/build/fallback entries appended (mirroring how `WORKFLOW_EDITOR_ASK/BUILD/WORKFLOW_EDITOR` triples exist), so `CurrentAgentContext` naming stays consistent.

## Server design

1. **New EE module `server/ee/libs/embedded/embedded-ai/embedded-ai-tool`** (mirror `automation-ai-tool`): `IntegrationWorkflowTools`/`ReadIntegrationWorkflowTools` — mirror the automation `ProjectWorkflowTools`/`ReadProjectWorkflowTools` **method-for-method**, re-keyed on integrations (`IntegrationService`, `IntegrationWorkflowService`, the integration workflow facade); `IntegrationCodeWorkflowTools`/`ReadIntegrationCodeWorkflowTools` — mirror CC-A's code-workflow tools against `IntegrationCodeWorkflowFacade` (componentName key, no workspaceId); `IntegrationWorkflowExecutionTools` — mirror `WorkflowExecutionTools` against `IntegrationWorkflowExecutionFacade`. Where an automation tool method has no clean embedded equivalent, omit it and document the omission (do not invent capability).
2. **New EE module `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot`**: a configuration defining the three embedded agent pairs (SpringAIAgent builders from `ai-copilot-service`, `ChatMemory`/`ChatModel`/`SecurityContextRehydrator` wrapping mirroring `CopilotConfiguration`'s bean style), gated `@ConditionalOnProperty(bytechef.ai.copilot.enabled)`, prompts cloned/adapted from the automation analogs (`prompt_workflow_editor_*` with Integration terminology; `prompt_code_workflow_*` content from CC-A adapted to the embedded componentName contract — `componentName`/`componentVersion` members per `IntegrationHandlerPolyglotEngine`, name-lock on componentName; `prompt_workflow_execution_*` adapted). EE headers.
3. **Automation `code_workflow` in-editor agents**: an `AutomationCodeWorkflowCopilotConfiguration` in EE `automation-ai-tool` (or a sibling EE config module if a dependency cycle appears — implementer's call, documented) defining `code_workflow_ask/build` SpringAIAgents with CC-A's tools + the CC-A prompt content (reused/adapted for in-editor use), gated on `bytechef.ai.copilot.enabled`.
4. **`CopilotApiController`** (EE): add the four source dispatch branches (`workflow_editor_embedded`, `code_workflow`, `code_workflow_embedded`, `workflow_execution_embedded`) mirroring the existing ones.

## Client design

5. **`Source` enum** (`useCopilotStore.ts`): add the four entries.
6. **`useWorkflowLayout.handleCopilotClick`**: select source by surface — `integrationId` route param present → (`codeWorkflow` flag ? `CODE_WORKFLOW_EMBEDDED` : `WORKFLOW_EDITOR_EMBEDDED`); else → (`codeWorkflow` ? `CODE_WORKFLOW` : `WORKFLOW_EDITOR`). Context parameters carry the surface ids (`projectId`/`integrationId`, plus `language` for code workflows), mirroring the existing context shape.
7. **Embedded executions sheet** (`client/src/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx` + its hook): mount an inline `CopilotPanel` exactly like the automation `WorkflowExecutionSheet` (local `copilotPanelOpen` state, `setContext({source: Source.WORKFLOW_EXECUTION_EMBEDDED, …})`), gated `ai.copilot.enabled && ff_4077` (same flags as automation for parity).

## Non-goals

- No new visual-editor capabilities beyond tool re-keying (the embedded workflow-editor agent gets the mirrored toolset; omissions documented).
- No AI Hub changes (CC-A covered AI Hub; the AI Hub remains automation-only).
- No CE changes beyond `CopilotAgentType` appends and the client.

## Testing

- Tools: mocked-facade unit tests mirroring their automation analogs (each tool class).
- Agent configs: compile + a bean-shape test if the analog has one; prompts reviewed against the verified contracts (embedded: `componentName` members; automation: `name` members).
- Controller: extend the existing dispatch test (if present) for the four new branches, else a focused test.
- Client: source-selection unit tests for `handleCopilotClick` (4 surface combinations); executions-sheet copilot mount test mirroring the automation sheet's.

## Rollout

All additive; agents appear only when `bytechef.ai.copilot.enabled` (and EE). The embedded editor's existing copilot button becomes *correct* rather than mis-routed. Client feature flags unchanged (`ff_1570` editor, `ff_4077` executions).
