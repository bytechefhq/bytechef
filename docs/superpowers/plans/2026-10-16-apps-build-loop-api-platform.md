# Apps — Build Loop for API Platform, Scheduled and Background Workflows — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Teach the app build loop (management MCP tools, `buildWorkflow`, the app-builder skill, the template `AGENTS.md`, `.agents/apps.md`) to prefer API Platform endpoints, to build scheduled workflows backed by data tables and background Call-workflow runs when the app needs them — then fold the work of all three plans into the Apps commit group on `0_732`.

**Architecture:** New DEVELOPMENT-only MCP tools live in a Copilot-independent class registered by `AppLoopMcpContributorConfiguration`, because `PluginSkillTagDriftIntTest` boots with Copilot off and every MCP fence's `uses:` must name a registered tool. `buildWorkflow` gains a `kind` that selects the instruction appended to the request and the checker run on the saved workflows. Guidance files follow the decision table from the spec; `mcp-instructions.md` is regenerated, never hand-edited.

**Tech Stack:** Java 25, Spring AI `@Tool`, Spring Boot 4, Markdown skill files parsed by `claude-code-plugin/plugin-tools`.

**Spec:** `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md` ("Scheduled workflows in the build loop", "Design — build-loop guidance", Decisions 1–5).

**Plan series:** 3 of 3. Needs plans 1 and 2 on the working branch.

## Global Constraints

- New MCP tool names must not collide with Copilot-registered tools (`createApiCollection`, `listApiCollections`, `createDataTable` exist on Copilot/AiHub surfaces; the registry fails fast on a collision). Names used here: `createAppApiCollection`, `createAppApiEndpoint`, `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`.
- Every new tool refuses any environment but DEVELOPMENT with `AppLoopTools.DEVELOPMENT_ONLY` via `ToolScope.of(toolContext)`, and returns refusals as values (never throws), like `AppLoopTools`.
- Order of preference, verbatim from the spec, everywhere guidance appears: **1. API Platform endpoint** (default, answers within a request) → **2. Call-workflow binding** (long-running, approval-waiting, background one-off runs: `waitSeconds: 0` + poll) → **3. direct action or tool** (a single operation, or tools for an app running its own LLM) → **4. proxy** (last resort). Going past option 1 states why.
- Scheduled = periodic via `schedule/v1/*` trigger, the app never starts it; writes rows to a data table; the app reads them through an API Platform endpoint. Background = one-off, started by the app via a Call-workflow binding with `waitSeconds: 0`.
- Scheduled workflows are enabled only by passing their uuid in `deployProject`'s `enabledWorkflows`; `deployProject`'s default stays "app-callable workflows".
- `mcp-instructions.md` is generated (`./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions`); `McpInstructionsStalenessTest` fails on drift.
- Template `AGENTS.md` may only name CLI commands/options that exist (`AppTemplateCliCommandDriftTest`); skill CLI fences likewise (`PluginSkillTagDriftIntTest`).
- No Apps docs page exists under `docs/content/docs` on `0_732`; the spec's "Apps docs page" item is satisfied by `.agents/apps.md` + the template README. Do not create a docs page.
- Commits: subject only; `fixup! <Apps commit subject>` as in plan 2; Task 6 autosquashes.

## Review Focus

1. **`createAppApiEndpoint` on a workflow whose trigger is not `apiPlatform/v1/newApiRequest`** — refused with a message naming the trigger it has, nothing created. Test in Task 1.
2. **Endpoint slot with zero or several usable connections** — not guessed: status `UNRESOLVED` listing candidates per slot, endpoint left disabled. Test in Task 1.
3. **`buildWorkflow` with `kind: SCHEDULED` producing a schedule workflow that writes nowhere** — reported as missing a data-table write, so the agent fixes it before deploying. Test in Task 2.
4. **The skill sending the agent to direct actions because they are fewer steps** — the guidance text states the order and "say why"; the drift tests prove every named tool/command exists. Covered by Task 4/5 drift tests.
5. **Re-running the loop after a workflow change** — `updateAppApiCollectionVersion` keeps app bindings and paths valid. Test in Task 1 (delegation) — behaviour itself pinned by plan 1, Task 4.

---

### Task 1: API Platform and data-table tools for the build loop

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppApiPlatformLoopTools.java`
- Modify: `.../automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/AppLoopMcpContributorConfiguration.java` (also build `ToolCallbacks.from(new AppApiPlatformLoopTools(...))`, wrapped in `WorkspaceScopedFlatToolCallback`; contribute nothing when `ApiCollectionFacade` or `DataTableFacade` is missing)
- Test: `.../automation-ai-tool/src/test/java/com/bytechef/ee/automation/ai/tool/AppApiPlatformLoopToolsTest.java`; `AppLoopMcpContributorConfigurationTest.java` (:44 expects 3 tools → 8)

**Interfaces:**
- Consumes: plan 1 `ApiCollectionFacade` (`createApiCollection`, `getApiCollections`, `getApiCollectionEndpoints`, `createApiCollectionEndpoint(dto, connections, inputs)`, `enableApiCollectionEndpoint`, `updateApiCollectionProjectVersion`); `ProjectDeploymentFacade.enableProjectDeployment(long, boolean)`; `ComponentConnectionFacade.getWorkflowComponentConnections(String workflowId)` for slots; `AppFacade` workspace connections (the source `listConnections` uses, `usable` flag) for candidates; `DataTableFacade` create (the method `CreateDataTableToolCallback` calls); `ProjectWorkflowService` / facade to resolve a workflow uuid at a project version and read its trigger type.
- Produces `@Tool` methods (each first `ToolScope.of(toolContext)`):
  - `ApiCollectionResult createAppApiCollection(long projectId, int projectVersion, String name, String contextPath, @Nullable Integer collectionVersion, @Nullable String description, ToolContext)` — creates in DEVELOPMENT and **enables the hidden deployment**; result `{status, message, apiCollectionId, contextPath, collectionVersion, bindCommand}` where `bindCommand` = `bytechef automation app bind api-collection --api-collection-id <id>`.
  - `ApiEndpointResult createAppApiEndpoint(long apiCollectionId, String name, String httpMethod, String path, String workflowUuid, @Nullable List<EndpointConnectionChoice> connections, @Nullable Map<String, Object> inputs, ToolContext)` — refuses unless the workflow's trigger is `apiPlatform/v1/newApiRequest`; per slot: explicit choice, else the only usable candidate, else unresolved with candidates; when all slots resolve, creates the endpoint with connections and **enables** it (`STATUS_DEPLOYED`), otherwise creates nothing and returns `STATUS_UNRESOLVED` with `unresolvedConnections[{workflowNodeName, key, componentName, candidates[{id, name}]}]`; result also carries `url` = `/api/app/v1/api/v{n}/{contextPath}/{path}`.
  - `record EndpointConnectionChoice(@ToolParam(required = true) String workflowNodeName, @ToolParam(required = true) String key, @ToolParam(required = true) Long connectionId)`
  - `ApiCollectionVersionResult updateAppApiCollectionVersion(long apiCollectionId, int projectVersion, @Nullable Boolean removeMissingEndpoints, ToolContext)` — delegates; returns `removedEndpointNames`, `missingConnections`, and a refusal message listing endpoints when the facade refuses.
  - `ApiCollectionListResult listAppApiCollections(@Nullable Long projectId, ToolContext)` — DEVELOPMENT collections of the workspace with endpoints (`method`, `path`, `workflowUuid`, `enabled`).
  - `DataTableResult createAppDataTable(String baseName, List<DataTableColumnInput> columns, @Nullable String description, ToolContext)` with `record DataTableColumnInput(String name, String type)` (types as `CreateDataTableToolCallback` accepts) — result `{status, message, baseName}`; the workflow's `dataTable/v1/*` actions take `table` = this `baseName`.

- [ ] **Step 1: Tests** (plain JUnit + Mockito; `toolContext(Environment)` helper copied from `AppLoopToolsTest:614`):
  - `testCreateAppApiCollectionEnablesDeploymentAndReturnsBindCommand`
  - `testCreateAppApiEndpointRefusesNonApiPlatformTrigger` (Review Focus 1)
  - `testCreateAppApiEndpointPicksOnlyUsableCandidate`
  - `testCreateAppApiEndpointUnresolvedWhenSeveralCandidates` and `…WhenNoCandidate` (Review Focus 2) — `createApiCollectionEndpoint` never called
  - `testCreateAppApiEndpointExplicitChoiceWins`
  - `testUpdateAppApiCollectionVersionReportsRefusal`
  - `testCreateAppDataTableReturnsBaseName`
  - `testEveryToolRefusesProduction` (parameterised over the five tools)
  - `AppLoopMcpContributorConfigurationTest.testContributesEightWrappedTools`, `testEndpointConnectionChoiceSchemaRequiresFields`
  Run `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:test` — FAIL.
- [ ] **Step 2: Implement.** Run — PASS.
- [ ] **Step 3: Commit** — `fixup! - Add Apps with secret-key authentication, …` (the app-loop tools came with the Apps server commit; if `AppLoopTools` was added by a different Apps-group commit, use that commit's subject — check with `git log --format=%s -1 -- <AppLoopTools path>` on `0_732`).

### Task 2: `buildWorkflow` kinds

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedSubAgentToolCallback.java` (`APP_CALLABLE_INPUT_SCHEMA` :66 → `INPUT_SCHEMA` with `kind`; `APP_CALLABLE_INSTRUCTION` :91 plus `API_ENDPOINT_INSTRUCTION`, `SCHEDULED_INSTRUCTION`; `call()` :140; `checkAppCallable` :208 → `checkKind`; `record WorkspaceScopedInput` :245)
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/workflow/WorkflowKind.java` (`APP_CALLABLE`, `API_ENDPOINT`, `SCHEDULED`) and `WorkflowKindChecker.java` (`WorkflowKind kind()`, `CallableWorkflowCheck check(String workflowId)`)
- Modify: `AppCallableWorkflowResolver` to also implement `WorkflowKindChecker` (`kind()` = `APP_CALLABLE`)
- Create: `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-service/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/workflow/ApiEndpointWorkflowChecker.java` (EE) and `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/workflow/ScheduledWorkflowChecker.java` (CE)
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java` (:76-78 pass `List<WorkflowKindChecker>` instead of the single checker)
- Test: `WorkspaceScopedSubAgentToolCallbackTest.java`, new `ApiEndpointWorkflowCheckerTest.java`, `ScheduledWorkflowCheckerTest.java`; `server/apps/server-app/src/test/java/com/bytechef/server/AppLoopToolsIntTest.java` (:173 still green with `kind: APP_CALLABLE`)

**Interfaces:**
- Produces:
  - Input `{request, workspaceId, environment, kind?}`; `kind` absent = no instruction, no check (today's behaviour without `appCallable`). `appCallable` is removed (unreleased; the template/skill are updated in Tasks 4–5).
  - `API_ENDPOINT_INSTRUCTION`: trigger `apiPlatform/v1/newApiRequest` with `request` (headers/parameters/body JSON Schemas) and `response` cases; the last task `apiPlatform/v1/responseToAPIRequest` with `responseType`.
  - `SCHEDULED_INSTRUCTION`: trigger one of `schedule/v1/cron|everyDay|everyWeek|everyMonth|interval`; results written with `dataTable/v1/createRecords` or `updateRecord` to the table named by `baseName`.
  - `ApiEndpointWorkflowChecker.check` problems: `"trigger is <type>, not apiPlatform/v1/newApiRequest"`, `"no responseToAPIRequest task"`, `"request schema unreadable"`.
  - `ScheduledWorkflowChecker.check` problems: `"trigger is <type>, not a schedule trigger"`, `"no dataTable write task"`.
  - Success text per kind: APP_CALLABLE unchanged; API_ENDPOINT `"API endpoint workflow: … Publish the project, then createAppApiCollection / createAppApiEndpoint."`; SCHEDULED `"Scheduled workflow: … Publish the project, then deployProject with this uuid in enabledWorkflows."`

- [ ] **Step 1: Tests**: checker tests for each problem string and the passing case; `WorkspaceScopedSubAgentToolCallbackTest`: `testKindApiEndpointAppendsInstructionAndChecks`, `testKindScheduledReportsMissingDataTableWrite` (Review Focus 3), `testNoKindSkipsCheck`, `testUnknownKindIsToolError`. Run — FAIL.
- [ ] **Step 2: Implement.** Run module tests + `./gradlew :server:apps:server-app:testIntegration --tests '*AppLoopToolsIntTest'` — PASS.
- [ ] **Step 3: Commit** — CE files (`server/libs/...`) and the EE checker: `fixup!` of the Apps commit that introduced `CallableWorkflowChecker` / `appCallable` (find with `git log --format=%s -S APP_CALLABLE_INSTRUCTION 0_732`); EE `ApiEndpointWorkflowChecker` goes with plan 1's API Platform group as `- Check that a workflow can back an API Platform endpoint`.

### Task 3: Skill — `bytechef-app-builder`

**Files:**
- Modify: `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`
- Regenerate: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md`
- Test (existing): `server/apps/server-app/src/test/java/com/bytechef/ai/mcp/server/config/PluginSkillTagDriftIntTest.java`, `claude-code-plugin/plugin-tools/src/test/.../McpInstructionsStalenessTest.java`

**Content (sections, in order):**
1. Frontmatter `description` adds "call a ByteChef API endpoint", "run something on a schedule", "run a background job".
2. **"How the app reaches ByteChef"** (new, after the intro) — the four-step order verbatim from Global Constraints, then the decision table from the spec ("Picking the workflow type from the need"), with `api.call`, `workflows.execute`, `workflows.execute` + `waitSeconds: 0`, bound action, bound tools, proxy.
3. "The app" (cli fence) — unchanged plus `automation app bind api-collection` in `uses:` and one example line.
4. **"API endpoints — the default"** (new mcp fence, `uses: createAppApiCollection, createAppApiEndpoint, updateAppApiCollectionVersion, listAppApiCollections, listConnections edition: ee`): `buildWorkflow` with `kind: API_ENDPOINT` → `publishProject` → `createAppApiCollection` (once per app area) → `createAppApiEndpoint` per endpoint (resolve `UNRESOLVED` slots with `connections`) → run the returned `bindCommand`; on a change: `buildWorkflow` → `publishProject` → `updateAppApiCollectionVersion`.
5. **"Scheduled work"** (new mcp fence, `uses: createAppDataTable, deployProject edition: ee`): `createAppDataTable` → `buildWorkflow kind: SCHEDULED` → `buildWorkflow kind: API_ENDPOINT` for the read endpoint (`dataTable/v1/findRecords`) → `publishProject` → `deployProject` with the scheduled uuid in `enabledWorkflows` → collection/endpoint as in 4.
6. "Workflows, only when the app needs one" → renamed **"Long-running, approval and background runs"**; `appCallable: true` → `kind: APP_CALLABLE`; adds the background pattern (`waitSeconds: 0`, poll `getRun`).
7. "Binding a deployed workflow" — unchanged.

- [ ] **Step 1:** Edit the skill. Run `./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions`.
- [ ] **Step 2:** Run `./gradlew :claude-code-plugin:plugin-tools:test :server:apps:server-app:testIntegration --tests '*PluginSkillTagDriftIntTest'` — PASS (every `uses:` name registered with Copilot off; every CLI command exists).
- [ ] **Step 3: Commit** — `fixup! --- docs - Add the Apps designs and plans, and document Apps, the app SDK and the app template` for `SKILL.md`; the regenerated `mcp-instructions.md` with the same subject (it shipped in that docs commit).

### Task 4: Template `AGENTS.md` and README

**Files:**
- Modify: `sdks/frontend/automation/app/create/template/AGENTS.md` ("Rules" :6 gets the order of preference; "Setting up the app with ByteChef" :39 gets the API-collection bind step before the workflows step (:61); "Recipes" :75 adds **"Call a ByteChef API endpoint"** (first recipe), **"Show results of scheduled work"**, **"Start a background job"**; the existing action recipe gets a "use only when…" lead)
- Modify: `sdks/frontend/automation/app/library/README.md` usage order: `api` → `workflows` → `actions`/`tools` → `proxy`
- Test (existing): `server/apps/server-app/src/test/java/com/bytechef/server/AppTemplateCliCommandDriftTest.java`

- [ ] **Step 1:** Edit. Run `./gradlew :server:apps:server-app:test --tests '*AppTemplateCliCommandDriftTest'` — PASS.
- [ ] **Step 2: Commit** — `fixup! - sdk - Add the @bytechef/app client library, …`

### Task 5: `.agents/apps.md` and the spec/plans

**Files:**
- Modify: `.agents/apps.md` — new section **"API collection bindings and the app API path"** after "Workflow bindings" (:298): table, resolver, facade gates, error codes 124–127, the shared `AbstractApiPlatformEndpointController`, `AUTOMATION_APP_API` surface; "Agent loop (server half)" (:362) lists the five new tools and `buildWorkflow` kinds; "SDK" (:442) adds `client.api`; "Open items" (:605) adds "push to the app's `url` when scheduled results are ready (spec Decision 5)".
- Move: `docs/superpowers/specs/2026-10-16-apps-api-platform-design.md` and the three `docs/superpowers/plans/2026-10-16-*.md` onto the working branch (they were written in a master-based worktree).

- [ ] **Step 1:** Edit and copy files.
- [ ] **Step 2: Commit** — `fixup! --- docs - Add the Apps designs and plans, …`

### Task 6: Fold, reorder and verify on `0_732`

- [ ] **Step 1:** On the working branch, list commits since the `0_732` tip: plan 1's group (`--- Gate API collection endpoint…`, `- Set API collection endpoint connections…`, `- List an API collection's endpoints…`, `- Move an API collection to a newer project version in place`, `- client - Update an API collection's project version`, `- Expose API collections through the public API`, `- Expose API collection endpoints…`, `- cli - Add the automation api-collection commands`, `- Check that a workflow can back an API Platform endpoint`, plan 2's `--- Share API Platform endpoint resolution…`) and the `fixup!` commits.
- [ ] **Step 2:** Pin the base SHA (`git rev-parse 0_732`), then `git rebase -i --autosquash <base of the Apps group>^` driven non-interactively (`GIT_SEQUENCE_EDITOR` script) so that: the API Platform group (with `--- Share API Platform endpoint resolution…` folded into it as its second commit) sits immediately before `--- docs - Add the Apps designs and plans…`, and every `fixup!` lands on its Apps commit. Keep `merge.renames=false` as in earlier `0_732` rebases.
- [ ] **Step 3:** Prove each rewritten commit builds: `git rebase --exec './gradlew compileJava --configure-on-demand -q' <new group base>^` (backgrounded; >2 min).
- [ ] **Step 4:** Full check on touched modules (plan 1 Task 8 + plan 2 Task 9 commands), `./gradlew :server:apps:server-app:testIntegration --tests '*AppLoopToolsIntTest' --tests '*PluginSkillTagDriftIntTest'`, `cd client && npm run check`, SDK `npm run check`.
- [ ] **Step 5:** Boot smoke: `server-app` starts with the dev infra; run the app loop end to end in DEVELOPMENT — `buildWorkflow kind: API_ENDPOINT` → publish → `createAppApiCollection` → `createAppApiEndpoint` → `bytechef automation app bind api-collection` → template's `api.call` returns the workflow's response.
- [ ] **Step 6:** Ask the user before moving `0_732` (`git branch -f 0_732 <working branch>` or a fast-forward); never push without an explicit yes.
