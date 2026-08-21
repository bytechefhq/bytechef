# AI Hub `selectPropertyOption` render tool — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan
- **Author:** Ivica Cardic
- **Follows:** `2026-06-10-ai-hub-property-options-lookup-design.md` and `...-ux-fixes-design.md`

## Context

The property-options lookup tool works — a live `lookupActionPropertyOptions` call for
`slack`/`sendChannelMessage`/`channel` returned **11 real channels**. But the agent then
re-typed only **4** of them into `askUserQuestion`, so the user saw a cherry-picked subset
(and, being ≤ 8, it rendered as buttons, never reaching the new combobox). The failure mode
is structural: **routing the option list through the LLM lets it drop options**. No amount
of prompt tightening or client combobox work removes that dependency, because the LLM
controls what reaches `askUserQuestion`.

The connection picker already solves this the right way: the `selectConnection` tool returns
a tiny marker `{kind:"select-connection", componentName, componentLabel}`; the client
(`AiHubRuntimeProvider` interceptor → `data-select-connection` part →
`AiHubSelectConnectionMessage`) renders a real `<Select>` whose contents the **client**
controls. The LLM never re-emits the list, so it cannot cherry-pick.

## Goal

Render component-property options as a real picker driven by the **tool result**, not the
LLM's re-emit — so every option the lookup found is shown and the workflow receives the
option's real **value** (e.g. the Slack channel ID `C06H2PR8LSV`), not just its label.

## Approach

Mirror the `selectConnection` split: keep `lookupActionPropertyOptions` as the read/inspect
tool, and add a dedicated **render** tool `selectPropertyOption`. One wrinkle distinguishes
it from `selectConnection`: the client cannot independently re-fetch action-property options
(the only client option query, `workflowNodeOptions.queries.ts`, is workflow-node-coupled,
and the agent hasn't built a workflow yet). But the tool **already fetches the options
server-side** via `executeOptions`. So unlike `selectConnection`, the `selectPropertyOption`
marker **embeds the options array** — the client renders them directly.

### Scope

Both action and trigger properties (full parity with the lookup tools): two tools,
`selectPropertyOption` (actions) and `selectTriggerPropertyOption` (triggers). Both emit the
**same** `select-property-option` marker shape, so the client has one interceptor branch and
one renderer — the action/trigger distinction is a server-side concern only.

## Server

### Shared resolution helper (DRY)

`LookupActionPropertyOptionsToolCallback` and the new `SelectPropertyOptionToolCallback`
share the exact same gating + fetch sequence (validate names → `action_not_found` /
`property_not_found` → `dependency_missing` → `connection_required` → `executeOptions` →
cap at 25). Extract that into a shared helper rather than duplicate it.

Add to `PropertyOptionsResolver` (it already holds `withUserSecurityContext` and the
envelope builders) a method that performs the full action-property resolution and returns a
sealed result:

```
sealed interface OptionsLookupResult
record Failure(Map<String,Object> envelope, String metricTag) implements OptionsLookupResult
record Success(List<Option> options, boolean truncated)        implements OptionsLookupResult
```

Two resolve methods (action and trigger differ only by which service/facade + the
`actionDefinesConnection` vs `triggerDefinesConnection` gate, exactly as the two lookup
callbacks already do):

- `resolveActionPropertyOptions(actionDefinitionService, actionDefinitionFacade,
  invocationContext, componentName, componentVersion, actionName, propertyName,
  inputParameters, connectionId, searchText, maxOptions)`
- `resolveTriggerPropertyOptions(triggerDefinitionService, triggerDefinitionFacade, …,
  triggerName, …)`

Each returns `Failure` (with the matching envelope + metric tag: `action_not_found` /
`trigger_not_found` / `property_not_found` / `dependency_missing` / `connection_required` /
`no_options`) or `Success` (capped options + `truncated`). All four callbacks (two lookup,
two select) call the appropriate resolve method, record the returned `metricTag`, and only
differ in how they format success:

- `LookupAction/TriggerPropertyOptionsToolCallback` → `buildSuccessEnvelope(...)` (options to LLM).
- `SelectPropertyOptionToolCallback` / `SelectTriggerPropertyOptionToolCallback` → the
  `select-property-option` marker (options to client).

This refactor re-touches the shipped lookup callbacks; their existing tests stay green (same
behavior, just relocated logic).

### `SelectPropertyOptionToolCallback` / `SelectTriggerPropertyOptionToolCallback`

Two new `ToolCallback`s, tool names `selectPropertyOption` (action) and
`selectTriggerPropertyOption` (trigger), in
`server/ee/libs/ai/ai-hub/ai-hub-service/.../tool/`. Same inputs as the corresponding lookup
tool (`componentName`, `componentVersion?`, `actionName`/`triggerName`, `propertyName`,
`connectionId?`, `inputParameters?`, `searchText?`). On `Failure` they return the same error
envelopes the lookup tools return (so the agent self-corrects with `valid` names). On
`Success` they return the same marker shape (the operation name is omitted — the client
doesn't need it):

```json
{
  "kind": "select-property-option",
  "componentName": "slack",
  "propertyName": "channel",
  "options": [{"label": "general", "value": "C06H2PR8LSV"}, ...],
  "truncated": false
}
```

`@JsonInclude(NON_NULL)`, mirroring `SelectConnectionOutput`. Both registered in **both** ASK
and BUILD tool catalogs alongside the lookup/connection tools (the same
`registerToolAttachStateVisibilityToolCallbacks` helper, or the connection-tool registration
site).

`DESCRIPTION` (action; trigger analogous with "TRIGGER"/`triggerName`): "Ask the user to pick
a value for a component ACTION property that has a dynamic option list (descriptor shows
`lookupRequired: true`) — e.g. a Slack channel. The client renders a searchable dropdown of
ALL options fetched from the connection; the user's pick (its real value/id) is captured as
their next message. Satisfy `lookupDependsOn` siblings via `inputParameters` and pass
`connectionId` when required. Use this instead of hand-building askUserQuestion for option
properties."

## Client

1. **Interceptor** — `AiHubRuntimeProvider.tsx`: add an `else if (toolCallName ===
   'selectPropertyOption' || toolCallName === 'selectTriggerPropertyOption')` branch
   mirroring the `selectConnection` branch (parse, validate `kind ===
   'select-property-option'` + `Array.isArray(options)`, else flip to retryable error). Both
   tools produce the same `data-select-property-option` part carrying
   `{kind, componentName, propertyName, options, truncated}`.
2. **Renderer** — new `AiHubSelectPropertyOptionMessage.tsx` (in `.../ai-hub/messages/` or a
   `.../connect/`-style folder). Renders a **searchable `ComboBox`** (`@/components/ComboBox`)
   of **all** `data.options` (`items = options.map(o => ({label: o.label, value: o.value}))`).
   On pick, append a system message
   `User picked: <label> (value: <value>)` via `threadRuntime.append` — so the agent writes
   the real **value** (channel ID) into the workflow, not the label. Dim once superseded by a
   later message (same `threadRuntime.subscribe` pattern as `AiHubSelectConnectionMessage`).
   Empty options → a "no options" note. If `truncated`, show a hint to refine via search/text.
3. **Registry** — `AiHubMessageContent.tsx`: add `'select-property-option':
   SelectPropertyOptionData` and the `DataMessagePartProps<SelectPropertyOptionDataI>`
   wrapper, mirroring `SelectConnectionData`.

The label-vs-value distinction is the key client behavior: the picker shows labels, submits
values. (`askUserQuestion` only ever round-trips labels, which is why the agent never gets
the channel ID through that path.)

## Prompt

`prompt_ai_hub_build.txt` / `prompt_ai_hub_ask.txt`: to let the user choose a value for a
property whose descriptor shows `lookupRequired: true`, call `selectPropertyOption` (action
property) or `selectTriggerPropertyOption` (trigger property) — they render a picker of all
options and return the real value — instead of fetching with `lookupAction/TriggerPropertyOptions`
and hand-listing options in `askUserQuestion`. The existing `*_not_found` self-correction
guidance still applies (same error envelopes).

## Testing

- **Server:** `SelectPropertyOptionToolCallbackTest` and
  `SelectTriggerPropertyOptionToolCallbackTest` — success returns the marker with ALL (capped)
  options + `kind`; the `action_not_found`/`trigger_not_found` / `property_not_found` /
  `connection_required` / `no_options` failures return the same envelopes as the lookup tools.
  `PropertyOptionsResolverTest` — cover the new `resolveActionPropertyOptions` /
  `resolveTriggerPropertyOptions` result type (Success + each Failure). The existing
  `LookupAction/TriggerPropertyOptionsToolCallbackTest` stay green after the refactor
  (behavior unchanged).
- **Client:** extend the message tests — a `select-property-option` data part renders a
  combobox of all options; selecting one appends `User picked: <label> (value: <value>)`;
  empty options shows the note. An `AiHubRuntimeProvider` interceptor test (mirroring the
  `selectConnection` test, if one exists) for the parse/validate/error path.
- **Wiring:** assert `selectPropertyOption` is in both ASK and BUILD catalogs.

## EE conventions

New server files under `server/ee/` keep the Enterprise license header + `@version ee`.
Client follows CLAUDE.md conventions (interface names end `I`/`Props`, sorted keys/imports,
`twMerge`, Lucide `*Icon`).

## Future: Copilot reuse (build extraction-friendly, don't wire yet)

Copilot (the in-editor assistant) currently has **no** interactive-tool-UI layer —
`CopilotRuntimeProvider` only streams text, `CopilotPanel` renders the generic `<Thread>`,
and there is no tool-result→`data-*`→component pipeline like AI Hub's. So Copilot is **out
of scope** this iteration. To keep a future Copilot port cheap, build the renderer for
extraction:

- The `select-property-option` marker is already **surface-agnostic** (no AI-Hub-specific
  fields; omits the action/trigger name). Keep it that way.
- Write `AiHubSelectPropertyOptionMessage` so its only host dependencies are the
  assistant-ui `threadRuntime` (for the pick `append`) and the `data` prop — no coupling to
  AI-Hub-only stores. That way it can later move to a shared module both
  `AiHubMessageContent` and a future Copilot message renderer consume.
- The server tools live in `ai-hub-service`, which `ai-copilot-app` already depends on, so a
  later Copilot port is mostly: register the tool in the Copilot agent catalog + add the
  runtime-provider interception hook. No server relocation needed.

## Open questions

None. Decisions locked in spec review: build **both** `selectPropertyOption` (action) and
`selectTriggerPropertyOption` (trigger); the pick submits the **real value** (e.g. channel
ID) as `User picked: <label> (value: <value>)`, the label shown only for readability;
Copilot deferred (build extraction-friendly).
