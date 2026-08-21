# Code Workflow AI Hub Copilot (CC-A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Give the AI Hub agent code-workflow capability: a `code_workflow_agent` ask/build subagent owning CRUD, an `openCodeWorkflowTab` open-in-panel tool recording a new `CODE_WORKFLOW_REFERENCED` artifact kind, and the client panel/sidebar wiring — an exact mirror of the shipped custom-component feature (SP-B tools/panel + SP-C subagent).

**Architecture:** Every task clones a named, shipped analog from the custom-component feature on this branch, substituting code-workflow names and the `ProjectCodeWorkflowFacade` (automation-only; embedded arrives in CC-B). CRUD lives behind the subagent; the main agents carry only the delegating tool + the signaling open-tab tool.

**Tech Stack:** Java 25 / Spring AI 2 (`@Tool`, `ToolCallback`, `ChatClient`), React 19 / TS / Zustand, GraphQL codegen.

## Global Constraints

- **Automation-only** (no `embedded-configuration` dependency anywhere in ai-hub).
- **Append-only enums**: `AiHubTaskArtifactKind.CODE_WORKFLOW_REFERENCED` = ordinal 25 (after `CUSTOM_COMPONENT_REFERENCED` 24); `CopilotAgentType.CODE_WORKFLOW_AGENT` appended last.
- **CRUD behind the subagent**: `CodeWorkflowTools`/`ReadCodeWorkflowTools` appear ONLY on the subagent ChatClients — never in the global tool catalogs. `openCodeWorkflowTab` on the main agents (recorder@BUILD, null@ASK).
- The build prompt MUST teach the ProjectHandler polyglot contract with the CW-A hard-won facts: plain-eval completion value exposing MEMBERS — JS bare object literal `({...})`, Python `types.SimpleNamespace(...)`, Ruby core `Struct` (NOT raw dict/hash, NOT OpenStruct — sandbox blocks `require 'ostruct'`); nested workflow/task entries stay raw maps; members `name`(req)/`version`/`description`/`workflows[{name,label,tasks[{name,label,perform}]}]`; `perform` zero-arg callable; project name is LOCKED after create (update with a different name is rejected).
- EE files: Enterprise header + `@version ee`; CE files (ai-copilot-tool): Apache 2.0.
- Client conventions; `npm run check` before client commits; `./gradlew spotlessApply` before server commits; fresh commits, never amend; stage only task files.

## Analog reference table (all shipped on this branch — clone them)

| CC-A deliverable | Shipped analog (grep for it) |
|---|---|
| `CODE_WORKFLOW_REFERENCED` 5 touchpoints | the `CUSTOM_COMPONENT_REFERENCED` commits: `AiHubTaskArtifactKind.java`, `ai-hub-artifact.graphqls`, `tasks.api.ts` `AiHubArtifactKindType`, `AiHubTaskArtifactKindWireFormatTest`, `EnumOrdinalStabilityTest` |
| `CodeWorkflowTools`/`ReadCodeWorkflowTools` | `CustomComponentTools`/`ReadCustomComponentTools` (EE `automation-ai-tool`) + their tests |
| `CodeWorkflowAgentConfiguration` + prompts | `CustomComponentAgentConfiguration` + `prompt_custom_component_ask/build.txt` (EE ai-hub-service resources) |
| `CodeWorkflowAgentToolCallback` + enum + registration | `CustomComponentAgentToolCallback` + `CopilotAgentType.CUSTOM_COMPONENT_AGENT` + `registerCopilotSubAgentToolCallbacks`'s `custom_component_agent` block |
| `OpenCodeWorkflowTabToolCallback` | `OpenCustomComponentTabToolCallback` + test |
| Client store/panel/provider/record/sidebar/switch | the `customComponent` branches in `useAiHubTabsStore.ts`, `AiHubResourcePanel.tsx`, `AiHubRuntimeProvider.tsx`, `useRecordReferencedArtifacts.ts`, `AiHubTasksSidebar.tsx`, `useSwitchTask.ts` + their tests |

---

### Task 1: `CODE_WORKFLOW_REFERENCED` artifact kind (5 touchpoints)

**Files:** the five analog files above + client GraphQL codegen output.
- [ ] Append `CODE_WORKFLOW_REFERENCED` to `AiHubTaskArtifactKind` (Java, last) and the GraphQL enum in `ai-hub-artifact.graphqls`.
- [ ] Extend `AiHubTaskArtifactKindWireFormatTest`'s ordered name array and `EnumOrdinalStabilityTest` (`expected.put("CODE_WORKFLOW_REFERENCED", 25)`).
- [ ] Client: add `'CODE_WORKFLOW_REFERENCED'` to `AiHubArtifactKindType` in `tasks.api.ts` (alphabetical); run `cd client && npx graphql-codegen` so `AiHubTaskArtifactKind.CodeWorkflowReferenced` generates.
- [ ] Run both server tests + `cd client && npm run check`. Commit server + client separately per commit convention if desired, or one commit: `732 Add CODE_WORKFLOW_REFERENCED artifact kind`.

### Task 2: EE `CodeWorkflowTools` + `ReadCodeWorkflowTools`

**Files:** EE `automation-ai-tool` (new two classes + tests); its `build.gradle.kts` (+ dep on EE `:server:ee:libs:automation:automation-configuration:automation-configuration-api`); possibly `ProjectCodeWorkflowFacade` (+Impl, +test) if a list method is missing.
**Interfaces:** Consumes `ProjectCodeWorkflowFacade.createEmptyCodeWorkflow(long workspaceId, String name, Language)/getCodeWorkflowSource(long)/updateCodeWorkflowSource(long, String)`. Produces `@Tool` methods: `createCodeWorkflow(workspaceId, name, language)` (returns created project id+name as text), `updateCodeWorkflowSource(projectId, content)`, `getCodeWorkflowSource(projectId)`, `listCodeWorkflows()`.
- [ ] Clone `CustomComponentTools`/`ReadCustomComponentTools` shape: each `@Tool` wraps the facade call in try/catch → the module's established tool-error pattern; language param maps by name to `CodeWorkflowContainer.Language` (JAVASCRIPT/PYTHON/RUBY only — reject JAVA in the tool with a clear message).
- [ ] `listCodeWorkflows()`: if no facade list method exists, add `List<Project> getCodeWorkflowProjects()` to `ProjectCodeWorkflowFacade`(+Impl via `ProjectCodeWorkflowService`/repository — a `findAll` join → distinct projects; add a focused facade test). Return id/name/language rows as text/JSON like `listCustomComponents` does.
- [ ] Tests mirror `CustomComponentToolsTest`/`ReadCustomComponentToolsTest` (mock facade; success + error branches).
- [ ] `spotlessApply`; module tests green; commit: `732 Add code workflow AI tools`.

### Task 3: Subagent prompts + `CodeWorkflowAgentConfiguration`

**Files:** EE ai-hub-service: `prompt_code_workflow_ask.txt`, `prompt_code_workflow_build.txt`, `CodeWorkflowAgentConfiguration.java`.
**Interfaces:** Produces beans `codeWorkflowAskSubAgentChatClient` (tools: `ReadCodeWorkflowTools`) + `codeWorkflowBuildSubAgentChatClient` (tools: `CodeWorkflowTools` + `ReadCodeWorkflowTools`). No open-tab tool on the subagents.
- [ ] Clone `CustomComponentAgentConfiguration` (same `@ConditionalOnProperty` ai.hub.enabled, `readPrompt` helper, EE header).
- [ ] Ask prompt: read-only Q&A over code workflows (list/get source; never mutate; include project id+name in answers so the parent can open tabs).
- [ ] Build prompt: the full polyglot contract from Global Constraints (verbatim facts: bare `({...})` / `SimpleNamespace` / `Struct`, nested raw maps, member list, zero-arg `perform`, name-lock) + workflow: choose name → `createCodeWorkflow(workspaceId, name, language)` → `updateCodeWorkflowSource(projectId, source)` iterating on compile errors → STOP and return a summary INCLUDING projectId + language + name (parent opens the tab). Never delete projects.
- [ ] `spotlessApply`; `compileJava` green; commit: `732 Add code workflow subagent ChatClients + prompts`.

### Task 4: CE `CodeWorkflowAgentToolCallback` + `CopilotAgentType` + test

**Files:** CE `ai-copilot-tool`: `CodeWorkflowAgentToolCallback.java`, `CopilotAgentType.java` (append `CODE_WORKFLOW_AGENT("code_workflow_agent", false)` last), `CodeWorkflowAgentToolCallbackTest.java`.
- [ ] Clone `CustomComponentAgentToolCallback` verbatim with code-workflow wording (tool `code_workflow_agent`, description: delegate code-workflow requests — whole projects authored as single scripts — to the specialist; returns markdown summary incl. project id/name/language). Same `CurrentAgentContext.callWith(CopilotAgentType.CODE_WORKFLOW_AGENT, ...)`, error arms, input record `{request}`.
- [ ] Clone the full `CustomComponentAgentToolCallbackTest` (13 cases incl. the parameterized upstream-failure sweep).
- [ ] `spotlessApply`; module test green; commit: `732 Add CodeWorkflowAgentToolCallback delegating tool`.

### Task 5: `OpenCodeWorkflowTabToolCallback` + `AiHubConfiguration` wiring + main prompts

**Files:** EE ai-hub-service: new `OpenCodeWorkflowTabToolCallback.java` + test; `AiHubConfiguration.java`; `prompt_ai_hub_ask.txt` + `prompt_ai_hub_build.txt`.
- [ ] Clone `OpenCustomComponentTabToolCallback`: tool `openCodeWorkflowTab`, input `{projectId, language, name}` (all required; blank → toolError), output `{opened, projectId, language, name}`, records `CODE_WORKFLOW_REFERENCED` with artifactId = projectId (recorder nullable). Clone its test.
- [ ] `AiHubConfiguration`: add `toolCallbacks.add(new OpenCodeWorkflowTabToolCallback(null))` @ASK and `(aiHubTaskArtifactRecorder)` @BUILD (next to the custom-component ones); add the two `@Qualifier("codeWorkflowAsk/BuildSubAgentChatClient")` provider params + call-site args; extend `registerCopilotSubAgentToolCallbacks` with a trailing provider param + `new ProgressReportingToolCallback(new CodeWorkflowAgentToolCallback(chatClient), "code_workflow_agent")` block (mirror the custom_component block; ASK site passes ask client, BUILD passes build client — an ask/build swap is a critical defect).
- [ ] Prompts: in the Specialist-subagents bullet lists, add `code_workflow_agent({request})` (delegate all code-workflow work; never author sources directly) and document `openCodeWorkflowTab({projectId, language, name})` (call after the subagent reports a built/changed code workflow).
- [ ] `spotlessApply`; compile + spotbugs + the new test green; commit: `732 Delegate code workflow work to subagent + open tab tool`.

### Task 6: Client — tab kind, panel, runtime provider, artifact recording

**Files:** `useAiHubTabsStore.ts`, `AiHubResourcePanel.tsx`, `AiHubRuntimeProvider.tsx`, `useRecordReferencedArtifacts.ts` + their tests.
- [ ] Store: add `{id, kind: 'codeWorkflow', language, name, projectId}` to `AiHubTabType` + `openCodeWorkflowTab(projectId, language, name)` (dedup by id `codeWorkflow-${projectId}`; mirror `openCustomComponentTab`); exhaustiveness case in `getTabGenericId`.
- [ ] Panel: `codeWorkflow` → `<ProjectCodeWorkflowDetail language={tab.language} projectId={tab.projectId} />` (import from `@/pages/platform/code-workflow/ProjectCodeWorkflowDetail`).
- [ ] Runtime provider: `else if (toolCallName === 'openCodeWorkflowTab')` branch + `validateOpenCodeWorkflowTabResult` (mirror the custom-component branch/validator incl. `surfaceTabOpenFailure`).
- [ ] Record hook: `codeWorkflow: AiHubTaskArtifactKind.CodeWorkflowReferenced` in `KIND_TO_ARTIFACT_KIND` + `case 'codeWorkflow'` in `resolveArtifactKey` (artifactId = projectId).
- [ ] Tests mirror the customComponent cases in each file's test. `npm run check`; commit: `732 client - Open code workflow tabs from AI Hub`.

### Task 7: Client — sidebar + task switching

**Files:** `AiHubTasksSidebar.tsx`, `useSwitchTask.ts` + tests.
- [ ] Sidebar: render `CODE_WORKFLOW_REFERENCED` artifacts (icon `CodeIcon`, label from artifact name), clickable (quick-open `openCodeWorkflowTab` — note: the artifact stores projectId as artifactId and name; language must ride in the artifact name or be refetched — mirror how the customComponent quick-open resolves its inputs; if language isn't recoverable from the artifact, fetch the project (its `codeWorkflowLanguage` field from CW-A) via the existing project query before opening), removable per the custom-component parity.
- [ ] `useSwitchTask`: replay `openCodeWorkflowTab` for `CODE_WORKFLOW_REFERENCED` artifacts (same language-resolution approach).
- [ ] Tests mirror the customComponent sidebar/switch cases. `npm run check`; commit: `732 client - Show code workflow artifacts in AI Hub sidebar`.

### Task 8: Full verification
- [ ] Server: `ai-hub-api`/`ai-hub-service`/`automation-ai-tool`/`ai-copilot-tool` tests + compile green.
- [ ] Client: `npm run check`.
- [ ] Sanity greps: tools ONLY on subagent clients (not catalogs); ask/build routing un-swapped; ordinal 25 pinned; no embedded deps added.

## Self-Review
Spec §1→T1, §2→T2, §3→T3, §4→T4, §5+§6→T5, §7-10→T6, §11→T7, §12→T1(client)+T6, verification→T8. Names consistent: `code_workflow_agent`, `CODE_WORKFLOW_AGENT`, `openCodeWorkflowTab`, `CODE_WORKFLOW_REFERENCED`(25), bean names `codeWorkflowAsk/BuildSubAgentChatClient`, tab kind `codeWorkflow` `{projectId, language, name}` across T5/T6/T7. Risk: T7's language-resolution for quick-open (artifact doesn't store language) — the task states the fallback (fetch project's `codeWorkflowLanguage`); implementer picks the cleaner of the two stated options.
