# Code Workflow AI Hub Tools + Subagent + Open-in-Panel (CC-A) — Design

**Date:** 2026-07-18
**Status:** Approved (design)
**Initiative:** CW-C copilot coverage. CC-A = AI Hub (automation) code workflows. Siblings: CC-B = embedded in-editor copilot parity (visual + code), CC-C = embedded workflow-executions copilot.
**Area:** EE ai-hub (`server/ee/libs/ai/ai-hub`), EE `automation-ai-tool`, CE `ai-copilot-tool`, AI Hub client.

## Problem

CW-A gave automation code-backed Projects source editing. The AI Hub agent has no code-workflow capability: no tools, no subagent, no open-in-panel. CC-A mirrors the custom-component treatment (SP-B tools/panel + SP-C subagent, both shipped on this branch) onto code workflows.

## Scope decisions

- **Automation-only.** The AI Hub is an automation product surface; `ai-hub-service` does not depend on `embedded-configuration` and should not (flagged by exploration). Embedded code-workflow copilot arrives in CC-B via the in-editor path.
- **CRUD behind the subagent** (the SP-C architecture, per the user's earlier decision for custom components): a `code_workflow_agent` delegating tool on the main ASK+BUILD agents; the CRUD/read tools live only on the subagent ChatClients. `openCodeWorkflowTab` stays on the main agent (signaling, both sites).
- **Editable panel**: the AI Hub panel renders the existing editable `ProjectCodeWorkflowDetail` (`client/src/pages/platform/code-workflow/`, props `{projectId, language}`) — same "editable" choice the user made for custom components.
- **Ask + build subagent pair** (full skills/custom-component mirror).

## Server design

1. **`AiHubTaskArtifactKind.CODE_WORKFLOW_REFERENCED`** (ordinal 25) appended across all five touchpoints: Java enum, `ai-hub-artifact.graphqls`, client `AiHubArtifactKindType` (`tasks.api.ts`), `AiHubTaskArtifactKindWireFormatTest`, `EnumOrdinalStabilityTest`.
2. **EE tools** in `automation-ai-tool` (add dep on EE `automation-configuration-api`):
   - `CodeWorkflowTools`: `createCodeWorkflow(workspaceId, name, language)` → `ProjectCodeWorkflowFacade.createEmptyCodeWorkflow` (returns id+name); `updateCodeWorkflowSource(projectId, content)` (compile-gated); each wrapping errors like `CustomComponentTools`.
   - `ReadCodeWorkflowTools`: `getCodeWorkflowSource(projectId)`; `listCodeWorkflows()` — projects with a code workflow (via the facade/services; if no list method exists, add `List<Project> getCodeWorkflowProjects()` to `ProjectCodeWorkflowFacade` or derive via the info supplier pattern — implementer maps to the real API).
   - No delete in v1 (deleting a code workflow = deleting the project; out of scope, the main agent's ProjectTools already handles project deletion).
3. **`CodeWorkflowAgentConfiguration`** (EE ai-hub-service, mirror `CustomComponentAgentConfiguration`): `codeWorkflowAskSubAgentChatClient` (read tools) + `codeWorkflowBuildSubAgentChatClient` (CRUD + read), prompts `prompt_code_workflow_ask.txt` / `prompt_code_workflow_build.txt`. The build prompt teaches the **ProjectHandler polyglot contract** (plain-eval completion value; members `name` req/`version`/`description`/`workflows[{name,label,tasks[{name,label,perform}]}]`; **JS bare object literal / Python `types.SimpleNamespace` / Ruby core `Struct` — NOT raw dict/hash/OpenStruct**; nested entries stay raw maps; `perform` zero-arg callable) + the create→update-until-compiles→report-id workflow + name-lock (project name can't change on update).
4. **CE `CodeWorkflowAgentToolCallback`** (ai-copilot-tool, clone of `CustomComponentAgentToolCallback`): tool `code_workflow_agent`, input `{request}`; `CopilotAgentType.CODE_WORKFLOW_AGENT` appended; registered in `registerCopilotSubAgentToolCallbacks` (ASK→ask client, BUILD→build client).
5. **`OpenCodeWorkflowTabToolCallback`** (EE ai-hub-service, clone of `OpenCustomComponentTabToolCallback`): tool `openCodeWorkflowTab`, input `{projectId, language, name}`, records `CODE_WORKFLOW_REFERENCED` (dedup-aware recorder@BUILD, null@ASK); registered on both main agents.
6. **Main prompts** (`prompt_ai_hub_ask/build.txt`): delegate code-workflow authoring/inspection to `code_workflow_agent`; after build/change call `openCodeWorkflowTab({projectId, language, name})`.

## Client design

7. `useAiHubTabsStore`: tab kind `{id, kind: 'codeWorkflow', name, projectId, language}` + `openCodeWorkflowTab(projectId, language, name)` (dedup by id).
8. `AiHubResourcePanel`: `codeWorkflow` → `<ProjectCodeWorkflowDetail projectId language />`.
9. `AiHubRuntimeProvider`: intercept `openCodeWorkflowTab` (+ validator, mirror custom-component branch).
10. `useRecordReferencedArtifacts`: `codeWorkflow → CodeWorkflowReferenced` + `resolveArtifactKey` case.
11. `AiHubTasksSidebar`: render/reopen/removable `CODE_WORKFLOW_REFERENCED` (icon `CodeIcon`); `useSwitchTask` replay.
12. GraphQL codegen regenerated for the new enum value.

## Error handling / testing / rollout

Mirrors the custom-component feature exactly: tool errors wrap facade `ConfigurationException`s; blank-input `toolError` in the open-tab callback; artifact-record failures logged+swallowed. Tests mirror `CustomComponentAgentToolCallbackTest`, `OpenCustomComponentTabToolCallbackTest`, the tools tests, the store/sidebar/record hooks tests, wire-format + ordinal tests. Append-only enums; all additive; CE/EE boundaries: tools + configs EE, delegating callback CE.
