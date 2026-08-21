# Component-defined Workflow Inputs — Design

- **Date:** 2026-05-31
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan

## Problem

Today a workflow author can only define **primitive** workflow inputs (Boolean, Date,
Date Time, Integer, Number, String, Time) in the "Workflow Inputs" dialog. At deploy
time, `ProjectDeploymentDialog` renders one field per input by synthesizing a
`PropertyAllType` from the primitive `type`.

We want authors to also pick **input fields defined by components**. Each component should
be able to declare a set of input properties — the same way actions/triggers declare
properties — and a workflow input can reference one of them. At deploy time such an input
renders as the component's real property (including dynamic, connection-backed option
dropdowns). Authors should also be able to declare **groups** of related properties (whose
options depend on one another via `optionsLookupDependsOn`); a group is picked and rendered
together as a single **compound input**.

## Goals

- Components expose reusable **input properties** at the component level (opt-in), distinct
  from action/trigger properties.
- Components can declare **explicit property groups** (compound inputs) whose members may
  cross-reference each other via `optionsLookupDependsOn`.
- The "Create a new Input" dialog lets an author pick a component property or group.
- `ProjectDeploymentDialog` renders component-defined inputs as the component's real
  property/compound group, with dynamic options resolved through an existing workflow node's
  deployment connection.
- Component-defined inputs are stored as a **reference** and resolved against the live
  component definition at render time.

## Non-goals (YAGNI)

- Inputs that declare their own standalone connection slot in the deploy dialog.
- Arbitrary nesting of groups within groups.
- Reaching into a specific action's/trigger's properties (top-level component properties only).

## Key decisions

| Decision | Choice |
| --- | --- |
| Property source | New **top-level component properties** (opt-in `.properties(...)` on `ComponentDefinition`), separate from actions/triggers. |
| Grouping | **Explicit group in the DSL** (`propertyGroup(...)`), not inferred from `optionsLookupDependsOn`. |
| Dynamic options at deploy | **Resolve via an existing node's connection** — component inputs reuse the connection of a workflow node already using that component. |
| Persistence | **Reference** (`componentName`, `componentVersion`, `propertyName`/`groupName`); resolved against the live component definition at render time. |
| Authoring UX | **Extend the Type selector** — add a "Component property" entry to the existing `Type` dropdown. |
| Connection sourcing | **Reuse existing node connection only.** A component input is only offerable when a node in the workflow already uses that component. Options load through that node's deployment connection; if no connection is set yet, the field **degrades to free-text** and upgrades to the live dropdown once a connection is available. |
| Compound value shape | **Nested object** — group members nest under the input name; referenced as `${input.<name>.<subProperty>}`. |

## Architecture

### Existing pipeline (reused)

- `convertInputToProperty` (`client/src/shared/components/InputConfigurationList.tsx`) turns a
  stored `WorkflowInput` into a `PropertyAllType`.
- `Property` / `Properties`
  (`client/src/pages/platform/workflow-editor/components/properties/`) render any
  `PropertyAllType`, including `SELECT` with `optionsDataSource` / `optionsLookupDependsOn`
  via `PropertyComboBox`.
- `getWorkflowNodeOptions` (`useGetWorkflowNodeOptionsQuery`,
  `client/src/shared/queries/platform/workflowNodeOptions.queries.ts`) resolves dynamic
  options **in the context of a workflow node + connection + sibling parameter values**.
- `SubflowInputGroup` (in `InputConfigurationList.tsx`) already renders a collapsible group of
  inputs whose values nest under a path — the pattern reused for compound groups.

### 1. Server — Component DSL (`ComponentDsl` / `ComponentDefinition`)

Add an opt-in, component-level set of input properties plus explicit groups:

```java
component("googleSheets")
    .properties(                                   // NEW: top-level component input properties
        string("spreadsheetId").label("Spreadsheet").options(...))
    .propertyGroups(
        propertyGroup("sheetSelection")            // NEW: explicit compound group
            .label("Sheet")
            .properties(
                string("spreadsheetId").label("Spreadsheet").options(...),
                string("sheetName").label("Sheet")
                    .optionsLookupDependsOn("spreadsheetId").options(...)));
```

- `ComponentDefinition` gains:
  - `Optional<List<? extends Property.ValueProperty<?>>> getProperties()`
  - `Optional<List<PropertyGroup>> getPropertyGroups()`
- `PropertyGroup` is a new lightweight definition: `name`, `label`, `properties(...)`.
  Conceptually an OBJECT whose members can cross-reference via `optionsLookupDependsOn`.
- Both are threaded through the component-definition DTO → REST/GraphQL model the client
  already consumes, so the authoring picker and deploy renderer can read them.

### 2. Server — `WorkflowInput` model (reference persistence)

Extend the workflow input record and its REST model with an optional reference block.
Primitive inputs are unchanged.

```
WorkflowInput {
  name, label, required, type            // existing (type stays for primitives)
  componentName?, componentVersion?      // NEW
  propertyName?                          // NEW — single component property
  groupName?                            // NEW — component property group
}
```

At render time the full property/group definition is resolved from the live component
definition (stays in sync as the component evolves). For a component input, `type` is taken
from the resolved property; the persisted `type` is not authoritative.

### 3. Server — options resolution

Reuse the **node-scoped** `getWorkflowNodeOptions` path. Under "reuse existing node connection
only", a component input is always backed by a node in the workflow that uses the same
component; its dynamic options are resolved through that node's deployment connection and the
sibling group-member values (for `optionsLookupDependsOn`).

If routing through the node context cleanly is insufficient, add a thin component-level options
resolver that locates the backing node and delegates. The exact server seam is confirmed during
planning.

### 4. Client — authoring (`WorkflowInputsEditDialog`)

- Add a **"Component property"** entry at the top of the existing `Type` dropdown.
- Selecting it reveals two cascading pickers:
  1. **Component** — restricted to components actually used by nodes in the current workflow
     (makes "reuse node connection" coherent).
  2. **Property or Group** — the chosen component's `properties` + `propertyGroups`.
- `Name` / `Label` auto-fill from the picked property/group but remain editable. `Required`
  stays.
- **Test Value:** for component inputs, reuse the backing node's test-configuration connection
  to show the live picker; fall back to plain text when unavailable.

### 5. Client — deploy rendering (`InputConfigurationList` / `convertInputToProperty`)

- When an input carries a component reference, resolve it to the component's property/group
  definition and produce a full `PropertyAllType` (with `optionsDataSource`,
  `optionsLookupDependsOn`, etc.) instead of a synthesized primitive.
- **Single property** → rendered by the existing `Property` (e.g. a live `PropertyComboBox`).
- **Group** → rendered as a compound input using the existing `SubflowInputGroup`-style
  collapsible container; members render via `Properties`, and their values **nest as an object**
  under the input name (`${input.<name>.<subProperty>}`).
- **Connection sourcing:** dynamic options load using the connection the deployer selected for
  the backing node (Connections tab). If that connection isn't set yet (or the property needs no
  connection), the field **degrades to free-text** and upgrades to the live dropdown once a
  connection is available.

### 6. End-to-end data flow

1. Author picks component + property/group in the Inputs dialog.
2. Stored as a reference in `workflow.definition.inputs[]`.
3. Deploy dialog resolves the reference against the live component definition.
4. Renders the real property / compound group.
5. Dynamic options fetched via the backing node's deployment connection.
6. Deployer's value saved into `projectDeploymentWorkflows[i].inputs[name]` (nested object for
   groups).

## Error handling / edge cases

- **Dangling reference** (component/property/group removed) → render a disabled field with a
  clear "input no longer available" message; never crash the deploy dialog.
- **Component used by multiple nodes** → use the node whose connection is configured; if several,
  the first deterministically (documented behavior).
- **No backing node connection set** → free-text fallback (see §5).
- **Name collisions** with existing inputs → same validation as today.

## Testing

- **Server**
  - DSL unit tests for `properties()` / `propertyGroup()` / `propertyGroups()`.
  - Component-definition serialization includes the new fields (REST/GraphQL model).
  - Options resolution for a group member depending on a sibling via `optionsLookupDependsOn`.
- **Client**
  - `WorkflowInputsEditDialog` component-property authoring flow (component + property/group
    pickers, name/label auto-fill).
  - `convertInputToProperty` reference resolution: single property, group, and missing reference.
  - Compound group renders with nested values.
  - Free-text fallback when no backing-node connection is set.

## Scope summary

- **In:** component-level input properties + explicit groups; reference persistence; extended
  authoring dialog; deploy-time rendering with dynamic options via reused node connection;
  nested compound values.
- **Out:** input-declared standalone connections; nested groups within groups; reaching into
  action/trigger properties.
