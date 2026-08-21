# AI Hub property-options lookup tools — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan
- **Author:** Ivica Cardic

## Problem

In the AI Hub workflow-builder agent, when a workflow step has a property whose
values come from a third-party service (e.g. a Slack channel, a Notion database, an
Airtable base), the agent **hallucinates** the options instead of fetching the real
ones. The screenshot that triggered this work showed the agent offering `#general`,
`#standup`, `#team` as Slack channel choices in an `askUserQuestion` — none of which
came from the user's actual Slack workspace.

The desired behavior: when a property has dynamic options to choose from, the agent
fetches the **real** options and renders them as an `askUserQuestion` select — exactly
the way it already does for connections via `listConnectionsForComponent`.

## Root cause

The capability is **half-built**. Two prior commits
(`9667a8ee3aeb20c7a24b8501f5470fe9e9dbad6e`, `fbad182d10cd6af72c7bc768afb9a9b5edff70f3`)
added all of the supporting plumbing but never built the tool callback that consumes
it, so the consuming methods are dead code.

What already exists and works:

- **Descriptor metadata.** `ToolUtils.appendLookupMetadata`
  (`platform-ai-tool`) already emits `"lookupRequired": true` and
  `"lookupDependsOn": [...]` into a property's descriptor when the property implements
  `OptionsDataSourceAware` with a non-null `OptionsDataSource`. Static (fixed-enum)
  options are emitted inline by `convertToOptionList`. This descriptor is rendered into
  the component action/trigger catalog tool via
  `ComponentTools.generateParametersJson(action.getProperties())` and reaches the agent
  through tool search. **The agent already knows `channel` needs a lookup.**
- **The options engine.** `ActionDefinitionFacade.executeOptions(...)` /
  `TriggerDefinitionFacade.executeOptions(...)` — the same engine the workflow editor's
  option dropdowns use (`WorkflowNodeOptionFacadeImpl`). Supports a `searchText`
  argument.

What was added but is **unused** (dead):

- `ActionDefinitionService` / `TriggerDefinitionService` gate methods:
  `actionDefinesConnection`, `getPropertyLookupDependsOn`,
  `propertyHasOptionsDataSource` (implemented in the `*Impl`, full Javadoc that names a
  `lookupActionPropertyOptions` tool callback — which was never written).
- `PropertyPathResolver` (package-private, `platform-component-service`) — resolves
  dotted property paths (`parent.child`, `arrayProp[].child`, implicit `arrayProp.child`).
- `PropertyOptionsResolver` (`ai-hub-service`) — its `buildSuccessEnvelope`,
  `dependencyMissingEnvelope`, `noOptionsForPropertyEnvelope`, and
  `connectionRequiredEnvelope` helpers are all unused; only `withUserSecurityContext`
  is reused (by `ListConnectionsForComponentToolCallback`).

What is **missing** (this slice):

- The tool callback(s) the agent calls to perform the lookup.
- Prompt guidance — the build prompt currently states the agent has **no** such tool.

## Goals

1. Give the AI Hub agent a tool to fetch real, dynamic property options.
2. Render the returned options through `askUserQuestion` (select buttons), matching the
   connections UX.
3. Bring the existing dead plumbing into use rather than reinventing it.

## Non-goals

- The descriptor metadata emission (`ToolUtils`) — already done.
- The service/facade option engine and the resolver/path plumbing — already done.
- Adding new option providers to individual components.
- A separate "describe action properties" tool — not needed, because the catalog tool
  descriptors already carry `lookupRequired` / `lookupDependsOn` and the static options.

## Design

### New tool callbacks

Two Spring-AI `ToolCallback`s in
`server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/`,
modeled on `ListConnectionsForComponentToolCallback`:

| Class | Tool name |
| --- | --- |
| `LookupActionPropertyOptionsToolCallback` | `lookupActionPropertyOptions` |
| `LookupTriggerPropertyOptionsToolCallback` | `lookupTriggerPropertyOptions` |

Both share the existing `PropertyOptionsResolver` (this turns its dead helpers live)
and depend on the corresponding facade (`ActionDefinitionFacade` /
`TriggerDefinitionFacade`) and definition service (`ActionDefinitionService` /
`TriggerDefinitionService`).

### Tool parameters

- `componentName` (required)
- `componentVersion` (optional; resolve latest when omitted — follow the convention
  already used by sibling AI Hub tools / `ListConnectionsForComponentToolCallback`)
- `actionName` (action variant) / `triggerName` (trigger variant) — required
- `propertyName` (required; dotted paths supported via `PropertyPathResolver`
  conventions — `parent.child`, `arrayProp[].child`, implicit `arrayProp.child`)
- `inputParameters` (optional map; sibling values needed for dependency-bearing
  lookups)
- `connectionId` (optional)
- `searchText` (optional; passed through to `executeOptions`)

### Resolution flow (per call)

1. `propertyHasOptionsDataSource(component, version, action/trigger, propertyName)`
   → `false` ⇒ return `PropertyOptionsResolver.noOptionsForPropertyEnvelope()`
   (the property is free-text; the agent should set the value directly).
2. `getPropertyLookupDependsOn(...)` → for any returned path **absent** from
   `inputParameters`, return `PropertyOptionsResolver.dependencyMissingEnvelope(missing)`.
3. `actionDefinesConnection(...)` is `true` **and** `connectionId` is null ⇒ return
   `PropertyOptionsResolver.connectionRequiredEnvelope(componentName)` (the agent then
   calls `listConnectionsForComponent` / `createConnection` and retries).
4. Otherwise, inside `PropertyOptionsResolver.withUserSecurityContext(userId, ...)`,
   call the facade `executeOptions(...)`:
   - cap the returned options at **N = 25**;
   - pass `searchText` through;
   - return `PropertyOptionsResolver.buildSuccessEnvelope(...)` with a `truncated`
     boolean set when the result was capped.

`buildSuccessEnvelope` must be extended (or a sibling overload added) to carry the
`truncated` flag; the current signature emits only
`componentName / actionName|triggerName / propertyName / options`.

The generic failure path stays with `ToolErrors.runtimeFailure(...)` (the
`lookup_failed` envelope), consistent with the resolver's documented contract.

### Connection requirement

For providers that call the third-party API (e.g. Slack channels), `executeOptions`
needs a real `connectionId`. The agent already calls `listConnectionsForComponent`
and the user picks one, so a `connectionId` is available in the conversation; it is
passed as a tool parameter. `actionDefinesConnection` gates the `connection_required`
envelope so connection-free components skip the requirement.

### Wiring

Register both callbacks in **both** the ASK-mode and BUILD-mode tool catalogs in
`AiHubConfiguration`, in the same two assembly methods where
`ListConnectionsForComponentToolCallback` and `AskUserQuestionToolCallback` are added.
Construct them with the facades, definition services, and the shared
`PropertyOptionsResolver` bean.

### Prompt changes

`server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`
(~lines 279–286): replace the paragraph that asserts "You have no tool that enumerates
a third-party service's own resources (Slack channels, Notion databases, Airtable
bases, etc.)" with guidance to:

- call `lookupActionPropertyOptions` / `lookupTriggerPropertyOptions` whenever a
  property descriptor shows `"lookupRequired": true`, before asking the user;
- first satisfy any `lookupDependsOn` siblings (include them in `inputParameters`) and
  select a connection if `connection_required` is returned;
- render the returned options as `askUserQuestion` options;
- when the success envelope is `truncated`, ask the user to narrow via `searchText` or
  to type the value directly, rather than presenting an incomplete button set;
- continue to never fabricate option values.

Mirror the relevant correction in the ASK prompt (`prompt_ai_hub_ask.txt`) if it
carries the same "no such tool" claim.

## Testing (TDD)

- `LookupActionPropertyOptionsToolCallbackTest` and
  `LookupTriggerPropertyOptionsToolCallbackTest` — cover each branch:
  `no_options_for_property`, `dependency_missing`, `connection_required`, and
  `success` (including the cap at 25 and `searchText` passthrough / `truncated` flag),
  mirroring `ListConnectionsForComponentToolCallbackTest`.
- `PropertyOptionsResolverTest` — add coverage for the now-live envelope builders
  (`buildSuccessEnvelope` incl. `truncated`, `dependencyMissingEnvelope`,
  `noOptionsForPropertyEnvelope`, `connectionRequiredEnvelope`).
- Extend the agent bean-wiring test (e.g. `AiHubAgentBeanWiringTest`) to assert both
  new tools are present in the ASK and BUILD catalogs.

## EE conventions

All new files live under `server/ee/` → ByteChef Enterprise license header and
`@version ee` Javadoc tag (Spotless selects the header by file content, so the tag is
required on every new file including tests).

## Open questions

None outstanding. Decisions locked during brainstorming:

- Scope: build **both** action and trigger variants (uses both service plumbings).
- Large result sets: **search param + cap** (N = 25, `searchText` passthrough,
  `truncated` flag).
- No separate "describe properties" tool — descriptors already carry the metadata.
