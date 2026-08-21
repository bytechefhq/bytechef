# Component-defined Inputs in the EE Integration Instance Configuration Dialog — Design

- **Date:** 2026-05-31
- **Branch:** `0_732-embedded-inputs` (off `0_732`)
- **Status:** Approved (design); pending implementation plan
- **Sub-project:** 1 of 2. Sub-project 2 (embedded SDK `ConnectDialog` + new embedded public endpoints) is a separate spec.

## Problem

The automation `ProjectDeploymentDialog` can now render **component-defined workflow inputs** — inputs
that reference a component's input property or property group, rendered at deploy time as the component's
real property (with dynamic, connection-backed option dropdowns), and groups rendered as one compound
input whose value is a nested object. (Feature merged into `0_732`; spec:
`docs/superpowers/specs/2026-05-31-component-defined-workflow-inputs-design.md`.)

The EE embedded **Integration Instance Configuration** dialog
(`client/src/ee/pages/embedded/integration-instance-configurations/...`) still renders workflow inputs
with its own inline 7-type converter and does **not** support component-defined inputs. We want it to
support them, reusing the automation work.

This spec covers ONLY the EE main-app client dialog. The standalone embedded SDK React library
(`sdks/frontend/embedded/library/react`, `ConnectDialog.tsx`) is deliberately out of scope here — it
shares no code with the main client and additionally needs new embedded public REST endpoints; it is
sub-project 2.

## Goals

- The EE Integration Instance Configuration dialog renders component-defined workflow inputs:
  - a single referenced component property renders as the component's real property;
  - a referenced property group renders as a compound, collapsible input whose value is a nested object;
  - dynamic, connection-backed options work when reachable (see §"Dynamic options"); otherwise the field
    degrades to free-text;
  - a dangling reference (component/property/group removed) renders a disabled "no longer available"
    placeholder and never crashes the dialog.
- Achieve this by **adopting the shared `InputConfigurationList`** (already extended for automation) in the
  EE dialog, replacing its inline converter — one code path, consistent with automation.

## Non-goals (YAGNI)

- The embedded SDK `ConnectDialog` and any new embedded **public** REST endpoints (sub-project 2).
- Authoring of component-reference inputs — that is the platform Workflow Inputs editor, already built and
  shared; embedded integrations use the same editor, so no separate authoring work here.
- Enabling the embedded facade's currently-commented-out input validation (noted, not changed).
- Subflow input support in the embedded dialog beyond whatever `InputConfigurationList` already provides.

## Key decisions

| Decision | Choice |
| --- | --- |
| Scope | EE client dialog first; SDK is a separate sub-project. |
| EE approach | **Adopt the shared `InputConfigurationList`**; delete the inline converter. |
| Groups | Support **single component properties AND compound groups** (nested-object values). |
| Dynamic options | **Full dynamic options** where the node-options endpoint is reachable in the embedded context; verify-first, free-text fallback otherwise. |

## What already comes for free from `0_732`

Confirmed by exploration:

- **atlas `Workflow.Input`** is the 8-field record (`name,label,type,required,componentName,componentVersion,
  propertyName,groupName`) with a backward-compatible 4-arg constructor and a parser that reads the 4
  reference fields + the reserved-words allow-list entries. Embedded integration workflows use this same
  domain record — free.
  (`server/libs/atlas/atlas-configuration/atlas-configuration-api/.../domain/Workflow.java`)
- **platform `WorkflowInput` OpenAPI schema** already has the 4 reference fields; **`PropertyGroup`** schema
  and **`ComponentDefinition.properties`/`propertyGroups`** already exist
  (`server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml`).
- **Embedded internal REST spec inherits platform `Workflow`** via `allOf $ref` to the platform schema, so
  the embedded `Workflow.inputs[]` items resolve to platform `WorkflowInput` (with the 4 ref fields) at the
  schema level
  (`server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml`).
- **Input value maps accept nested objects.** `IntegrationInstanceConfigurationWorkflow.inputs` is a
  `MapWrapper`/`Map<String,?>`; group values (nested objects) persist without schema/entity change.
- **Shared `InputConfigurationList`** already implements: `resolveComponentInputProperty`,
  `resolveComponentInputGroup`, `resolveBackingWorkflowNodeName`, the `useQueries` component-definition
  fetch, dynamic options via the backing node (`ComponentReferenceDynamicSelect`), free-text fallback, and
  dangling-reference placeholders (`client/src/shared/components/InputConfigurationList.tsx`).
- **Nested-object value validation** is already handled on the platform side; the embedded facade's
  equivalent validation is currently commented out (so there is no nested-value crash to fix in embedded).

## Architecture

### Current EE dialog (to be changed)

- `IntegrationInstanceConfigurationDialogWorkflowsStep.tsx` → per-workflow
  `IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` → delegates inputs to
  `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx`, an **inline converter** that maps the
  7 primitive input types to `PropertyAllType` and renders the shared `Properties` (NOT
  `InputConfigurationList`). It does not handle component references.
- Input values bind to `integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs` (a
  `{ [key: string]: any }` map on `IntegrationInstanceConfigurationWorkflow`).
- The step item already computes `componentConnections` by walking `workflow.tasks`/`workflow.triggers`
  connections (excluding the integration's own component).

### Target EE dialog

- `IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx` renders the **shared
  `InputConfigurationList`**, mirroring automation's `ProjectDeploymentDialogWorkflowsStepItem`:
  - `inputs` = the workflow's inputs
  - `controlPath` = `integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`
  - `control`, `formState` (cast to `FieldValues`/`FormState<FieldValues>` as automation does)
  - `workflowId` = `workflow.id`
  - `componentConnections` = the already-computed component connections
  - `configuredConnectionIds` = the integration-instance-configuration workflow's connection ids, mapped
    **positionally** to `componentConnections` (same alignment automation uses to decide which backing node
    has a configured connection)
- `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx` is **deleted** (folded into the shared
  component), exactly as the automation `...StepItemInputs.tsx` was removed.

### Component reference resolution (reused, no new logic)

`InputConfigurationList` already: collects distinct `{componentName, componentVersion}` from reference
inputs, fetches their definitions via `useGetComponentDefinitionQuery` (a `useQueries` keyed on the distinct
set, hook-rules-safe), resolves single/group references to `PropertyAllType`, renders groups as a
collapsible binding members under `${controlPath}.${inputName}` (nested object), and falls back to free-text
/ disabled placeholder as appropriate. The EE dialog gets all of this by adopting the component.

## Client changes

1. **`IntegrationInstanceConfigurationDialogWorkflowsStepItem.tsx`** — replace the
   `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs` usage with `InputConfigurationList`
   (import from `@/shared/components/InputConfigurationList`), passing the props listed above. Build
   `configuredConnectionIds` from the form's connection values for this workflow, aligned positionally to
   `componentConnections` (read the automation step item for the exact derivation and mirror it).
2. **Delete `IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs.tsx`** and remove its imports.
3. **Embedded `WorkflowInput` client model** — the embedded internal spec inherits platform `Workflow`, so
   regenerating the embedded internal client models SHOULD surface the 4 reference fields on the embedded
   `WorkflowInput.ts`. Plan must verify after regen:
   - if regen yields the 4 fields → done;
   - if the embedded spec pins its own `WorkflowInput` schema lacking them → extend that schema to match
     platform and regenerate;
   - if regeneration is impractical → add a client augmentation
     `client/src/ee/shared/middleware/embedded/workflowInput.augmentation.d.ts` adding
     `componentName?`, `componentVersion?`, `propertyName?`, `groupName?` (mirroring the platform
     augmentation pattern).
4. No authoring changes (shared platform Workflow Inputs editor already supports defining these inputs).

## Dynamic options (verify-first)

`InputConfigurationList` loads connection-backed options via `useGetWorkflowNodeOptionsQuery`
(`@/shared/queries/platform/workflowNodeOptions.queries`, calling platform `WorkflowNodeOptionApi`). The one
real unknown: whether that node-options endpoint is reachable and authorized from the embedded EE client
context (it is a platform `/workflows/{id}/workflow-nodes/{node}/options` route).

- **Verify-first in planning:** confirm the embedded EE client can call the platform node-options endpoint
  for an integration's workflow with the configured connection.
- If reachable → dynamic dropdowns (including `optionsLookupDependsOn`) work for free.
- If not reachable → component inputs degrade to the existing free-text fallback in `InputConfigurationList`
  (no crash, value still editable). Adding an embedded options route is explicitly deferred (it overlaps
  sub-project 2's server work).

This is a verify-first item, not a design blocker.

## Server changes

Expected **none beyond regeneration** for sub-project 1:

- Embedded internal `Workflow`/`WorkflowInput` schema inherits platform (which already has the 4 ref
  fields). Regenerate embedded internal Java + TS models if needed to surface them.
- Input value persistence already accepts nested objects (`MapWrapper`/`Map<String,?>`), so group values
  store without entity/schema change.
- The embedded facade input validation is commented out; no nested-value crash to fix. Leave as-is (note in
  the plan).

If regeneration reveals the embedded internal spec does NOT actually inherit the ref fields (contrary to the
allOf observation), the plan adds them to the embedded internal `WorkflowInput`/`Workflow` schema and
regenerates — scoped to the internal spec only (NOT the public spec, which is sub-project 2).

## Error handling / edge cases

- **Dangling reference** → disabled "This input is no longer available." placeholder (reused).
- **No configured backing-node connection** → free-text fallback with helper text (reused).
- **Component used by multiple nodes** → first node with a configured connection, deterministically (reused).
- **Disabled workflow** → the dialog already blanks `inputs` to `{}` when a workflow is disabled on save;
  unchanged.

## Testing

- **Client component test** for `IntegrationInstanceConfigurationDialogWorkflowsStepItem` (or the dialog):
  with `useGetComponentDefinitionQuery` mocked, a referenced single property renders as the component
  property and a referenced group renders as a compound collapsible binding under
  `...workflows.${i}.inputs.${name}`. Mirror the automation `InputConfigurationList`/dialog test patterns
  and `vi.hoisted` mock conventions.
- The shared `resolveComponentInput*` resolvers are already unit-tested in
  `client/src/shared/components/InputConfigurationList.test.tsx`; no duplication needed.
- `cd client && npm run check` green (lint + typecheck + tests).
- Touched embedded modules compile / regenerate cleanly.

## Scope summary

- **In:** adopt shared `InputConfigurationList` in the EE Integration Instance Configuration dialog; delete
  the inline converter; surface the 4 reference fields on the embedded internal `WorkflowInput` client model
  (regen or augmentation); single + group rendering with nested-object values; dynamic options where
  reachable, free-text fallback otherwise; dangling-reference handling.
- **Out:** embedded SDK `ConnectDialog`; new embedded **public** REST endpoints (full ComponentDefinition,
  dynamic options resolution); authoring; enabling embedded facade validation. All sub-project 2 / separate.

## Rollback

Additive and opt-in. The EE dialog change swaps one rendering component for the shared one; reverting the
step-item commit restores the inline converter. Model regeneration/augmentation is additive (new optional
fields). No DB migrations (inputs are a JSON map). Revert the commits to back out.
