# Component-defined Inputs in the Embedded SDK ConnectDialog — Design

- **Date:** 2026-05-31
- **Branch:** `0_732-embedded-inputs` (off `0_732`)
- **Status:** Approved (design); pending implementation plan
- **Sub-project:** 2 of 2. Sub-project 1 (EE Integration Instance Configuration dialog) is a separate, completed
  spec: `docs/superpowers/specs/2026-05-31-embedded-ee-component-defined-inputs-design.md`.

## Problem

The embedded SDK React library (`sdks/frontend/embedded/library/react`, `ConnectDialog.tsx`) renders workflow
inputs as plain text/select fields with no notion of component-defined inputs. We want it to render inputs that
reference a component's input property or property group — including dynamic, connection-backed option dropdowns
and dependent options (`optionsLookupDependsOn`) — and groups rendered as compound inputs whose value is a nested
object.

Unlike sub-project 1 (a main-app surface that reused the shared `InputConfigurationList`), the SDK is a
**standalone, hand-written library** that shares no code with the main client and talks only to the embedded
**public** REST API (`/api/embedded/v1/...`). Today that public surface has a 4-field `Input` schema, no
component-definition-with-properties payload, and no dynamic-options endpoint. So this sub-project requires
**new server (public REST) work AND a hand-written SDK dialog rework**.

## Goals

- The SDK `ConnectDialog` renders component-defined workflow inputs:
  - a single referenced component property renders as the component's real property (scalar, static select, or
    connection-backed dynamic select);
  - a referenced property group renders as a compound input (a labeled group of member fields) whose value is a
    nested object;
  - dynamic selects fetch their options from a new public endpoint, scoped to the integration instance's
    connection; dependent options refetch when a parent value changes (`optionsLookupDependsOn`);
  - a reference whose resolved definition is missing degrades to a plain text input (no crash).
- Server: extend the embedded **public** REST surface to (a) carry the 4 reference fields on `Input`, (b) embed
  resolved property/group definitions in the integration payload, and (c) resolve dynamic options against the
  integration instance's connection.

## Non-goals (YAGNI)

- The EE main-app dialog (sub-project 1, done).
- Per-node / multi-component connection contexts. Options resolve against the single integration-instance
  connection (the embedded connect flow is built around one integration connection).
- Authoring component-reference inputs (platform editor; unrelated to the SDK).
- Reusing any main-client component in the SDK (it is intentionally standalone).
- Full platform `Property` fidelity in the public payload — only the subset the SDK renders (see schema below).

## Key decisions

| Decision | Choice |
| --- | --- |
| Definitions delivery | **Embed resolved property/group definitions in the integration payload** (`GET /api/embedded/v1/integrations/{id}`), so the SDK needs no extra definition fetch. |
| Dynamic options | **New public options endpoint** resolving against the integration instance's connection. |
| Connection context | **The integration instance's connection** (`IntegrationInstance.getConnectionId()`). |
| SDK rendering scope | **Single properties + compound groups + dynamic options + dependencies** (full parity). |
| Public Property schema | **Minimal subset** the SDK needs (name, label, type, controlType, options, a dynamic-options flag, optionsLookupDependsOn, group members) — not the full platform `Property`. |

## What already exists / comes for free

Confirmed by exploration:

- **atlas `Workflow.Input`** is the 8-field record (carries the 4 reference fields) and its parser reads them —
  embedded integration workflows use it, so the server already HAS the reference data per input.
- **Domain `ComponentDefinition`** (`com.bytechef.platform.component.domain.ComponentDefinition`) already exposes
  top-level `getProperties()` and `getPropertyGroups()`; `ComponentDefinitionService.getComponentDefinition(name,
  version)` returns it. `ComponentDefinitionApiController` in the embedded public module **already injects**
  `ComponentDefinitionService` — proving the service is reachable from the embedded public layer.
- **Option-resolution machinery** exists at the component-facade level: `ActionDefinitionFacade.executeOptions(...)`
  and `TriggerDefinitionFacade.executeOptions(...)` accept `inputParameters`, a `ComponentConnection`,
  `lookupDependsOnPaths`, and `searchText`, and invoke the component's `OptionsFunction`
  (`ActionDefinitionServiceImpl.executeOptions` → `OptionsFunction.apply(...)`). These are injectable Spring beans
  in the monolith.
- **Connection access:** `ConnectionService.getConnection(long id)` returns a `Connection` with decrypted
  parameters; a `ComponentConnection` is built from it (as `ActionDefinitionFacadeImpl.getComponentConnection`
  does).
- **Integration instance connection:** `IntegrationInstance.getConnectionId()` yields the connection the embedded
  user authorized.
- **Input value persistence** already accepts nested objects (`Map<String,?>`), so group values store unchanged.

Why the platform `WorkflowNodeOptionFacade` is NOT reused directly: it requires `workflowId`, a workflow test
configuration, and `environmentId` — workflow-editor concepts that do not fit the embedded connect context. We
reuse one level lower (`ActionDefinitionFacade`/`TriggerDefinitionFacade`) via a new embedded-scoped facade.

## Architecture

### Data flow

1. SDK opens → `GET /api/embedded/v1/integrations/{id}` returns workflows whose inputs now carry the 4 reference
   fields AND, for component references, an embedded resolved `property` (or `group` with member properties).
2. SDK renders each input: scalar/static → existing field; dynamic single property → dynamic select; group →
   compound fieldset of member fields.
3. For a dynamic select, SDK calls the new options endpoint with the input's `propertyName` (or group-member
   property), the current sibling/dependency values, and optional search text → `[{label, value}]`.
4. When a value that another option depends on (`optionsLookupDependsOn`) changes, SDK refetches the dependent
   options.
5. On submit, SDK PUTs `{ inputs: { [name]: value } }`; a group's value is a nested object
   `{ [memberName]: value }`.

### Server units

- **Public `Input` schema** (extended): add `componentName`, `componentVersion`, `propertyName`, `groupName`, and
  an embedded resolved definition (`property` for a single ref, `group` for a group ref).
- **Public `Property` / `PropertyGroup` schema** (new, minimal): `Property = {name, label, type, controlType,
  required?, options?: Option[], dynamicOptions?: boolean, optionsLookupDependsOn?: string[]}`;
  `PropertyGroup = {name, label?, properties: Property[]}`; `Option = {label, value}`.
- **`IntegrationApiController` mapping** (extended): when mapping a workflow input that is a component reference,
  resolve its component definition via `ComponentDefinitionService` and attach the minimal `property`/`group`
  (mark `dynamicOptions: true` when the property has an `optionsDataSource`; copy `optionsLookupDependsOn`).
- **New embedded options facade** (`EmbeddedWorkflowInputOptionFacade` or similar, embedded-configuration-service):
  `getOptions(integrationInstanceId, workflowUuid, propertyName, lookupDependsOnValues, searchText)` →
  resolves the instance's `connectionId` → `ConnectionService.getConnection` → `ComponentConnection` →
  delegates to `ActionDefinitionFacade`/`TriggerDefinitionFacade.executeOptions(...)` → `[Option]`. (Action vs.
  trigger + the component/operation are derived from the referenced input's component and the workflow node that
  uses it; planning pins the exact lookup.)
- **New public options endpoint** in `IntegrationInstanceWorkflowApiController` (or a sibling): e.g.
  `POST /api/embedded/v1/integration-instances/{id}/workflows/{workflowUuid}/options` with body
  `{propertyName, lookupDependsOnValues?, searchText?}` → `[{label, value}]`. Exact path/verb finalized in
  planning; also add the `{externalUserId}` variant consistent with the existing public endpoints.

### SDK units (hand-written; no codegen)

- **`types.ts`:** extend `WorkflowInputType` with `componentName?`, `componentVersion?`, `propertyName?`,
  `groupName?`, and a resolved `property?: ComponentPropertyType` / `group?: ComponentPropertyGroupType`, where
  `ComponentPropertyType = {name, label, type, controlType, required?, options?: OptionType[],
  dynamicOptions?: boolean, optionsLookupDependsOn?: string[]}`, `ComponentPropertyGroupType = {name, label?,
  properties: ComponentPropertyType[]}`, `OptionType = {label: string, value: string}`.
- **`index.tsx` hook (`useConnectDialog`):** integration fetch already returns inputs; consume the new resolved
  defs. Add an `apiClient` method calling the options endpoint; maintain an options cache keyed by
  `(workflowUuid, inputName|memberName, serialized dependency values)`; expose handlers to load options and to
  invalidate dependents when a value changes. Group values stored as nested objects under the input name.
- **`ConnectDialog.tsx`:** extend `DialogInputField` and the workflow-inputs renderer with: a dynamic select
  (loading/empty states, fetches via the hook), dependency-aware refetch, and a compound-group renderer (a
  labeled fieldset whose members bind to nested keys). Missing resolved definition → plain text fallback.

## Error handling / edge cases

- **Missing/dangling resolved definition** → plain text input fallback (no crash).
- **Options endpoint error or empty** → dynamic select shows a "no options" state but remains usable; scalar
  fallbacks unaffected.
- **No instance connection yet** → dynamic options need the authorized connection; before the connect step,
  dependent dynamic selects render a disabled "connect first" state and populate after connection.
- **Dependency not yet satisfied** (`optionsLookupDependsOn` parent empty) → dependent select disabled until the
  parent has a value, then fetches.
- **Group value** persists as a nested object; a partially-filled group submits the filled members.

## Testing

- **Server:**
  - Unit test the embedded options facade: resolves the integration instance's connection and delegates to the
    action/trigger options execution, returning options; handles missing connection / unknown property.
  - Controller/mapping test: the integration payload embeds resolved `property`/`group` defs for reference inputs
    (and `dynamicOptions`/`optionsLookupDependsOn` flags), and leaves primitive inputs unchanged.
  - Public OpenAPI regenerate (server models); new `Property`/`PropertyGroup`/`Option` schemas + extended `Input`.
- **SDK:** (confirm the lib's test runner in planning — `package.json` uses `vite build`; add/confirm Vitest)
  - dynamic select fetches and renders options;
  - changing a parent value refetches dependent options;
  - group renders member fields and submits a nested-object value;
  - dangling reference falls back to a text input.

## Implementation phasing (within this sub-project)

The plan will sequence these independently-reviewable slices:

1. Server: extend public `Input` + add minimal `Property`/`PropertyGroup`/`Option` schemas; embed resolved defs
   in the integration payload via `ComponentDefinitionService`. Regenerate public models.
2. Server: embedded options facade + new public options endpoint (+ `{externalUserId}` variant), delegating to
   action/trigger options execution scoped to the instance connection.
3. SDK: `types.ts` + `index.tsx` data flow (consume resolved defs; options fetch + cache + dependency
   invalidation; nested group values).
4. SDK: `ConnectDialog.tsx` rendering — single dynamic property, then compound groups, then dependency wiring;
   dangling fallback.
5. Tests + manual E2E (author a component-reference input on an embedded integration; open the SDK dialog;
   verify dynamic dropdown, dependent refetch, group nested submit).

## Scope summary

- **In:** extended public `Input` + minimal `Property`/`PropertyGroup`/`Option` public schemas; resolved defs
  embedded in the integration payload; embedded options facade + new public options endpoint scoped to the
  instance connection; SDK type + data-flow + rendering rework for single properties, compound groups, dynamic
  options, and dependencies; dangling fallback.
- **Out:** EE main-app dialog (sub-project 1); per-node/multi-component connection contexts; authoring; reuse of
  main-client components; full platform `Property` fidelity in the public payload.

## Rollback

Server changes are additive (new optional `Input` fields, new schemas, a new endpoint, a new facade) — primitive
inputs and existing endpoints are unaffected. SDK changes are additive in `types.ts` and gated by the presence of
resolved defs (inputs without them render exactly as today). No DB migrations (inputs are JSON maps). Revert the
commits to back out; the new endpoint simply becomes unused.
