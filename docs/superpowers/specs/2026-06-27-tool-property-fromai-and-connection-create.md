# Tool-property `fromAi` & function-autocomplete fixes + connection creation in MCP tool dialog

Date: 2026-06-27
Tickets: automation/shared → **2445**, embedded → **2446**

## Background

Tool properties (AI-agent tool cluster elements and MCP-server tool configuration) let a value be
either typed/expression-based or "filled in by the model" via a `=fromAi('name', 'TYPE', {…})`
expression. The UI for these properties is the shared property stack under
`client/src/pages/platform/workflow-editor/components/properties/`:

- **Managed mode** (workflow node-details panel, no `control`): renders the TipTap-based
  `PropertyMentionsInput` → `PropertyMentionsInputEditor`. In formula mode this gives a function
  autocomplete dropdown (sourced from the `evaluatorFunctionDefinitions` GraphQL query) and a fromAi
  toggle button.
- **Form-controller mode** (`control` present — MCP tool dialog, array items, object entries):
  renders the plain `PropertyInput` (`components/property-input/PropertyInput.tsx`), an `<input>` with
  **no** autocomplete and no TipTap editor.

The automation and embedded MCP tool-config popovers
(`.../mcp-component-list/McpComponentToolPropertiesPopover.tsx` in both `pages/automation` and
`ee/pages/embedded`) both delegate to the **same** shared `Properties`/`Property` components, passing
`toolsMode` and react-hook-form `control`. Therefore the shared-component fixes below cover both
automation and embedded MCP simultaneously; there is no embedded-specific rendering path for the
property inputs.

The "Select Tools from <component>" dialog
(`.../mcp-component-dialog/McpComponentDialogToolSelectionStep.tsx`) has a "Select Connection"
dropdown but no way to create a new connection inline. Automation and embedded each have their own
copy of this step with their own connection queries/mutations.

## Problems

1. Typing `fromAi` in a tool-property function field shows no dropdown suggestion. `fromAi` is not in
   the server evaluator catalog (89 functions), and the dropdown sources items purely from that
   catalog (`functionSuggestionOptions.ts` → `editor.storage.FunctionSuggestion.functionDefinitions`).
2. The placeholder `"Use '$' for data pills and '=' for an expression"`
   (`PropertyMentionsInputEditor.tsx:223`) is confusing for tool properties, where data pills do not
   apply — the field is effectively always an expression (AI button or a hand-written `=fromAi(...)`).
3. The fromAi toggle's X button (tooltip "Customize AI generation") clears the field.
   `handleFromAiClick(false)` (`useProperty.ts`) restores the *old* `propertyParameterValue` rather
   than keeping the `=fromAi(...)` expression, so the function the user was customizing disappears.
4. Form-controller mode has no function autocomplete at all (plain `PropertyInput`), and the fromAi
   button is missing for string properties in the MCP dialog. The editor's existing tools-context
   gate reads `currentNode?.clusterElementType === 'tools'` from the workflow store, which is empty
   on the MCP Servers page.
5. No inline "create new connection" in the "Select Tools from …" dialog.

## Decisions

- **fromAi dropdown behaviour**: behaves like any other function — selecting it inserts `fromAi()`
  with the caret between the parens (standard `buildFunctionInsertion`). It is shown **only** for tool
  properties.
- **Placeholder** (tool-property function mode): `Click the AI button or write =fromAi(...)`.
- **Customize (X)**: reveal the editable `=fromAi('name','TYPE',{…})` expression (drop the fromAi
  flag, keep the expression text, make the field editable, formula mode on).
- **Form-controller autocomplete**: render the existing mention editor in controlled function-mode so
  autocomplete is consistent with managed formula mode. The editor never auto-saves in controlled
  mode (it defers to the react-hook-form `onChange`/`onValueChange`).
- **Tools context** must be driven by an explicit `toolProperty` prop threaded from
  `Property`/`useProperty` (`isToolsClusterElement`), not by the workflow store's `currentNode`, so it
  works on the MCP page.
- **Connection "+"**: reuse the shared `ConnectionDialog`, wired per surface (automation vs embedded).
- **Commit split**: shared + automation under 2445; embedded-specific connection wiring under 2446.
  Each fix is its own commit.

## Design

### Shared property components (ticket 2445)

A new `toolProperty: boolean` prop is threaded:
`Property` (`isToolsClusterElement`) → `PropertyMentionsInput` → `PropertyMentionsInputEditor`.
It drives three things in the editor:

1. **fromAi suggestion injection.** In the effect that assigns
   `editor.storage.FunctionSuggestion.functionDefinitions`, when `toolProperty` is true, append a
   synthetic `fromAi` definition to the catalog:

   ```ts
   const FROM_AI_FUNCTION_DEFINITION: EvaluatorFunctionDefinition = {
       category: EvaluatorFunctionCategory.Utility,
       description: 'Let the AI model supply this value at runtime.',
       example: "=fromAi('name', 'STRING', {'required': true})",
       name: 'fromAi',
       parameters: [
           {description: 'Identifier the model sees for this value.', name: 'name', required: true, type: EvaluatorFunctionType.String},
           {description: 'Value type, e.g. STRING.', name: 'type', required: true, type: EvaluatorFunctionType.String},
           {description: 'Optional metadata (description, defaultValue, options, required).', name: 'options', required: false, type: EvaluatorFunctionType.Map},
       ],
       returnType: EvaluatorFunctionType.String,
       title: 'fromAi',
   };
   ```

   Selecting it inserts `fromAi()` like any other function. No special filtering elsewhere is needed —
   `filterFunctionDefinitions` already matches on name/title.

2. **fromAi toggle-button visibility.** Change the gate at `PropertyMentionsInputEditor.tsx:648` from
   `currentNode?.clusterElementType === 'tools'` to the `toolProperty` prop, so it shows on the MCP
   page too.

3. **Placeholder.** When `toolProperty` is set and no explicit placeholder is provided, default to
   `Click the AI button or write =fromAi(...)` instead of the data-pill string.

#### Customize (X) — reveal editable expression

- Managed mode `handleFromAiClick(false)` (`useProperty.ts`): set the editor content to
  `fromAiExpression`, keep it editable, turn formula mode on, set the saved value to
  `fromAiExpression`, and `saveProperty({fromAi: false, value: fromAiExpression})`. `controlledFromAi`
  → false.
- Controlled mode `handleFromAiToggle(false, fieldOnChange)` (`useProperty.ts`): set the form value to
  `fromAiExpression` (not `''`) so the field reveals the editable expression in function mode.

#### Form-controller autocomplete

In `Property.tsx`, the controlled function-mode branches currently render `PropertyInput`:

- `control && controlledDynamicMode` branch (≈ lines 373–450).
- `control && !controlledDynamicMode && (isValidControlType || isNumericalInput)` STRING branch, when
  `isExpressionMode || isFieldFromAi` (≈ lines 510–664).

Render `PropertyMentionsInput` in those function-mode cases instead, wired to react-hook-form:

- `value` = the field's `=`-prefixed value; `onValueChange` writes back through the Controller's
  `onChange`, re-applying the `=` prefix in formula mode.
- Pass `toolProperty`, `expressionEnabled`, `isFromAi`, and a `handleFromAiClick` adapter that routes
  to `handleFromAiToggle(_, fieldOnChange)`.
- Add a `disableAutoSave` (controlled) prop to `PropertyMentionsInput`/`PropertyMentionsInputEditor`
  that short-circuits `saveMentionInputValue` so the editor never persists directly in controlled
  mode (the form owns persistence). This makes controlled reuse explicit rather than relying on
  `workflow.id` being undefined (workflow-editor array-items have a real `workflow.id`).

The plain `PropertyInput` remains for non-expression controlled values (plain text/number/etc.).

### Connection creation in the tool-selection dialog

#### Automation (ticket 2445)
`pages/automation/.../McpComponentDialogToolSelectionStep.tsx`: add a `PlusIcon` button next to the
"Select Connection" trigger. Clicking it opens the shared `ConnectionDialog` scoped to the dialog's
`selectedComponent` (need its full `ComponentDefinition` + the `componentDefinitions` list), wired
with automation's `useCreateConnectionMutation`, `useGetConnectionTagsQuery`, and connection query
keys. On `onConnectionCreate(newId)`: invalidate the connections query, then call
`onConnectionChange(newConnection)` to auto-select it.

#### Embedded (ticket 2446)
`ee/pages/embedded/.../McpComponentDialogToolSelectionStep.tsx`: same UI, wired with the embedded
connection mutations/queries. Also verify the `evaluatorFunctionDefinitions` GraphQL query is exposed
on the embedded surface; if it is not, the function autocomplete will be silently empty in embedded —
flag/handle under 2446.

## Out of scope

- Adding `fromAi` to the server evaluator catalog (it is a UI affordance for tool properties only,
  not a general evaluator function).
- Data-pill (`$`) support inside MCP/tool configs (no upstream nodes exist there).
- Any change to non-tool property behaviour in the workflow editor.

## Testing

- Unit tests for the synthetic `fromAi` injection (present only when `toolProperty`), the placeholder
  selection, and the customize-reveal value transitions (extend the existing tests under
  `properties/hooks/tests/` and `property-mentions-input/tests/`).
- Manual verification in both automation and embedded MCP: function autocomplete + `fromAi` suggestion
  in form mode, fromAi button on string properties, customize reveals editable expression, and the
  connection "+" creates and selects a connection.

## Commit plan

Ticket 2445 (each its own commit):
1. `fromAi` in function-autocomplete dropdown for tool properties.
2. Tool-property function-mode placeholder.
3. Customize (X) reveals editable `=fromAi(...)`.
4. Function autocomplete + fromAi button in controlled/form mode.
5. Connection "+" in the automation tool-selection dialog.

Ticket 2446:
6. Connection "+" in the embedded tool-selection dialog (+ verify embedded evaluator-function-defs
   access).
