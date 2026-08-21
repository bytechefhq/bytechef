# Formula Function Autocomplete — Design

- **Date:** 2026-06-01
- **Status:** Approved (design)
- **Edition:** CE (no AI / EE dependency)
- **Author:** Ivica Cardic

## Context

This is the first of three planned features that together bring Workato-style
"Copilot in Property" capabilities to ByteChef:

1. **Formula autocomplete** (this spec) — discover and insert SpEL evaluator
   functions while editing a property in formula mode.
2. Copilot in formula mode — natural language → SpEL formula (separate spec).
3. Copilot in text mode — natural language → text content with datapills
   (separate spec).

Autocomplete is sequenced first because it is the foundational, edition-agnostic
piece: it builds the function-catalog plumbing and insertion mechanics that the
formula copilot later reuses to produce *valid* formulas, and it ships value on
its own without any AI work.

### Current state (what already exists)

- Property fields that support mentions render through
  `PropertyMentionsInputEditor` (a TipTap/ProseMirror editor) — file:
  `client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor.tsx`.
- **Datapills** are inserted as ProseMirror `mention` *nodes*, suggested by a
  `Mention.extend({ suggestion: getSuggestionOptions() })` source triggered by
  `$` (`propertyMentionsInputEditorSuggestionOptions.ts`). This works in both
  text and formula mode and is **out of scope to change**.
- **Formula mode** is toggled by typing `=` at the start of a field and tracked
  by `FormulaMode.extension.ts` (editor storage `FormulaMode.isFormulaMode`).
  The editor also keeps an `isFormulaModeRef` to read the flag without
  recreating the editor.
- A serialized formula is plain text: `=concat(${step.field}, "x")`. SpEL
  function calls are **plain text**, not nodes; only datapills are nodes.
- The backend already exposes the full function catalog over GraphQL:
  `evaluatorFunctionDefinitions(name: String): [EvaluatorFunctionDefinition!]!`
  (controller: `EvaluatorFunctionDefinitionGraphQlController`). The record is:

  ```
  EvaluatorFunctionDefinition(
      String name, String title, String description,
      EvaluatorFunctionCategory category,
      List<EvaluatorFunctionParameter> parameters,   // (name, description, type, required)
      EvaluatorFunctionType returnType,
      String example)
  ```

  ~80 functions across STRING, COLLECTION, DATE_TIME, MAP, TYPE, UTILITY.
- The evaluator `.graphqls` schema is already in `client/codegen.ts`'s schema
  globs (generated TS types exist), but there is **no client operation document
  yet**, so nothing fetches the catalog.

## Goal

Add IDE-style autocomplete of SpEL evaluator functions inside property fields
that are in **formula mode**, in the existing `PropertyMentionsInputEditor`.

## Non-goals (explicitly deferred)

- Live signature / parameter-help tooltip while typing arguments (a natural
  fast-follow; not in this scope).
- Natural-language → formula copilot (features 2 & 3 above).
- Category grouping in the dropdown (type-to-filter makes it unnecessary for v1).
- Autocomplete in the Monaco `CODE_EDITOR` control type — a different editor and
  a separate effort.
- Any change to the existing `$` datapill suggestion behavior.

## Design

### Decisions (validated during brainstorming)

- **Trigger model:** auto-suggest as you type (IDE-style), **only in formula
  mode**. No explicit hotkey. `$` continues to trigger the datapill dropdown in
  both modes.
- **Insertion behavior:** selecting a function inserts `name()` and places the
  caret between the parens. Signature/description/example are shown in the
  dropdown *before* selection (no live param tooltip in v1).
- **Dropdown layout:** Layout 1, flat — one compact row per function; the
  highlighted row expands to show description + example. No category grouping.

### 1. Data source & fetching

- No backend change. Add a client GraphQL operation:
  `client/src/graphql/platform/configuration/evaluatorFunctionDefinitions.graphql`
  querying the full list (call with no `name` argument) selecting
  `name, title, description, category, returnType, example, parameters { name, description, type, required }`.
- Run codegen (`cd client && npx graphql-codegen`) → generated
  `useEvaluatorFunctionDefinitionsQuery` hook in
  `client/src/shared/middleware/graphql.ts`.
- The catalog is static per deployment: fetch once, `staleTime: Infinity`.
  Filter client-side by the typed prefix.

### 2. Editor wiring — a second, formula-gated suggestion source

New extension `FunctionSuggestion.extension.ts` built on the bare
`@tiptap/suggestion` plugin (a **text** producer, not a Mention node):

- **Trigger:** on word characters, no explicit trigger char. The `allow`
  callback returns true **only when** `FormulaMode` storage
  `isFormulaMode === true` (read via the editor, mirroring the existing
  `isFormulaModeRef` approach) and the active token is word-led. This keeps it
  dormant in text mode and prevents any competition with the `$` source.
- **items:** case-insensitive `name` prefix match, with a `title`-contains
  fallback; result count capped (~50).
- **command:** insert the text `"<name>()"` at the suggestion range, then move
  the caret between the parens via a `TextSelection`. No node is created.
- The function list is pushed into editor storage (same pattern as
  `MentionStorage.extension.ts`) so the `items` callback can read it without
  recreating the editor.

**Implementation risk (flagged):** TipTap's suggestion utility triggering with
no explicit `char` can be finicky. If the `char: ''`/word-trigger approach
misbehaves, the fallback is a small custom ProseMirror plugin/input rule that
opens the same React dropdown. This is a plan-phase detail, not a design change.

### 3. Dropdown UI — `FunctionSuggestionList.tsx` (+ `.css`)

- Mirrors `PropertyMentionsInputEditorSuggestionList`: the same Tippy popup and
  keyboard navigation (↑/↓/Enter/Esc via `forwardRef` + `useImperativeHandle`).
- Layout 1, flat: one row per function = `name` + monospace
  `(params): returnType`. The highlighted/selected row expands to also show
  `description` and `example`. No category grouping.
- Empty state: "No functions found."

### 4. Signature rendering

- `formatFunctionSignature(def)` helper builds a `(name: type, …): returnType`
  string from `parameters` + `returnType`, handling varargs and the zero-arg
  case. Pure function, unit-tested.

## Testing

- **Unit:**
  - `formatFunctionSignature` — varargs, zero-arg, multi-param.
  - Filtering — prefix match, case-insensitivity, `title` fallback, result cap.
  - Insertion command — produces `name()` and the correct caret position.
  - Gating — `allow` returns false outside formula mode.
- **Editor tests** (follow `PropertyMentionsInputEditor.test.tsx` patterns):
  - Type `=conc` → `concat` appears in the dropdown.
  - Enter inserts `concat()`.
  - Typing letters in text mode shows no function dropdown.
  - `$` still shows datapills in both text and formula mode.

## Files

**New (client):**

- `src/graphql/platform/configuration/evaluatorFunctionDefinitions.graphql`
- `.../property-mentions-input/FunctionSuggestion.extension.ts`
- `.../property-mentions-input/functionSuggestionOptions.ts`
- `.../property-mentions-input/FunctionSuggestionList.tsx` (+ `.css`)
- `.../property-mentions-input/formatFunctionSignature.ts` (+ test)

**Modified (client):**

- `.../property-mentions-input/PropertyMentionsInputEditor.tsx` — register the
  new extension and push the function list into editor storage.
- `src/shared/middleware/graphql.ts` — regenerated by codegen.

**Server:** none.
