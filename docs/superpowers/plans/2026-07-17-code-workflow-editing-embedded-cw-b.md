# Code Workflow Editing — Embedded Integrations (CW-B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Bring code-workflow source editing + create-from-scratch to embedded Integrations, mirroring the automation CW-A feature — building the net-new `IntegrationCodeWorkflow*` deploy stack and reusing the generic container/file-storage layer and the shared client editor.

**Architecture:** Net-new `IntegrationCodeWorkflow*` persistence/service/facade (mirror `ProjectCodeWorkflow*`) that deploys an `IntegrationHandler` artifact into an `Integration` keyed on `componentName`; GraphQL + REST surfaces; a `codeWorkflow` flag on `IntegrationDTO` populated by DIRECT EE injection (no SPI, since `IntegrationFacadeImpl` is EE); and a client refactor extracting a presentational `CodeWorkflowSourceEditor` so the shared `WorkflowEditorLayout` branch renders a project- or integration-backed editor by route param.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, GraalVM polyglot (`IntegrationHandlerLoader`), React 19 / TS / Zustand, Monaco, GraphQL codegen.

## Global Constraints

- **All CW-B server code is EE** (`server/ee/**`): Enterprise license header + `@version ee`.
- **Polyglot-only, no Java editing.** Create + edit support JAVASCRIPT/PYTHON/RUBY only; Java-backed → visual editor / `LANGUAGE_NOT_SUPPORTED`.
- **Key = `componentName`** (Integrations have no `workspaceId`). Deploy resolves target by componentName; create rejects a pre-existing componentName (`CODE_WORKFLOW_ALREADY_EXISTS`).
- **Compile-gate + name-lock** on update (re-loaded `componentName` must equal the integration's).
- **Starters are REAL loadable `IntegrationHandler` scripts** — proven through the actual `IntegrationHandlerLoader` (JS object literal / Python `types.SimpleNamespace` / Ruby core `Struct` — members exposed, NOT raw dict/hash; Python/Ruby behind an availability assumption).
- **Reuse, don't fork:** the generic `CodeWorkflowContainer`, `CodeWorkflowFileStorage.readCodeWorkflowFileContent`, `CodeWorkflowErrorType` (all CW-A, platform-generic) are reused as-is.
- **Client**: interface names end `I`/`Props`; `sort-keys` alphabetical; icons `…Icon`; `twMerge` not `cn`; `npm run check` before any client commit.
- Reuse analogs — automation `ProjectCodeWorkflow*` (`server/ee/libs/automation/automation-configuration/**`) and CW-A client files — as templates. Commit only task files; fresh commits; never amend on `0_732`; `./gradlew spotlessApply` before server commits.

## Analog reference table (find exact paths with grep before editing)

| CW-B new class | Automation analog to clone |
|---|---|
| `IntegrationCodeWorkflow` domain | `ProjectCodeWorkflow` |
| `IntegrationCodeWorkflowRepository` | `ProjectCodeWorkflowRepository` |
| `IntegrationCodeWorkflowService(Impl)` | `ProjectCodeWorkflowService(Impl)` |
| `IntegrationCodeWorkflowFacade(Impl)` | `ProjectCodeWorkflowFacade(Impl)` |
| `IntegrationCodeWorkflowGraphQlController` + `code-workflow.graphqls` | `CodeWorkflowGraphQlController` (automation-configuration-graphql) |
| `IntegrationCodeWorkflowApiController` | `ProjectCodeWorkflowApiController` |
| `integration_code_workflow` migration | `project_code_workflow` changelog |

---

### Task 1: `IntegrationCodeWorkflow` persistence stack + migration

**Files (EE `embedded-configuration`):**
- Create: `IntegrationCodeWorkflow` domain (`@Table("integration_code_workflow")`, fields `id`, `integrationId`, `integrationVersion`, `codeWorkflowContainerId`) — clone `ProjectCodeWorkflow`.
- Create: `IntegrationCodeWorkflowRepository` (Spring Data JDBC; add `findFirstByIntegrationIdOrderByIdDesc(long)`), in the `*-service` module (repos live in `-service`, never `-api`).
- Create: `IntegrationCodeWorkflowService` (api) + `IntegrationCodeWorkflowServiceImpl` (service): `IntegrationCodeWorkflow create(CodeWorkflowContainer container, Integration integration)`, `Optional<IntegrationCodeWorkflow> fetchIntegrationCodeWorkflow(long integrationId)` (latest by id).
- Create: Liquibase changelog creating `integration_code_workflow` (id PK, integration_id, integration_version, code_workflow_container_id + FKs) — clone the `project_code_workflow` changelog; register it in the embedded-configuration master changelog.

**Interfaces:**
- Produces: `IntegrationCodeWorkflowService.create(...)` / `.fetchIntegrationCodeWorkflow(integrationId)`; table `integration_code_workflow`.

- [ ] Step 1: Read `ProjectCodeWorkflow`, `ProjectCodeWorkflowRepository`, `ProjectCodeWorkflowService(Impl)`, and the `project_code_workflow` Liquibase changelog (find via `grep -rln "project_code_workflow" server/ee`).
- [ ] Step 2: Create the domain (EE header + `@version ee`), mirroring `ProjectCodeWorkflow` field-for-field with `integrationId`/`integrationVersion`.
- [ ] Step 3: Create the repository + service + impl (EE headers), mirroring the automation trio; `fetchIntegrationCodeWorkflow` uses `findFirstByIntegrationIdOrderByIdDesc`.
- [ ] Step 4: Create + register the Liquibase changelog; after adding, delete any stale `build/resources/**` copy of the master changelog.
- [ ] Step 5: A service integration test (or a repository test) that a saved join row is fetched back by integrationId (mirror any existing `ProjectCodeWorkflowService` test; if none, a focused `@SpringBootTest` slice or skip per the analog's coverage).
- [ ] Step 6: `./gradlew spotlessApply`; compile the module; commit: `git commit -m "732 Add IntegrationCodeWorkflow persistence stack + migration"`

---

### Task 2: `IntegrationService.fetchIntegration(componentName)`

**Files (EE `embedded-configuration`):**
- Modify: `IntegrationService` + `IntegrationServiceImpl`, `IntegrationRepository`.
- Test: extend the existing `IntegrationService`/repo test.

**Interfaces:**
- Produces: `Optional<Integration> fetchIntegration(String componentName)` (case-insensitive), backed by `IntegrationRepository.findByComponentNameIgnoreCase`.

- [ ] Step 1: Read `ProjectService.fetchProject(name)` + `ProjectRepository.findByNameIgnoreCase` as the analog.
- [ ] Step 2: Add `Optional<Integration> findByComponentNameIgnoreCase(String componentName)` to `IntegrationRepository` (Spring Data derived query).
- [ ] Step 3: Add `Optional<Integration> fetchIntegration(String componentName)` to `IntegrationService` + impl delegating to the repo.
- [ ] Step 4: Test: save an integration, fetch it by componentName (case-insensitive); absent name → empty.
- [ ] Step 5: `spotlessApply`; compile; commit: `git commit -m "732 Add IntegrationService.fetchIntegration(componentName)"`

---

### Task 3: Starter templates + `IntegrationCodeWorkflowFacade.save`/`createEmptyCodeWorkflow`

**Files (EE `embedded-configuration`):**
- Create: `integration-code-workflow-templates/starter.js|py|rb` (resources).
- Create: `IntegrationCodeWorkflowFacade` (api) + `IntegrationCodeWorkflowFacadeImpl` (service).
- Test: `IntegrationCodeWorkflowFacadeCreateEmptyTest`.

**Interfaces:**
- Consumes: `IntegrationHandlerLoader.loadIntegrationHandler(...)`, `CodeWorkflowContainerFacade.create(...)` (PlatformType.EMBEDDED), `IntegrationCodeWorkflowService` (Task 1), `IntegrationService.fetchIntegration` (Task 2), `IntegrationWorkflowService.addWorkflow`, `IntegrationService.create/publishIntegration`.
- Produces: `void save(byte[] bytes, Language language)`; `Integration createEmptyCodeWorkflow(String componentName, Language language)`; private `deployInto(Integration, IntegrationDefinition, byte[], Language)`.

- [ ] Step 1: Read the IntegrationHandler polyglot contract in `IntegrationHandlerPolyglotEngine.load` (members: `componentName` req, `componentVersion`, `description`, `version`, `workflows:[{name,label,description,tasks:[{name,label,description,perform}]}]`) and `ProjectCodeWorkflowFacadeImpl` (`save`, `deployInto`, `loadProjectDefinition`, `createEmptyCodeWorkflow`) as the structural analog.
- [ ] Step 2: Draft the three starters (member-exposing shapes). JS:
```
({
    componentName: "__NAME__",
    componentVersion: 1,
    version: "0.0.1",
    description: "A code workflow integration.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            description: "An example workflow.",
            tasks: [
                { name: "my-task", label: "My Task", description: "An example task.", perform: function () { return "hello"; } }
            ]
        }
    ]
})
```
Python uses `types.SimpleNamespace(...)` at the top level (nested workflow/task entries stay dicts, read via `.as(List<Map>)`), `perform` a zero-arg-callable `lambda *args: "hello"`. Ruby uses `Struct.new(:componentName, :componentVersion, :version, :description, :workflows).new(...)` at the top level (nested hashes), `perform` a `lambda { |*args| "hello" }`. (These are DRAFTS — the loader is the source of truth; iterate in Step 3.)
- [ ] Step 3: RED test `assertStarterLoads` — render each starter (substitute `__NAME__`), write to a temp file, load via `IntegrationHandlerLoader.loadIntegrationHandler(...)`, assert `IntegrationDefinition.getComponentName()` == the substituted name and `getWorkflows()` non-empty. Guard Python/Ruby with the availability assumption pattern from `IntegrationHandlerPolyglotEngineTest`. Iterate each template until GREEN.
- [ ] Step 4: Implement `save(bytes, language)`: `def = loadIntegrationDefinition(language, bytes)`; `integration = fetchIntegration(def.getComponentName()).map(existing -> updateIntegration(existing, def)).orElseGet(() -> createIntegration(def))`; `deployInto(integration, def, bytes, language)`. `deployInto`: `container = codeWorkflowContainerFacade.create(def.getComponentName(), def.getVersion(), workflowNameToId(def), language, bytes, PlatformType.EMBEDDED)`; `integrationCodeWorkflowService.create(container, integration)`; per workflow `integrationWorkflowService.addWorkflow(integration.getId(), integration.getLastIntegrationVersion(), workflowId)`; `integrationService.publishIntegration(integration.getId(), null)`.
- [ ] Step 5: Implement `createEmptyCodeWorkflow(componentName, language)`: validate componentName (reject null/blank/`"`/`\`/`\n`/`\r` → `INVALID_CODE_WORKFLOW_NAME`); validate language ∈ {JS,PY,RUBY} (else `LANGUAGE_NOT_SUPPORTED`); **if `fetchIntegration(componentName).isPresent()` throw `CODE_WORKFLOW_ALREADY_EXISTS`**; render+substitute the starter; `save(bytes, language)`; return `fetchIntegration(componentName).orElseThrow(...)`.
- [ ] Step 6: Tests — createEmpty happy path (JS deploys a loadable integration; verify `integrationService.create` + `integrationCodeWorkflowService.create` called); blank/invalid componentName rejected; Java rejected; **existing componentName → `CODE_WORKFLOW_ALREADY_EXISTS` (no create)**. Mirror CW-A's `ProjectCodeWorkflowFacadeCreateEmptyTest` (mock the service layer; call the Impl directly for `@PreAuthorize`).
- [ ] Step 7: `spotlessApply`; run tests; commit: `git commit -m "732 Add integration code workflow starters + save/createEmpty"`

---

### Task 4: `getCodeWorkflowSource` + `updateCodeWorkflowSource` (compile-gate + name-lock)

**Files:** `IntegrationCodeWorkflowFacade` + Impl; test `IntegrationCodeWorkflowFacadeSourceTest`.

**Interfaces:**
- Produces: `String getCodeWorkflowSource(long integrationId)`; `void updateCodeWorkflowSource(long integrationId, String content)`.

- [ ] Step 1: Read CW-A's `ProjectCodeWorkflowFacadeImpl.getCodeWorkflowSource`/`updateCodeWorkflowSource` as the analog.
- [ ] Step 2: RED tests: get returns text for polyglot / throws `LANGUAGE_NOT_SUPPORTED` for Java; update valid same-componentName re-deploys; unparseable → `SOURCE_LOAD_FAILED`; re-loaded `componentName` ≠ integration's → `CODE_WORKFLOW_NAME_MISMATCH`.
- [ ] Step 3: Implement `getCodeWorkflowSource`: resolve container via `IntegrationCodeWorkflowService.fetchIntegrationCodeWorkflow(integrationId)` → `CodeWorkflowContainerService`; Java → `LANGUAGE_NOT_SUPPORTED`; else `codeWorkflowFileStorage.readCodeWorkflowFileContent(container.getWorkflows())`.
- [ ] Step 4: Implement `updateCodeWorkflowSource`: load `def` from content in try/catch → `SOURCE_LOAD_FAILED`; if `!def.getComponentName().equals(integration.getComponentName())` → `CODE_WORKFLOW_NAME_MISMATCH`; else `deployInto(integration, def, bytes, language)`.
- [ ] Step 5: `spotlessApply`; run tests; commit: `git commit -m "732 Add integration code workflow source get/update"`

---

### Task 5: GraphQL surface (embedded)

**Files (EE `embedded-configuration-graphql`):** `code-workflow.graphqls` + `IntegrationCodeWorkflowGraphQlController`; test.

**Interfaces:** GraphQL `codeWorkflowSource(integrationId: ID!): String!`, `updateCodeWorkflowSource(integrationId: ID!, content: String!): Boolean!`, `createCodeWorkflow(componentName: String!, language: CodeWorkflowLanguage!): ID!`; enum `CodeWorkflowLanguage {JAVASCRIPT,PYTHON,RUBY}`.

- [ ] Step 1: Read automation `CodeWorkflowGraphQlController` + its `code-workflow.graphqls` as the analog.
- [ ] Step 2: Write `code-workflow.graphqls` in this module (integrationId param, no workspaceId).
- [ ] Step 3: Write `IntegrationCodeWorkflowGraphQlController` (EE, `@ConditionalOnCoordinator @ConditionalOnEEVersion`): delegate to `IntegrationCodeWorkflowFacade`; `createCodeWorkflow` returns `String.valueOf(integration.getId())`; map `CodeWorkflowLanguage`→`Language` by name.
- [ ] Step 4: Register the schema path in `client/codegen.ts` if the embedded graphql resources dir isn't already globbed.
- [ ] Step 5: Controller test (mock facade; verify delegation + returns). `spotlessApply`; compile+test; commit: `git commit -m "732 Add embedded code workflow GraphQL surface"`

---

### Task 6: `IntegrationDTO`/`IntegrationModel` flag (direct EE injection)

**Files (EE `embedded-configuration`):** `IntegrationDTO`, `IntegrationFacadeImpl`, `IntegrationModel` OpenAPI yaml (internal); test.

**Interfaces:** `IntegrationDTO.codeWorkflow`/`.codeWorkflowLanguage`; generated `IntegrationModel` fields.

- [ ] Step 1: Read CW-A's `ProjectDTO` + `ProjectFacadeImpl.toProjectDTO` change — but NOTE: no SPI here. `IntegrationFacadeImpl` is EE; inject `IntegrationCodeWorkflowService` + `CodeWorkflowContainerService` directly.
- [ ] Step 2: Add `boolean codeWorkflow` + `String codeWorkflowLanguage` to `IntegrationDTO` (record + builder + both constructors).
- [ ] Step 3: In `IntegrationFacadeImpl.toIntegrationDTO` (the single-fetch path): `var cw = integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(integration.getId()).map(j -> codeWorkflowContainerService.getCodeWorkflowContainer(j.getCodeWorkflowContainerId())); codeWorkflow = cw.isPresent(); codeWorkflowLanguage = cw.map(c -> c.getLanguage().name()).orElse(null);` (never throw). Leave the bulk `getIntegrations` path false/null.
- [ ] Step 4: Add the two fields to the internal `IntegrationModel` OpenAPI schema; regenerate `IntegrationModel` only (revert any timestamp churn on other generated files, per CW-A).
- [ ] Step 5: Test: integration with a code-workflow join → flag true + language; without → false/null. `spotlessApply`; compile+test; commit: `git commit -m "732 Add codeWorkflow flag to IntegrationDTO"`

---

### Task 7: REST deploy controller (embedded)

**Files (EE `embedded-configuration-rest` or the module hosting `ProjectCodeWorkflowApiController`'s analog):** `IntegrationCodeWorkflowApiController`; test.

**Interfaces:** multipart upload → `IntegrationCodeWorkflowFacade.save(bytes, language)`.

- [ ] Step 1: Read `ProjectCodeWorkflowApiController` (automation) — its route, multipart handling, admin auth, `Language` param.
- [ ] Step 2: Write `IntegrationCodeWorkflowApiController` mirroring it (no workspaceId param), delegating to `IntegrationCodeWorkflowFacade.save`. Match the automation controller's auth + module placement (internal vs public — mirror the automation choice).
- [ ] Step 3: Test (mock facade; verify multipart → `save(bytes, language)`), or a focused web-layer test mirroring the automation controller's test if one exists.
- [ ] Step 4: `spotlessApply`; compile+test; commit: `git commit -m "732 Add embedded code workflow deploy REST controller"`

---

### Task 8: Client — refactor `CodeWorkflowDetail` → presentational editor + project wrapper

**Files (client):**
- Create: `client/src/pages/platform/code-workflow/CodeWorkflowSourceEditor.tsx` (presentational: `{language, source, isLoading, isSaving, onSave}`).
- Create: `client/src/pages/platform/code-workflow/ProjectCodeWorkflowDetail.tsx` (project hooks → CodeWorkflowSourceEditor).
- Modify: `WorkflowEditorLayout.tsx` import (temporarily still renders the project one; Task 10 generalizes).
- Remove/replace: the current combined `CodeWorkflowDetail.tsx` (fold its Monaco/dirty logic into the presentational editor). Update `CodeWorkflowDetail.test.tsx` → split into presentational + wrapper tests.

**Interfaces:**
- Produces: `<CodeWorkflowSourceEditor language source isLoading isSaving onSave />`; `<ProjectCodeWorkflowDetail projectId language />`.

- [ ] Step 1: Extract the Monaco + Save + dirty-tracking JSX from CW-A's `CodeWorkflowDetail` into `CodeWorkflowSourceEditor` (props only, no data hooks).
- [ ] Step 2: `ProjectCodeWorkflowDetail` calls `useCodeWorkflowSourceQuery({projectId})` + `useUpdateCodeWorkflowSourceMutation`, passing `source`/`isLoading`/`isSaving`/`onSave` into `CodeWorkflowSourceEditor`. Behavior identical to the old component.
- [ ] Step 3: Point `WorkflowEditorLayout` at `ProjectCodeWorkflowDetail` (keep it working for automation; Task 10 adds the integration branch).
- [ ] Step 4: Tests — presentational editor (renders source, Save fires `onSave(content)`, dirty toggles) + `ProjectCodeWorkflowDetail` (wires the project hooks). `cd client && npm run check`; commit: `git commit -m "732 client - Extract presentational CodeWorkflowSourceEditor + project wrapper"`

---

### Task 9: Client — embedded GraphQL ops + `IntegrationCodeWorkflowDetail`

**Files (client):**
- Create: `client/src/graphql/integration-code-workflow/*.graphql` (`codeWorkflowSource(integrationId)`, `updateCodeWorkflowSource`, `createCodeWorkflow(componentName, language)`); run `npx graphql-codegen`.
- Create: `client/src/pages/platform/code-workflow/IntegrationCodeWorkflowDetail.tsx`.
- Test: `IntegrationCodeWorkflowDetail.test.tsx`.

**Interfaces:** generated `useIntegrationCodeWorkflowSourceQuery` (or the embedded-scoped names codegen produces), `useUpdateIntegrationCodeWorkflowSourceMutation`, `useCreateIntegrationCodeWorkflowMutation` (names depend on the `.graphql` operation names — name them distinctly from the automation ops to avoid collisions, e.g. `integrationCodeWorkflowSource`).

- [ ] Step 1: Write the `.graphql` ops. IMPORTANT: the operation names must be UNIQUE across the codegen document set (the automation ops are `codeWorkflowSource`/`updateCodeWorkflowSource`/`createCodeWorkflow`) — name the embedded ops distinctly, e.g. `integrationCodeWorkflowSource`, `updateIntegrationCodeWorkflowSource`, `createIntegrationCodeWorkflow`, each selecting the embedded schema's fields (`codeWorkflowSource(integrationId:)`, etc.). Run `cd client && npx graphql-codegen`; confirm the 3 new hooks generate and no automation hook collides.
- [ ] Step 2: `IntegrationCodeWorkflowDetail` calls the embedded source query/update mutation hooks with `{integrationId}` and renders `CodeWorkflowSourceEditor`.
- [ ] Step 3: Test (mock the generated hooks): renders fetched source; Save calls the mutation with `{integrationId, content}`.
- [ ] Step 4: `cd client && npm run check`; commit: `git commit -m "732 client - Add embedded code workflow GraphQL ops + IntegrationCodeWorkflowDetail"`

---

### Task 10: Client — generalize `WorkflowEditorLayout` branch (project vs integration)

**Files (client):** `WorkflowEditorLayout.tsx`; test.

**Interfaces:** renders `ProjectCodeWorkflowDetail` or `IntegrationCodeWorkflowDetail` by route param.

- [ ] Step 1: In the code-workflow branch, read `const {projectId, integrationId} = useParams()`. Keep `isCodeWorkflow` (flag + polyglot language). When true: if `integrationId` → `<IntegrationCodeWorkflowDetail integrationId={integrationId} language={codeWorkflowLanguage} />`; else if `projectId` → `<ProjectCodeWorkflowDetail projectId={projectId} language={codeWorkflowLanguage} />`.
- [ ] Step 2: Tests — flag+JS with `projectId` param → project detail; with `integrationId` param → integration detail; flag unset → visual editor; JAVA → visual.
- [ ] Step 3: `cd client && npm run check`; commit: `git commit -m "732 client - Branch code editor by project vs integration"`

---

### Task 11: Client — `Integration.tsx` flag population

**Files (client):** `client/src/ee/pages/embedded/integration/Integration.tsx`; test if the file has one.

- [ ] Step 1: Replace the CW-A `undefined` placeholders in the `WorkflowEditorProvider` value with `codeWorkflow: integration?.codeWorkflow` and `codeWorkflowLanguage: integration?.codeWorkflowLanguage` (from the `getIntegration` query result — the fields added in Task 6). Mirror `Project.tsx`.
- [ ] Step 2: `cd client && npm run check`; commit: `git commit -m "732 client - Populate codeWorkflow flag for embedded integrations"`

---

### Task 12: Client — "New code workflow" entry (embedded)

**Files (client):**
- Create: `client/src/ee/pages/embedded/integrations/components/NewIntegrationCodeWorkflowDialog.tsx`
- Modify: `client/src/ee/pages/embedded/integrations/Integrations.tsx` (add a DropdownMenu + item)
- Test: `NewIntegrationCodeWorkflowDialog.test.tsx`

**Interfaces:** `useCreateIntegrationCodeWorkflowMutation` (Task 9).

- [ ] Step 1: Clone CW-A's `NewCodeWorkflowDialog.tsx` — but the field is **componentName** + language (NO workspaceId). On submit `createIntegrationCodeWorkflow({componentName, language})`; on success fetch the integration (`IntegrationApi().getIntegration`) to read `integrationWorkflowIds[0]`, then navigate to `/embedded/integrations/{id}/integration-workflows/{firstWorkflowId}`. Guard the Cancel button with `disabled={isPending}` (the CW-A fix). Global-toast error handling.
- [ ] Step 2: `Integrations.tsx` today has only a plain "New Integration" button — wrap it in a `DropdownMenu` (mirror `Projects.tsx`) with "New Integration" + "New code workflow" items; the latter opens the dialog.
- [ ] Step 3: Test: submitting calls `createIntegrationCodeWorkflow` with `{componentName, language}` and navigates.
- [ ] Step 4: `cd client && npm run check`; commit: `git commit -m "732 client - Add New code workflow entry for embedded integrations"`

---

### Task 13: Full verification

- [ ] Step 1: Server — `./gradlew` check/compile the touched EE embedded-configuration modules (service, graphql, rest) + confirm the Liquibase changelog applies (an integration test that boots the context).
- [ ] Step 2: Client — `cd client && npm run check`.
- [ ] Step 3: Sanity greps — embedded deploy resolves by componentName; `IntegrationCodeWorkflowDetail` uses the embedded hooks (not the automation ones); `WorkflowEditorLayout` picks the right wrapper by param; no automation CW-A regression (ProjectCodeWorkflowDetail equivalent).
- [ ] Step 4: Commit any format-only changes per the owning task.

---

## Self-Review

**Spec coverage:** §1 persistence → Task 1. §2 fetchIntegration → Task 2. §3 facade save/createEmpty + starters → Task 3. §4 templates → Task 3. §3 get/update → Task 4. §5 GraphQL → Task 5. §6 DTO flag → Task 6. §7 REST → Task 7. §8 client refactor → Task 8. §11 embedded ops + wrapper → Task 9. §9 layout branch → Task 10. §10 Integration.tsx → Task 11. §12 new-code-workflow entry → Task 12. Testing/rollout → per-task tests + Task 13.

**Placeholder scan:** Python/Ruby starter shapes in Task 3 are drafts explicitly gated by the RED loader test (Step 3) — same discipline as CW-A Task 2, which proved the drafts wrong and the loader right. All else concrete.

**Type consistency:** `createEmptyCodeWorkflow(String componentName, Language)` (Task 3) ↔ GraphQL `createCodeWorkflow(componentName, language)` (Task 5) ↔ client `useCreateIntegrationCodeWorkflow` (Tasks 9,12). `get/updateCodeWorkflowSource(long integrationId)` (Task 4) ↔ GraphQL (Task 5) ↔ client hooks (Task 9). `codeWorkflow`/`codeWorkflowLanguage` consistent across Task 6 (DTO/model), Task 11 (Integration.tsx), Task 10 (branch). Distinct GraphQL operation names (Task 9 Step 1) prevent codegen collision with automation ops.

**Risk notes:** Task 3 (polyglot starter correctness for the IntegrationHandler contract — `componentName` not `name`, and the member-exposure rule) is highest-risk — its TDD gate against the real `IntegrationHandlerLoader` is mandatory. Task 8's behavior-preserving refactor of CW-A's shipped `CodeWorkflowDetail` must not regress automation. Task 9's operation-name uniqueness avoids a codegen collision. Task 1's Liquibase changelog must be registered + stale build copies cleared.
