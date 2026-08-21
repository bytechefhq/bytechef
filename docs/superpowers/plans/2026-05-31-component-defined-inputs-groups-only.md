# Component-defined inputs: groups-only + typed reference — Implementation Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax. Validated by compile + existing
> tests + snapshot regen (this is a restructure, not greenfield TDD).

**Goal:** Collapse component-defined workflow inputs to a groups-only model (`getInputs()` always
returns property groups) and replace the six-optional component reference with one atomic
`ComponentInputReference`, full-stack.

**Architecture:** Single backing list of property groups in the DSL/domain; a lone property is a
one-property group. Workflow inputs reference a component via one nested all-or-nothing object.

**Tech Stack:** Java 25 / component-api SDK, Spring Data JDBC (atlas), MapStruct + SpringDoc
OpenAPI generation, React/TS (automation editor + embedded SDK).

**Commits:** (1) **automation** = SDK core + atlas + platform domain + config-rest + editor
client. (2) **embedded** = embedded public-rest + controller/facade + ConnectDialog SDK.

---

## COMMIT 1 — automation

### Task 1: SDK DSL groups-only
**Files:** Modify `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java`, `ComponentDefinition.java`, `WorkflowComponentDefinition.java`; `src/test/java/.../ComponentDslPropertyGroupTest.java`.

- [ ] `ModifiableComponentDefinition`: replace `properties`/`propertyGroups` fields with `List<ModifiablePropertyGroup> inputs`.
- [ ] Add overloads:
```java
@SafeVarargs
public final <P extends Property.ValueProperty<?>> ModifiableComponentDefinition inputs(P... inputs) {
    if (inputs != null) {
        this.inputs = Arrays.stream(inputs)
            .map(property -> new ModifiablePropertyGroup(property.getName()).properties(property))
            .toList();
    }

    return this;
}

public ModifiableComponentDefinition inputs(ModifiablePropertyGroup... inputs) {
    if (inputs != null) {
        this.inputs = List.of(inputs);
    }

    return this;
}
```
(add `import java.util.Arrays;`)
- [ ] Replace getters with single `@Override public Optional<List<? extends PropertyGroup>> getInputs() { return Optional.ofNullable(inputs); }`. Remove `getProperties()` + `getPropertyGroups()` overrides.
- [ ] equals/hashCode/toString: replace `properties, propertyGroups` with `inputs`.
- [ ] `WorkflowComponentDefinition`: replace `getProperties()` with `getInputs(): Optional<List<? extends PropertyGroup>>` (import PropertyGroup).
- [ ] `ComponentDefinition`: remove `getPropertyGroups()` declaration.
- [ ] `ComponentDslPropertyGroupTest`: rewrite both tests to use `inputs(...)` and assert `getInputs()` returns groups (single property -> one group named after the property).
- [ ] Verify: `./gradlew :sdks:backend:java:component-api:test --tests "*ComponentDslPropertyGroupTest"`.

### Task 2: platform domain
**Files:** Modify `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/ComponentDefinition.java`, `definition/AbstractComponentDefinitionWrapper.java`.

- [ ] domain `ComponentDefinition`: drop `properties` field; keep `List<PropertyGroup> inputs`. Constructor `this.inputs = getInputs(componentDefinition);`. Replace getters with `getInputs()`. Replace the two static helpers with one `getInputs(sdk)` mapping `sdk.getInputs()` -> `CollectionUtils.map(groups, PropertyGroup::new)`. Update equals/hashCode/toString (`inputs`).
- [ ] `AbstractComponentDefinitionWrapper`: single `inputs` field, `getInputs()`, drop the two old fields/getters/imports for the removed one.
- [ ] Verify: `./gradlew :server:libs:platform:platform-component:platform-component-api:compileJava`.

### Task 3: atlas Workflow.Input typed reference
**Files:** Modify `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/Workflow.java`, `constant/WorkflowConstants.java`; `src/test/java/.../WorkflowInputParsingTest.java`.

- [ ] `Workflow.Input`: replace the 4 reference fields with `ComponentInputReference componentReference`; keep the 4-arg convenience ctor delegating with `null`.
- [ ] Add nested `public record ComponentInputReference(String componentName, Integer componentVersion, String groupName) implements Serializable {}`.
- [ ] Parsing (`Workflow` ~line 168): build `componentReference` only if `COMPONENT_NAME` present, reading `COMPONENT_NAME`/`COMPONENT_VERSION`/`GROUP_NAME` (drop `PROPERTY_NAME`).
- [ ] `WorkflowConstants`: remove `PROPERTY_NAME`; drop it from `WORKFLOW_DEFINITION_CONSTANTS`.
- [ ] `WorkflowInputParsingTest`: assert nested `componentReference` shape; remove propertyName cases.
- [ ] Verify: `./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "*WorkflowInputParsingTest"`.

### Task 4: config-rest OpenAPI + mapper + regen
**Files:** Modify `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`; `platform-configuration-rest-api/.../mapper/ComponentDefinitionMapper.java` (+ workflow-input mapper if separate).

- [ ] `ComponentDefinitionModel`: replace `properties`+`propertyGroups` with `inputs: array of PropertyGroup`.
- [ ] `WorkflowInput` model: replace `componentName/componentVersion/propertyName/groupName` with `componentReference: $ref ComponentInputReference`; add `ComponentInputReference {componentName, componentVersion, groupName}` schema.
- [ ] Update `ComponentDefinitionMapper` so `inputs` maps from domain `getInputs()` (PropertyGroup -> PropertyGroupModel); remove stale property/group mappings. Update the workflow Input mapping to the nested ref.
- [ ] Regenerate: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api:openApiGenerate` (or module build). Fix mapper compile errors.
- [ ] Verify: module `compileJava` + any mapper tests.

### Task 5: automation workflow-editor client
**Files:** `client/` — regenerate types; update InputConfigurationList + any input-reference consumers.

- [ ] Regenerate REST/GraphQL types (`npx graphql-codegen` and/or openapi regen per `client/codegen.ts`).
- [ ] Update components reading `componentDefinition.properties/propertyGroups` -> `inputs`; `input.{componentName,propertyName,groupName}` -> `input.componentReference`.
- [ ] Verify: `cd client && npm run check`.

- [ ] **Commit 1 (automation):** stage SDK + atlas + platform-component + config-rest + client files.
```bash
git commit -m "732 Component-defined inputs: groups-only model + typed component reference (automation)"
```

---

## COMMIT 2 — embedded

### Task 6: embedded public-rest OpenAPI + controller/facade
**Files:** Modify `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`; `.../web/rest/IntegrationApiController.java`; `.../facade/EmbeddedWorkflowInputOptionFacade.java` (+ impl).

- [ ] `Input` schema: drop `componentName/componentVersion/propertyName/groupName/property`; add `componentReference: $ref ComponentInputReference` with `ComponentInputReference {componentName, componentVersion, groupName, group: ComponentPropertyGroup}`.
- [ ] Regenerate models (module openApiGenerate).
- [ ] `IntegrationApiController.toInputModel`: build `componentReference` (resolve `group` via `toComponentPropertyGroupModel`). Remove the flat `toComponentPropertyModel(def, propertyName)` path; `toComponentPropertyGroupModel` iterates `componentDefinition.getInputs()`.
- [ ] `EmbeddedWorkflowInputOptionFacade`: keep `propertyName` (intra-group option lookup); adjust any reference-field reads to `componentReference`.
- [ ] Verify: module compile + tests.

### Task 7: embedded SDK ConnectDialog
**Files:** `sdks/frontend/embedded/library/react/src/components/connect-dialog/types.ts`, `ConnectDialog.tsx`, `utils.ts`, `*.test.tsx`.

- [ ] `types.ts`: Input gets `componentReference?: {componentName; componentVersion; groupName; group?: ComponentPropertyGroup}`; drop `property`, flat ref fields.
- [ ] `ConnectDialog.tsx renderWorkflowInput`: collapse if-ladder to `const group = input.componentReference?.group; if (group) {...}`; delete the `input.property` branch and the `value as unknown as Record<…>` cast.
- [ ] Update `utils.ts` + tests (`ConnectDialog.dynamic.test.tsx`, `ConnectDialog.test.tsx`) to the nested shape.
- [ ] Verify: SDK `npm run check` / test.

- [ ] **Commit 2 (embedded):** stage embedded public-rest + controller/facade + ConnectDialog SDK files.
```bash
git commit -m "732 Component-defined inputs: groups-only + typed reference (embedded EE + SDK)"
```

---

## Self-review notes
- Spec coverage: Tasks 1-2 (groups-only DSL/domain), 3 (atlas ref), 4-5 (config-rest + editor),
  6-7 (embedded + SDK), Slack call site folded into Task 1 verification — see below.
- **Slack call site:** `SlackComponentHandler` `.properties(` -> `.inputs(`; regenerate
  `slack_v1.json`. Do this in Task 1 (same compile unit) and include in Commit 1.
- Type consistency: `ComponentInputReference {componentName, componentVersion, groupName}`
  (config-rest/atlas) vs embedded adds resolved `group`. `getInputs()` everywhere returns groups.
