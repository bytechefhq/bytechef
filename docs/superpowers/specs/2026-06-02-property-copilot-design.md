# Property Copilot — Design

- **Date:** 2026-06-02
- **Status:** Approved (design)
- **Edition:** EE only
- **Author:** Ivica Cardic

## Context

This is the second and third of three planned "Copilot in Property" features, designed
together as one cohesive feature:

1. Formula function autocomplete (shipped — `2026-06-01-formula-function-autocomplete-design.md`).
2. **Copilot in formula mode** — natural language → SpEL `=formula`.
3. **Copilot in text mode** — natural language → text content with `${datapill}` references.

(2) and (3) share one UI surface, one GraphQL mutation, and one insertion path. They differ
only in prompt grounding (formula mode also receives the evaluator function catalog) and output
validation (formula mode validates the result via the SpEL evaluator). They are therefore one
feature with a mode switch, not two sub-projects.

### Existing infrastructure this builds on (verified)

- **Property editing:** mention-capable property fields render through `PropertyMentionsInputEditor`
  (TipTap). Datapills are `mention` nodes; a serialized value uses `${path}` references; a formula
  is plain text beginning with `=`. Formula mode is tracked by `FormulaMode.extension.ts`.
- **Controls row above the property:** `Property.tsx` renders a row above the input containing the
  label/description and `PropertyInputTypeSwitch` (the "dynamic toggle"), gated by
  `showInputTypeSwitchButton` (~Property.tsx:284). The copilot trigger goes in this row, after the
  switch.
- **`FromAiToggleButton` is a different feature** (`=fromAi(...)` for tool-cluster parameters) and is
  left untouched; the copilot is distinct.
- **Existing copilot is EE-only, chat-style, side-panel-only** (`/api/platform/internal/ai/chat/{agentId}`
  SSE). We do NOT reuse it for this single-shot fill.
- **Focused single-shot precedent:** `generateSpecification` — an EE GraphQL mutation in
  `platform-api-connector-configuration-graphql` (`ApiConnectorGraphQlController`, `@MutationMapping`
  returning a typed payload). Our mutation mirrors this.
- **SpEL evaluator:** `Evaluator.evaluate(map, context, lenient=true)` validates a `=formula` without
  throwing (invalid → returned unchanged); `SpelEvaluator` whitelists functions. The function catalog
  comes from `List<EvaluatorFunctionDefinitionFactory>` (already injected in
  `EvaluatorFunctionDefinitionGraphQlController`).
- **Edition gating:** server uses `@ConditionalOnProperty bytechef.ai.copilot.enabled` +
  `@ConditionalOnEEVersion`; client shows copilot UI only when EE + `ai.copilot.enabled` (+ feature
  flag), mirroring `CopilotButton`.

## Goal

An EE-only, single-shot, mode-aware copilot on a workflow property field: the user clicks a `✦`
trigger above the field, types a natural-language request, and the field is filled with either text
containing datapill references (text mode) or a SpEL `=formula` (formula mode), grounded in the
previous step outputs available to that field.

## Non-goals (deferred)

- Multi-turn conversation / refine loop (single-shot only; the inserted value is editable/undoable).
- Streaming the result token-by-token into the field (single completion).
- A preview/accept gate (the value is inserted directly).
- Copilot in the Monaco `CODE_EDITOR` (separate editor; the existing side-panel copilot already
  serves it).
- Reusing or modifying the existing chat `CopilotPanel`.
- Any change to `FromAiToggleButton` / `=fromAi(...)`.

## Design

### Decisions (validated during brainstorming)

- **UX surface:** inline popover (Workato-style), trigger in the controls row above the field after
  `PropertyInputTypeSwitch`.
- **Backend:** focused EE GraphQL mutation (not the chat SSE infra, not a plain REST endpoint).
- **Apply result:** insert directly into the field editor, editable and undoable; popover closes.
- **Grounding context:** prompt + available previous-step outputs (paths + types + truncated sample
  values) + property metadata + (formula mode) the evaluator function catalog. Sample values are sent
  to the LLM provider.
- **Mode:** implicit from the field's current state — formula mode → `=formula`; otherwise → text.

### 1. Client

- **`PropertyCopilotButton`** — the `✦` trigger rendered in `Property.tsx`'s controls row (after
  `PropertyInputTypeSwitch`). Gated: EE + `ai.copilot.enabled` (+ feature flag), and only for
  mention-capable controls (the same control types that use `PropertyMentionsInput`).
- **`PropertyCopilotPopover`** — anchored popover with a prompt textarea, a Generate button, and
  loading/error states. Submitting calls the mutation hook; on success it inserts and closes.
- **`useGeneratePropertyValue`** — hook wrapping the generated GraphQL mutation.
- **Insertion** — reuse the existing `PropertyMentionsInputEditor` value path: set the field value to
  the returned string (text with `${path}` refs, or `=formula`). The editor's existing
  `${id}`→mention-node rendering turns pill references into real pills; a `=formula` puts the field
  into formula mode. The change is saved through the normal property-save path so it is undoable.
- **Mode detection** — derive `mode` (`TEXT`/`FORMULA`) from whether the field is currently in
  formula mode.

### 2. GraphQL mutation (EE)

```graphql
input GeneratePropertyValueInput {
    prompt: String!
    mode: PropertyCopilotMode!        # TEXT | FORMULA  (SCREAMING_SNAKE per GraphQL convention)
    workflowId: ID!
    workflowNodeName: String!
    propertyPath: String!
    propertyType: String              # steering hint (STRING, NUMBER, ...)
}

type GeneratePropertyValuePayload {
    value: String!                    # text with ${path} refs, or "=formula"
    valid: Boolean!                   # formula validation result (always true for TEXT mode)
    message: String                   # optional note (e.g. "formula could not be validated")
}

extend type Mutation {
    generatePropertyValue(input: GeneratePropertyValueInput!): GeneratePropertyValuePayload!
}
```

Implemented by `PropertyCopilotGraphQlController` (`@MutationMapping`), mirroring
`ApiConnectorGraphQlController`.

### 3. Server generation flow

`PropertyCopilotGenerator` service:

1. **Resolve grounding** from `(workflowId, workflowNodeName)`: the available previous-step outputs
   (datapills) as `(referencePath, type, truncatedSampleValue?)`, from the existing workflow
   output/sample infrastructure. **Open item resolved in the plan:** identify the exact service that
   yields previous-node outputs + sample values (read the datapill/output source before writing tasks).
   For FORMULA mode, also load the function catalog from `List<EvaluatorFunctionDefinitionFactory>`.
2. **Build the prompt** (`PropertyCopilotPromptBuilder`, pure/unit-tested): user request + property
   metadata + grounded pills (+ function catalog for formula). Instructions: reference ONLY the listed
   datapills via `${path}`; for formula, use ONLY catalog functions and return a single expression
   starting with `=`; for text, return the literal text with inline `${path}` references.
3. **Call the LLM** via the existing copilot chat-client infrastructure, single completion
   (non-streaming).
4. **Formula validation/repair** (FORMULA mode only): validate the returned `=formula` via
   `Evaluator` (lenient). If invalid, one repair attempt with the validation error appended to the
   prompt. If still invalid, return `valid=false` + `message`; the client still inserts the value so
   the user can fix it.
5. Return `{value, valid, message}`.

### 4. Edition gating & metrics

- Server controller/service: `@ConditionalOnProperty bytechef.ai.copilot.enabled` + `@ConditionalOnEEVersion`.
- Client trigger: hidden unless EE + `ai.copilot.enabled` (+ feature flag), mirroring `CopilotButton`.
- Metric: `bytechef_property_copilot_generate{mode,outcome}` counter — outcomes `success`,
  `invalid_formula`, `error`. Wired via `ObjectProvider<MeterRegistry>` (lightweight-app safe).

## Testing

- **Server:**
  - `PropertyCopilotPromptBuilder` — pills + catalog appear in the prompt; the only-existing-pills /
    only-catalog-functions instructions are present; TEXT vs FORMULA differ correctly.
  - Formula validation/repair — valid formula passes; invalid triggers exactly one repair; still-invalid
    returns `valid=false` + message.
  - `PropertyCopilotGraphQlController` — wired with a mocked chat client + mocked grounding; returns the
    payload; respects mode.
- **Client:**
  - Popover opens/closes; Generate submits the prompt; disabled while loading; error renders.
  - `useGeneratePropertyValue` maps the mutation result to an editor insert.
  - Mode detection picks `FORMULA` in formula mode, `TEXT` otherwise.
  - Gating: trigger hidden in CE / when `ai.copilot.enabled` is false.
  - An inserted `${path}` renders as a mention pill; a `=formula` enters formula mode.

## Files (high level)

**Server — new EE module(s)** (placement mirrors `platform-api-connector-configuration-graphql`):
- `GeneratePropertyValueInput`, `GeneratePropertyValuePayload`, `PropertyCopilotMode` models.
- `PropertyCopilotGraphQlController` (`@MutationMapping`).
- `PropertyCopilotGenerator` service (grounding + LLM + validation) and `PropertyCopilotPromptBuilder`.
- `property-copilot.graphqls`.
- Metric wiring.

**Client — new:**
- `.graphql` operation for `generatePropertyValue` + codegen.
- `useGeneratePropertyValue` hook.
- `PropertyCopilotButton`, `PropertyCopilotPopover`.

**Client — modified:**
- `Property.tsx` — render the trigger in the controls row after `PropertyInputTypeSwitch`; wire
  insertion into the field editor.
- regenerated `graphql.ts`.

## Open item for the plan phase

Identify the exact server-side source of previous-step outputs + sample values for a given
`(workflowId, workflowNodeName)` (the datapill/output schema/sample service). This is the only
unresolved implementation detail; it is pinned down before writing tasks.
