# Code Workflow Source Editing — Automation (CW-A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an automation code-backed Project be created from scratch (JS/Python/Ruby starter) and its single source artifact edited in a Monaco editor mounted in the project view, with compile-gated, name-locked saves.

**Architecture:** Extend the existing `ProjectCodeWorkflowFacadeImpl.save(...)` deploy path with source read/write/create-empty; expose them over GraphQL; add a code-backed flag to `ProjectDTO`/`ProjectModel` (populated via an optional EE SPI so the CE facade stays clean); and branch the shared platform `WorkflowEditorLayout` to render a shared `CodeWorkflowDetail` (Monaco) when the project is code-backed. This is the direct analog of custom-component SP-A — reuse those files as templates.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Spring GraphQL, GraalVM polyglot (`ProjectHandlerLoader`), React 19 / TS / Zustand, Monaco, GraphQL codegen.

## Global Constraints

- **Polyglot only — no Java.** Create-empty and source editing support **JavaScript, Python, Ruby** only. Java code workflows are out of scope: no create, no source editor; a source getter reaching a Java-backed container returns a typed `LANGUAGE_NOT_SUPPORTED` error.
- **Compile-gate + name-lock on writes.** `updateCodeWorkflowSource` re-loads the edited script via `ProjectHandlerLoader`; invalid → typed loader error, not persisted. The re-loaded `ProjectDefinition.getName()` must equal the existing project name, else reject (no silent project fork).
- **One artifact = one source string.** The editor edits the container's single script; not per-workflow, not multi-file.
- **EE files** (`server/ee/**`): ByteChef Enterprise license header + `@version ee` Javadoc tag. **CE files** (`server/libs/**`): Apache 2.0 header.
- **GraphQL enum values** SCREAMING_SNAKE_CASE. **Client**: interface names end `I`/`Props`; object keys alph/`sort-keys`; icon imports `…Icon`; `twMerge` not `cn`; run `npm run check` before any client commit.
- Reuse these just-built custom-component analogs as templates (do NOT re-invent): `CustomComponentFileStorage.readCustomComponentFileContent`, `CustomComponentFacadeImpl.getCustomComponentSource/updateCustomComponentSource/createEmptyCustomComponent`, `custom-component-templates/starter.js`, `CustomComponentGraphQlController` + `custom-component.graphqls`, `client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx`.
- Commit only files this task touches; fresh commits (never amend on `0_732`); `./gradlew spotlessApply` before server commits.

---

### Task 1: `CodeWorkflowFileStorage.readCodeWorkflowFileContent` + error types

**Files:**
- Modify: `server/ee/libs/platform/platform-code-workflow/platform-code-workflow-file-storage/platform-code-workflow-file-storage-api/src/main/java/.../CodeWorkflowFileStorage.java` (+ its Impl in the sibling `-service` module)
- Modify: the `CodeWorkflowErrorType` enum (find it: `grep -rn "enum CodeWorkflowErrorType" server/ee --include=*.java`)
- Test: the file-storage service test module (mirror `CustomComponentFileStorage` tests if present)

**Interfaces:**
- Consumes: `com.bytechef.file.storage.domain.FileEntry`, `FileStorageService.readFileToString`.
- Produces: `String readCodeWorkflowFileContent(FileEntry fileEntry)` on the interface + impl; `CodeWorkflowErrorType` values `LANGUAGE_NOT_SUPPORTED`, `INVALID_CODE_WORKFLOW_NAME`, `CODE_WORKFLOW_NAME_MISMATCH`, `SOURCE_LOAD_FAILED` (append; keep existing values/ordinals).

- [ ] **Step 1: Read the analog** — `CustomComponentFileStorage.readCustomComponentFileContent` and its impl, to mirror exactly (same `readFileToString` call, same `@version ee` header).

- [ ] **Step 2: Add the interface method + impl.** Interface:
```java
String readCodeWorkflowFileContent(FileEntry fileEntry);
```
Impl (mirror the custom-component impl — resolve via the injected `FileStorageService` and the code-workflow file-storage directory constant already used by the storage impl):
```java
@Override
public String readCodeWorkflowFileContent(FileEntry fileEntry) {
    return fileStorageService.readFileToString(getFileStorageDirectory(), fileEntry);
}
```
(Use whatever directory accessor the existing `store`/artifact methods in this impl already use.)

- [ ] **Step 3: Append the error-type constants** at the END of `CodeWorkflowErrorType` (append-only), following its existing constructor shape.

- [ ] **Step 4: Test** — a focused unit test that a stored artifact round-trips through `readCodeWorkflowFileContent` (mirror the custom-component file-storage test; if none exists, assert via a stubbed `FileStorageService`).

- [ ] **Step 5: Build + commit**
```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:platform:platform-code-workflow:platform-code-workflow-file-storage:platform-code-workflow-file-storage-service:compileJava
git add <the three files>
git commit -m "732 Add code workflow source file read + error types"
```

---

### Task 2: Starter templates + `createEmptyCodeWorkflow` (JS/Python/Ruby)

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/code-workflow-templates/starter.js`, `starter.py`, `starter.rb`
- Modify: `ProjectCodeWorkflowFacade.java` + `ProjectCodeWorkflowFacadeImpl.java` (EE `automation-configuration-service`)
- Test: `ProjectCodeWorkflowFacadeCreateEmptyTest.java` (new)

**Interfaces:**
- Consumes: `ProjectHandlerLoader.loadProjectHandler(...)` (via the existing private `loadProjectDefinition(language, bytes)` in the Impl), the existing `save`-path internals, `CodeWorkflowContainer.Language`.
- Produces: `Project createEmptyCodeWorkflow(long workspaceId, String name, CodeWorkflowContainer.Language language)`.

- [ ] **Step 1: Discover the exact polyglot contract.** Read `ProjectHandlerPolyglotEngine.java:50-107` — the non-Java `load` path evaluates the script and reads members `name` (req String), `description` (String), `version` (String), `workflows` (list); each workflow map has `name`, `label`, `tasks`; each task map has `name`, `label`, `perform` (a `function()` returning the task output, invoked via `apply(null)`). The completion value must be a **bare object literal** (plain eval, NOT a module).

- [ ] **Step 2: Write `starter.js`** — a `__NAME__` placeholder for the project name; one example workflow + task:
```
({
    name: "__NAME__",
    version: "1",
    description: "A code workflow.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            tasks: [
                {
                    name: "my-task",
                    label: "My Task",
                    perform: function () {
                        return "hello";
                    }
                }
            ]
        }
    ]
})
```

- [ ] **Step 3: Write `starter.py`** — Python dict as the completion value (last expression). Author to the same member names; a Python `lambda`/`def` for `perform` compatible with `apply(null)` (a zero-arg callable). Draft:
```
{
    "name": "__NAME__",
    "version": "1",
    "description": "A code workflow.",
    "workflows": [
        {
            "name": "my-workflow",
            "label": "My Workflow",
            "tasks": [
                {"name": "my-task", "label": "My Task", "perform": lambda *args: "hello"}
            ]
        }
    ]
}
```

- [ ] **Step 4: Write `starter.rb`** — a Ruby Hash as the completion value; a `->` proc / lambda for `perform`. Draft:
```
{
  "name" => "__NAME__",
  "version" => "1",
  "description" => "A code workflow.",
  "workflows" => [
    {
      "name" => "my-workflow",
      "label" => "My Workflow",
      "tasks" => [
        { "name" => "my-task", "label" => "My Task", "perform" => lambda { |*args| "hello" } }
      ]
    }
  ]
}
```

- [ ] **Step 5: Write the RED test** proving each starter loads. This is the correctness gate for the drafts above — the exact Python/Ruby member/callable shape must be whatever `ProjectHandlerPolyglotEngine` actually accepts; iterate the template until the test passes (Ruby/Python may be skipped if the polyglot language isn't available on the CI platform — guard with the same availability assumption `ProjectHandlerPolyglotEngineTest` uses):
```java
@Test
void testCreateEmptyJavaScriptDeploysLoadableProject() {
    Project project = facade.createEmptyCodeWorkflow(workspaceId, "my-code-project", Language.JAVASCRIPT);
    assertThat(project.getId()).isNotNull();
    assertThat(project.getName()).isEqualTo("my-code-project");
}
```
Run: `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectCodeWorkflowFacadeCreateEmptyTest"` → expect FAIL (method missing), then FAIL/iterate on template load until GREEN.

- [ ] **Step 6: Implement `createEmptyCodeWorkflow`.** Validate `name` (reject null/blank/`"`/`\`/`\n`/`\r` → `INVALID_CODE_WORKFLOW_NAME`), validate `language ∈ {JAVASCRIPT, PYTHON, RUBY}` (else `LANGUAGE_NOT_SUPPORTED`), load the matching starter resource, substitute `__NAME__` with the validated name, and deploy via the existing `save` path (which creates the project of that name). Return the created `Project` (fetch by name after save). Add the interface method too.

- [ ] **Step 7: GREEN** — run the test for all available languages; confirm pristine output. Commit:
```bash
./gradlew spotlessApply
git add <starters + facade + impl + test>
git commit -m "732 Add code workflow starters + createEmptyCodeWorkflow (JS/Python/Ruby)"
```

---

### Task 3: `getCodeWorkflowSource` + `updateCodeWorkflowSource` (compile-gate + name-lock)

**Files:**
- Modify: `ProjectCodeWorkflowFacade.java` + `ProjectCodeWorkflowFacadeImpl.java`
- Test: `ProjectCodeWorkflowFacadeSourceTest.java` (new)

**Interfaces:**
- Consumes: `ProjectCodeWorkflowService` (resolve container by projectId), `CodeWorkflowContainerService`, `CodeWorkflowFileStorage.readCodeWorkflowFileContent` (Task 1), the Impl's `loadProjectDefinition`, `ProjectService`.
- Produces: `String getCodeWorkflowSource(long projectId)`; `void updateCodeWorkflowSource(long projectId, String content)`.

- [ ] **Step 1: Read the analog** — `CustomComponentFacadeImpl.getCustomComponentSource` / `updateCustomComponentSource` (compile-gate + name-lock pattern).

- [ ] **Step 2: RED tests** (mirror the custom-component source tests):
  - `getCodeWorkflowSource` returns the stored script for a polyglot container; throws `LANGUAGE_NOT_SUPPORTED` for a Java container.
  - `updateCodeWorkflowSource` with a valid same-name script re-deploys (artifact updated); with an unparseable script throws `SOURCE_LOAD_FAILED`; with a script whose `ProjectDefinition.getName()` differs throws `CODE_WORKFLOW_NAME_MISMATCH`.
  Run the test → expect FAIL (methods missing).

- [ ] **Step 3: Implement `getCodeWorkflowSource`** — resolve the container for `projectId` via `ProjectCodeWorkflowService`; if `container.getLanguage() == JAVA` throw `LANGUAGE_NOT_SUPPORTED`; return `codeWorkflowFileStorage.readCodeWorkflowFileContent(container.getWorkflows())`.

- [ ] **Step 4: Implement `updateCodeWorkflowSource`** — resolve the container + owning project; `bytes = content.getBytes(UTF_8)`; `ProjectDefinition def = loadProjectDefinition(container.getLanguage(), bytes)` inside try/catch → on `Exception` throw `ConfigurationException(..., SOURCE_LOAD_FAILED)`; if `!def.getName().equals(project.getName())` throw `CODE_WORKFLOW_NAME_MISMATCH`; then re-store the artifact + re-register workflows + publish, constrained to this project (factor the shared body out of `save`, or call a private `deployInto(project, def, bytes, language)`), NOT the name-based create-or-update in `save`.

- [ ] **Step 5: GREEN + commit**
```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:test --tests "*ProjectCodeWorkflowFacadeSourceTest"
git add <facade + impl + test>
git commit -m "732 Add code workflow source get/update (compile-gate + name-lock)"
```

---

### Task 4: GraphQL surface — schema + `CodeWorkflowGraphQlController`

**Files:**
- Create: `.../code-workflow.graphqls` (in the EE automation-configuration graphql resources; locate the module by `grep -rl "custom-component.graphqls\|graphqls" server/ee --include=*.graphqls | head` and follow the custom-component controller's module layout)
- Create: `CodeWorkflowGraphQlController.java` (EE)
- Register the schema path in `client/codegen.ts` `schema` array (client task will regenerate)
- Test: `CodeWorkflowGraphQlControllerTest.java`

**Interfaces:**
- Consumes: `ProjectCodeWorkflowFacade` (Tasks 2-3).
- Produces GraphQL ops: `codeWorkflowSource(projectId: ID!): String!`, `updateCodeWorkflowSource(projectId: ID!, content: String!): Boolean!`, `createCodeWorkflow(workspaceId: ID!, name: String!, language: CodeWorkflowLanguage!): ID!`.

- [ ] **Step 1: Read the analog** — `CustomComponentGraphQlController` + `custom-component.graphqls`.

- [ ] **Step 2: Write the schema:**
```graphql
enum CodeWorkflowLanguage { JAVASCRIPT, PYTHON, RUBY }

extend type Query {
    codeWorkflowSource(projectId: ID!): String!
}

extend type Mutation {
    updateCodeWorkflowSource(projectId: ID!, content: String!): Boolean!
    createCodeWorkflow(workspaceId: ID!, name: String!, language: CodeWorkflowLanguage!): ID!
}
```

- [ ] **Step 3: Write the controller** (EE header + `@version ee`), `@QueryMapping`/`@MutationMapping` delegating to the facade, mapping `CodeWorkflowLanguage` → `CodeWorkflowContainer.Language`; `updateCodeWorkflowSource` returns `true`; `createCodeWorkflow` returns the new project id as a String/ID. Mirror the custom-component controller's argument handling + return types.

- [ ] **Step 4: Test** — mock the facade, verify each mapping calls the right facade method and returns the mapped value (mirror the custom-component controller test).

- [ ] **Step 5: Build + commit**
```bash
./gradlew spotlessApply
./gradlew :<the graphql module>:compileJava :<the graphql module>:test --tests "*CodeWorkflowGraphQlControllerTest"
git add <schema + controller + test + client/codegen.ts>
git commit -m "732 Add code workflow GraphQL surface (source/update/create)"
```

---

### Task 5: `ProjectDTO`/`ProjectModel` code-backed flag via optional EE SPI

**Files:**
- Create (CE): `ProjectCodeWorkflowInfoSupplier.java` interface + a `CodeWorkflowInfo(boolean present, String language)` record, in the CE `automation-configuration-api`.
- Create (EE): `ProjectCodeWorkflowInfoSupplierImpl.java` `@Component` in EE `automation-configuration-service`, backed by `ProjectCodeWorkflowService` + `CodeWorkflowContainerService`.
- Modify (CE): `ProjectDTO.java` (add `boolean codeWorkflow`, `String codeWorkflowLanguage`), `ProjectFacadeImpl.toProjectDTO` (inject `ObjectProvider<ProjectCodeWorkflowInfoSupplier>`, populate), and the OpenAPI spec for `ProjectModel` (add the two fields) — then regenerate.
- Test: `ProjectFacadeImplCodeWorkflowFlagTest` or extend an existing `ProjectFacadeImpl` test.

**Interfaces:**
- Consumes: `ObjectProvider<ProjectCodeWorkflowInfoSupplier>` (absent in CE).
- Produces: `ProjectDTO.codeWorkflow` / `.codeWorkflowLanguage`; generated `ProjectModel` fields.

- [ ] **Step 1: Define the CE SPI:**
```java
public interface ProjectCodeWorkflowInfoSupplier {
    Optional<CodeWorkflowInfo> fetchCodeWorkflowInfo(long projectId);

    record CodeWorkflowInfo(String language) {}
}
```

- [ ] **Step 2: EE impl** (`@Component`, EE header + `@version ee`): resolve the container for the project via `ProjectCodeWorkflowService`; return `Optional.of(new CodeWorkflowInfo(container.getLanguage().name()))` if present, else `Optional.empty()`.

- [ ] **Step 3: Add the DTO fields + populate in `toProjectDTO`.** Inject the `ObjectProvider`; in `toProjectDTO`, `var info = supplierProvider.getIfAvailable(); var cw = info == null ? Optional.empty() : info.fetchCodeWorkflowInfo(project.getId());` set `codeWorkflow = cw.isPresent()`, `codeWorkflowLanguage = cw.map(CodeWorkflowInfo::language).orElse(null)`. Add the two fields to the `ProjectDTO` record + its construction sites.

- [ ] **Step 4: OpenAPI** — add `codeWorkflow` (boolean) + `codeWorkflowLanguage` (string, nullable) to the `Project` schema in the automation-configuration OpenAPI yaml; regenerate (`./gradlew :<automation-configuration-rest module>:openApiGenerate` or the project's generate task).

- [ ] **Step 5: Test** — with no supplier bean → `codeWorkflow=false`, language null; with a stub supplier returning a language → `codeWorkflow=true` + language set.

- [ ] **Step 6: Build + commit**
```bash
./gradlew spotlessApply
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*CodeWorkflowFlag*"
git add <spi + impl + dto + facade + openapi + generated + test>
git commit -m "732 Add codeWorkflow flag to ProjectDTO via optional EE supplier"
```

---

### Task 6: Client — regenerate types + thread flag through the shared workflow-editor store

**Files:**
- Regenerate: `client/src/shared/middleware/automation/configuration/**` (from the updated OpenAPI) so `Project` has `codeWorkflow` / `codeWorkflowLanguage`.
- Modify: `client/src/pages/automation/project/Project.tsx` (pass the flag into `WorkflowEditorProvider`), the shared `WorkflowEditorProvider` / `useWorkflowDataStore` (add `codeWorkflow?: boolean; codeWorkflowLanguage?: string` to the shared state both project + integration flows populate). Locate the provider/store: `client/src/pages/platform/workflow-editor/**` (from Explore: `WorkflowEditorProvider` + `useWorkflowDataStore`).
- Test: a store/provider unit test that the flag is exposed.

**Interfaces:**
- Consumes: `project.codeWorkflow`, `project.codeWorkflowLanguage` from the prefetched `getProject`.
- Produces: shared-store fields `codeWorkflow`, `codeWorkflowLanguage` readable by `WorkflowEditorLayout` (Task 8).

- [ ] **Step 1: Regenerate the client middleware** for automation-configuration and confirm `Project.ts` gained the two fields.

- [ ] **Step 2: Add the fields to the shared store/provider** and set them from the project in `Project.tsx` (guard: integration flow leaves them undefined for now — CW-B wires it).

- [ ] **Step 3: Test + `npm run check` + commit**
```bash
cd client && npm run check
git add <regenerated middleware + provider/store + Project.tsx + test>
git commit -m "732 client - Thread codeWorkflow flag through shared workflow-editor store"
```

---

### Task 7: Client — shared `CodeWorkflowDetail` (Monaco + GraphQL source ops)

**Files:**
- Create GraphQL operations: `client/src/graphql/code-workflow/*.graphql` (`codeWorkflowSource` query, `updateCodeWorkflowSource` + `createCodeWorkflow` mutations); run `npx graphql-codegen`.
- Create: `client/src/pages/platform/code-workflow/CodeWorkflowDetail.tsx` (shared/platform location so CW-B reuses it).
- Test: `CodeWorkflowDetail.test.tsx`.

**Interfaces:**
- Consumes: generated `useCodeWorkflowSourceQuery`, `useUpdateCodeWorkflowSourceMutation`; `MonacoEditorWrapper`.
- Produces: `<CodeWorkflowDetail projectId={string} language={string} />`.

- [ ] **Step 1: Add + codegen the GraphQL ops.** Add the schema path to `client/codegen.ts`, write the `.graphql` files, run `cd client && npx graphql-codegen`.

- [ ] **Step 2: Clone `CustomComponentDetail.tsx`** into `CodeWorkflowDetail.tsx`, stripping the Java-definition branch entirely (no Java): lazy `MonacoEditorWrapper`, Monaco language from `language` (`JAVASCRIPT→javascript`, `PYTHON→python`, `RUBY→ruby`), `useCodeWorkflowSourceQuery({projectId})`, Save via `useUpdateCodeWorkflowSourceMutation` with dirty tracking (`latestSourceRef`/`isSourceDirty`), errors via the global toast. Props: `{projectId: string; language: string}`.

- [ ] **Step 3: Test** (mock the generated hooks): renders the editor with fetched source; Save calls the mutation with `{projectId, content}`; dirty state toggles.

- [ ] **Step 4: `npm run check` + commit**
```bash
cd client && npm run check
git add <graphql ops + generated graphql.ts + CodeWorkflowDetail.tsx + test + codegen.ts>
git commit -m "732 client - Add shared CodeWorkflowDetail Monaco source editor"
```

---

### Task 8: Client — branch `WorkflowEditorLayout` to the source editor

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/WorkflowEditorLayout.tsx`
- Test: `WorkflowEditorLayout.test.tsx` (branch coverage)

**Interfaces:**
- Consumes: shared-store `codeWorkflow` / `codeWorkflowLanguage` (Task 6); `CodeWorkflowDetail` (Task 7); the layout's existing `projectId` context.
- Produces: source editor rendered in place of `WorkflowEditor` when code-backed + polyglot.

- [ ] **Step 1: Add the branch.** Near the `WorkflowEditor` mount, read `codeWorkflow`/`codeWorkflowLanguage` from the shared store; if `codeWorkflow && codeWorkflowLanguage in {JAVASCRIPT,PYTHON,RUBY}`, render `<CodeWorkflowDetail projectId={projectId} language={codeWorkflowLanguage} />` instead of `<WorkflowEditor .../>`. Java-backed (or non-code) projects fall through to the visual editor unchanged.

- [ ] **Step 2: Hide visual-only header/toolbar actions** for code projects if they render errors (keep minimal — only what breaks). Note anything deferred as a code comment (no `TODO:`).

- [ ] **Step 3: Test** — flag set + JS language → `CodeWorkflowDetail` rendered; flag unset → `WorkflowEditor` rendered; language JAVA → visual editor (not the source editor).

- [ ] **Step 4: `npm run check` + commit**
```bash
cd client && npm run check
git add <WorkflowEditorLayout.tsx + test>
git commit -m "732 client - Render source editor for code-backed projects"
```

---

### Task 9: Client — "New code workflow" entry point

**Files:**
- Create: `client/src/pages/automation/projects/components/NewCodeWorkflowDialog.tsx`
- Modify: `client/src/pages/automation/projects/Projects.tsx` (add the dropdown item)
- Test: `NewCodeWorkflowDialog.test.tsx`

**Interfaces:**
- Consumes: generated `useCreateCodeWorkflowMutation` (Task 7 codegen), `useNavigate`, current `workspaceId`.
- Produces: a dialog (name + language ∈ {JavaScript, Python, Ruby}) that creates a code workflow and navigates to the new project.

- [ ] **Step 1: Build the dialog** (mirror `CreateCustomComponentDialog.tsx`): a name input + a language `Select` (JavaScript/Python/Ruby) → on submit call `createCodeWorkflow({workspaceId, name, language})` → on success `navigate('/automation/projects/' + newProjectId + '/project-workflows/' + <first workflow id or a landing route>)`. If the project route needs a `projectWorkflowId`, navigate to the project and let the loader/redirect resolve the first workflow (match how "New Project" navigates today).

- [ ] **Step 2: Add the dropdown item** "New code workflow" to the `DropdownMenu` in `Projects.tsx` (alongside "From Template" / "Import Project"), opening the dialog.

- [ ] **Step 3: Test** — submitting calls `createCodeWorkflow` with the chosen name+language and navigates to the returned id.

- [ ] **Step 4: `npm run check` + commit**
```bash
cd client && npm run check
git add <dialog + Projects.tsx + test>
git commit -m "732 client - Add New code workflow create dialog"
```

---

### Task 10: Full verification

- [ ] **Step 1: Server** — `./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-service:check` and the platform-code-workflow + graphql modules `check`. Expect BUILD SUCCESSFUL.
- [ ] **Step 2: Client** — `cd client && npm run check`. Expect pass.
- [ ] **Step 3: Wiring sanity** — grep that `ProjectDTO` populates the flag only via the `ObjectProvider` (no CE→EE compile dep), and that `CodeWorkflowDetail` has no Java branch.
- [ ] **Step 4: Commit** any spotless/format-only changes per the owning task; otherwise no-op.

---

## Self-Review

**Spec coverage:**
- §1 file storage → Task 1. §2 facade get/update/create → Tasks 2 (create) + 3 (get/update). §3 starters → Task 2. §4 DTO flag + EE SPI → Task 5. §5 GraphQL → Task 4. §6 client flag threading → Task 6. §7 `CodeWorkflowDetail` → Task 7. §8 `WorkflowEditorLayout` branch → Task 8. §9 New-code-workflow entry → Task 9. Testing/rollout → Tasks 2-9 tests + Task 10.

**Placeholder scan:** the Python/Ruby starter bodies (Task 2 Steps 3-4) are drafts explicitly gated by the RED test in Step 5 — the loader is the source of truth, so the implementer iterates the template until it loads rather than trusting the draft. All other code steps are concrete. No TBD/TODO.

**Type consistency:** `createEmptyCodeWorkflow(long, String, Language)` (Task 2) ↔ GraphQL `createCodeWorkflow` (Task 4) ↔ client `useCreateCodeWorkflowMutation` (Tasks 7,9). `getCodeWorkflowSource`/`updateCodeWorkflowSource` (Task 3) ↔ GraphQL (Task 4) ↔ client hooks (Task 7). `codeWorkflow`/`codeWorkflowLanguage` fields consistent across Task 5 (DTO/model), Task 6 (store), Task 8 (branch). `CodeWorkflowLanguage` enum {JAVASCRIPT,PYTHON,RUBY} consistent (Java excluded) across Tasks 4, 7, 9.

**Risk notes for execution:** Task 2 (polyglot starter correctness) is the highest-risk task — its TDD gate against the real `ProjectHandlerLoader` is mandatory, and Python/Ruby may be `assumeTrue`-skipped on platforms lacking those GraalVM languages (mirror `ProjectHandlerPolyglotEngineTest`'s availability guard). Task 5's CE/EE boundary (optional `ObjectProvider` supplier) and Task 8's shared-layout branch are the next-most-delicate.
