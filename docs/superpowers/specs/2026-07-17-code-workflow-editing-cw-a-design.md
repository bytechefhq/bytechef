# Code Workflow Source Editing — Automation (CW-A) — Design

**Date:** 2026-07-17
**Status:** Approved (design)
**Initiative:** Code Workflows — the "same treatment" custom components received (details page + Monaco editor + create-empty + AI Hub copilot + open-in-panel), applied to code-backed workflows.
**This sub-project (CW-A):** Automation-side source editing + create-empty. Sibling sub-projects (own specs, later): **CW-B** = embedded Integrations (net-new deploy stack + editing), **CW-C** = AI Hub copilot (automation only for now; embedded copilots added later as a batch).
**Area:** EE `platform-code-workflow`, EE `automation-configuration` (`ProjectCodeWorkflow*`), CE `automation-configuration` (`ProjectFacadeImpl`/`ProjectDTO`), automation client Projects + the shared platform workflow-editor layer.

## Background

A "code workflow" is a whole code-backed **Project**: an artifact (JS/Python/Ruby script, or a Java JAR) authored with the SDK `ProjectHandler`/`WorkflowDsl` DSL and deployed. `ProjectCodeWorkflowFacadeImpl.save(workspaceId, byte[], Language)` already loads the artifact via `ProjectHandlerLoader`, reads `ProjectDefinition.getName()`, creates/updates the Project of that name, stores the artifact as the container's `FileEntry`, registers the workflows, and publishes. Today there is **no source read/write, no create-from-scratch, and no client editor** for code workflows — only admin upload/deploy. CW-A adds the read/write/create surface and swaps the client editor to Monaco when a project is code-backed, mirroring custom-component SP-A.

## Scope decisions (from brainstorming)

- **Under the Project, not settings.** The editor mounts inside the automation project view via a single shared branch in the platform `WorkflowEditorLayout` (which both `Project.tsx` and, later, embedded `Integration.tsx` feed) — gated on a code-backed flag. No new settings list.
- **No Java, at all.** The treatment is **polyglot-only** (JavaScript, Python, Ruby): every code workflow in scope is editable source. There is no read-only Java definition view and no Java create. Java-backed code workflows (deployable via the existing upload path) are explicitly **out of scope** for CW-A — they get no source editor.
- **Create-empty for JS, Python, and Ruby.** The "New code workflow" flow offers all three polyglot languages; each has a starter `ProjectHandler` template with the chosen project name embedded. First save deploys it, creating a new code-backed project.
- **One editable unit = one artifact.** A `CodeWorkflowContainer` holds one source artifact (one script) that may define multiple workflows. The editor edits that single script; it is not a per-workflow or multi-file editor.
- **Compile-gated + name-locked writes.** `updateCodeWorkflowSource` re-loads the edited script through `ProjectHandlerLoader`; an invalid script is rejected with the loader error, and the re-loaded `ProjectDefinition.getName()` must equal the existing project's name (a source rename must not silently fork a new project).

## Non-goals (CW-A)

- Embedded Integrations (CW-B) and the AI Hub copilot (CW-C).
- Java code workflows (no create, no source editor — out of scope entirely).
- Per-workflow or multi-file editing (one artifact = one source string).
- Changing the existing upload/deploy REST path or its admin authorization.

## Server design

### 1. `CodeWorkflowFileStorage` (EE `platform-code-workflow-file-storage`)

Add `String readCodeWorkflowFileContent(FileEntry fileEntry)` (via `fileStorageService.readFileToString`), mirroring `CustomComponentFileStorage.readCustomComponentFileContent`. The container already stores the artifact as `FileEntry workflows`.

### 2. `ProjectCodeWorkflowFacade` + Impl (EE `automation-configuration-service`)

Add three methods (all resolving the container for a project via `ProjectCodeWorkflowService` → `CodeWorkflowContainerService`):

- `String getCodeWorkflowSource(long projectId)` — read the container artifact's script text. Rejects a Java-backed container with a typed error (`LANGUAGE_NOT_SUPPORTED` / a `CodeWorkflowErrorType`), since Java is out of scope.
- `void updateCodeWorkflowSource(long projectId, String content)` — **compile-gate**: load `content` via `ProjectHandlerLoader` into a `ProjectDefinition`; on failure surface the loader error. **Name-lock**: reject if `projectDefinition.getName()` ≠ the existing project's name. On success, restore the artifact `FileEntry` from `content`, re-register the workflows, and publish — reusing the existing `save`-path internals constrained to this project id (do not create a new project).
- `Project createEmptyCodeWorkflow(long workspaceId, String name, Language language)` — validate `name` (reject blank / quotes / backslashes / newlines, as custom components do) and `language ∈ {JAVASCRIPT, PYTHON, RUBY}` (else `LANGUAGE_NOT_SUPPORTED`); render the language's starter template with `name` substituted; deploy the rendered bytes through the existing `save` path (creates the project); return the created `Project`.

Authorization mirrors the existing deploy path (`@PreAuthorize` admin) unless a narrower rule is already established for project editing — match whatever `save` uses so create/update are consistent with deploy.

### 3. Starter templates (EE `automation-configuration-service` resources)

`code-workflow-templates/starter.js`, `starter.py`, `starter.rb` — each a minimal valid `ProjectHandler` script (per the SDK `ProjectDsl`/`WorkflowDsl`) with a `__NAME__` placeholder for the project name and one trivial example workflow. Content authored against the polyglot loader fixtures used by `ProjectHandlerPolyglotEngineTest` so each starter loads clean on first deploy.

### 4. `ProjectDTO` + `ProjectModel` code-backed flag

Add `codeWorkflow: boolean` and `codeWorkflowLanguage` (nullable string enum) to `ProjectDTO` and the generated `ProjectModel` (OpenAPI). Populate in `ProjectFacadeImpl.toProjectDTO`. Because `ProjectFacadeImpl` is CE and `ProjectCodeWorkflowService` is EE, resolution goes through an **optional EE SPI**: a CE interface `ProjectCodeWorkflowInfoSupplier { Optional<CodeWorkflowInfo> fetch(long projectId); }` injected as `ObjectProvider`/`Optional`, with an EE `@Component` impl. Absent (CE) → flag false / language null. For Java-backed containers the supplier reports `codeWorkflow = true` but a `codeWorkflowLanguage` of `JAVA`, and the client simply does not render the source editor for `JAVA` (out of scope), so no Java editor path is ever reached.

### 5. GraphQL surface

New `code-workflow.graphqls` + `CodeWorkflowGraphQlController` (EE):
- `codeWorkflowSource(projectId: ID!): String!`
- `updateCodeWorkflowSource(projectId: ID!, content: String!): Boolean!`
- `createCodeWorkflow(workspaceId: ID!, name: String!, language: CodeWorkflowLanguage!): ID!` (returns the new project id)

`CodeWorkflowLanguage` enum = `JAVASCRIPT | PYTHON | RUBY` (SCREAMING_SNAKE per convention; Java intentionally excluded from the create/edit GraphQL surface). Errors surface via the global GraphQL error handling (toast).

## Client design

### 6. Regenerated types + shared flag threading

Regenerate the REST middleware so `Project.codeWorkflow` / `Project.codeWorkflowLanguage` exist. Expose the flag on the shared `WorkflowEditorProvider`/store that both `Project.tsx` and (later) `Integration.tsx` feed, so the shared layer can branch. `Project.tsx` already prefetches `getProject` (route loader), so the flag is available without an extra fetch.

### 7. Shared `CodeWorkflowDetail.tsx`

A clone of `CustomComponentDetail` in a **shared/platform** location (so CW-B reuses it): lazy `MonacoEditorWrapper`, GraphQL `useCodeWorkflowSourceQuery` / `useUpdateCodeWorkflowSourceMutation`, Monaco language chosen from `codeWorkflowLanguage` (js/python/ruby). Compile-gated Save with dirty tracking (`latestSourceRef`/`isSourceDirty`), surfacing the server's loader/name-lock errors via the global toast. No Java branch — the component is only mounted for JS/Python/Ruby.

### 8. One shared branch in `WorkflowEditorLayout`

If the shared flag says the project is code-backed **and** `codeWorkflowLanguage ∈ {JS, Python, Ruby}`, render `<CodeWorkflowDetail projectId=.../>` in place of the React Flow `WorkflowEditor`; otherwise render the visual editor unchanged. Visual-editor-only header/toolbar actions are hidden for code projects (minor); the project left sidebar remains (informational workflow list).

### 9. "New code workflow" entry point

Add a "New code workflow" item to the Projects "New Project" `DropdownMenu` (alongside "From Template" / "Import Project"). It opens a dialog (name + language ∈ {JavaScript, Python, Ruby}) → `createCodeWorkflow` mutation → navigate to the returned project id, which (being code-backed) renders the source editor with the deployed starter.

## Data flow

1. User picks "New code workflow" → name + language → `createCodeWorkflow` → server renders the starter, deploys it (new code-backed project), returns the project id.
2. Client navigates to the project; the route loader's `getProject` returns `codeWorkflow=true` + `codeWorkflowLanguage`; the shared branch renders `CodeWorkflowDetail`.
3. User edits the script and Saves → `updateCodeWorkflowSource` (compile-gate + name-lock) → artifact re-stored, workflows re-registered, project published; a compile/name error returns as a toast and the edit is not persisted.

## Error handling

- Invalid script on create/update → `ProjectHandlerLoader` error wrapped as a typed `CodeWorkflowErrorType`, surfaced to the client toast; the edit is rejected.
- Source rename (name mismatch) → typed rejection; the client keeps the buffer.
- Java-backed container reaching a source getter → `LANGUAGE_NOT_SUPPORTED` (should be unreachable from the UI, which never mounts the editor for Java).

## Testing

- Server: facade tests for `getCodeWorkflowSource` (polyglot returns text; Java rejected), `updateCodeWorkflowSource` (happy path re-deploys; invalid script rejected; name mismatch rejected), `createEmptyCodeWorkflow` (each of JS/Python/Ruby deploys a loadable starter; blank/invalid name rejected; Java rejected). A `CodeWorkflowGraphQlController` test for the three operations.
- Client: `CodeWorkflowDetail` renders + saves (mocked GraphQL); the `WorkflowEditorLayout` branch renders the source editor when the flag is set and the visual editor otherwise; the "New code workflow" dialog calls `createCodeWorkflow` and navigates.

## Rollout / compatibility

- Additive: new facade methods, GraphQL, DTO fields, starter resources, and client component + one branch. The existing upload/deploy path is untouched.
- CE stays functional: the optional EE supplier is absent, so `codeWorkflow` is always false and the visual editor renders as before.
- The shared `CodeWorkflowDetail` + shared-store flag are built to be reused by CW-B (embedded) without rework.
