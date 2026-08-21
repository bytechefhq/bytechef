# Dedicated Component-Input Options Endpoint for Embedded ConnectDialog — Design

- **Date:** 2026-06-10
- **Status:** Design
- **Area:** Embedded EE (`server/ee/libs/embedded/embedded-configuration`) + platform component
  registry/service + Embedded SDK (`sdks/frontend/embedded/library/react`)
- **Branch:** `0_732`

## Problem

The embedded `ConnectDialog` renders a workflow's component-defined inputs. A component-defined
input is a property **group** whose members may have **dynamic options** — e.g. the Slack `channel`
input, whose values come from `SlackUtils::getChannelIdOptions`, registered at the **component-input
level**:

```java
component("slack")
    .inputs(
        string(CHANNEL)
            .label("Channel")
            .options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)
            .required(true))
```

Today the embedded options call (`POST .../integration-instances/{id}/workflows/{uuid}/options`,
served by `EmbeddedWorkflowInputOptionFacadeImpl`) resolves options by **finding a workflow node**
that uses the component (`findNodeReference`) and running **that node's** action/trigger property
options:

```java
NodeReference nodeReference = findNodeReference(workflow, componentName);   // a TASK or TRIGGER using "slack"

if (nodeReference.trigger()) {
    return triggerDefinitionFacade.executeOptions(componentName, version, nodeReference.operation(), propertyName, ...);
}
return actionDefinitionFacade.executeOptions(componentName, version, nodeReference.operation(), propertyName, ...);
```

This couples component-input options to the workflow's node graph. It fails (empty dropdown / "No
options") whenever:

1. the workflow has **no action node** for that component → `findNodeReference` throws
   `IllegalArgumentException: No workflow node uses component: <name>` → 500; or
2. the only matching node is a **trigger** that doesn't declare the property (e.g.
   `SlackAnyEventTrigger` has no `channel`) → `executeOptions` finds nothing → empty list.

The options function registered on `component(...).inputs(...)` is never executed; the path only
works by coincidence when a matching **action** node declares the same property with the same
options function.

## Goal

A dedicated public endpoint that executes the **component-input-level** `OptionsFunction` directly,
keyed by the component reference the SDK already holds (`componentName`, `componentVersion`,
`groupName`, `propertyName`), using the integration instance's connection — with **no dependency on
the workflow's node graph**. The old node-coupled path is removed so there is exactly one way to
resolve input options.

## Non-goals

- Typeahead `searchText` wiring in the dialog (endpoint accepts it; dialog omits it in v1, matching
  current behavior).
- Changes to how group-member **values are persisted** — persistence stays workflow-input-keyed via
  the existing `PUT .../workflows/{uuid}` and `PUT .../mcp-workflows/{uuid}` endpoints. Only the
  **options-loading** call changes.

## Feasibility (verified)

- The raw `com.bytechef.component.definition.ComponentDefinition.getInputs()` returns
  `Optional<List<? extends PropertyGroup>>`; each `PropertyGroup.getProperties()` exposes the value
  properties. `component(...).inputs(P... properties)` wraps each lone property in a
  `PropertyGroup` **named after the property** — so Slack's input group `channel` holds one property
  `channel` carrying the options function.
- `ComponentDefinitionRegistry.getActionProperty(...)` already shows the resolution pattern
  (`getProperties()` → `getProperty(propertyName, properties, ...)`, with dynamic-property handling).
  A parallel `getComponentInputProperty(...)` is a faithful mirror.
- `OptionsFunction.apply(...)` requires an `ActionContext` (used for the HTTP client). In
  `ContextFactoryImpl.createActionContext`, `actionName` is only passed through as context metadata
  (logging / HTTP scoping), **not** used for routing — so an `ActionContext` minted with a synthetic
  action name runs a component-input options function correctly.
- The SDK already carries `componentName`, `componentVersion`, `groupName` in
  `input.componentReference` (`ComponentInputReferenceType`) — no extra request-side server plumbing.

## Design

### Endpoint (component-ref keyed, instance-scoped for connection + ownership)

```
POST /api/embedded/v1/integration-instances/{id}/component-input-options                 (frontend; externalUserId from JWT/security)
POST /api/embedded/v1/{externalUserId}/integration-instances/{id}/component-input-options (backend)
```

The instance id stays in the path because the **connection** (e.g. the Slack token) is only
reachable through the integration instance, and because instance ownership is the authorization /
anti-enumeration boundary.

**Request body** `ComponentInputOptionsRequest`:

| Field                   | Required | Meaning                                                  | SDK source                          |
|-------------------------|----------|----------------------------------------------------------|-------------------------------------|
| `componentName`         | ✓        | Component owning the input group.                        | `input.componentReference.componentName` |
| `componentVersion`      | ✓        | Component version.                                       | `input.componentReference.componentVersion` |
| `groupName`             | ✓        | The input group; disambiguates same-named properties.    | `input.componentReference.groupName` |
| `propertyName`          | ✓        | The group member whose options to resolve.               | `member.name`                       |
| `lookupDependsOnValues` | –        | Current values of properties this lookup depends on.     | collected from sibling member values |
| `searchText`            | –        | Optional typeahead filter.                               | omitted in v1                       |

**Response:** `List<OptionModel>` → `{label, value}` (value `String.valueOf(option.getValue())`).
Instance not owned by the connected user → **empty list** (anti-enumeration), matching today's
facade behavior.

### Server layers

Each layer is a small, single-purpose unit mirroring the existing action-options machinery.

1. **`ComponentDefinitionRegistry.getComponentInputProperty(componentName, componentVersion,
   groupName, propertyName, inputParameters, connectionParameters, lookupDependsOnPaths, context)`** —
   mirror of `getActionProperty`:
   - `getComponentDefinition(componentName, componentVersion)`;
   - `getInputs().orElse(List.of())`, find the `PropertyGroup` whose name equals `groupName`
     (`orElseThrow` with a clear message if absent);
   - `getProperty(propertyName, group.getProperties(), inputParameters, connectionParameters,
     lookupDependsOnPaths, context)` — reuses the existing dynamic-property resolution.

2. **Execution + connection-resolution** on the component facade/service (the component-scoped home,
   alongside the existing action/trigger facades). Two methods, matching the action shape:
   - Service: `executeComponentInputOptions(componentName, componentVersion, groupName, propertyName,
     inputParameters, lookupDependsOnPaths, searchText, ComponentConnection componentConnection)` —
     mirror of `ActionDefinitionServiceImpl.doExecuteOptions`:
       - `contextFactory.createActionContext(componentName, componentVersion, <synthetic action
         name>, null, null, null, null, null, componentConnection, null, null, true)`;
       - `convert(inputParameters, lookupDependsOnPaths, componentConnection)`;
       - `getComponentInputProperty(...)` → `DynamicOptionsProperty` → `getOptionsDataSource()`
         (`orElseThrow`) → `getOptions()` → `OptionsFunction.apply(...)` → map to `Option`;
       - wrap failures in `ConfigurationException` as the action path does.
   - Facade: `executeComponentInputOptions(..., @Nullable Long connectionId)` — resolves
     `connectionId → ComponentConnection` (as `ActionDefinitionFacadeImpl.executeOptions` does) and
     delegates to the service.

   > The synthetic action name is metadata-only. Use a stable constant (e.g.
   > `"__componentInput__"`) so logs are recognizable.

3. **`ConnectedUserIntegrationInstanceFacade.getComponentInputOptions(externalUserId,
   integrationInstanceId, componentName, componentVersion, groupName, propertyName,
   lookupDependsOnValues, searchText)`** (EE embedded service):
   - `getIntegrationInstance(id)`; if not owned by the connected user → `return List.of()`
     (same anti-enumeration guard as the current `getIntegrationInstanceWorkflowInputOptions`);
   - `connectionId = integrationInstance.getConnectionId()`;
   - `lookupDependsOnPaths = List.copyOf(lookupDependsOnValues.keySet())`;
   - delegate to the component facade method (2).

4. **Controller** in `embedded-configuration-public-rest` (frontend + backend variants), resolving
   `externalUserId` from security for the frontend variant, then mapping `List<Option>` →
   `List<OptionModel>`.

### Removed (node-coupled path retired)

- `EmbeddedWorkflowInputOptionFacade` and `EmbeddedWorkflowInputOptionFacadeImpl` (incl.
  `findNodeReference` / `NodeReference`).
- `ConnectedUserIntegrationInstanceFacade.getIntegrationInstanceWorkflowInputOptions` (+ impl) and
  its callers.
- OpenAPI operations `getFrontendIntegrationInstanceWorkflowInputOptions` /
  `getIntegrationInstanceWorkflowInputOptions` and the `WorkflowInputOptionsRequest` schema from the
  embedded public spec; regenerate. The controller methods backing them are deleted.

### SDK (`sdks/frontend/embedded/library/react`)

- **`useWorkflowInputOptions.ts`**: retarget to the new endpoint. `loadOptions(componentName,
  componentVersion, groupName, propertyName, lookupDependsOnValues)` POSTs
  `{componentName, componentVersion, groupName, propertyName, lookupDependsOnValues}` to
  `/api/embedded/v1/integration-instances/${integrationInstanceId}/component-input-options`. Cache
  key becomes `(componentName, componentVersion, groupName, propertyName, lookupDependsOnValues)`
  (so two workflow inputs referencing the same component group+property correctly share one cache
  entry). Keep the in-flight dedup, missing-`integrationInstanceId` no-op, generation/reset, and
  `console.error`-on-failure semantics. Update `optionsCacheKey` in `utils.ts` accordingly.
- **`ConnectDialog.tsx`** `DialogGroupField` / `DialogDynamicSelectField`: thread the
  `componentReference` (`componentName`, `componentVersion`, `groupName`) into the options call.
  `propertyName` stays `member.name`. **Persistence is unchanged** — `handleWorkflowGroupInputChange`
  continues to key by `inputName`.
- **`index.tsx`**: no transport change (already passes `apiFetch` + `integrationInstanceId`).
- The same hook continues to serve regular and MCP workflows (component reference is identical
  regardless of how the workflow is exposed).

## Testing

**Server**
- `ComponentDefinitionRegistry.getComponentInputProperty` resolves a property from a component input
  group; throws clearly on unknown group / property.
- Component-input options execution runs the **component-input** `OptionsFunction` (Slack `channel`)
  and returns options **with no workflow node present** — the regression the old path couldn't
  handle.
- EE facade `getComponentInputOptions` returns `[]` when the instance is not owned by the connected
  user; delegates with the instance connection when owned.
- Controller contract: request body binds; response maps `{label, value}`.

**SDK**
- `useWorkflowInputOptions`: request body + new cache key contract; in-flight dedup; missing
  `integrationInstanceId` issues no request; `resetOptions` clears cache.
- `ConnectDialog.dynamic.test.tsx`: a dynamic group member loads options from the new
  `component-input-options` path with the component-ref body; dependent member stays disabled until
  its dependency has a value, then loads; selecting a value still reports via the (unchanged)
  group-change handler keyed by `inputName`; MCP-workflow group member uses the same path.

**Before committing**
- Server: `./gradlew spotlessApply` then `./gradlew check`.
- SDK (`sdks/frontend/embedded/library/react`): `npm run lint`, `npx tsc --noEmit`, `npx vitest run`,
  `npm run format:fix`.

## Risks / open points

- **Synthetic `ActionContext` action name** — verified metadata-only in `ContextFactoryImpl`; if any
  options function reflected on the action name it would break, but none do (they use connection +
  HTTP).
- **Dynamic component-input properties** (`optionsLoadedDynamically`) — `getProperty` handles dynamic
  resolution for action properties; the mirrored path inherits it. Covered by reusing `getProperty`
  rather than re-implementing.
- **OpenAPI regen churn** — removing the old operations + adding the new one regenerates embedded
  REST models; commit generated changes alongside the spec/operation files.

## Files touched (indicative)

| File | Change |
|------|--------|
| `…/platform/component/ComponentDefinitionRegistry.java` | New `getComponentInputProperty(...)`. |
| `…/component/service/ComponentDefinitionService(Impl).java` (or sibling) | New `executeComponentInputOptions(...)`. |
| `…/component/facade/ComponentDefinitionFacade(Impl).java` | New `executeComponentInputOptions(..., connectionId)`. |
| `…/embedded/configuration/facade/ConnectedUserIntegrationInstanceFacade(Impl).java` | Add `getComponentInputOptions(...)`; remove `getIntegrationInstanceWorkflowInputOptions`. |
| `…/embedded/configuration/facade/EmbeddedWorkflowInputOptionFacade(Impl).java` | **Delete.** |
| embedded public OpenAPI spec + generated models/controller | Add `component-input-options` op + `ComponentInputOptionsRequest`; remove old options ops + `WorkflowInputOptionsRequest`. |
| `…/public_/web/rest/IntegrationInstance*ApiController.java` | New handler; remove old options handlers. |
| `…/connect-dialog/useWorkflowInputOptions.ts` + `utils.ts` | Retarget endpoint + new cache key. |
| `…/connect-dialog/ConnectDialog.tsx` | Thread component reference into options call. |
| `…/connect-dialog/*.test.ts(x)` | Update to new endpoint/body. |
