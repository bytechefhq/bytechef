# Property Copilot: constant-value defaulting & immediate data-pill rendering

Date: 2026-06-04
Status: Approved (brief spec)

## Problem

Three issues with the Property Copilot feature:

1. **Over-eager data pills.** In `TEXT` mode the LLM is always told to prefer data-pill
   `${nodeName.path}` references and only fall back to constants. The copilot should return only
   constant values, except when the property is a string in `TEXT` mode with dynamic input active.
2. **Pills don't render on apply.** When the copilot applies a value containing `${...}` (TEXT or
   FORMULA mode), the text is inserted verbatim and only renders as visual data-pill chips after a
   refresh.
3. **No guard for option properties.** When a property has predefined options and is in text mode,
   the copilot button should be disabled.

## Decisions

- "Dynamic mode" maps to the client `mentionInput` state (the "Dynamic" switch / mention editor).
- The copilot button stays only in the mention-editor branch; Part 3 is a forward-correct guard.

## Design

### Part 1 — Constant-only unless string + dynamic (TEXT mode)

Plumb a `dynamic` boolean end-to-end:

- GraphQL `GeneratePropertyValueInput` gains `dynamic: Boolean!` (schema `.graphqls` + regenerated
  TS types).
- `PropertyCopilotRequest` record gains `boolean dynamic`; the GraphQL controller maps it through.
- `PropertyCopilotPromptBuilder.build()` — TEXT branch only: emit the "prefer data pill
  `${nodeName.path}` references" instruction **only when `request.dynamic() && "STRING".equals(propertyType)`**.
  Otherwise emit: "Return ONLY a single constant literal value. Do not reference previous step
  outputs or use `${...}`." FORMULA and JSON_SCHEMA branches unchanged.
- Client: `PropertyCopilotButton` gains a `dynamic` prop; `Property.tsx` passes
  `dynamic={mentionInput}`.

Net effect today: `mentionInput` is always `true` at the button, so live behavior is
*string + TEXT → pills allowed; non-string TEXT → constants only*. The flag is plumbed so
"dynamic off → constants only" is honored if the button ever renders in a constant context.

### Part 2 — Immediate data-pill rendering on apply

Extract the `${...}` → `<span data-type="mention" data-id="…">` conversion currently inline in
`PropertyMentionsInputEditor.getContent` into a shared exported helper
`buildPropertyMentionsContent(value, controlType)` (in the `property-mentions-input/` folder).

- `getContent` calls the helper (no behavior change).
- `Property.tsx#handleCopilotApply` runs the applied value through the helper before
  `editor.commands.setContent(...)`, for **both** the formula branch (`value.substring(1)`) and the
  text branch, with `{parseOptions: {preserveWhitespace: 'full'}}` to match the editor's own
  content-set effect.

### Part 3 — Disable guard for option properties

- `PropertyCopilotButton` gains a `disabled` prop. When `true`, the `Button` renders disabled and
  the click handler short-circuits (popover does not open).
- `Property.tsx` passes `disabled={!!options?.length && !isFormulaMode && !mentionInput}`.

## Testing

- Server: update `PropertyCopilotPromptBuilderTest` (TEXT + string + dynamic → pill instruction;
  TEXT + non-string → constant-only instruction; TEXT + dynamic=false → constant-only) and the
  controller/generator tests for the new `dynamic` field.
- Client: extend `PropertyCopilotButton.test.tsx` for `disabled` behavior; add a unit test for the
  extracted `buildPropertyMentionsContent` helper covering `${...}` → mention-span conversion.

## Out of scope

- Rendering the copilot button in constant-input (non-mention) paths.
- Changes to FORMULA / JSON_SCHEMA prompt behavior.