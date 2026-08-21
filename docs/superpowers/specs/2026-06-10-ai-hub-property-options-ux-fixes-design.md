# AI Hub property-options lookup — UX & robustness fixes — design

- **Date:** 2026-06-10
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan
- **Author:** Ivica Cardic
- **Follows:** `docs/superpowers/specs/2026-06-10-ai-hub-property-options-lookup-design.md` (the feature that added the lookup tools)

## Context

The property-options lookup tools shipped and the backend works: a live call
`lookupActionPropertyOptions({componentName:"slack", actionName:"sendChannelMessage",
propertyName:"channel", connectionId:1107})` returned **11 real Slack channels**
(`random, general, testing, sales-team, tech-team, …`, `truncated:false`). But the
end-to-end experience still failed for three distinct reasons, none of them in the
tool's recognition logic:

1. **The agent passed only a subset of the fetched options to `askUserQuestion`.** The
   lookup returned 11 channels; the question rendered 4 (`general / tech-team /
   sales-team / random`) + `Other…`. The user couldn't tell the shown options were real
   (they looked like placeholders) and most channels were missing.
2. **The options render as a vertical button list, not a select.** Even with all 11,
   `AiHubAskUserQuestionMessage.tsx`'s `SingleSelectStep` stacks one `<Button>` per
   option — unusable for large channel/page/record lists. The user wants a **searchable
   combobox** for large sets.
3. **A wrong-name retry dead-ended on a misleading error.** When challenged ("are those
   real names?"), the agent re-queried with hallucinated names — `actionName:"sendMessage"`
   (doesn't exist) and `propertyName:"channelId"` (the real name is `channel`; "Channel ID"
   is only the label). Both returned the generic `no_options_for_property` envelope, which
   is **indistinguishable** from "this property genuinely has no options." The agent then
   falsely told the user "the Slack API requires the channel to be entered directly rather
   than picked from a dropdown" — contradicting its own earlier success.

The descriptor already exposes the canonical names: `ToolUtils.generateParametersJson`
emits each property under `property.getName()` (`"channel"`), not its label. So the agent
*had* the right names and still guessed wrong — meaning the tool must be self-correcting
rather than relying on perfect grounding.

## Goals

1. Surface **all** real options the lookup returns, clearly marked as fetched from the
   connection.
2. Render large single-select option sets as a **searchable combobox**, not a button list.
3. Make the lookup tool **self-correcting**: a wrong action/property name returns an
   actionable error listing the valid names, instead of a misleading "no options."

## Non-goals

- Changing the lookup engine, the gate methods, or the 25-cap/`truncated` logic.
- Adding option providers to components.
- Reworking the multi-question wizard flow in `askUserQuestion`.

## Fix 1 — Prompt (server)

`prompt_ai_hub_build.txt` and `prompt_ai_hub_ask.txt`, in the property-options paragraph
added by the prior spec. Add/clarify:

- When a lookup returns options, present **every** returned option in `askUserQuestion`
  (do not cherry-pick a subset), and state they were fetched from the connection.
- **Reuse** a successful lookup result. Do not re-query the same property with different
  names, and never contradict a result you already obtained.
- Pass the **canonical property `name` and `actionName`/`triggerName`** exactly as they
  appear in the action/trigger descriptor — never the human label (e.g. property name is
  `channel`, not the "Channel ID" label).
- Only tell the user to type a value directly when the tool returns
  `no_options_for_property` for the **correct** name; an `action_not_found` /
  `property_not_found` error means you used the wrong name — retry with one of the valid
  names the tool returned.

## Fix 2 — Tool self-correction (server)

In both `LookupActionPropertyOptionsToolCallback` and
`LookupTriggerPropertyOptionsToolCallback`, before the existing
`propertyHasOptionsDataSource` gate, validate existence and return actionable errors:

1. **Action/trigger existence.** Resolve valid names via
   `actionDefinitionService.getActionDefinitions(componentName, componentVersion)` (resp.
   `triggerDefinitionService.getTriggerDefinitions(...)`), collecting each definition's
   `getName()`. If the requested `actionName`/`triggerName` is not among them, return an
   `action_not_found` (resp. `trigger_not_found`) envelope carrying the valid names.
2. **Property existence.** Resolve the definition via `getActionDefinition(...)` /
   `getTriggerDefinition(...)`, collect top-level property names from `getProperties()`
   (`Property.getName()`). If the **first path segment** of `propertyName` is not among
   them, return a `property_not_found` envelope carrying the valid property names. (Using
   the first segment keeps dotted paths like `parent.child` working — only the top-level
   container must exist; deeper resolution stays with the existing engine.)
3. Otherwise proceed exactly as today (`propertyHasOptionsDataSource` → dependency gate →
   connection gate → `executeOptions` → success).

New `PropertyOptionsResolver` envelope helpers:

- `entityNotFoundEnvelope(String errorCode, String entityKey, String requested,
  List<String> valid)` — generic builder producing
  `{error:<errorCode>, <entityKey>:<requested>, valid:[...], hint:"..."}`. Used for both
  `action_not_found`/`trigger_not_found` (entityKey `actionName`/`triggerName`) and
  `property_not_found` (entityKey `propertyName`). The hint tells the agent to retry with
  one of the listed valid names.

The `no_options_for_property` envelope stays for the genuine case (property exists, no
options data source).

## Fix 3 — Client searchable combobox (client)

`client/src/pages/automation/ai-hub/messages/AiHubAskUserQuestionMessage.tsx`,
`SingleSelectStep`:

- When the real option count (excluding any injected `Other…`) exceeds a threshold
  (`COMBOBOX_OPTION_THRESHOLD = 8`), render the existing
  `@/components/ComboBox/ComboBox` (searchable, cmdk-based) instead of the stacked button
  list. `items` map from `question.options` as `{label, value: label}` (the wizard submits
  by label today; preserve that). Selecting an item calls the same `handleClick(label)`
  path, so the `Other`-label and free-form behavior is unchanged.
- Keep the `Other…` affordance: when no LLM-supplied Other option exists, append an
  `Other…` item to the combobox list (selecting it opens the existing free-form input),
  matching today's button behavior.
- At or below the threshold, keep the current button list (small sets read better as
  buttons).
- `MultiSelectStep`: above the same threshold, render `@/components/MultiSelect/MultiSelect`
  (already a searchable multi-select) instead of the stacked checkbox list; below it, keep
  checkboxes. The submitted value format (`labels.join(', ')`) is unchanged.

Client conventions: interface names end in `I`/`Props`; alphabetical key/import sort;
`twMerge` not `cn`; Lucide `*Icon` imports; run `npm run check` before commit.

## Testing

- **Server tool:** unit tests for both callbacks — `action_not_found` (with valid action
  names), `property_not_found` (wrong name like `channelId` returns valid names incl.
  `channel`), and that a valid action+property still reaches success/`no_options` as before.
  Extend `PropertyOptionsResolverTest` for the new `entityNotFoundEnvelope` helper.
- **Client:** extend `AiHubAskUserQuestionMessage.test.tsx` — ≤8 options renders buttons;
  >8 renders the combobox; selecting a combobox item submits the label; `Other…` still
  opens free-form input.
- **Prompt:** `grep` assertions that the new guidance text is present (no behavioral test).

## EE conventions

New/changed server files under `server/ee/` keep the Enterprise license header and
`@version ee`. Trigger-side comments reference `TriggerDefinitionServiceImpl`.

## Open questions

None. Decisions locked: all three fixes, design-first; client uses a searchable combobox
above a threshold (small sets keep buttons); threshold = 8.
