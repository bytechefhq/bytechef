# ConnectDialog Self-Fetched Workflow Input Options & Group-Member Persistence — Design

- **Date:** 2026-06-04
- **Status:** Design
- **Area:** Embedded SDK — `sdks/frontend/embedded/library/react`
- **Scope:** Client only (server endpoints already exist and are public)

## Problem

The embedded `ConnectDialog` renders a workflow's component-defined inputs. A component-defined
input is rendered as a property **group**; group members may have **dynamic options** (e.g. the
Slack `CHANNEL` input, whose values come from `SlackUtils::getChannelIdOptions` server-side, and
which may depend on a sibling member such as a workspace selection).

Tracing the code shows the entire group-input feature is currently inert because the host hook
`useConnectDialog` (`index.tsx`) passes **none** of the three group-related props to
`ConnectDialog`; they all default to no-ops:

1. `loadWorkflowInputOptions` → no-op: dynamic options never load.
2. `workflowInputOptions` → `{}`: no options cache.
3. `handleWorkflowGroupInputChange` → no-op: selecting a group-member value never persists, and
   there is no group-member save logic in the hook at all (only flat `handleWorkflowInputChange`
   for plain inputs).

The combined effect: group fields render, but the dropdowns stay empty and any selection is lost.
Dependent options (workspace → channel) can never work because the dependency value never lands in
state.

This is true for **both** regular workflows and MCP workflows.

## Goal

Deliver a working feature:

- **Option loading lives inside `ConnectDialog`.** The dialog fetches options itself from the
  existing public endpoint, caches them, gates on dependencies, and resets when the
  integration/instance changes. The hook supplies only transport (`fetch`) and context
  (`integrationInstanceId`) — no options logic.
- **Group-member selections persist** for both regular and MCP workflows, via the existing
  per-workflow update endpoints, so dropdown selections save and dependent options resolve.

The SDK consumer's surface (`useConnectDialog`) is unchanged.

## Existing server endpoints (no change)

### Options resolution — one endpoint, used for regular *and* MCP workflows

`POST /api/embedded/v1/integration-instances/{id}/workflows/{workflowUuid}/options`
(`getFrontendIntegrationInstanceWorkflowInputOptions`), in the **public-rest** module, secured
with `jwtBearerAuth` (the frontend SDK token the dialog already carries).

Options resolution is keyed purely by `(integrationInstanceId, workflowUuid)` and has **no**
"regular vs MCP" distinction:

```
ConnectedUserIntegrationInstanceFacadeImpl.getIntegrationInstanceWorkflowInputOptions
  -> EmbeddedWorkflowInputOptionFacadeImpl.getWorkflowInputOptions
       getWorkflowId(integrationInstanceId, workflowUuid)               // by uuid
         -> integrationWorkflowRepository.findByIntegrationInstanceIdAndUuid(...)
       load workflow, find input by inputName, read componentReference
       run the component's options function with the instance's connectionId
```

An MCP workflow is just an `IntegrationWorkflow` row under the same instance, addressed by its
uuid, so this endpoint resolves MCP-workflow group-member options too. The separate
`/mcp-workflows/...` endpoints exist only for operations that genuinely differ between regular and
MCP exposure (enable/disable/update) — there is intentionally **no** `/mcp-workflows/.../options`
endpoint and none is needed.

**Request body** (`WorkflowInputOptionsRequest`):

| Field                   | Meaning                                                                    | Source in dialog                     |
|-------------------------|----------------------------------------------------------------------------|--------------------------------------|
| `inputName` (required)  | The workflow input whose referenced component the options resolve against. | `input.name`                         |
| `propertyName` (req.)   | The component property (group member) whose options to resolve.            | `member.name`                        |
| `lookupDependsOnValues` | Current values of the properties this lookup depends on.                   | collected from sibling member values |
| `searchText`            | Optional typeahead filter.                                                 | omitted in v1                        |

**Response:** `Option[]` → `{label, value}` (mapped server-side).

`inputName` is the **workflow input** name (`input.name`), not the group name. The currently
dangling wiring passed `group.name`; these usually coincide (the resolved group is named after the
input) but are not guaranteed equal, so the new code uses `input.name` for both the options request
and persistence.

### Persistence — existing per-workflow update endpoints

- Regular: `PUT /api/embedded/v1/integration-instances/{id}/workflows/{workflowUuid}` with body
  `{ inputs: Map<String,Object> }` (already used by `handleWorkflowInputChange`).
- MCP: `PUT /api/embedded/v1/integration-instances/{id}/mcp-workflows/{workflowUuid}` (already used
  by `handleMcpWorkflowInputChange`).

A group input's value is a **nested object keyed by member name** — e.g. input `channel` →
`{ channelId: "C1", workspace: "W1" }`. The update body therefore carries
`inputs[input.name] = { [member.name]: value, ... }`. The endpoints accept `Map<String,Object>`,
so nested objects need no contract change.

> **Implementation must verify:** that the workflow engine / storage reads a component-defined
> input's value as this nested member-keyed object. The client types and existing dynamic tests
> already model it this way (`value: { workspace: "W1" }`), and the project convention is
> "definition FLAT, model/REST NESTED", so nested is expected — but confirm against the server
> read path before relying on it.

## Approach (B): dedicated internal hook for option loading

Option loading lives behind a small hook used inside `ConnectDialog`, keeping the dialog focused on
rendering and making fetch/cache/dedup independently testable. Persistence stays in
`useConnectDialog` alongside the existing input/save logic, because it owns the override state and
the debounced PUTs.

### 1. New `useWorkflowInputOptions.ts`

```
useWorkflowInputOptions(apiFetch, integrationInstanceId)
  -> { optionsByKey, loadOptions, resetOptions }
```

- `optionsByKey: Record<string, OptionType[]>` — cache keyed by
  `optionsCacheKey(workflowUuid, inputName, propertyName, lookupDependsOnValues)` (existing
  `utils.ts` helper; already includes dependency values so distinct dependency combinations cache
  separately and same-name properties on different inputs do not collide).
- `loadOptions(workflowUuid, inputName, propertyName, lookupDependsOnValues)`:
  - Computes the cache key.
  - Returns early if the key is already cached **or** currently in flight (in-flight keys tracked
    in a `Set` ref so the effect firing on mount + dependency change does not issue duplicates).
  - Returns early if `integrationInstanceId` is missing.
  - `POST`s to the options endpoint with `{inputName, propertyName, lookupDependsOnValues}` via the
    injected `apiFetch`, stores the returned `OptionType[]` under the key, and clears the in-flight
    marker on both success and error.
- `resetOptions()` clears `optionsByKey` and the in-flight set.

Errors are logged (`console.error`, matching the existing hook layer) and leave the field showing
"No options"; a failed lookup must not throw into render.

The same hook serves regular and MCP workflows — `loadOptions` always targets the
`/workflows/{workflowUuid}/options` path regardless of how the workflow is exposed.

### 2. `ConnectDialog.tsx`

- **Remove** the `loadWorkflowInputOptions` and `workflowInputOptions` *input* props.
- **Add** `apiFetch` (the host's `fetch` client) and `integrationInstanceId` props.
- Call `useWorkflowInputOptions(apiFetch, integrationInstanceId)` and pass the resulting
  `loadOptions` / `optionsByKey` down through the existing internal plumbing for **both** the
  workflows container and the tools container. Sub-component signatures
  (`DialogGroupField` → `DialogDynamicSelectField`) are unchanged — only the *source* of
  `loadOptions` / options changes from props to internal state.
- Thread `input.name` into `DialogGroupField` (new `inputName` prop) so both the options request
  and the change handler use the correct workflow input name rather than `group.name`.
- `DialogToolsContainer` receives the **real** `loadOptions`/`optionsByKey` (today it gets them via
  props that are never supplied) and a **real** MCP group-change handler (today it passes
  `handleWorkflowGroupInputChange: () => {}` to `renderWorkflowInput`).
- Add an effect that calls `resetOptions()` when `integration?.id` or `integrationInstanceId`
  changes. Required because the dialog is never unmounted on close (it returns `null` while
  `isOpen === false`), so without a reset a second integration would display the first one's cached
  options.

### 3. `index.tsx` (`useConnectDialog`)

- Pass `apiFetch={fetch}` and `integrationInstanceId={currentIntegrationInstanceId}` into the
  rendered `<ConnectDialog>`.
- Pass the now-implemented group-change handlers (below).
- No options logic is added to the hook; the previously-dangling options props are simply gone.

#### Group-member persistence (regular + MCP)

- Widen the override state value type from `Record<string, Record<string, string>>` to allow nested
  group values: `Record<string, Record<string, string | Record<string, string>>>` for both
  `inputOverrides` and `mcpWorkflowInputOverrides`.
- Add `handleWorkflowGroupInputChange(workflowUuid, inputName, memberName, value)`:
  - merges the member into the input's object value:
    `inputOverrides[workflowUuid][inputName] = { ...existing, [memberName]: value }`;
  - debounced PUT to the regular update endpoint with the merged `inputs` map (reusing the existing
    debounce-per-workflow pattern from `handleWorkflowInputChange`).
- Add `handleMcpWorkflowGroupInputChange(...)` — the MCP mirror, writing to
  `mcpWorkflowInputOverrides` and PUTting to the `mcp-workflows` update endpoint.
- `mergedWorkflows` / `mergedMcpWorkflows`: for a component-defined (group) input, default the
  merged `value` to `{}` instead of `''` so `renderWorkflowInput` reads a member-keyed object
  (`memberValues`). Detect group inputs via `input.componentReference?.group`.

### State persistence note

`useConnectDialog` re-renders the dialog imperatively via `rootRef.current.render(<ConnectDialog
… />)`. Because it is the same component type at the same position in the same nested root, React
reconciles it as one instance, so `useState`/refs inside `useWorkflowInputOptions` persist across
those re-renders. The explicit `resetOptions()` effect bounds the cache lifetime, not unmount.

## Testing

- **`useWorkflowInputOptions.test.ts`** (new): cache hit avoids a second fetch; in-flight dedup
  (two synchronous calls for one key → one request); missing `integrationInstanceId` issues no
  request; `resetOptions` clears the cache; request body carries the correct
  `inputName` / `propertyName` / `lookupDependsOnValues`.
- **`ConnectDialog.dynamic.test.tsx`** (update): replace injected
  `loadWorkflowInputOptions`/`workflowInputOptions` props with a mocked `apiFetch`. Assert:
  (a) a single-property dynamic group member renders a populated select after the mocked fetch
  resolves; (b) the request body matches the contract (using `input.name` as `inputName`);
  (c) a dependent member stays disabled ("Select dependencies first") and issues no request until
  its dependency has a value, then loads with the dependency value; (d) selecting a group-member
  value invokes the group-change handler with `(workflowUuid, input.name, member.name, value)`;
  (e) a dangling component reference (no resolved group) falls back to a plain text input;
  (f) an MCP-workflow group member loads options from the same `/workflows/{uuid}/options` path and
  reports member changes via the MCP group-change handler.
- **`index.test.tsx`** (update/extend): group-change handlers merge nested member values and issue
  the debounced PUT with `inputs[input.name]` as a member-keyed object, for both regular and MCP
  update endpoints.
- Before committing, from `sdks/frontend/embedded/library/react`: `npm run lint`, `npx tsc --noEmit`
  (typecheck), and `npx vitest run` (this package has no aggregate `check` script). Run
  `npm run format:fix` for formatting.

## Out of scope (v1)

- **Typeahead `searchText`.** The endpoint supports it; the dialog will not send it yet.

(No other carve-outs: regular and MCP workflow component-defined group inputs are both fully
supported — options loading and member-value persistence.)

## Files touched

| File | Change |
|------|--------|
| `…/connect-dialog/useWorkflowInputOptions.ts` | New hook: fetch + cache + in-flight dedup + reset. |
| `…/connect-dialog/ConnectDialog.tsx` | Drop options data props; add `apiFetch`/`integrationInstanceId`; call hook; thread `input.name`; real `loadOptions` + MCP group handler for the tools container; reset effect. |
| `…/connect-dialog/index.tsx` | Pass `apiFetch` + `integrationInstanceId`; implement + pass `handleWorkflowGroupInputChange` and `handleMcpWorkflowGroupInputChange`; widen override types; group-aware `value` defaulting in merge memos. |
| `…/connect-dialog/types.ts` | Widen override-related types if any are shared; otherwise unchanged. |
| `…/connect-dialog/useWorkflowInputOptions.test.ts` | New unit tests. |
| `…/connect-dialog/ConnectDialog.dynamic.test.tsx` | Mock `apiFetch`; cover regular + MCP group members and member-change reporting. |
| `…/connect-dialog/index.test.tsx` | Cover group-member persistence (regular + MCP). |

No server files change.
