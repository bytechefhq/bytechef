# Field Mapping for the Embedded Connect Portal — Design

**Date:** 2026-06-04
**Status:** Approved design (pre-plan)
**Branch:** `0_732`
**Scope:** Embedded only

## 1. Summary

Bring [Paragon-style field mapping](https://docs.useparagon.com/connect-portal/field-mapping)
to ByteChef's embedded **Connect Portal**. The end user, inside `ConnectDialog`, picks a remote
**object type** and maps their application's fields to the connected integration's fields. The
resulting mapping is persisted as a workflow-input value and is referenceable as data pills in the
embedded workflow builder.

This design implements a deliberately reduced subset of Paragon's feature:

- **Included:** [Dynamic Application Fields](https://docs.useparagon.com/connect-portal/field-mapping#dynamic-application-fields)
  (application fields supplied by the embedding app at runtime), the object-type selector,
  [sample mapping without paginated dropdowns](https://docs.useparagon.com/connect-portal/field-mapping#sample-mapping-without-paginated-dropdowns)
  (flat, non-cursor option lists),
  [user-configurable mappings](https://docs.useparagon.com/connect-portal/field-mapping#user-configurable-mappings)
  (`userCanRemoveMappings` + `defaultFields`),
  [user-creatable fields](https://docs.useparagon.com/connect-portal/field-mapping#user-creatable-fields)
  (`userCanCreateFields`), and
  [testing in the workflow editor](https://docs.useparagon.com/connect-portal/field-mapping#testing-field-mapping-in-the-workflow-editor)
  (a static sample structure that renders mapped fields as data pills).
- **Excluded (explicit non-goals):** paginated / cursor / search-as-you-type dropdowns; a generic
  raw-URL client proxy (`paragon.request` over arbitrary URLs); and Paragon's built-in runtime
  transform that emits `mappedIntegrationObject` / `mappedApplicationObject`. The stored mapping is
  the input value; the workflow author applies it themselves.

## 2. Background: how the pieces already exist

ByteChef already ships almost all of the machinery this feature needs. The design is mostly
**wiring + one new input type + new mapping UI**, not new subsystems.

- **ConnectDialog renders workflow inputs.** `sdks/frontend/embedded/library/react/src/components/connect-dialog/`
  already renders plain inputs, dynamic selects, and component-defined group inputs, and persists
  values through a debounced `inputs` PUT
  (`PUT /api/embedded/v1/integration-instances/{id}/workflows/{workflowUuid}`).
- **Generic action execution against live credentials.** `ActionApiController.executeAction`
  (`server/ee/libs/embedded/embedded-execution/embedded-execution-public-rest/.../ActionApiController.java`)
  runs any component action against the connected account's stored OAuth credentials. Route:
  `POST /api/embedded/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/actions/{actionName}`,
  with an `X-Instance-Id` header. This is ByteChef's equivalent of `paragon.request`, at *action*
  granularity. It is **not currently called by the embedded frontend SDK** — this feature adds the
  first caller.
- **Workflow inputs.** `Workflow.Input(name, label, type, required, componentReference)`
  (`server/libs/atlas/.../domain/Workflow.java`). `type` is a free-form string; structured inputs
  today resolve through `ComponentInputReference`.
- **Design-time sample values.** A workflow input's test value is stored in
  `WorkflowTestConfiguration.inputs[name]` (an `any` map) and is read by the data-pill panel.
- **Data pills.** `DataPillPanelBodyInputsItem.tsx` renders one pill per workflow input. Child-pill
  expansion is driven by a declared `property.properties`/`items` schema (`DataPill.tsx`), **not** by
  the test value's object shape — the test value only supplies preview values today. The embedded
  workflow builder (`EmbeddedWorkflowBuilder` → iframe → `client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx`)
  reuses the platform `WorkflowEditorLayout` + `DataPillPanel`, so changes to the shared data-pill
  components light up there automatically.

## 3. Architecture overview

The fetch model is **Paragon-faithful**: the embedding developer supplies `objectTypes.get` /
`integrationFields.get` callbacks at runtime; the SDK gives those callbacks a context-bound
`executeAction` helper that proxies, server-side, to a component action against the connected
account. The component author writes no field-mapping-specific server code, and there is no new
generic proxy endpoint.

```
Design time (workflow builder)
  InputEditor (WorkflowInputsEditDialog)
    type = FIELD_MAPPING
    test value = static mapObjectFields-shaped JSON  ──┐
                                                        │ derive objectName (top-level key) → input def
                                                        │ derive applicationFields → data pills
  DataPillPanel (shared)  ◄───────────────────────────┘
    one child pill per applicationFields.fields[].value

Runtime (end user, ConnectDialog)
  SDK useConnectDialog({ mapObjectFields: { <objectName>: { objectTypes.get, integrationFields.get, applicationFields: { fields, ... } } } })
  FieldMappingField
    objectType <select>  ── objectTypes.get({ executeAction, search })
    rows: applicationField (left) → integrationField <select> ── integrationFields.get({ executeAction, objectType })
    executeAction(component, version, action, input)  ──►  POST .../actions/{action}  (X-Instance-Id bound)  ──► live creds
    mapping value  ──►  debounced inputs PUT
```

## 4. Surfaces and changes

### Surface 1 — Embedded SDK: ConnectDialog field-mapping UI

Location: `sdks/frontend/embedded/library/react/src/components/connect-dialog/`.

- **`useConnectDialog` config.** Mirror Paragon's `mapObjectFields`, keyed by object name:

  ```ts
  useConnectDialog({
    // ...existing options...
    mapObjectFields: {
      Contacts: {
        objectTypes:       { get: async ({executeAction, search}) =>
                               (await executeAction('hubspot', 1, 'listObjects', {}))
                                 .map((o) => ({label: o.name, value: o.id})) },
        integrationFields: { get: async ({executeAction, objectType}) =>
                               (await executeAction('hubspot', 1, 'listObjectFields', {objectType}))
                                 .map((f) => ({label: f.label, value: f.id})) },
        applicationFields: {
          fields: [{label: 'Title', value: 'title'}, {label: 'Email', value: 'email'}],
          defaultFields: [],            // [] = none shown initially; omitted = all shown
          userCanRemoveMappings: true,  // optional
          userCanCreateFields: true,    // optional
        },
      },
    },
  })
  ```

  - `objectTypes.get` signature accepts `{executeAction, cursor, search}`. `cursor` is accepted but
    ignored (no pagination). `integrationFields.get` accepts `{executeAction, objectType, search}`.
  - `executeAction(componentName, componentVersion, actionName, input)` is **provided by the SDK**,
    not the developer. It is bound to the current dialog's `integrationInstanceId` (sent as
    `X-Instance-Id`) and JWT, and posts to the generic action endpoint, returning the action
    `result`. The developer never threads the instance id, and cannot target another user's
    instance through this helper.

- **`FieldMappingField` component.** New branch in the ConnectDialog input dispatcher
  (`renderWorkflowInput`), selected when an input's type is `FIELD_MAPPING`. It matches the input to
  `mapObjectFields[objectName]` (objectName comes from the input definition — see Surface 2) and
  renders:
  - An **object-type `<select>`** populated from `objectTypes.get`.
  - A list of **mapping rows**: left = application field label; right = integration-field `<select>`
    populated from `integrationFields.get({objectType})`. The integration-field select is disabled
    until an object type is chosen (reuse the existing dependency-gating pattern).
  - Row management per the toggles:
    - `defaultFields` seeds which application-field rows are visible initially.
    - `userCanRemoveMappings` → per-row remove + re-add of fields from the configured `fields` set.
    - `userCanCreateFields` → an "add custom field" affordance that lets the user invent an
      application field not present in `fields`.

- **Persistence.** The mapping is the input's value, saved through the **existing** debounced
  `inputs` PUT (sibling-preserving). Stored shape is self-describing so user-created fields survive:

  ```json
  {
    "<inputName>": {
      "objectType": "contacts",
      "mappings": [
        {"applicationField": {"label": "Title", "value": "title", "custom": false}, "integrationField": "first_name"},
        {"applicationField": {"label": "Priority", "value": "priority", "custom": true},  "integrationField": "hs_priority"}
      ]
    }
  }
  ```

### Surface 2 — Server: embedded configuration + security fix

- **New workflow input type `FIELD_MAPPING`.** Carried in the workflow definition. The input needs
  one derived attribute, `objectName`, so the runtime ConnectDialog can match it to the live
  `mapObjectFields[objectName]`. `objectName` is **derived from the input's test-value JSON
  top-level key** on save (see Surface 3) and surfaced through the embedded REST workflow model that
  ConnectDialog already consumes. No `sampleStructure` attribute and no component-api DSL property
  are introduced — the sample lives entirely in the test value.

- **Security fix (required, gates browser exposure).** `executeAction` resolves a connection from
  `X-Instance-Id` via `ConnectionIdHelper.getConnectionId(externalUserId, componentName, instanceId, environment)`
  (`server/ee/libs/embedded/embedded-execution/embedded-execution-service/.../util/ConnectionIdHelper.java`).
  When `instanceId` is non-null it currently fetches the integration instance by id **without
  verifying the `externalUserId` owns it** — an IDOR. Because this feature makes the endpoint
  reachable from the browser for the first time, the fix is in-scope: verify the resolved
  integration instance belongs to the connected user (resolve the `ConnectedUser` by
  `externalUserId` + environment and assert the instance's owner matches) before returning its
  `connectionId`; reject otherwise.

### Surface 3 — InputEditor: single JSON test-value field

Location: `client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx`
(the input-editor dialog; there is no file literally named `InputEditorDialog`).

- Add `field_mapping` to the type dropdown (alongside `component`, `boolean`, `date`, `date_time`,
  `integer`, `number`, `string`, `time`).
- When the selected type is `field_mapping`, render the existing **test value** field as a
  **JSON/object editor** (not a scalar input). It holds the static, `mapObjectFields`-shaped sample —
  the design-time mirror of the runtime SDK callbacks:

  ```json
  {
    "Contacts": {
      "objectTypes":       [{"label": "Contacts", "value": "contacts"}, {"label": "Leads", "value": "leads"}],
      "integrationFields": [{"label": "First Name", "value": "first_name"}, {"label": "Last Name", "value": "last_name"}],
      "applicationFields": {
        "fields": [{"label": "Title", "value": "title"}, {"label": "Email", "value": "email"}]
      }
    }
  }
  ```

  The `mapObjectFields` wrapper is optional for a single input; the **object-name key** is what
  matters. There is intentionally **one** field — object name, sample object types, sample
  integration fields, and application fields all live inside this one JSON value, avoiding a separate
  `objectName` control.

- **On save**, the JSON is stored as the test value in `WorkflowTestConfiguration.inputs[name]`
  (the `value: any` already supports objects), and the system **derives `objectName`** (the
  top-level key) onto the input definition (Surface 2).

### Surface 4 — Data pills (shared, reused by embedded builder)

Location: `client/src/pages/platform/workflow-editor/components/datapills/`.

- For `FIELD_MAPPING` inputs only, expand the test value's `applicationFields.fields[]` into one child pill
  per field in `DataPillPanelBodyInputsItem` / `DataPill` (e.g. `contactMapping.title`,
  `contactMapping.email`), using each field's `label`/`value` for the pill label and preview. This
  is a **type-gated** addition: it builds a synthetic `properties[]` from the sample for this type
  and does not change how any other input type renders.
- Because the embedded builder reuses the shared `DataPillPanel`, these pills appear in the embedded
  workflow builder automatically.

## 5. Consumption model

The stored mapping is the workflow-input value (Section 4, Surface 1). The workflow author reads it
and applies it however they wish (e.g. a Code step, or as an action parameter). There is **no**
server-side transform that emits `mappedIntegrationObject` / `mappedApplicationObject`. A built-in
"apply field mapping" transform may be a later, separate spec.

## 6. Data shapes (reference)

- **SDK config entry** (`mapObjectFields[objectName]`, runtime): `{objectTypes: {get}, integrationFields: {get}, applicationFields: {fields: Option[], defaultFields?: string[], userCanRemoveMappings?: boolean, userCanCreateFields?: boolean}}` where `Option = {label: string, value: string}`.
- **Test value** (`WorkflowTestConfiguration.inputs[name]`, design time): `{ <objectName>: {objectTypes: Option[], integrationFields: Option[], applicationFields: {fields: Option[]}} }` (static arrays).
- **Name correspondence:** the static test value uses `applicationFields` for the application-field
  array; at runtime the same list is the SDK config's `fields`. `objectTypes` / `integrationFields`
  are static `Option[]` arrays in the test value but callback objects (`{get}`) at runtime. This is
  the deliberate static-preview vs. live-callback split (Section 3); both are keyed by the same
  object name.
- **Persisted mapping** (runtime input value): `{objectType: string, mappings: Array<{applicationField: {label, value, custom}, integrationField: string}>}`.
- **Input definition attribute:** `type = "FIELD_MAPPING"`, `objectName` (derived from the test-value key).

## 7. Testing strategy

- **Embedded SDK (Vitest):** `FieldMappingField` — object-type gating, `defaultFields` seeding,
  remove/re-add (`userCanRemoveMappings`), custom-field creation (`userCanCreateFields`), and the
  persisted mapping shape; the `executeAction` helper (binds `X-Instance-Id`, returns `result`);
  config matching by `objectName`.
- **InputEditor (Vitest):** `field_mapping` type renders the JSON editor; save stores the object and
  derives `objectName`.
- **Data pills (Vitest):** a `FIELD_MAPPING` input expands `applicationFields` into child pills;
  other input types unaffected.
- **Server (JUnit):** `FIELD_MAPPING` input parsing + `objectName` derivation surfaced through the
  embedded workflow REST model; `ConnectionIdHelper` ownership check (rejects a foreign
  `X-Instance-Id`, accepts the owner's). EE files carry the Enterprise license header and
  `@version ee`.

## 8. Open questions / future work

- A server-side "apply field mapping" transform (Paragon's `mappedIntegrationObject`) — deferred.
- Paginated / searchable dropdowns for large object/field lists — deferred (current scope is flat
  arrays only).
- Whether `objectName` should ever be authored independently of the test-value key (currently always
  derived from it).
