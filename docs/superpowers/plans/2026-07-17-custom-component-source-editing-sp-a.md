# Custom Component Source Editing + Create-Empty (SP-A) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Add in-app viewing/editing of a custom component's single source file (Monaco, non-Java, compile-gate save) plus create-empty for JavaScript, on the existing single-`FileEntry` storage model.

**Architecture:** Extend the existing `CustomComponentFileStorage` (read-content), `CustomComponentFacade` (get-source / update-source compile-gate / create-empty), and the GraphQL controller; add a client detail route with `MonacoEditorWrapper` and a create dialog. Reuse the existing `loadComponentDefinition` compile-gate helper (temp file → `ComponentHandlerLoader` → `getDefinition`).

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, GraphQL, GraalVM polyglot loader, React 19 / TS, Monaco, Vitest, JUnit 5.

## Global Constraints

- EE files (`server/ee/**`, `client/src/ee/**` server-side counterparts) use the ByteChef Enterprise license header; Java EE classes carry `@version ee`.
- All facade mutations admin-gated: `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")` (match `save`/`delete`).
- Compile-gate: source edits/creates go through `loadComponentDefinition(language, bytes)` which throws if the source doesn't eval; a rejected save must NOT overwrite the stored file.
- Java components: no source read/edit/create — reject with a clear error.
- The component `name` is the workflow-facing identity: `updateCustomComponentSource` rejects a save whose reloaded definition name differs from the stored name.
- SP-A create-empty ships **JavaScript only**; `createEmptyCustomComponent` rejects `PYTHON`/`RUBY`/`JAVA`.
- Client: interface names end `I`/`Props`; sort-keys ascending; named imports sorted; icons `*Icon`; `twMerge` not `cn()`. Run `cd client && npm run check` before client commits. Run `./gradlew spotlessApply` + the touched module `check` before server commits.
- Commit prefix: server `732 <desc>`, client `732 client - <desc>`.
- Spec: `docs/superpowers/specs/2026-07-17-custom-component-source-editing-sp-a-design.md`.

Key existing files:
- Facade iface: `.../platform-custom-component-configuration-api/.../facade/CustomComponentFacade.java`
- Facade impl: `.../platform-custom-component-configuration-service/.../facade/CustomComponentFacadeImpl.java` (has `loadComponentDefinition(language, bytes)`, `create(...)`, `update(...)`, `customComponentService.getCustomComponent(id)`)
- Storage iface: `.../platform-custom-component-file-storage-api/.../CustomComponentFileStorage.java` (+ impl in `-file-storage-impl`)
- GraphQL: `.../platform-custom-component-configuration-graphql/.../web/graphql/CustomComponentGraphQlController.java` + `src/main/resources/graphql/custom-component.graphqls`
- Domain: `.../configuration-api/.../domain/CustomComponent.java` (enum `Language{JAVA,JAVASCRIPT,PYTHON,RUBY}`, `getLanguage()` returns `Language`)

---

## Task 1: Read source — storage read-content + facade `getCustomComponentSource` + GraphQL query

**Files:**
- Modify: `CustomComponentFileStorage.java` (+ its impl in `-file-storage-impl`)
- Modify: `CustomComponentFacade.java` + `CustomComponentFacadeImpl.java`
- Modify: `custom-component.graphqls` + `CustomComponentGraphQlController.java`
- Test: `CustomComponentFacadeImplTest`/IntTest (find with `git grep -l CustomComponentFacade -- '*Test*'`)

**Interfaces:**
- Produces: `String CustomComponentFacade.getCustomComponentSource(long id)`; `String CustomComponentFileStorage.readCustomComponentFileContent(FileEntry)`.

- [ ] **Step 1: Failing facade test** — `getCustomComponentSource(id)` returns the stored source text for a non-Java component; throws for a Java component. (Mock `customComponentService.getCustomComponent(id)` → a `CustomComponent` with language JAVASCRIPT + a `FileEntry`; mock storage `readCustomComponentFileContent` → `"({name:'x'})"`. For Java case, language JAVA → expect exception.)

- [ ] **Step 2: Run → FAIL.** `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:test`

- [ ] **Step 3: Storage read method** — add to `CustomComponentFileStorage`:
```java
    String readCustomComponentFileContent(FileEntry componentFile);
```
Impl (mirror how the impl already resolves the underlying file-storage service for `storeCustomComponentFile`; read bytes → `new String(bytes, StandardCharsets.UTF_8)`, or use the file-storage service's read-as-string if one exists — read the impl to match its existing service field).

- [ ] **Step 4: Facade method** — interface + impl:
```java
    // interface
    String getCustomComponentSource(long id);

    // impl
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String getCustomComponentSource(long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        if (customComponent.getLanguage() == Language.JAVA) {
            throw new ConfigurationException(
                "Java custom components have no editable source",
                CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
        }

        return customComponentFileStorage.readCustomComponentFileContent(customComponent.getComponent());
    }
```
(Use whatever `CustomComponentErrorType` value fits, or add a `JAVA_SOURCE_NOT_EDITABLE` value if the enum is easy to extend — check the enum first.)

- [ ] **Step 5: GraphQL query** — schema `customComponentSource(id: ID!): String!`; controller:
```java
    @QueryMapping
    String customComponentSource(@Argument Long id) {
        return customComponentFacade.getCustomComponentSource(id);
    }
```

- [ ] **Step 6: Run → PASS** (module test). Also `compileJava` the graphql module.

- [ ] **Step 7: Commit**
```bash
./gradlew spotlessApply
git add -A server/ee/libs/platform/platform-custom-component
git commit -m "732 Add custom component source read (storage + facade + GraphQL)"
```

---

## Task 2: Edit source — `updateCustomComponentSource` compile-gate + GraphQL mutation

**Files:** same facade + graphql files as Task 1; same test class.

**Interfaces:**
- Consumes: existing `loadComponentDefinition(Language, byte[])`, `update(CustomComponent, ComponentDefinition)`, `customComponentFileStorage.storeCustomComponentFile`.
- Produces: `void CustomComponentFacade.updateCustomComponentSource(long id, String content)`.

- [ ] **Step 1: Failing tests** — (a) valid content: reloads, overwrites the file, and refreshes metadata (verify `storeCustomComponentFile` + `customComponentService.update` called); (b) invalid content: `loadComponentDefinition` throws → `updateCustomComponentSource` propagates AND `storeCustomComponentFile` is NOT called; (c) name-change: reloaded definition name != stored name → throws, no store. (Mock `loadComponentDefinition` behavior by feeding real minimal JS for the valid case, and a broken string for the invalid case; if `loadComponentDefinition` can't run in the test env — polyglot — restructure to mock the load boundary. Read the existing facade test to see whether it exercises the real loader or mocks it; MATCH that.)

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Facade method** — interface + impl:
```java
    // interface
    void updateCustomComponentSource(long id, String content);

    // impl
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateCustomComponentSource(long id, String content) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        Language language = customComponent.getLanguage();

        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java custom components have no editable source",
                CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            if (!Objects.equals(componentDefinition.getName(), customComponent.getName())) {
                throw new ConfigurationException(
                    "Renaming a component by editing its source is not supported (expected name '"
                        + customComponent.getName() + "')",
                    CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
            }

            FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
                componentDefinition.getName() + "_" + componentDefinition.getVersion() + "."
                    + language.getExtension(),
                bytes);

            customComponent.setComponent(componentFileEntry);

            update(customComponent, componentDefinition);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
```
(If a dedicated `CustomComponentErrorType` for validation/rename reads better and the enum is trivial to extend, add `SOURCE_VALIDATION_FAILED` / `SOURCE_RENAME_UNSUPPORTED` and use them instead of reusing the upload-disabled type. Check the enum first; prefer accurate types.)

- [ ] **Step 4: GraphQL mutation** — schema `updateCustomComponentSource(id: ID!, content: String!): Boolean!`; controller returns `true` after the facade call. The facade exception propagates as a GraphQL error carrying the message.

- [ ] **Step 5: Run → PASS** (module test + graphql compile).

- [ ] **Step 6: Commit**
```bash
./gradlew spotlessApply
git add -A server/ee/libs/platform/platform-custom-component
git commit -m "732 Add compile-gate custom component source update (facade + GraphQL)"
```

---

## Task 3: Create-empty (JavaScript) — starter template + facade + GraphQL mutation

**Files:** facade + graphql; new resource `.../configuration-service/src/main/resources/custom-component-templates/starter.js`.

**Interfaces:**
- Produces: `CustomComponent CustomComponentFacade.createEmptyCustomComponent(String name, CustomComponent.Language language)`.

- [ ] **Step 1: Add the JS starter template** at `.../configuration-service/src/main/resources/custom-component-templates/starter.js` — a minimal component that evals to the required object. Use a placeholder token `__NAME__` for the component name:
```js
({
    name: "__NAME__",
    title: "__NAME__",
    version: 1,
    description: "A custom component.",
    actions: [
        {
            name: "myAction",
            title: "My Action",
            description: "An example action.",
            perform: function (inputParameters, connectionParameters, context) {
                return {};
            }
        }
    ]
})
```

- [ ] **Step 2: Failing facade test** — `createEmptyCustomComponent("acme", JAVASCRIPT)` loads the template (name substituted), persists a component (verify `customComponentService.create` called with name "acme", language JAVASCRIPT), returns it. `createEmptyCustomComponent("acme", PYTHON)` throws (unsupported). Duplicate name (`fetchCustomComponent("acme", 1)` present) throws. (Again: if the test env can't run the polyglot loader, mock the load boundary and assert the template resource is read + name substituted; the actual load is covered by an IntTest that runs in CI/Docker — mark it `@Disabled`/tag if it can't run locally, mirroring how the module's other loader-dependent tests handle it.)

- [ ] **Step 3: Run → FAIL.**

- [ ] **Step 4: Facade method** — interface + impl:
```java
    // interface
    CustomComponent createEmptyCustomComponent(String name, CustomComponent.Language language);

    // impl
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent createEmptyCustomComponent(String name, Language language) {
        if (language != Language.JAVASCRIPT) {
            throw new ConfigurationException(
                "Create-empty currently supports JavaScript only",
                CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
        }

        if (customComponentService.fetchCustomComponent(name, 1)
            .isPresent()) {
            throw new ConfigurationException(
                "A custom component named '" + name + "' already exists",
                CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
        }

        String template = readTemplate(language).replace("__NAME__", name);

        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
                componentDefinition.getName() + "_" + componentDefinition.getVersion() + "."
                    + language.getExtension(),
                bytes);

            CustomComponent customComponent = new CustomComponent();

            customComponent.setComponentVersion(componentDefinition.getVersion());
            customComponent.setComponent(componentFileEntry);
            customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
            customComponent.setEnabled(true);
            customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
            customComponent.setName(componentDefinition.getName());
            customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));
            customComponent.setLanguage(language);

            return customComponentService.create(customComponent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readTemplate(Language language) {
        String resource = "custom-component-templates/starter." + language.getExtension();

        try (InputStream inputStream =
            CustomComponentFacadeImpl.class.getClassLoader().getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
```
(This duplicates the create() block — acceptable, or refactor `create()` to return the entity and reuse. Keep it readable.)

- [ ] **Step 5: GraphQL mutation** — schema `createCustomComponent(name: String!, language: CustomComponentLanguage!): CustomComponent!`; controller delegates to `customComponentFacade.createEmptyCustomComponent(name, language)`. (The GraphQL `CustomComponentLanguage` enum → `CustomComponent.Language` mapping already exists for the `language` field; reuse it.)

- [ ] **Step 6: Run → PASS.**

- [ ] **Step 7: Commit**
```bash
./gradlew spotlessApply
git add -A server/ee/libs/platform/platform-custom-component
git commit -m "732 Add create-empty custom component (JavaScript starter template + facade + GraphQL)"
```

---

## Task 4: Client GraphQL operations + codegen

**Files:** new `.graphql` files under `client/src/graphql/platform/custom-component/`; regen `graphql.ts`.

- [ ] **Step 1:** Add operation files mirroring the existing ones in that dir:
  - `customComponentSource.graphql` — `query customComponentSource($id: ID!) { customComponentSource(id: $id) }`
  - `updateCustomComponentSource.graphql` — `mutation updateCustomComponentSource($id: ID!, $content: String!) { updateCustomComponentSource(id: $id, content: $content) }`
  - `createCustomComponent.graphql` — `mutation createCustomComponent($name: String!, $language: CustomComponentLanguage!) { createCustomComponent(name: $name, language: $language) { id name language } }`

- [ ] **Step 2:** `cd client && npx graphql-codegen`. Verify the new hooks (`useCustomComponentSourceQuery`, `useUpdateCustomComponentSourceMutation`, `useCreateCustomComponentMutation`) appear.

- [ ] **Step 3:** `npm run typecheck`; commit the operation files + regenerated types:
```bash
cd client && git add src/graphql/platform/custom-component src/shared/middleware/graphql*.ts
git commit -m "732 client - Add custom component source/create GraphQL operations"
```

---

## Task 5: Detail route + Monaco editor (non-Java editable, Java read-only)

**Files:** new `client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx`; modify `client/src/routes.tsx` (add `custom-components/:id` under the custom-components route, lazy-imported like the api-connector sub-pages at routes.tsx:80-86,312).

- [ ] **Step 1:** Build `CustomComponentDetail` — read `:id` via `useParams`; `useCustomComponentQuery({id})` for metadata (name/title/language) + `useCustomComponentSourceQuery({id}, enabled: language !== 'JAVA')`. For non-Java: render `MonacoEditorWrapper` (props `{value, onChange, onMount, options, language}`; derive Monaco language from `CustomComponentLanguage` → `'javascript'|'python'|'ruby'`) with a **Save** button calling `useUpdateCustomComponentSourceMutation`. On error, the global fetch interceptor shows the toast (see CLAUDE.md); keep the buffer (don't reset on error). For Java: render read-only metadata + the definition (reuse `useCustomComponentDefinitionQuery`), no editor.
- [ ] **Step 2:** Add the route. `npm run check`.
- [ ] **Step 3:** Commit `CustomComponentDetail.tsx` + `routes.tsx`:
```bash
cd client && git add src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx src/routes.tsx
git commit -m "732 client - Add custom component detail route with Monaco editor"
```

---

## Task 6: List navigation to the detail route

**Files:** modify `client/src/ee/pages/settings/platform/custom-components/components/CustomComponentListItem.tsx` (and/or `CustomComponentList.tsx`).

- [ ] **Step 1:** Read the current expandable-row behavior. For a **non-Java** component, make the row (or its name) navigate (`useNavigate`) to `/settings/platform/custom-components/:id`. For a **Java** component, keep today's expand behavior. Keep the enable toggle + delete on the list.
- [ ] **Step 2:** `npm run check` (extend the item test if one exists to assert non-Java click navigates).
- [ ] **Step 3:** Commit.

---

## Task 7: "New component" create dialog (JavaScript)

**Files:** new `client/src/ee/pages/settings/platform/custom-components/components/CreateCustomComponentDialog.tsx`; wire a "New component" action into `CustomComponents.tsx` header (next to the existing Upload).

- [ ] **Step 1:** Dialog with a component **name** input + a **language** select (JavaScript only in SP-A — a single option, or a select seeded with JavaScript). On submit → `useCreateCustomComponentMutation({name, language: 'JAVASCRIPT'})` → on success `navigate` to the returned component's detail route. Handle the duplicate-name error via the global toast.
- [ ] **Step 2:** `npm run check`.
- [ ] **Step 3:** Commit.

---

## Task 8: Full-stack verification

- [ ] **Step 1:** `./gradlew :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service:check :server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-graphql:check` → BUILD SUCCESSFUL.
- [ ] **Step 2:** `cd client && npm run check` → green.
- [ ] **Step 3:** (optional, if a dev stack is up) create a JS component → edit → save valid (persists) → save broken (rejected, error shown, buffer kept) → confirm Java component opens read-only.

---

## Self-review notes (coverage vs spec)
- Spec §Storage read → Task 1. §Facade get-source → Task 1; update-source (compile-gate + name-lock) → Task 2; create-empty (JS) → Task 3. §GraphQL query/mutations → Tasks 1-3. §Client detail route+editor → Task 5; list nav → Task 6; create dialog → Task 7; graphql ops/codegen → Task 4. §Testing → folded into each task + Task 8.
- Java-source-not-editable enforced in Tasks 1, 2 (facade) and 5 (client read-only). Name-lock in Task 2. JS-only create in Task 3 + 7.
