# `internalOnly` Input Flag — Route Inputs Between the Embedded Dialogs

**Date:** 2026-06-05
**Status:** Approved design (pre-plan)
**Branch:** `0_732`
**Scope:** Embedded (admin configuration + end-user connect portal)

## 1. Summary

Add a boolean **`internalOnly`** flag to a workflow input. It strictly partitions *where* the input
is rendered and configured:

- `internalOnly = true` → only the **admin** `IntegrationInstanceConfigurationDialog` (the SaaS
  developer configures the value once, per integration-instance configuration).
- `internalOnly = false` (default) → only the **end-user** `ConnectDialog` (the connected user
  supplies the value in the embedded connect portal).

Today an input that isn't tied to the integration's own component renders in **both** dialogs. This
feature makes the split exclusive. The flag rides in the workflow-definition JSON — exactly like the
field-mapping `objectName` flag added earlier — so it flows through the existing input save and
round-trip with no new persistence layer.

**Non-goals:** changing how input *values* are stored or merged at runtime (see §7 — a verification
point, not new code); any UI beyond the editor checkbox and the two render filters; applying the
flag outside the embedded dialogs.

## 2. Default and direction

- Default `false` → the connect portal (end-user). New and existing inputs are end-user inputs unless
  marked otherwise. **Migration consequence:** existing inputs (which have no `internalOnly` key,
  i.e. `false`) will render only in the `ConnectDialog` and will no longer appear in the admin
  `IntegrationInstanceConfigurationDialog`. This is the intended behavior — the admin explicitly marks
  an input `internalOnly` to move it into the admin dialog.

## 3. The data flow

```
Workflow definition JSON
  inputs: [{ name, label, type, required, internalOnly, ... }]
        │
        ├─ parsed → Workflow.Input.internalOnly (atlas)
        │
        ├─ platform-configuration WorkflowInput.internalOnly  (REST) ──► client workflow-editor + admin dialog
        │
        └─ embedded InputModel.internalOnly (REST, via ConnectedUserIntegrationMapper) ──► SDK ConnectDialog

Editor (WorkflowInputsEditDialog): "Internal only" checkbox  → internalOnly in the definition (via ...rest round-trip)

Render routing:
  ConnectDialog (SDK)                         renders inputs where !internalOnly
  IntegrationInstanceConfigurationDialog      renders inputs where  internalOnly  (and not the integration's own component)
```

## 4. Surfaces and changes

### 4.1 Server: `Workflow.Input` + parsing
`server/libs/atlas/atlas-configuration/atlas-configuration-api/.../domain/Workflow.java`

- Add `boolean internalOnly` as the **last** component of the `Input` record:
  `Input(String name, String label, String type, boolean required, ComponentInputReference componentReference, String objectName, boolean internalOnly)`.
  Add/extend convenience constructors so all existing `new Input(...)` call sites (4-arg, 5-arg,
  6-arg) keep compiling, defaulting `internalOnly` to `false`.
- In the inputs-parsing block, pass `MapUtils.getBoolean(map, WorkflowConstants.INTERNAL_ONLY, false)`
  as the new last argument.
- `WorkflowConstants`: add `public static final String INTERNAL_ONLY = "internalOnly";`. If a
  reserved-words validator (`WORKFLOW_DEFINITION_CONSTANTS`) rejects unknown keys, register
  `INTERNAL_ONLY` there too (the `objectName` work required this).

### 4.2 Platform-configuration REST `WorkflowInput`
The client workflow-editor and the admin `IntegrationInstanceConfigurationDialog` both read the
platform-configuration `WorkflowInput` model. Add `internalOnly` so both can filter/type on it:

- `platform-configuration-rest` `openapi.yaml`: add to the `WorkflowInput` schema
  `internalOnly: { type: boolean, default: false, description: "..." }`. Regenerate the Java +
  TypeScript models.
- This removes the need for an `as WorkflowInput` cast in the admin filter (unlike the field-mapping
  `objectName`, which lives only on the client intersection type).

### 4.3 Embedded REST `InputModel` + mapper
`server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/`

- `openapi.yaml`: add to the `Input` schema `internalOnly: { type: boolean, default: false, description: "If true, configured in the admin IntegrationInstanceConfigurationDialog; if false (default), rendered in the end-user ConnectDialog." }`. Regenerate `InputModel`.
- `ConnectedUserIntegrationMapper.map(Workflow.Input)`: add `.internalOnly(input.internalOnly())`
  (alongside the existing `.objectName(...)`/`.required(...)`/`.type(...)`).

### 4.4 Inputs editor: the checkbox
`client/src/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog.tsx`

- Add an **`Internal only`** checkbox bound to the form field `internalOnly` (boolean, default
  `false`). Use a real checkbox control (`@/components/ui/checkbox`) — your stated preference — rather
  than the true/false `Select` the `required` field uses.
- `client/src/shared/types.ts`: the editor's `WorkflowInputType` gains `internalOnly?: boolean`. Once
  §4.2 regenerates the platform `WorkflowInput` with `internalOnly`, the intersection inherits it
  automatically; otherwise add it explicitly.
- `hooks/useWorkflowInputs.ts` (`saveWorkflowInput`) and `utils/toWorkflowDefinitionInput.ts` /
  `fromWorkflowDefinitionInput.ts`: **no change needed** — they spread `...rest`, so `internalOnly`
  round-trips like `objectName`. (Confirm: it must not be stripped the way `testValue` is.)
- The flag applies to **all** input types (no special-casing field-mapping or component-group inputs).

### 4.5 ConnectDialog filter (SDK) — `!internalOnly`
`sdks/frontend/embedded/library/react/src/components/connect-dialog/`

- `types.ts` `WorkflowInputType`: add `internalOnly?: boolean`.
- `ConnectDialog.tsx` `DialogWorkflowsContainer`: filter `inputs.filter((input) => !input.internalOnly)`
  before the `.map(...)` that renders inputs — in **both** the regular-workflow loop and the
  MCP-workflow loop.

### 4.6 Admin dialog filter — `internalOnly`
`client/src/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx`

- The existing `inputs` derivation excludes the integration's own component
  (`input.componentReference?.componentName !== componentName`). Add `.filter((input) => input.internalOnly)`
  so the admin sees **only** `internalOnly` inputs (and still not the own-component ones).

## 5. Data shapes (reference)

- Definition input entry: `{ name, label, type, required, internalOnly?, objectName?, componentName?, componentVersion?, groupName? }` (`internalOnly` omitted ⇒ `false`).
- `Workflow.Input.internalOnly()` → `boolean` (Java).
- Platform `WorkflowInput.internalOnly` and embedded `InputModel.internalOnly` → `boolean` (default false).
- Client/SDK `WorkflowInputType.internalOnly?: boolean`.

## 6. Error handling / edge cases

- Absent `internalOnly` in the definition ⇒ parsed as `false` (existing inputs behave as end-user
  inputs). No migration script needed.
- An input marked `internalOnly` that also references the integration's own component is excluded from
  the admin dialog by the pre-existing own-component filter (it would have nowhere to render); this is
  an unusual authoring combination and is acceptable — the admin filter is `internalOnly && not-own-component`.

## 7. Value-flow verification point (not new code)

This feature routes **rendering** only. The admin dialog already persists its inputs into the
`IntegrationInstanceConfiguration`; the `ConnectDialog` persists into the `IntegrationInstance`. For an
admin-set `internalOnly` value to actually apply when the workflow runs for a connected user, the
execution path must already **merge** config-level and instance-level input values. The plan must
**verify** this merge exists; if it does not, threading `internalOnly` values into execution is a
separate follow-up spec — it is out of scope here, which is the rendering split you requested.

## 8. Testing strategy

- **Java:** `WorkflowTest` — a definition input with `internalOnly: true` parses to
  `input.internalOnly() == true`; absent ⇒ `false`. `ConnectedUserIntegrationMapperTest` —
  `map(Workflow.Input(..., internalOnly=true))` yields `InputModel.getInternalOnly() == true`.
- **Vitest (main client):** `WorkflowInputsEditDialog` renders the checkbox and saving persists
  `internalOnly` into the definition (round-trips via `toWorkflowDefinitionInput`/`fromWorkflowDefinitionInput`);
  `IntegrationInstanceConfigurationDialogWorkflowsStepItem` renders only `internalOnly` inputs.
- **Vitest (SDK):** `ConnectDialog` does not render an input whose `internalOnly` is true (renders the
  others).
- EE files under `server/ee/**` carry the Enterprise header + `@version ee`.

## 9. Open questions / future work

- The runtime config+instance input value merge (§7) — verify during planning.
- Whether the admin dialog should visually indicate that the remaining (non-internal) inputs are
  end-user-supplied (out of scope; cosmetic).
