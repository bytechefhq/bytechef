# Phase 3 — Manager-to-Slice Conversions — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Copilot opens from the Deployments, MCP Servers and API Collections listing pages, scoped to an agent that can manage those resources — without changing the tool names AI Hub and external MCP clients already depend on.

**Architecture:** Three manager sub-agents (`deployment_manager`, `mcp_manager`, `api_collection_manager`) each gain a *panel surface*: a `Source` value, an ask/build `LocalAgent` pair, and a prompt pair, following `ProjectAgentConfiguration`. Their existing delegate wiring into AI Hub and the management MCP server is left **untouched**. Each manager's tool set is split into read-only and write factories so the ASK agent is genuinely read-only.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, AG-UI; React 19, TypeScript 5.9, Zustand, TanStack Query, Vitest.

## Global Constraints

- Worktree: `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase3`, branch `claude/copilot-phase3-manager-slices`, cut from **phase 2's branch** (`claude/copilot-phase2-project-slice`) at `55eee64ff6c`, because this phase depends on phase 2's convention-based controller dispatch. Phases 2 and 3 land as one stack.
- **0_732 is rewritten frequently.** Never `git rebase 0_732` — the branch point stops being an ancestor and the rebase replays other people's commits. Always `git rebase --onto <target> <original-branch-point>`, and back up the ref first.
- **Verification runs module-scoped `check`, NOT `test`.** Phase 2 ran only `test` for six consecutive tasks and shipped a SpotBugs failure that sat undetected from task 2 until the final fix wave. `check` includes checkstyle, PMD and SpotBugs.
- Server: Apache 2.0 header under `server/libs/`, ByteChef Enterprise header **and** `@version ee` under `server/ee/` (Spotless selects by tag content, not path). `@author Ivica Cardic`. One blank line before control statements except right after an opening `{`; no blank line before a class's closing `}`. Test methods camelCase, no underscores. No `TODO:` comments.
- **A test class whose field initializer can throw must be declared `final`** — otherwise SpotBugs `CT_CONSTRUCTOR_THROW` fails the build. Precedent: `ProjectAgentConfigurationTest`, `MultiIssuerJwtDecoderTest`.
- Client: object keys alphabetical (`sort-keys` — `--fix` does NOT repair it); named imports alphabetical inside `{}`; interfaces ending `I`/`Props`; descriptive names including arrow-function parameters; `twMerge` not `cn()`; Lucide icons with `Icon` suffix; hook ordering with `useEffect`s last; header action rows use `gap-1`.
- Commit prefix `---` (server) / `--- client - ` (client). **No ticket numbers** — do not take one from any memory or context file; a phase 2 task nearly committed a fabricated `5504`.
- Stage explicit paths. Never `git add -A`.

## The load-bearing design decision

`ManagerSubAgentToolCallback.getToolDefinition()` derives the tool name **solely** from `agentType.key()`. Two consequences:

1. **Tool-name stability is structural.** As long as every factory keeps passing the same `ManagerAgentType` constant, `deployment_manager` / `mcp_manager` / `api_collection_manager` cannot drift. Do not add ask/build variants to `ManagerAgentType` — that would change the public names.
2. **Ask and build delegates must never be registered on the same parent agent** — they would claim one tool name twice.

Therefore **the ask/build split is a panel-surface concern only.** The panel gets two distinct `LocalAgent` beans (`<source>_ask`, `<source>_build`) with distinct agent ids. AI Hub and the management MCP server keep exactly **one** delegate per manager, backed by the BUILD `ChatClient`, wired exactly as today — the BUILD tool set already contains the reads, so nothing is lost. **This phase does not modify `AiHubConfiguration`, `ManagerMcpContributorConfiguration`, or `ApiCollectionManagerMcpContributorConfiguration`.** If a task finds itself editing one of those, it has taken a wrong turn.

## Read/write tool split (established by exploration — do not re-derive)

| Manager | Read-only | Write |
|---|---|---|
| deployment | `listProjectDeployments` | `createProjectDeployment`, `updateProjectDeployment`, `deleteProjectDeployment` (destructive), `rollbackProjectDeployment` (destructive), `toggleProjectDeployment`, `promoteWorkflow` |
| mcp | `listMcpServers`, `listMcpProjectWorkflows` | `createMcpServer`, `updateMcpServer`, `createMcpProject`, `cloneMcpProject`, `updateMcpProjectWorkflowParameters` |
| api collection | `listApiCollections` | `createApiCollection`, `cloneApiCollection` |

Two gotchas:
- `ListMcpProjectWorkflowsToolCallback` needs four services (`McpProjectService`, `McpProjectWorkflowService`, `ProjectDeploymentWorkflowService`, `WorkflowService`), so an mcp read factory cannot be narrowed to the two facades its `@ConditionalOnBean` gates on.
- `TOOL_NAME` visibility is inconsistent: `private static final` on `ListMcpServersToolCallback` and `ListApiCollectionsToolCallback`, package-private on the other 15. Widen only if a test genuinely needs it, and never reference the EE `ListApiCollectionsToolCallback.TOOL_NAME` from CE.

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `.../ai-copilot-api/.../util/Source.java` | add `PROJECT_DEPLOYMENT`, `MCP_SERVER`, `API_COLLECTION` | 1 |
| `.../ai-copilot-tool/.../tool/CopilotAgentType.java` | ask/build/coarse constants for all three | 1 |
| `.../automation-ai-tool/.../ProjectDeploymentToolCallbacksFactory.java` | NEW — read/write split | 2 |
| `.../automation-ai-tool/.../McpServerToolCallbacksFactory.java` | NEW — read/write split | 3 |
| `server/ee/.../ee/automation/ai/tool/ApiCollectionToolCallbacksFactory.java` | NEW — read/write split (EE) | 4 |
| `.../ai-copilot-service/.../agent/ManagerSliceSpringAIAgent.java` | NEW — one agent class reused by all three | 2 |
| `.../ai-copilot-service/.../config/ProjectDeploymentAgentConfiguration.java` + prompt pair | panel agents | 2 |
| `.../ai-copilot-service/.../config/McpServerAgentConfiguration.java` + prompt pair | panel agents | 3 |
| `server/ee/.../automation-ai-copilot/.../config/ApiCollectionAgentConfiguration.java` + prompt pair | panel agents (EE) | 4 |
| `client/src/shared/components/copilot/stores/useCopilotStore.ts` | three new `Source` values | 5 |
| `client/src/pages/automation/project-deployments/ProjectDeployments.tsx` | button + post-turn | 5 |
| `client/src/pages/automation/mcp-servers/McpServers.tsx` | button + post-turn | 5 |
| `client/src/ee/pages/automation/api-platform/api-collections/ApiCollections.tsx` | button + post-turn | 6 |

---

### Task 1: Source values, agent types, and the shared agent class

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/ManagerSliceSpringAIAgent.java`
- Test: `.../agent/ManagerSliceSpringAIAgentTest.java`

**Interfaces produced:** `Source.PROJECT_DEPLOYMENT`, `Source.MCP_SERVER`, `Source.API_COLLECTION`; nine `CopilotAgentType` constants; `ManagerSliceSpringAIAgent.builder()` with the same builder surface as `ProjectSpringAIAgent`.

- [ ] **Step 1: Add the enum constants.** Append the three `Source` values to the existing comma-separated declaration. Add to `CopilotAgentType`, matching the file's `(key, fallback)` pattern and moving the semicolon:

```java
    PROJECT_DEPLOYMENT_ASK("project_deployment_ask", false),
    PROJECT_DEPLOYMENT_BUILD("project_deployment_build", false),
    PROJECT_DEPLOYMENT("project_deployment", true),
    MCP_SERVER_ASK("mcp_server_ask", false),
    MCP_SERVER_BUILD("mcp_server_build", false),
    MCP_SERVER("mcp_server", true),
    API_COLLECTION_ASK("api_collection_ask", false),
    API_COLLECTION_BUILD("api_collection_build", false),
    API_COLLECTION("api_collection", true);
```

- [ ] **Step 2: Write the failing test** for `ManagerSliceSpringAIAgent`. It is a plain domain agent — copy `DataTableSpringAIAgent`'s shape (NOT `ProjectSpringAIAgent`'s, which adds scope/intent handling these three do not need). Assert it builds with a given agent id and that `createSystemMessage` includes the supplied system message and the `State:` block. Declare the test class `final` (SpotBugs).

- [ ] **Step 3: Run it, capture real red.**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase3 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ai.copilot.agent.ManagerSliceSpringAIAgentTest" > /tmp/p3-t1-red.log 2>&1; echo "exit=$?"; grep -E "BUILD SUCCESSFUL|BUILD FAILED|error:|cannot find symbol" /tmp/p3-t1-red.log | head -8
```

- [ ] **Step 4: Create `ManagerSliceSpringAIAgent`** as a verbatim copy of `DataTableSpringAIAgent` with the three class-name occurrences renamed. One shared class serves all three slices — do not create three near-identical agent classes.

- [ ] **Step 5: Green, then `check`, then commit.**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase3 && ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check :server:libs:ai:ai-copilot:ai-copilot-api:check :server:libs:ai:ai-copilot:ai-copilot-tool:check > /tmp/p3-t1-check.log 2>&1; echo "exit=$?"; grep -E "BUILD SUCCESSFUL|BUILD FAILED|^> Task .* FAILED" /tmp/p3-t1-check.log
```

Commit: `--- Add the manager slice source values, agent types and shared agent`

---

### Task 2: Deployment slice

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ProjectDeploymentToolCallbacksFactory.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_project_deployment_ask.txt` and `prompt_project_deployment_build.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ProjectDeploymentAgentConfiguration.java`
- Test: `.../config/ProjectDeploymentAgentConfigurationTest.java`, `.../automation/ai/tool/ProjectDeploymentToolCallbacksFactoryTest.java`

**Interfaces:** consumes Task 1's enums and `ManagerSliceSpringAIAgent`. Produces `project_deployment_ask` / `project_deployment_build` beans and a `ProjectDeploymentToolCallbacksFactory` with `readToolCallbacks()` / `writeToolCallbacks()`, mirroring `DataTableToolCallbacksFactory`.

- [ ] **Step 1: Write the factory test first** — `readToolCallbacks()` returns exactly `[listProjectDeployments]`; `writeToolCallbacks()` returns all seven (writes include the reads, matching how every existing manager client is composed). Run it, capture red.

- [ ] **Step 2: Create the factory.** Constructor takes `ProjectDeploymentFacade`. Read `DataTableToolCallbacksFactory` for the established shape.

- [ ] **Step 3: Write the prompt pair.** Base them on `prompt_deployment_manager.txt`, which already contains the right policy — including the confirm-before-destroy rule for `deleteProjectDeployment` and `rollbackProjectDeployment`. Two adaptations: the manager prompt is written for a *delegated* subagent ("the parent's request", "return a short status summary"); the panel prompts talk to the user directly. The ASK prompt must state it cannot modify anything and offers only `listProjectDeployments`.

  **Do not name `askUserQuestion` in either prompt** unless you register it — the panel agents do not, and naming an unregistered tool kills the turn with "No ToolCallback found". Phrase confirmation as plain-text-and-wait, matching `prompt_data_table_build.txt` and `prompt_project_build.txt`.

- [ ] **Step 4: Write the configuration test, then the configuration.** Model on `ProjectAgentConfiguration`: field-level `@Value`, private `state`, package-private `askToolCallbacks`/`buildToolCallbacks` helpers so the test can assert resolved tool names, `RehydrateContextToolCallback` wrapping, and the gate.

  **Gate on `@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")`** — matching `ProjectAgentConfiguration`. Do NOT use the copilot-or-hub OR-gate: phase 2's review showed that lets a slice register while beans it depends on are absent.

  The ASK agent must be genuinely read-only — assert in the test that no write tool name appears.

- [ ] **Step 5: `check` both modules, then commit.** `--- Add the project deployment copilot slice`

---

### Task 3: MCP server slice

Same shape as Task 2. Files: `McpServerToolCallbacksFactory` (CE `automation-ai-tool`), `prompt_mcp_server_ask/build.txt`, `McpServerAgentConfiguration`, plus tests.

- [ ] **Step 1: factory test → factory.** Read set `[listMcpServers, listMcpProjectWorkflows]`; write set adds the five mutating tools. The factory constructor needs all six collaborators (`McpProjectFacade`, `McpProjectService`, `McpProjectWorkflowService`, `ProjectDeploymentWorkflowService`, `WorkflowService`, `WorkspaceMcpServerFacade`) because `ListMcpProjectWorkflowsToolCallback` alone needs four of them.
- [ ] **Step 2: prompt pair.** `prompt_mcp_manager.txt` carries a detailed `fromAi` authoring playbook — keep it in the BUILD prompt, it is the substance of this agent. Strip the delegation framing. ASK gets the read tools and an explicit "cannot modify" statement.
- [ ] **Step 3: configuration + test**, same gate and structure as Task 2. Preserve the existing `@ConditionalOnBean({McpProjectFacade.class, WorkspaceMcpServerFacade.class})` condition on the beans — the mcp facades are optional and the slice must skip silently when they are absent.
- [ ] **Step 4: `check`, commit.** `--- Add the MCP server copilot slice`

---

### Task 4: API collection slice (EE)

Same shape, in EE. Files: `ApiCollectionToolCallbacksFactory` (EE `automation-ai-tool`), `prompt_api_collection_ask/build.txt`, `ApiCollectionAgentConfiguration` in EE `automation-ai-copilot`, plus tests.

- [ ] **Step 1: factory test → factory.** Read `[listApiCollections]`; write adds `createApiCollection`, `cloneApiCollection`.
- [ ] **Step 2: prompt pair** from `prompt_api_collection_manager.txt`, delegation framing stripped.
- [ ] **Step 3: configuration + test.** Read `ContextStoreAgentConfiguration` in the same EE module for the established EE slice shape. **EE conventions:** Enterprise license header AND `@version ee` on every file including tests. Keep `@ConditionalOnBean(ApiCollectionFacade.class)`.
- [ ] **Step 4: `check` the EE modules, commit.** `--- Add the API collection copilot slice`

---

### Task 5: Wire the two CE listing pages

**Files:** `useCopilotStore.ts` (three new `Source` values), `ProjectDeployments.tsx`, `McpServers.tsx`.

- [ ] **Step 1: add the `Source` values** matching the server enum exactly — `PROJECT_DEPLOYMENT`, `MCP_SERVER`, `API_COLLECTION`. The client posts `source.toLowerCase()` and the server appends `_<mode>`, so a mismatched string breaks routing silently.
- [ ] **Step 2: wire both pages** following `DataTables.tsx`: bare `<CopilotButton source={...} />` with no external `copilotEnabled &&`, `useCopilotPostTurnRegistry((state) => state.register)` (never `.getState()`), effect last with deps `[queryClient, registerPostTurn]`, header row `gap-1`.

  Both pages have the empty-state ternary that collapses to a bare `<EnvironmentSelect />`. Apply the same rework the other listing pages got — `(items.length > 0 || !isLoading) && (...)` with the create action still gated on `length > 0` — so the button survives an empty list. **Trace all three states** (loading+empty, loaded+empty, loaded+non-empty) in your report.

- [ ] **Step 3: invalidation.** Deployments: `ProjectDeploymentKeys.projectDeployments` (import the factory; do not hardcode). MCP servers: the codegen keys are inline literals — `['workspaceMcpServers']`, `['mcpProjects']` — verify them in `graphql.ts` before using, and note that `useMcpServers` passes `workspaceId` as a **string** while the REST pages pass numbers.
- [ ] **Step 4: `npm run format`, `npm run check`, commit.** `--- client - Add Copilot to the deployments and MCP servers listing pages`

---

### Task 6: Wire the EE API collections page

**Files:** `client/src/ee/pages/automation/api-platform/api-collections/ApiCollections.tsx`.

- [ ] **Step 1: wire it** as in Task 5, invalidating `ApiCollectionKeys.apiCollections`.

  Two pre-existing inconsistencies on this page — **fix neither unless it blocks you**, and note them in your report: its sidebar `Header` omits `position="sidebar"`, and its empty-state `right` has no loading guard (bare `<EnvironmentSelect />`, unlike the other pages' `!isLoading &&`). The second one interacts with your rework: decide whether to add the guard for consistency or preserve current behaviour, and say which and why.

  Note also that the EE query-key factory member is misnamed `filteredProjectDeployments` (copy-paste), and both EE query files live under `ee/shared/**/mutations/**` despite containing only queries.

- [ ] **Step 2: `npm run format`, `npm run check`, commit.** `--- client - Add Copilot to the API collections listing page`

---

## Phase verification

Server: one combined `check` across `ai-copilot-api`, `ai-copilot-tool`, `ai-copilot-service`, CE `automation-ai-tool`, EE `automation-ai-tool`, EE `automation-ai-copilot`. Client: `npm run check`.

**Regression check that matters most:** confirm AI Hub and the management MCP server still expose `deployment_manager`, `mcp_manager` and `api_collection_manager` with unchanged names and behaviour. This phase must not have touched `AiHubConfiguration`, `ManagerMcpContributorConfiguration`, or `ApiCollectionManagerMcpContributorConfiguration` — verify with `git diff --stat` against the branch point.

Manual pass (needs a running backend): open Copilot from each of the three pages, confirm ASK cannot mutate, and confirm a BUILD turn refreshes the list.
