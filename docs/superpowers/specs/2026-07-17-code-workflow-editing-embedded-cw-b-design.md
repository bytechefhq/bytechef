# Code Workflow Editing — Embedded Integrations (CW-B) — Design

**Date:** 2026-07-17
**Status:** Approved (design)
**Initiative:** Code Workflows — "same treatment" as custom components. CW-A (automation) is done. CW-B = embedded Integrations. CW-C = copilot (both surfaces).
**Area:** EE `embedded-configuration` (new `IntegrationCodeWorkflow*` stack), EE `embedded-code-workflow-loader` (existing `IntegrationHandlerLoader`), EE `platform-code-workflow` (generic container/file-storage, reused), embedded client (`client/src/ee/pages/embedded/**`) + the shared platform workflow-editor layer.

## Background

CW-A built automation code-workflow editing on the existing `ProjectCodeWorkflow*` deploy path. Embedded has **no deploy path at all** — only `IntegrationHandlerLoader` (`embedded-code-workflow-loader`), which is wired to nothing deploy-related (only the runtime `CodeWorkflowTaskExecutor` consumes it). CW-B builds the net-new `IntegrationCodeWorkflow*` stack mirroring `ProjectCodeWorkflow*`, plus the source read/write/create + DTO flag + client editor, reusing the generic `CodeWorkflowContainer`/file-storage layer (`PlatformType.EMBEDDED`) and the shared `CodeWorkflowDetail` UI from CW-A.

## Scope decisions (from brainstorming + exploration)

- **Mirror `ProjectCodeWorkflow*` exactly**, adjusting for the two embedded divergences:
  1. **Key = `componentName`, not `name`; no `workspaceId`.** An `Integration` carries `componentName` + `componentVersion` and has no workspace. Deploy resolves the target via a new `IntegrationService.fetchIntegration(componentName)` (create-or-update), and `createEmptyCodeWorkflow` rejects a pre-existing `componentName` (mirroring CW-A's create-only guard).
  2. **No optional SPI.** `IntegrationFacadeImpl` is in an EE module, so it injects `IntegrationCodeWorkflowService` + `CodeWorkflowContainerService` **directly** and populates the flag inline in `toIntegrationDTO` (no `ObjectProvider<Supplier>` indirection — that existed only because CE `ProjectFacadeImpl` couldn't see EE code).
- **Polyglot-only, no Java editing** (same as CW-A). Java-backed integrations get no source editor.
- **Create-empty for JS/Python/Ruby**, starter `IntegrationHandler` scripts embedding the chosen `componentName`.
- **Reuse the shared UI via a small refactor**: extract a presentational `CodeWorkflowSourceEditor` (Monaco + Save; props `{source, language, isLoading, isSaving, onSave}`) from CW-A's `CodeWorkflowDetail`; provide `ProjectCodeWorkflowDetail` (project hooks) and `IntegrationCodeWorkflowDetail` (integration hooks) as thin data-wrappers. The shared `WorkflowEditorLayout` branch renders the right wrapper by route param (`projectId` vs `integrationId`).

## The IntegrationHandler polyglot contract

The completion value of a JS/Python/Ruby integration script must expose members (per `IntegrationHandlerPolyglotEngine.load`):
`componentName` (String, required), `componentVersion` (int), `description` (String), `version` (String, default "0.0.1"), `workflows` (list). Each workflow: `name`, `label`, `description`, `tasks`; each task: `name`, `label`, `description`, `perform` (zero-arg callable via `apply(null)`). Same member-exposure rule as CW-A (JS object literal / Python `types.SimpleNamespace` / Ruby `Struct` — NOT raw dict/hash).

## Non-goals (CW-B)

- The copilot (CW-C) — separate sub-project (now covering both surfaces).
- Java code workflows (no create, no editor).
- Reworking the automation `ProjectCodeWorkflow*` stack (only the shared client `CodeWorkflowDetail` is refactored, behavior-preserving, for reuse).
- Per-workflow/multi-file editing (one artifact = one source).

## Server design (all EE)

### 1. `IntegrationCodeWorkflow` persistence stack (`embedded-configuration`)
- `IntegrationCodeWorkflow` domain (`@Table("integration_code_workflow")`, join `Integration` ↔ `CodeWorkflowContainer`; fields `integrationId`, `integrationVersion`, `codeWorkflowContainerId`) — mirror `ProjectCodeWorkflow`.
- `IntegrationCodeWorkflowRepository` (+ `findFirstByIntegrationIdOrderByIdDesc` — latest container) — mirror `ProjectCodeWorkflowRepository`.
- `IntegrationCodeWorkflowService` + `Impl` (`create(container, integration)`, `getIntegrationCodeWorkflow(integrationId)`) — mirror `ProjectCodeWorkflowService`.
- Liquibase migration creating `integration_code_workflow` (mirror the `project_code_workflow` changelog).

### 2. `IntegrationService.fetchIntegration(String componentName)`
Add to `IntegrationService`/`IntegrationServiceImpl` + `IntegrationRepository.findByComponentNameIgnoreCase` — the mirror of `ProjectService.fetchProject(name)`, used by the deploy target resolution + the create-only guard.

### 3. `IntegrationCodeWorkflowFacade` + Impl
- `save(byte[] bytes, Language language)` (no workspaceId): load via `IntegrationHandlerLoader.loadIntegrationHandler` → `IntegrationDefinition`; resolve target via `fetchIntegration(componentName)` → update, else create a new `Integration` (componentName/componentVersion/description from the definition); create the `CodeWorkflowContainer` (`PlatformType.EMBEDDED`); `IntegrationCodeWorkflowService.create` the join; `integrationWorkflowService.addWorkflow` per workflow; `integrationService.publishIntegration`. Factor a private `deployInto(integration, definition, bytes, language)` so update targets a specific integration.
- `createEmptyCodeWorkflow(String componentName, Language language)` (JS/Py/Ruby only): validate componentName (reject blank/quotes/backslashes/newlines) + language; **reject if `fetchIntegration(componentName)` present** (`CODE_WORKFLOW_ALREADY_EXISTS`); render the language starter (embed componentName) → deploy via `save`; return the created `Integration`.
- `getCodeWorkflowSource(long integrationId)` → artifact text; Java container → `LANGUAGE_NOT_SUPPORTED`.
- `updateCodeWorkflowSource(long integrationId, String content)` → **compile-gate** via `IntegrationHandlerLoader` (`SOURCE_LOAD_FAILED`) + **name-lock** (re-loaded `componentName` must equal the integration's `componentName` → else `CODE_WORKFLOW_NAME_MISMATCH`); then `deployInto` the same integration.
- Reuse the generic `CodeWorkflowFileStorage.readCodeWorkflowFileContent` + `CodeWorkflowErrorType` (both from CW-A, platform-generic).

### 4. Starter templates (`embedded-configuration` resources)
`integration-code-workflow-templates/starter.js|py|rb` — minimal valid `IntegrationHandler` scripts with a `__NAME__` placeholder for `componentName`, member-exposing shapes (JS object literal / Python `SimpleNamespace` / Ruby `Struct`), one example workflow+task. TDD-verified against the real `IntegrationHandlerLoader` (Python/Ruby behind the availability assumption).

### 5. GraphQL surface (`embedded-configuration-graphql`)
New `code-workflow.graphqls` + `IntegrationCodeWorkflowGraphQlController` (keyed on `integrationId`, no workspaceId):
- `codeWorkflowSource(integrationId: ID!): String!`
- `updateCodeWorkflowSource(integrationId: ID!, content: String!): Boolean!`
- `createCodeWorkflow(componentName: String!, language: CodeWorkflowLanguage!): ID!` (returns the new integration id)

`CodeWorkflowLanguage` = JAVASCRIPT|PYTHON|RUBY (Java excluded). Distinct from the automation schema (different module, `integrationId` param) — do not generalize.

### 6. `IntegrationDTO`/`IntegrationModel` flag
Add `boolean codeWorkflow` + `String codeWorkflowLanguage` to `IntegrationDTO` (record + builder + constructors) and the internal `IntegrationModel` OpenAPI schema (regenerate `IntegrationModel` only). Populate **directly** in `IntegrationFacadeImpl.toIntegrationDTO` (single-fetch path) by resolving the join → container → `language.name()`; not present → false/null. (Bulk `getIntegrations` stays false/null, mirroring CW-A.) No SPI.

### 7. REST deploy controller (mirror `ProjectCodeWorkflowApiController`)
`IntegrationCodeWorkflowApiController` (internal or public per the automation analog) exposing the multipart upload → `IntegrationCodeWorkflowFacade.save(bytes, language)`, so pre-built artifacts (incl. Java JARs) can be deployed — parity with automation and a dependency of the later plugin initiative. Admin-gated as the automation controller is.

## Client design

### 8. Refactor CW-A's `CodeWorkflowDetail` into presentational + wrapper
- Extract `CodeWorkflowSourceEditor.tsx` (platform/code-workflow): Monaco + Save + dirty tracking, props `{language, source, isLoading, isSaving, onSave}` — no data hooks.
- `ProjectCodeWorkflowDetail.tsx` (project source query/mutation hooks → `CodeWorkflowSourceEditor`), replacing the current combined component; behavior-preserving for CW-A.
- `IntegrationCodeWorkflowDetail.tsx` (embedded source query/mutation hooks → `CodeWorkflowSourceEditor`).

### 9. Shared `WorkflowEditorLayout` branch (generalize CW-A Task 8)
Read `codeWorkflow`/`codeWorkflowLanguage` from the shared Context (already there). Determine context by route param: `integrationId` present → `<IntegrationCodeWorkflowDetail integrationId=… language=… />`; `projectId` → `<ProjectCodeWorkflowDetail projectId=… language=… />`. Polyglot allow-list + undefined→false unchanged.

### 10. `Integration.tsx` flag population
Populate the `WorkflowEditorProvider` value's `codeWorkflow`/`codeWorkflowLanguage` from `integration?.codeWorkflow`/`integration?.codeWorkflowLanguage` (replacing CW-A's `undefined` placeholders), mirroring `Project.tsx`.

### 11. Embedded GraphQL ops + codegen
`client/src/graphql/integration-code-workflow/*.graphql` (`codeWorkflowSource(integrationId)`, `updateCodeWorkflowSource`, `createCodeWorkflow(componentName, language)`) → codegen the hooks. Register the embedded schema path in `client/codegen.ts` if not already globbed.

### 12. "New code workflow" entry (embedded)
Add a `DropdownMenu` to `Integrations.tsx` (today only a plain "New Integration" button) with a "New code workflow" item → `NewIntegrationCodeWorkflowDialog` (componentName + language ∈ {JS,Py,Ruby}, **no workspaceId**) → `createCodeWorkflow` → navigate to `/embedded/integrations/{id}/integration-workflows/{firstWorkflowId}`.

## Data flow (create an embedded code workflow)
1. "New code workflow" → componentName + language → `createCodeWorkflow` → server rejects a colliding componentName else renders starter + deploys (new code-backed integration) → returns integration id.
2. Client navigates; `getIntegration` returns `codeWorkflow=true` + language; `Integration.tsx` feeds the shared Context; `WorkflowEditorLayout` renders `IntegrationCodeWorkflowDetail`.
3. Edit + Save → `updateCodeWorkflowSource` (compile-gate + componentName-lock) → artifact re-stored, workflows re-registered, integration published.

## Testing
- Server: `IntegrationCodeWorkflowFacade` create/get/update tests (create-only guard on componentName collision; compile-gate; name-lock; Java rejected); starter-loads TDD via the real `IntegrationHandlerLoader` (Python/Ruby availability-guarded); `IntegrationCodeWorkflowGraphQlController` test; `IntegrationFacadeImpl` flag populated on single fetch; migration applies.
- Client: `CodeWorkflowSourceEditor` renders+saves; both wrappers fetch via their hooks; `WorkflowEditorLayout` picks the right wrapper by param; `NewIntegrationCodeWorkflowDialog` calls `createCodeWorkflow` + navigates; `Integration.tsx` threads the flag.

## Rollout / compatibility
- Additive server stack + one behavior-preserving client refactor. Automation CW-A behavior unchanged (the refactor keeps `ProjectCodeWorkflowDetail` equivalent).
- New `integration_code_workflow` table via Liquibase.
- After CW-B, the shared `WorkflowEditorLayout` branch fires for both surfaces; CW-C copilot then layers on both.
