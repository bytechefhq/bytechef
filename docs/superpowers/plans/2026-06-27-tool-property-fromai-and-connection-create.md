# Tool-property `fromAi` & function-autocomplete fixes + MCP connection-create — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `fromAi` a first-class function-autocomplete entry for tool properties, fix the placeholder and "Customize" behaviour, bring function autocomplete to form-controller mode, and let users create a connection inline from the MCP "Select Tools" dialog.

**Architecture:** All property-input behaviour lives in the shared stack under `client/src/pages/platform/workflow-editor/components/properties/`. Both automation and embedded MCP tool-config popovers delegate to the same `Properties`/`Property`, so the property fixes (Tasks 1–4) cover both surfaces at once. Connection creation is surface-specific (Tasks 5–6). We follow the repo's testing pattern: extract pure helpers into small modules and unit-test them with Vitest; component wiring is verified by `npm run check` + manual QA.

**Tech Stack:** React 19 + TypeScript, TipTap editor, react-hook-form, TanStack Query, Vitest, shadcn UI, Tailwind (twMerge).

## Global Constraints

- Commit messages: client changes use `<ticket> client - <description>`. Tasks 1–5 use ticket **2445**; Task 6 uses **2446**. Each task is its own commit. End every commit message with:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- Only stage files you modified for the task — do not stage pre-existing unrelated unstaged changes.
- Branch is `0_732`. Never `git commit --amend` (user commits in parallel); always fresh commits.
- Client code style: object keys in natural ascending order (ESLint `sort-keys`, not auto-fixed); named imports sorted alphabetically within `{}`; interface names end in `I` or `Props`; lucide icons imported with the `Icon` suffix (`PlusIcon`); use `twMerge`, never `cn()`; no short/cryptic variable names.
- Files under `server/ee/` need the EE license header + `@version ee` — N/A here (all changes are client; embedded client files do NOT need the Java header).
- Before each commit run, from `client/`: `npm run check` (lint + typecheck + tests). A single test file runs with `npx vitest run <path>`.
- `EvaluatorFunctionDefinition`, `EvaluatorFunctionType`, `EvaluatorFunctionCategory` are imported from `@/shared/middleware/graphql`.

---

## File structure

**Task 1 (2445):**
- Create `client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/fromAiFunctionDefinition.ts`
- Create `.../property-mentions-input/tests/fromAiFunctionDefinition.test.ts`
- Modify `.../property-mentions-input/PropertyMentionsInputEditor.tsx` (add `toolProperty` prop; inject fromAi into suggestion catalog)
- Modify `.../property-mentions-input/PropertyMentionsInput.tsx` (forward `toolProperty`)
- Modify `.../properties/Property.tsx` (pass `toolProperty={isToolsClusterElement}` to the managed editor)

**Task 2 (2445):**
- Create `.../property-mentions-input/mentionsInputPlaceholder.ts`
- Create `.../property-mentions-input/tests/mentionsInputPlaceholder.test.ts`
- Modify `PropertyMentionsInputEditor.tsx` (use the helper for the Placeholder)

**Task 3 (2445):**
- Create `.../properties/hooks/fromAiToggle.ts`
- Modify `.../properties/hooks/tests/handleFromAiToggle.test.ts` (import the helper; update toggle-OFF expectations)
- Modify `.../properties/hooks/useProperty.ts` (`handleFromAiToggle` + `handleFromAiClick` reveal the expression)

**Task 4 (2445):**
- Create `.../property-mentions-input/controlledExpressionValue.ts`
- Create `.../property-mentions-input/tests/controlledExpressionValue.test.ts`
- Modify `PropertyMentionsInputEditor.tsx` + `PropertyMentionsInput.tsx` (add `disableAutoSave`; change fromAi-button gate to `toolProperty`)
- Modify `Property.tsx` (render `PropertyMentionsInput` in the two controlled function-mode branches)

**Task 5 (2445):**
- Modify `client/src/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialogToolSelectionStep.tsx`
- Modify `.../mcp-component-dialog/hooks/useMcpComponentDialogToolSelectionStep.ts` (expose full `componentDefinition`)

**Task 6 (2446):** Embedded connection create — scoping decision (see task).

---

## Task 1: `fromAi` appears in the function-autocomplete dropdown for tool properties

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/fromAiFunctionDefinition.ts`
- Test: `client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/fromAiFunctionDefinition.test.ts`
- Modify: `.../property-mentions-input/PropertyMentionsInputEditor.tsx`, `.../property-mentions-input/PropertyMentionsInput.tsx`, `.../properties/Property.tsx`

**Interfaces:**
- Produces: `FROM_AI_FUNCTION_DEFINITION: EvaluatorFunctionDefinition`, `buildToolFunctionDefinitions(definitions: EvaluatorFunctionDefinition[], toolProperty: boolean): EvaluatorFunctionDefinition[]`
- Produces: `PropertyMentionsInputEditor` and `PropertyMentionsInput` gain a `toolProperty?: boolean` prop.

- [ ] **Step 1: Write the failing test**

Create `tests/fromAiFunctionDefinition.test.ts`:

```ts
import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {FROM_AI_FUNCTION_DEFINITION, buildToolFunctionDefinitions} from '../fromAiFunctionDefinition';

const baseDefinition = {
    category: 'STRING',
    description: 'Concatenates.',
    example: '=concat(a, b)',
    name: 'concat',
    parameters: [],
    returnType: 'STRING',
    title: 'concat',
} as unknown as EvaluatorFunctionDefinition;

describe('buildToolFunctionDefinitions', () => {
    it('returns the base catalog unchanged for non-tool properties', () => {
        const result = buildToolFunctionDefinitions([baseDefinition], false);

        expect(result).toEqual([baseDefinition]);
        expect(result.some((definition) => definition.name === 'fromAi')).toBe(false);
    });

    it('prepends the fromAi definition for tool properties', () => {
        const result = buildToolFunctionDefinitions([baseDefinition], true);

        expect(result[0]).toBe(FROM_AI_FUNCTION_DEFINITION);
        expect(result).toHaveLength(2);
    });

    it('does not duplicate fromAi when it is already present', () => {
        const result = buildToolFunctionDefinitions([FROM_AI_FUNCTION_DEFINITION, baseDefinition], true);

        expect(result.filter((definition) => definition.name === 'fromAi')).toHaveLength(1);
    });
});

describe('FROM_AI_FUNCTION_DEFINITION', () => {
    it('is named fromAi and returns a string', () => {
        expect(FROM_AI_FUNCTION_DEFINITION.name).toBe('fromAi');
        expect(FROM_AI_FUNCTION_DEFINITION.returnType).toBe('STRING');
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/fromAiFunctionDefinition.test.ts`
Expected: FAIL — cannot find module `../fromAiFunctionDefinition`.

- [ ] **Step 3: Create the helper module**

Create `fromAiFunctionDefinition.ts`:

```ts
import {EvaluatorFunctionCategory, EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';

// Synthetic catalog entry so `fromAi` appears in the function-autocomplete dropdown for tool
// properties. It is NOT a real evaluator function (the server catalog has no `fromAi`); it exists
// purely as a UI affordance and behaves like any other function when selected (inserts `fromAi()`).
export const FROM_AI_FUNCTION_DEFINITION: EvaluatorFunctionDefinition = {
    category: EvaluatorFunctionCategory.Utility,
    description: 'Let the AI model supply this value at runtime.',
    example: "=fromAi('name', 'STRING', {'required': true})",
    name: 'fromAi',
    parameters: [
        {
            description: 'Identifier the model sees for this value.',
            name: 'name',
            required: true,
            type: EvaluatorFunctionType.String,
        },
        {
            description: 'Value type, for example STRING.',
            name: 'type',
            required: true,
            type: EvaluatorFunctionType.String,
        },
        {
            description: 'Optional metadata: description, defaultValue, options, required.',
            name: 'options',
            required: false,
            type: EvaluatorFunctionType.Map,
        },
    ],
    returnType: EvaluatorFunctionType.String,
    title: 'fromAi',
};

// The function-suggestion catalog for the editor: the base evaluator definitions, plus the synthetic
// `fromAi` entry when the field is a tool property. fromAi is prepended so it surfaces first when the
// user types "fr...".
export function buildToolFunctionDefinitions(
    definitions: EvaluatorFunctionDefinition[],
    toolProperty: boolean
): EvaluatorFunctionDefinition[] {
    if (!toolProperty) {
        return definitions;
    }

    if (definitions.some((definition) => definition.name === 'fromAi')) {
        return definitions;
    }

    return [FROM_AI_FUNCTION_DEFINITION, ...definitions];
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/fromAiFunctionDefinition.test.ts`
Expected: PASS. If the import of `EvaluatorFunctionCategory` fails typecheck, confirm it is exported from `@/shared/middleware/graphql` (it is re-exported from `graphql-types.ts`).

- [ ] **Step 5: Thread `toolProperty` into the editor and inject the catalog**

In `PropertyMentionsInputEditor.tsx`:
- Add `toolProperty?: boolean;` to `PropertyMentionsInputEditorProps` (keep the interface keys alphabetical).
- Destructure `toolProperty` in the component signature (alphabetical position).
- Add the import: `import {buildToolFunctionDefinitions} from './fromAiFunctionDefinition';`
- Change the storage-sync effect (currently lines ~586–592) to inject fromAi:

```ts
useEffect(() => {
    if (!editor || editor.storage.FunctionSuggestion === undefined) {
        return;
    }

    editor.storage.FunctionSuggestion.functionDefinitions = buildToolFunctionDefinitions(
        evaluatorFunctionDefinitions,
        toolProperty ?? false
    );
}, [editor, evaluatorFunctionDefinitions, toolProperty]);
```

- [ ] **Step 6: Forward `toolProperty` through `PropertyMentionsInput`**

In `PropertyMentionsInput.tsx`:
- Add `toolProperty?: boolean;` to `PropertyMentionsInputProps` (alphabetical).
- Destructure `toolProperty` in the signature.
- Pass `toolProperty={toolProperty}` to the `<PropertyMentionsInputEditor ... />` render (alphabetical prop position).

- [ ] **Step 7: Pass `toolProperty` from `Property.tsx` (managed editor)**

In `Property.tsx`, the managed `<PropertyMentionsInput ... />` (around line 247) — add prop `toolProperty={isToolsClusterElement}` (alphabetical position; `isToolsClusterElement` is already destructured from `useProperty`).

- [ ] **Step 8: Verify build + manual check**

Run: `npm run check`
Expected: PASS (lint, typecheck, tests).
Manual QA (optional now, required before final): open an AI-agent tool property in function mode, type `fromAi` → `fromAi(...)` appears in the dropdown; selecting inserts `fromAi()`. In a NON-tool property, typing `fromAi` shows nothing.

- [ ] **Step 9: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/fromAiFunctionDefinition.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/fromAiFunctionDefinition.test.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor.tsx \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput.tsx \
        client/src/pages/platform/workflow-editor/components/properties/Property.tsx
git commit -m "2445 client - Show fromAi in function autocomplete for tool properties

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Tool-property function-mode placeholder

**Files:**
- Create: `.../property-mentions-input/mentionsInputPlaceholder.ts`
- Test: `.../property-mentions-input/tests/mentionsInputPlaceholder.test.ts`
- Modify: `.../property-mentions-input/PropertyMentionsInputEditor.tsx`

**Interfaces:**
- Produces: `getMentionsInputPlaceholder({expressionEnabled, placeholder, toolProperty}): string`, `TOOL_PROPERTY_PLACEHOLDER: string`

- [ ] **Step 1: Write the failing test**

Create `tests/mentionsInputPlaceholder.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {TOOL_PROPERTY_PLACEHOLDER, getMentionsInputPlaceholder} from '../mentionsInputPlaceholder';

describe('getMentionsInputPlaceholder', () => {
    it('uses the tool-property placeholder for tool properties', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, toolProperty: true})).toBe(
            TOOL_PROPERTY_PLACEHOLDER
        );
    });

    it('uses the data-pill placeholder for non-tool properties', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, toolProperty: false})).toBe(
            "Use '$' for data pills and '=' for an expression"
        );
    });

    it('prefers an explicit placeholder over the tool-property default', () => {
        expect(
            getMentionsInputPlaceholder({expressionEnabled: true, placeholder: 'Custom', toolProperty: true})
        ).toBe('Custom');
    });

    it('returns empty (or explicit) when expressions are disabled', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: false, toolProperty: true})).toBe('');
        expect(getMentionsInputPlaceholder({expressionEnabled: false, placeholder: 'X', toolProperty: true})).toBe(
            'X'
        );
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/mentionsInputPlaceholder.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Create the helper**

Create `mentionsInputPlaceholder.ts`:

```ts
export const TOOL_PROPERTY_PLACEHOLDER = 'Click the AI button or write =fromAi(...)';

const DEFAULT_PLACEHOLDER = "Use '$' for data pills and '=' for an expression";

// Mirrors the Placeholder.configure logic in PropertyMentionsInputEditor. An explicit placeholder
// always wins. With expressions disabled there is no expression hint. Tool properties are effectively
// always expression fields (AI button or a hand-written =fromAi(...)), so the data-pill hint is
// replaced with a tools-specific message.
export function getMentionsInputPlaceholder({
    expressionEnabled,
    placeholder,
    toolProperty,
}: {
    expressionEnabled: boolean | undefined;
    placeholder?: string;
    toolProperty?: boolean;
}): string {
    if (placeholder) {
        return placeholder;
    }

    if (expressionEnabled === false) {
        return '';
    }

    if (toolProperty) {
        return TOOL_PROPERTY_PLACEHOLDER;
    }

    return DEFAULT_PLACEHOLDER;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run .../tests/mentionsInputPlaceholder.test.ts`
Expected: PASS.

- [ ] **Step 5: Use the helper in the editor**

In `PropertyMentionsInputEditor.tsx`:
- Add import: `import {getMentionsInputPlaceholder} from './mentionsInputPlaceholder';`
- Replace the `Placeholder.configure({...})` block (currently lines ~219–224) with:

```ts
Placeholder.configure({
    placeholder: getMentionsInputPlaceholder({expressionEnabled, placeholder, toolProperty}),
}),
```

- Add `toolProperty` to the `extensions` `useMemo` dependency array.

- [ ] **Step 6: Verify build + manual check**

Run: `npm run check` → PASS.
Manual: an empty tool property in function mode shows "Click the AI button or write =fromAi(...)"; a non-tool property still shows the data-pill hint.

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/mentionsInputPlaceholder.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/mentionsInputPlaceholder.test.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor.tsx
git commit -m "2445 client - Clarify tool-property function-mode placeholder

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: "Customize" (X) reveals the editable `=fromAi(...)` expression

**Files:**
- Create: `.../properties/hooks/fromAiToggle.ts`
- Modify: `.../properties/hooks/tests/handleFromAiToggle.test.ts`
- Modify: `.../properties/hooks/useProperty.ts`

**Interfaces:**
- Produces: `computeFromAiToggle({custom, fromAi, fromAiExpression, hasPath, hasWorkflowId}): {value: string; savePayload: {fromAi: boolean; includeInMetadata: boolean; value: string} | null}`
- Consumes: `fromAiExpression` (already built in `useProperty`).

- [ ] **Step 1: Write the helper module first (source of truth)**

Create `hooks/fromAiToggle.ts`:

```ts
export interface FromAiToggleResultI {
    savePayload: {fromAi: boolean; includeInMetadata: boolean; value: string} | null;
    value: string;
}

// Single source of truth for what the fromAi toggle does to the field value and the save payload.
// Toggling ON locks the field to the fromAi expression. Toggling OFF ("Customize AI generation")
// keeps the SAME expression but as an editable value so the user can tweak it — fromAi:false removes
// the path from the fromAi metadata while the value stays equal to the expression.
export function computeFromAiToggle({
    custom = false,
    fromAi,
    fromAiExpression,
    hasPath = true,
    hasWorkflowId = true,
}: {
    custom?: boolean;
    fromAi: boolean;
    fromAiExpression: string;
    hasPath?: boolean;
    hasWorkflowId?: boolean;
}): FromAiToggleResultI {
    const value = fromAiExpression;

    if (!hasPath || !hasWorkflowId) {
        return {savePayload: null, value};
    }

    return {
        savePayload: {fromAi, includeInMetadata: custom || fromAi, value},
        value,
    };
}
```

- [ ] **Step 2: Rewrite the existing test to import the helper and assert the new behaviour**

In `hooks/tests/handleFromAiToggle.test.ts`, replace the local `FromAiToggleResultI` interface and `computeFromAiToggle` function with an import, and update the toggle-OFF expectations from `''` to the expression. Replace the top of the file and the `toggling OFF` / `guards` blocks:

```ts
import {describe, expect, it} from 'vitest';

import {computeFromAiToggle} from '../fromAiToggle';

const FROM_AI_EXPRESSION = "=fromAi('fieldName')";

const toggle = (overrides: Partial<Parameters<typeof computeFromAiToggle>[0]> & {fromAi: boolean}) =>
    computeFromAiToggle({fromAiExpression: FROM_AI_EXPRESSION, ...overrides});
```

Update assertions:
- `toggling ON` → `value` is `FROM_AI_EXPRESSION`; `savePayload` equals `{fromAi: true, includeInMetadata: true, value: FROM_AI_EXPRESSION}`.
- `toggling OFF` → **`value` is now `FROM_AI_EXPRESSION` (not `''`)**; `savePayload` equals `{fromAi: false, includeInMetadata: false, value: FROM_AI_EXPRESSION}`; custom keeps `includeInMetadata` true.
- `guards` → missing path/workflow id ⇒ `savePayload` is null, `value` is `FROM_AI_EXPRESSION`.

Replace every `computeFromAiToggle({...})` call in the `handleFromAiToggle` describe block with `toggle({...})`. Leave the `handleControlledModeSwitch fromAi cleanup` describe block (and its local `computeControlledModeSwitch`) unchanged.

- [ ] **Step 3: Run test to verify it fails**

Run: `npx vitest run src/pages/platform/workflow-editor/components/properties/hooks/tests/handleFromAiToggle.test.ts`
Expected: PASS for the helper import, but the assertions only pass once Step 1's module exists. (If the module already compiled in Step 1, this step confirms the new OFF expectations hold against the helper.)

- [ ] **Step 4: Wire the helper into `useProperty.ts` — controlled toggle**

In `handleFromAiToggle` (currently lines ~707–743): add `import {computeFromAiToggle} from './fromAiToggle';` at the top, then replace the body's value/save computation:

```ts
const handleFromAiToggle = useCallback(
    (fromAi: boolean, fieldOnChange: (value: string) => void) => {
        setControlledFromAi(fromAi);

        const {savePayload, value} = computeFromAiToggle({
            custom,
            fromAi,
            fromAiExpression,
            hasPath: !!path,
            hasWorkflowId: !!workflow.id,
        });

        fieldOnChange(value);

        if (!savePayload || !path || !workflow.id || !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)) {
            return;
        }

        saveProperty({
            ...savePayload,
            path,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflowId: workflow.id,
        });
    },
    [custom, fromAiExpression, path, type, updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation, workflow.id]
);
```

- [ ] **Step 5: Wire the reveal into `useProperty.ts` — managed click**

In `handleFromAiClick` (currently lines ~1167–1216), change the OFF branch so the editor shows the editable expression. Replace the body:

```ts
const handleFromAiClick = useCallback(
    (fromAi: boolean) => {
        if (!path || !workflow.id) {
            return;
        }

        setControlledFromAi(fromAi);

        const {savePayload, value} = computeFromAiToggle({
            custom,
            fromAi,
            fromAiExpression,
            hasPath: !!path,
            hasWorkflowId: !!workflow.id,
        });

        setPropertyParameterValue(value);

        const editorContent = value.startsWith('=') ? value.substring(1) : value;

        if (fromAi) {
            editorRef.current?.commands.setContent(value);
            editorRef.current?.setEditable(false);
        } else {
            // "Customize AI generation": reveal the =fromAi(...) expression as an editable formula.
            setIsFormulaMode(true);

            editorRef.current?.commands.setContent(editorContent);
            editorRef.current?.setEditable(true);
            editorRef.current?.commands.focus();

            setFocusedInput(editorRef.current);
        }

        if (savePayload) {
            saveProperty({
                ...savePayload,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                workflowId: workflow.id,
            });
        }
    },
    [custom, fromAiExpression, path, setFocusedInput, setIsFormulaMode, type, updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation, workflow.id]
);
```

- [ ] **Step 6: Run tests + build**

Run: `npx vitest run src/pages/platform/workflow-editor/components/properties/hooks/tests/handleFromAiToggle.test.ts` → PASS.
Run: `npm run check` → PASS.
Manual QA: in a tool property, click the AI (sparkles) button → field locks to "Automatically defined by the model"; click the X ("Customize AI generation") → field becomes editable showing `fromAi('name','STRING',{...})` in formula mode (f(x) icon), and is editable.

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/hooks/fromAiToggle.ts \
        client/src/pages/platform/workflow-editor/components/properties/hooks/tests/handleFromAiToggle.test.ts \
        client/src/pages/platform/workflow-editor/components/properties/hooks/useProperty.ts
git commit -m "2445 client - Reveal editable =fromAi() expression when customizing AI value

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Function autocomplete + fromAi button in form-controller mode

**Files:**
- Create: `.../property-mentions-input/controlledExpressionValue.ts`
- Test: `.../property-mentions-input/tests/controlledExpressionValue.test.ts`
- Modify: `PropertyMentionsInputEditor.tsx`, `PropertyMentionsInput.tsx`, `Property.tsx`

**Interfaces:**
- Produces: `reconstructControlledExpressionValue(rawValue: string | number): string`
- Produces: `PropertyMentionsInputEditor`/`PropertyMentionsInput` gain `disableAutoSave?: boolean`; the editor's fromAi-button gate uses `toolProperty` instead of `currentNode?.clusterElementType === 'tools'`.

- [ ] **Step 1: Write the failing test**

Create `tests/controlledExpressionValue.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {reconstructControlledExpressionValue} from '../controlledExpressionValue';

describe('reconstructControlledExpressionValue', () => {
    it('prefixes = when missing', () => {
        expect(reconstructControlledExpressionValue('concat(a, b)')).toBe('=concat(a, b)');
    });

    it('keeps an existing = prefix', () => {
        expect(reconstructControlledExpressionValue('=concat(a, b)')).toBe('=concat(a, b)');
    });

    it('returns empty for blank content', () => {
        expect(reconstructControlledExpressionValue('   ')).toBe('');
        expect(reconstructControlledExpressionValue('')).toBe('');
    });

    it('coerces numbers to a prefixed string', () => {
        expect(reconstructControlledExpressionValue(42)).toBe('=42');
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run .../tests/controlledExpressionValue.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Create the helper**

Create `controlledExpressionValue.ts`:

```ts
// In controlled (react-hook-form) function mode the TipTap editor reports its content WITHOUT the
// leading "=". Re-add it so the form value stays a well-formed expression; empty content clears.
export function reconstructControlledExpressionValue(rawValue: string | number): string {
    const stringValue = typeof rawValue === 'string' ? rawValue : String(rawValue ?? '');
    const trimmed = stringValue.trim();

    if (trimmed === '') {
        return '';
    }

    return trimmed.startsWith('=') ? trimmed : `=${trimmed}`;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run .../tests/controlledExpressionValue.test.ts` → PASS.

- [ ] **Step 5: Add `disableAutoSave` to the editor and switch the fromAi-button gate**

In `PropertyMentionsInputEditor.tsx`:
- Add `disableAutoSave?: boolean;` to `PropertyMentionsInputEditorProps` (alphabetical) and destructure it.
- At the top of `saveMentionInputValue` (the `useDebouncedCallback`, ~line 269), add an early return:

```ts
const saveMentionInputValue = useDebouncedCallback((editorValue: string | number) => {
    if (disableAutoSave) {
        return;
    }
    // ...existing body unchanged
}, 600);
```

- Change the fromAi-button render gate (currently line ~648) from:

```ts
{handleFromAiClick && expressionEnabled !== false && currentNode?.clusterElementType === 'tools' && (
```

to:

```ts
{handleFromAiClick && expressionEnabled !== false && toolProperty && (
```

(`currentNode` may now be unused in this file — remove it from the `useWorkflowNodeDetailsPanelStore` selector if lint flags it, keeping `currentComponent`.)

In `PropertyMentionsInput.tsx`:
- Add `disableAutoSave?: boolean;` to `PropertyMentionsInputProps`, destructure it, and pass `disableAutoSave={disableAutoSave}` to `<PropertyMentionsInputEditor />`.

- [ ] **Step 6: Render `PropertyMentionsInput` in the controlled dynamic-mode branch of `Property.tsx`**

Add imports at the top of `Property.tsx`:

```ts
import {reconstructControlledExpressionValue} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/controlledExpressionValue';
```

In the `control && controlledDynamicMode && calculatedPath` Controller (currently renders `<PropertyInput .../>`, lines ~399–446), replace the returned `<PropertyInput>` with:

```tsx
return (
    <PropertyMentionsInput
        controlType={controlType || 'TEXT'}
        deletePropertyButton={deletePropertyButton}
        description={description}
        disableAutoSave
        error={hasError}
        errorMessage={errorMessage}
        expressionEnabled={expressionEnabled}
        handleFromAiClick={
            isToolsClusterElement ? (fromAi) => handleFromAiToggle(fromAi, fieldOnChange) : undefined
        }
        handleInputTypeSwitchButtonClick={() => {
            fieldOnChange('');
            handleControlledModeSwitch(false);
        }}
        isFormulaMode
        isFromAi={isFieldFromAi}
        label={label || name}
        leadingIcon={typeIcon}
        onValueChange={(value) => fieldOnChange(reconstructControlledExpressionValue(value))}
        path={calculatedPath}
        placeholder="Use '=' for an expression"
        required={required}
        setIsFormulaMode={() => {}}
        showInputTypeSwitchButton
        toolProperty={isToolsClusterElement}
        type={type}
        value={displayValue}
    />
);
```

Note: `displayValue`, `isFieldFromAi`, `fieldOnChange` already exist in this render scope (lines ~381–397). The `inputOverlay`/"Automatically defined by the model" handling moves into the editor (it renders that itself when `isFromAi` is true).

- [ ] **Step 7: Render `PropertyMentionsInput` in the controlled STRING expression branch**

In the `control && !controlledDynamicMode && (isValidControlType || isNumericalInput)` Controller (lines ~510–663), wrap the `<PropertyInput>` so that, for tool string properties in expression/fromAi mode, the editor renders instead. Inside the `render` callback, after the existing `const ...` derivations, change the returned fragment so the first child is conditional:

```tsx
return (
    <>
        {showFromAi && (isExpressionMode || isFieldFromAi) ? (
            <PropertyMentionsInput
                controlType={controlType || 'TEXT'}
                deletePropertyButton={deletePropertyButton}
                description={description}
                disableAutoSave
                error={!!fieldState.error || !!controlledBlurError}
                errorMessage={fieldState.error?.message || controlledBlurError}
                expressionEnabled={expressionEnabled}
                handleFromAiClick={(fromAi) => handleFromAiToggle(fromAi, fieldOnChange)}
                isFormulaMode
                isFromAi={isFieldFromAi}
                label={label || name}
                leadingIcon={typeIcon}
                onValueChange={(value) => fieldOnChange(reconstructControlledExpressionValue(value))}
                path={calculatedPath}
                placeholder={getMentionsInputPlaceholder({expressionEnabled, toolProperty: true})}
                required={required}
                setIsFormulaMode={() => {}}
                toolProperty
                type={type}
                value={displayValue}
            />
        ) : (
            <PropertyInput
                {/* ...existing PropertyInput props unchanged... */}
            />
        )}

        {!!options?.length && (
            <PropertySelect {/* ...unchanged... */} />
        )}
    </>
);
```

Add the import `import {getMentionsInputPlaceholder} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/mentionsInputPlaceholder';` to `Property.tsx`. Keep the existing `<PropertyInput>` block verbatim in the `else` branch — the plain input still serves non-expression controlled values, and its `trailingAction` `FromAiToggleButton` is what lets the user switch INTO fromAi mode.

- [ ] **Step 8: Verify build + manual QA (critical for this task)**

Run: `npm run check` → PASS.
Manual QA in the **automation MCP** "Edit … Tool" dialog:
- A string property shows the AI (sparkles) button. Clicking it locks to "Automatically defined by the model"; the X reveals the editable `=fromAi(...)`.
- Turning on Dynamic / entering `=` expression mode shows the function-autocomplete dropdown (type `con` → `concat`, `contains`; type `fromAi` → fromAi).
- Editing the expression and saving persists the `=`-prefixed value (inspect the saved tool parameters).
Repeat in the **embedded MCP** "Edit … Tool" dialog (same shared component) — confirm identical behaviour. If the autocomplete dropdown is empty in embedded, the `evaluatorFunctionDefinitions` query is not reachable there → record it for Task 6.

- [ ] **Step 9: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/controlledExpressionValue.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/tests/controlledExpressionValue.test.ts \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor.tsx \
        client/src/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput.tsx \
        client/src/pages/platform/workflow-editor/components/properties/Property.tsx
git commit -m "2445 client - Add function autocomplete and fromAi button to controlled tool inputs

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: "+" create-connection in the automation "Select Tools" dialog

**Files:**
- Modify: `client/src/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialogToolSelectionStep.tsx`
- Modify: `.../mcp-component-dialog/hooks/useMcpComponentDialogToolSelectionStep.ts`

**Interfaces:**
- Consumes the shared `ConnectionDialog` from `@/shared/components/connection/ConnectionDialog` with props: `componentDefinition`, `componentDefinitions`, `connectionTagsQueryKey`, `connectionsQueryKey`, `onClose`, `onConnectionCreate`, `useCreateConnectionMutation`, `useGetConnectionTagsQuery` (template: `AiHubConnectConnectionDialog.tsx`).

- [ ] **Step 1: Expose the full component definition from the hook**

In `useMcpComponentDialogToolSelectionStep.ts`, the hook already fetches `componentDefinition` via `useGetComponentDefinitionQuery`. Add `componentDefinition` to the returned object so the step can pass it to `ConnectionDialog`.

- [ ] **Step 2: Add the "+" button and wire `ConnectionDialog` in the step**

In `McpComponentDialogToolSelectionStep.tsx`:
- Add imports (alphabetical):

```ts
import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
```

- Add local state + data and a `+` button next to the `SelectTrigger`:

```tsx
const [showConnectionDialog, setShowConnectionDialog] = useState(false);
const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
const queryClient = useQueryClient();
const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});
const connectionTagsQueryResult = useGetConnectionTagsQuery(currentWorkspaceId ?? 0);
```

Wrap the existing `<Select>` and the new button in a flex row:

```tsx
<div className="flex items-center gap-2">
    <div className="min-w-0 flex-1">
        <Select /* ...existing props... */>
            {/* ...existing trigger + content... */}
        </Select>
    </div>

    {selectedComponent && (
        <Button
            icon={<PlusIcon />}
            onClick={() => setShowConnectionDialog(true)}
            size="icon"
            title="Create a new connection"
            variant="outline"
        />
    )}
</div>
```

- Render the dialog (sibling, not nested) below the connection block:

```tsx
{showConnectionDialog && componentDefinition && componentDefinitions && currentWorkspaceId != null && (
    <ConnectionDialog
        componentDefinition={componentDefinition}
        componentDefinitions={componentDefinitions}
        connectionTagsQueryKey={ConnectionKeys.connectionTags(currentWorkspaceId)}
        connectionsQueryKey={ConnectionKeys.connections}
        onClose={() => setShowConnectionDialog(false)}
        onConnectionCreate={(newConnectionId) => {
            queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

            const created = connections.find((connection) => connection.id === newConnectionId);

            onConnectionChange(created ?? null);

            setShowConnectionDialog(false);
        }}
        useCreateConnectionMutation={useCreateConnectionMutation}
        useGetConnectionTagsQuery={() => connectionTagsQueryResult}
    />
)}
```

Expose `componentDefinition` from the destructured hook result. (The newly created connection may not yet be in `connections`; the `invalidateQueries` refetch will include it, and the next render's `connections.find` resolves it. If `created` is undefined on the first pass, the select still updates once the refetch completes via the existing `selectedConnection` plumbing — acceptable. If immediate selection is required, store `newConnectionId` and select in an effect when `connections` updates.)

- [ ] **Step 3: Verify build + manual QA**

Run: `npm run check` → PASS.
Manual: open the automation "Select Tools from <component>" dialog → a `+` sits next to "Select Connection" → clicking opens the shared connection dialog scoped to the component → creating a connection refreshes the list and selects it.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialogToolSelectionStep.tsx \
        client/src/pages/automation/mcp-servers/components/mcp-component-dialog/hooks/useMcpComponentDialogToolSelectionStep.ts
git commit -m "2445 client - Add inline connection creation to MCP tool selection dialog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6 (ticket 2446): Embedded connection create — SCOPING DECISION REQUIRED

**Finding:** The embedded tool-selection step (`src/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialogToolSelectionStep.tsx`) and its hook currently have **no connection selector at all** — unlike automation, they never fetch or display connections. So "add a `+`" to embedded is not a mirror of Task 5; it requires first deciding whether the embedded MCP tool-config flow should carry a connection, and if so, adding the whole selector.

**Property-input fixes (Tasks 1–4) already cover embedded** because the embedded `McpComponentToolPropertiesPopover` delegates to the same shared `Properties`/`Property`. The only open embedded item is connection creation, plus verifying the `evaluatorFunctionDefinitions` GraphQL query is reachable from the embedded surface (checked in Task 4 Step 8).

**Embedded wiring is available** if we proceed (template: `IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection.tsx`):
- `useCreateConnectionMutation` from `@/ee/shared/mutations/embedded/connections.mutations`
- `useGetComponentDefinitionsQuery` from `@/ee/shared/queries/embedded/componentDefinitions.queries`
- `ConnectionKeys`, `useGetConnectionTagsQuery` from `@/ee/shared/queries/embedded/connections.queries`
- `ConnectionKeys.connectionTags`, `ConnectionKeys.connections`

**Decision needed before writing Task 6 steps:**
- (A) **Mirror automation:** add a "Select Connection" dropdown (fetch embedded connections) + the `+` create dialog to the embedded tool-selection step. Larger than automation since the selector doesn't exist yet.
- (B) **Confirm embedded model first:** embedded connections are often resolved at runtime via connected accounts rather than chosen at tool-config time. If that's the case here, a tool-config connection selector may be the wrong UX, and Task 6 becomes "no-op / different surface".

This task is intentionally left without bite-sized steps pending that decision. Once chosen, append the steps (option A is a near-mirror of Task 5 with the embedded symbols above; commit under ticket 2446).

---

## Self-review notes

- Spec coverage: problems 1–5 → Tasks 1, 2, 3, 4, 5; embedded coverage → Tasks 1–4 (shared) + Task 6 (connection, flagged). Embedded evaluator-defs verification → Task 4 Step 8 / Task 6.
- Type consistency: `toolProperty`, `disableAutoSave`, `buildToolFunctionDefinitions`, `getMentionsInputPlaceholder`, `computeFromAiToggle`, `reconstructControlledExpressionValue` are used with the same signatures across tasks.
- Risk: Task 4 is the highest-risk (TipTap/react-hook-form interplay); its pure helper is unit-tested and the wiring is gated behind explicit manual QA before commit.
