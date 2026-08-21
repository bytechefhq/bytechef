# Component-defined workflow inputs — groups-only model + typed component reference

**Date:** 2026-05-31
**Branch:** 0_732
**Status:** Design — pending review
**Builds on:** `4d72832c98` (component-level properties/propertyGroups DSL), `1e0064d1d53`
(embedded EE dialog + SDK ConnectDialog), `7e14da4fed7` (Slack component-level channel input).

## Problem

The component-defined-inputs feature currently exposes **two** parallel concepts and a
loosely-typed reference:

1. **DSL/domain** — a component can declare flat `properties` *and* `propertyGroups`
   (`ComponentDefinition.getProperties()` + `getPropertyGroups()`). Two backing fields, two
   getters, two REST model fields — for what is conceptually one list of selectable inputs.

2. **Reference (S2 finding)** — a `Workflow.Input` references a component input via **six
   independent optionals**: `componentName`, `componentVersion`, `propertyName`, `groupName`,
   and the resolved `property` / `group`. This makes illegal shapes representable:
   `propertyName`+`groupName` both set, `property`+`group` both set, a dangling `componentName`
   with no reference, etc. The real discriminated union lives only in the ConnectDialog
   `renderWorkflowInput` if-ladder, betrayed by a `value as unknown as Record<…>` cast.

## Goals

- Collapse component inputs to a **single groups-only model**: `getInputs()` always returns a
  list of property **groups**. A lone property is a group with one property.
- Make the workflow-input → component reference **illegal-state-free** with one atomic nested
  object. No `oneOf` — groups-only removes the bare-property variant, so there is exactly one
  kind of reference (a group reference).
- Apply full-stack: SDK DSL, atlas domain, platform domain, both REST OpenAPI surfaces,
  generated models, MapStruct, automation workflow-editor client, embedded SDK ConnectDialog.

## Design

### 1. SDK DSL (`ComponentDsl` / `component-api`)

`ModifiableComponentDefinition`:

- Single backing field `List<ModifiablePropertyGroup> inputs`.
- `inputs(P... properties)` where `P extends Property.ValueProperty<?>` — **converts each
  property to a single-property group** (group `name` = property name, no label). This is the
  ergonomic path component authors use.
- `inputs(ModifiablePropertyGroup... propertyGroups)` — stores groups as-is (compound inputs).
  Distinguished from the above by argument type.
- `getInputs(): Optional<List<? extends PropertyGroup>>` — **always groups**.
- Remove `properties`/`propertyGroups` fields, `getProperties()`/`getPropertyGroups()`, and the
  `properties`/`propertyGroups` lines in equals/hashCode/toString.

Interfaces:
- `WorkflowComponentDefinition`: replace `getProperties()` with
  `getInputs(): Optional<List<? extends PropertyGroup>>`.
- `ComponentDefinition`: remove `getPropertyGroups()`.

`ModifiablePropertyGroup`, the `propertyGroup(name)` factory, and the `PropertyGroup` interface
are unchanged (groups remain the unit).

### 2. Platform domain (`platform-component-api`)

- `domain.ComponentDefinition`: single `List<PropertyGroup> inputs` field + `getInputs()`; one
  static `getInputs(sdkDefinition)` mapper. Drop the flat `properties` field/getter and the
  `getPropertyGroups` getter/helper. Update equals/hashCode/toString.
- `AbstractComponentDefinitionWrapper`: single `inputs` field + `getInputs()`; drop the two old
  fields/getters.
- `domain.PropertyGroup` unchanged.

### 3. Atlas (`atlas-configuration-api`)

Replace the four reference fields on `Workflow.Input` with one nested record:

```java
public record Input(
    String name, String label, String type, boolean required,
    ComponentInputReference componentReference) implements Serializable {

    public Input(String name, String label, String type, boolean required) {
        this(name, label, type, required, null);
    }
}

public record ComponentInputReference(
    String componentName, Integer componentVersion, String groupName) implements Serializable {}
```

- `Workflow` parsing: build `ComponentInputReference` only when `componentName` is present;
  read `groupName` (drop `propertyName`).
- `WorkflowConstants`: drop `PROPERTY_NAME`; keep `COMPONENT_NAME`, `COMPONENT_VERSION`,
  `GROUP_NAME`. Update `WORKFLOW_DEFINITION_CONSTANTS`.
- `WorkflowInputParsingTest`: update to the nested-reference shape.

### 4. Config-rest OpenAPI (`platform-configuration-rest-impl/openapi.yaml`)

- `ComponentDefinitionModel`: replace `properties` + `propertyGroups` with a single
  `inputs: array of PropertyGroup`.
- `WorkflowInput` model: replace `componentName`/`componentVersion`/`propertyName`/`groupName`
  with `componentReference: ComponentInputReference` (`{componentName, componentVersion,
  groupName}`).
- Regenerate models; update `ComponentDefinitionMapper` (auto-maps `inputs`; PropertyGroup
  nested mapping) and the workflow-input mapper.

### 5. Embedded public-rest OpenAPI (`embedded-configuration-public-rest/openapi.yaml`)

- `Input`: drop `componentName`/`componentVersion`/`propertyName`/`groupName`/`property`; add
  `componentReference: ComponentInputReference` where
  `ComponentInputReference { componentName, componentVersion, groupName, group: ComponentPropertyGroup }`
  (the resolved group is server-populated and lives inside the reference — all-or-nothing).
  Note: the embedded reference embeds the **resolved** `group` (the SDK gets pre-resolved data);
  the config-rest reference omits it (the editor already holds the full component definition and
  reads `componentDefinition.inputs` separately). Same name, consumer-appropriate payload.
- `ComponentProperty` / `ComponentPropertyGroup` schemas unchanged.
- `WorkflowInputOptionsRequest` keeps `propertyName` — it names the property *within* the
  resolved group whose dynamic options to fetch; that is a different axis from the input
  reference and is not part of S2.
- Regenerate; update `IntegrationApiController.toInputModel` /
  `toComponentPropertyGroupModel` (resolve against `getInputs()`), drop
  `toComponentPropertyModel(componentDefinition, propertyName)` flat path, and
  `EmbeddedWorkflowInputOptionFacade` as needed.

### 6. Clients

- **Automation workflow editor**: regenerate GraphQL/REST types; the InputConfigurationList
  reads `componentDefinition.inputs` (groups) and `input.componentReference`.
- **Embedded SDK ConnectDialog** (`sdks/frontend/embedded/library/react`): regenerate/adjust
  `types.ts`; rewrite `renderWorkflowInput` to the single `input.componentReference?.group`
  shape, deleting the `property` branch and the `value as unknown as Record<…>` cast; update
  `ConnectDialog.tsx`, `utils.ts`, and the dynamic/connect tests. Run `npm run check`.

### 7. Slack call site

`SlackComponentHandler`: `.properties(string(CHANNEL)…)` → `.inputs(string(CHANNEL)…)`
(auto-wrapped into a single-property group "channel"). Regenerate `slack_v1.json` snapshot.

## Out of scope

- Renaming the `PropertyGroup` type itself.
- Multi-property-group authoring for Slack (still one channel input).
- The `WorkflowInputOptionsRequest.propertyName` axis (intra-group option lookup).

## Verification

- `./gradlew :…:component-api:test` (ComponentDsl + parsing tests), affected platform/embedded
  modules compile + test, Slack snapshot test.
- `cd client && npm run check`; SDK `npm run check` / tests.
- Grep audit: no remaining component-level `getProperties()`/`getPropertyGroups()`,
  `propertyName`/`property` on the workflow-input reference path.
