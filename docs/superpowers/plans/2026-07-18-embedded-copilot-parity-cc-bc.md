# Embedded Copilot Parity + Code-Workflow In-Editor Copilot (CC-B/C) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make the embedded Integration editor's copilot correct (integration-scoped agents instead of mis-routed project agents), add in-editor copilot for code workflows on both surfaces, and add copilot to embedded workflow executions — via four new EE-defined ask/build agent pairs with their own Sources.

**Architecture:** `CopilotApiController` (EE) already registries `List<LocalAgent>` by agentId with `{source}`→`_ask`/`_build` dispatch — EE agent beans join automatically. So: a new EE `embedded-ai-tool` module (integration-keyed tool mirrors), a new EE `embedded-ai-copilot` module (three embedded agent pairs + prompts), an automation `code_workflow` agent pair beside CC-A's tools, four controller dispatch branches, four client `Source` entries + surface-based source selection, and an executions-sheet copilot mount.

**Tech Stack:** Java 25 / Spring AI 2 (SpringAIAgent builders from `ai-copilot-service`), React 19 / TS.

## Global Constraints

- All new server modules/classes are EE: Enterprise header + `@version ee`. CE touches ONLY: `CopilotAgentType` appends (append-only, at the end) + nothing else server-side.
- Agent ids exactly: `workflow_editor_embedded_ask/build`, `code_workflow_ask/build`, `code_workflow_embedded_ask/build`, `workflow_execution_embedded_ask/build`. Client Source keys = the base ids. A mismatch anywhere = dead chat.
- Tool mirrors are method-for-method against their named automation analogs; a method with no clean embedded equivalent is OMITTED and documented (never invented).
- Embedded code-workflow prompt facts: `componentName` (req)/`componentVersion`/`version`/`description`/`workflows` members; JS bare `({...})`, Python `types.SimpleNamespace`, Ruby core `Struct` (no OpenStruct); nested raw maps; zero-arg `perform`; componentName LOCKED on update; JS/PY/RUBY only.
- Config gating: `@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")` (NOT ai.hub).
- Client: existing flags stay (`ff_1570` editor button, `ff_4077` executions); conventions as always; `npm run check` green before client commits. `./gradlew spotlessApply` before server commits. Fresh commits; never amend; stage only task files.

## Analogs

| New | Analog (grep) |
|---|---|
| `IntegrationWorkflowTools`/`ReadIntegrationWorkflowTools` | `ProjectWorkflowTools`/`ReadProjectWorkflowTools` (CE `automation-ai-tool`) |
| `IntegrationCodeWorkflowTools`/`ReadIntegrationCodeWorkflowTools` | CC-A `CodeWorkflowTools`/`ReadCodeWorkflowTools` (EE `automation-ai-tool`) |
| `IntegrationWorkflowExecutionTools` | `WorkflowExecutionTools` (CE `automation-ai-tool`) |
| embedded agent pairs + prompts | `workflowEditorAsk/BuildSpringAIAgent`, `workflowExecutionAsk/BuildSpringAIAgent` beans + their prompts in `CopilotConfiguration` / `ai-copilot-service` resources |
| `code_workflow` agent pair | same bean pattern + CC-A's `prompt_code_workflow_ask/build.txt` content |
| Controller branches | the existing `workflow_editor`/`workflow_execution` branches in `CopilotApiController` |
| Executions-sheet copilot | automation `WorkflowExecutionSheet.tsx` + `useWorkflowExecutionSheet.ts` |

---

### Task 1: EE module `embedded-ai-tool` — integration workflow + execution tools

**Files:** new module `server/ee/libs/embedded/embedded-ai/embedded-ai-tool` (settings.gradle.kts registration, build.gradle.kts deps on `embedded-configuration-api`, `embedded-workflow-execution-api`, commons/exception like the analog module) with `IntegrationWorkflowTools`, `ReadIntegrationWorkflowTools`, `IntegrationWorkflowExecutionTools` + error-type enums + tests.
- [ ] Read the three automation analogs fully; map each `@Tool` to the embedded services (`IntegrationService`, `IntegrationWorkflowService`, `IntegrationWorkflowFacade` if present, `IntegrationWorkflowExecutionFacade`). Omit-and-document unmappable methods (e.g. workspace-scoped ones).
- [ ] `IntegrationWorkflowExecutionTools` is a thin re-bind: same tool methods (`getWorkflowExecution(long id)` etc.) against `IntegrationWorkflowExecutionFacade` (identical surface; platform-generic DTOs).
- [ ] Tests mirror the analogs' tests (mock facades/services).
- [ ] `spotlessApply`; module `check` green; commit: `732 Add embedded AI tool module (workflow + execution tools)`.

### Task 2: `embedded-ai-tool` — integration code-workflow tools

**Files:** same module: `IntegrationCodeWorkflowTools` (`createIntegrationCodeWorkflow(componentName, language)` — NO workspaceId, `updateIntegrationCodeWorkflowSource(integrationId, content)`), `ReadIntegrationCodeWorkflowTools` (`getIntegrationCodeWorkflowSource(integrationId)`, `listIntegrationCodeWorkflows()` — add a `getCodeWorkflowIntegrations()` facade/service list method if missing, mirroring CC-A's addition) + tests; dep on `embedded-configuration-api` (has `IntegrationCodeWorkflowFacade`).
- [ ] Clone CC-A's `CodeWorkflowTools`/`ReadCodeWorkflowTools` re-keyed (componentName create; integrationId get/update; JAVA rejected). Distinct tool names (`…IntegrationCodeWorkflow…`) so they can coexist with automation tools in any future shared context.
- [ ] `spotlessApply`; tests green; commit: `732 Add integration code workflow AI tools`.

### Task 3: EE module `embedded-ai-copilot` — three embedded agent pairs + prompts

**Files:** new module `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot` (deps: `embedded-ai-tool`, `ai-copilot-service` (builders/rehydrator), config-api etc. mirroring how `ai-hub-service` consumes ai-copilot): `EmbeddedCopilotConfiguration.java` + 6 prompt files.
- [ ] Clone the `CopilotConfiguration` bean pattern (agent builder, `.agentId(...)`, chatMemory, chatModel, systemMessage via prompt resource, wrapped tools via `SecurityContextRehydrator`, overrideChatClientResolver) for: `workflowEditorEmbeddedAsk/BuildSpringAIAgent` (Read-/IntegrationWorkflowTools + the same shared CE tools the automation editor agents carry — `WorkflowValidatorTools` etc. — check `CopilotConfiguration`'s workflow-editor beans and mirror the shareable ones), `codeWorkflowEmbeddedAsk/BuildSpringAIAgent` (Task 2 tools), `workflowExecutionEmbeddedAsk/BuildSpringAIAgent` (Task 1 execution tools + read tools as the automation execution agents do). Agent ids per Global Constraints. Gate on ai.copilot.enabled. If a needed builder/helper from `CopilotConfiguration` is private/inaccessible, replicate it locally (documented) — do not modify CE ai-copilot-service unless adding a public accessor is unavoidable (then keep it minimal).
- [ ] Prompts: `prompt_workflow_editor_embedded_ask/build.txt` (clone automation workflow-editor prompts, Integration terminology, integration ids); `prompt_code_workflow_embedded_ask/build.txt` (CC-A code-workflow prompt content adapted to the embedded contract per Global Constraints); `prompt_workflow_execution_embedded_ask/build.txt` (clone automation execution prompts, integration terminology).
- [ ] Register module in `settings.gradle.kts` + add to `server-app` (and the EE app that carries copilot — mirror where `automation-ai-tool` is included) so beans load.
- [ ] Append to CE `CopilotAgentType`: `WORKFLOW_EDITOR_EMBEDDED_ASK/BUILD/WORKFLOW_EDITOR_EMBEDDED(fallback)`, `CODE_WORKFLOW_ASK/BUILD/CODE_WORKFLOW(fallback)`, `CODE_WORKFLOW_EMBEDDED_ASK/BUILD/CODE_WORKFLOW_EMBEDDED(fallback)`, `WORKFLOW_EXECUTION_EMBEDDED_ASK/BUILD/WORKFLOW_EXECUTION_EMBEDDED(fallback)` — mirroring the existing triple pattern, appended at the end.
- [ ] `spotlessApply`; compile + boot-sanity (module check) green; commit: `732 Add embedded copilot agents module`.

### Task 4: Automation `code_workflow` in-editor agent pair

**Files:** EE `automation-ai-tool`: `AutomationCodeWorkflowCopilotConfiguration.java` + `prompt_code_workflow_ask/build.txt` copies (or resource-reference) — agents `code_workflow_ask/build` with CC-A's `CodeWorkflowTools`/`ReadCodeWorkflowTools`; dep on `ai-copilot-service` added. If that dep creates a cycle, create sibling module `automation-ai-copilot` instead (documented).
- [ ] Clone the Task 3 bean pattern; gate ai.copilot.enabled; agent ids `code_workflow_ask`/`code_workflow_build`.
- [ ] The prompts may reuse CC-A's ai-hub prompt files' content — but these agents serve the in-editor surface: copy the files into this module's resources (a served agent must not depend on ai-hub-service resources).
- [ ] `spotlessApply`; compile green; commit: `732 Add automation code workflow in-editor copilot agents`.

### Task 5: `CopilotApiController` dispatch branches

**Files:** EE `ai-copilot-rest` `CopilotApiController.java` (+ its test if present).
- [ ] Add four branches mirroring the existing ones: `workflow_editor_embedded`, `code_workflow`, `code_workflow_embedded`, `workflow_execution_embedded` → `_ask`/`_build` by the same mode condition the siblings use.
- [ ] Extend/add the dispatch test. `spotlessApply`; test green; commit: `732 Route embedded and code workflow copilot sources`.

### Task 6: Client — Source entries + surface-based selection

**Files:** `client/src/shared/components/copilot/stores/useCopilotStore.ts` (Source enum), `client/src/pages/platform/workflow-editor/hooks/useWorkflowLayout.ts` (`handleCopilotClick`), tests.
- [ ] Add `CODE_WORKFLOW = 'code_workflow'`, `CODE_WORKFLOW_EMBEDDED = 'code_workflow_embedded'`, `WORKFLOW_EDITOR_EMBEDDED = 'workflow_editor_embedded'`, `WORKFLOW_EXECUTION_EMBEDDED = 'workflow_execution_embedded'` (sorted per file convention).
- [ ] `handleCopilotClick`: read `integrationId`/`projectId` route params + the shared `codeWorkflow`/`codeWorkflowLanguage` Context (Task 6 of CW-A threaded it): source = integration ? (codeWorkflow ? CODE_WORKFLOW_EMBEDDED : WORKFLOW_EDITOR_EMBEDDED) : (codeWorkflow ? CODE_WORKFLOW : WORKFLOW_EDITOR). Context `parameters` carry the surface ids (+ `language` when code-backed), preserving the existing parameter shape otherwise.
- [ ] Tests: 4 combinations select the right source + params. `npm run check`; commit: `732 client - Route copilot by surface (embedded + code workflows)`.

### Task 7: Client — embedded executions sheet copilot

**Files:** `client/src/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet.tsx` + its hook + test.
- [ ] Mirror the automation `WorkflowExecutionSheet`: local `copilotPanelOpen` state, copilot toggle button (gated `ai.copilot.enabled && ff_4077`), inline `<CopilotPanel />`, hook sets `setContext({source: Source.WORKFLOW_EXECUTION_EMBEDDED, parameters: {...execution ids as the automation hook does}})`.
- [ ] Test mirrors the automation sheet's copilot test (if none exists, add a focused mount/gating test). `npm run check`; commit: `732 client - Add copilot to embedded workflow executions`.

### Task 8: Full verification
- [ ] Server: new modules `check`; `ai-copilot-rest` test; grep agent-id ↔ Source ↔ controller-branch consistency (all four).
- [ ] Client: `npm run check`.
- [ ] Sanity: no CE module gained an EE dep; `CopilotAgentType` appends only at the end.

## Self-Review
Spec table rows ↔ T1/T2 (tools), T3/T4 (agents+prompts+enum), T5 (controller), T6/T7 (client). Agent-id naming consistent across T3/T4/T5/T6 (the linchpin — verified strings listed in Global Constraints). Embedded prompt contract facts restated. Risks: T3 is the heavyweight (new module + bean-pattern replication + accessibility of CopilotConfiguration helpers — fallback documented); T1's method-for-method mapping may hit unmappable workspace-scoped methods (omit-and-document rule); T6's context-parameter shape must match what the agents expect (mirror existing shape).
