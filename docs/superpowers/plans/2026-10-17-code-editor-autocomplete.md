# Code Editor Autocomplete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Monaco completions for `context`, `input`, `connection`, components, actions, built-in expression functions and earlier task outputs in the script, custom component, code workflow and workflow JSON/YAML editors.

**Architecture:** One module under `client/src/shared/components/monaco/completion/` registers a Monaco completion provider per language exactly once. Each editor attaches a completion context to its Monaco model through a registry keyed by model URI; the provider looks the context up at request time and returns nothing for models without one. Pure scanners read the perform signature, the enclosing action or task, and the JSON/YAML cursor position; a catalog built from existing queries supplies components, actions, connection properties, functions and node outputs.

**Tech Stack:** React 19, TypeScript 5.9, monaco-editor 0.56 via @monaco-editor/react, TanStack Query, Vitest 4 with jsdom. No new runtime dependency.

**Spec:** `docs/superpowers/specs/2026-10-17-code-editor-autocomplete-design.md`

## Global Constraints

- No server change; every data source is an existing GraphQL or REST query.
- No new dependency for the JavaScript, Python and Ruby scanners; hand-written tolerant scanners that never throw on partial code.
- Providers are registered for `javascript`, `python`, `ruby`, `json` and `yaml` only; never for `r` or `java`.
- A provider is registered once per page load, guarded by a module-level flag, from `ensureMonacoConfigured` in `MonacoEditorWrapper`.
- A model without a registered completion context gets no items from our providers.
- The script `context` offers only `component`. The custom component `context` offers `http` and `log`. The code workflow `context` offers `component`, `input`, `parameters`, `connection` and `log`.
- Components with a declared connection sort before the rest and carry the detail text `connected`.
- Client conventions from `CLAUDE.md`: interfaces end in `I` or `Props`, type aliases end in `Type`, object keys sorted, `Ref` suffix on refs, no `cn()`, hooks ordered `useState` → `useRef` → stores → custom hooks → memo/callback → `useEffect`.
- Commit messages: client work is `<ticket> client - <description>`. This work has no ticket; use `- client - <description>` per the repo's bare-ticket convention. No attribution trailers.

## Review Focus

1. A JSON document mid-edit with an unterminated string at the cursor (`"script": "function perform(input, connection, context) { input.`) must still classify as inside the script string. Pinned in Task 6.
2. A `perform` written as `perform: (context) =>` with no parentheses around a single parameter (`perform: context =>`) must map `context` as the context name. Pinned in Task 2.
3. A custom component source with two actions, cursor in the second, must offer only the second action's properties. Pinned in Task 3.
4. A `connection['googleMaps'].` bracket access with single or double quotes must offer the same properties as dot access. Pinned in Task 7.
5. Two Monaco editors mounted at once (script dialog over the workflow sheet) must each get their own context; closing one must not remove the other's. Pinned in Task 5.

---

## File Structure

All new files live under `client/src/shared/components/monaco/completion/`:

| File | Responsibility |
|---|---|
| `completionContextRegistry.ts` | Map from model URI to `CompletionContextType`; `registerCompletionContext`, `unregisterCompletionContext`, `getCompletionContext`. |
| `completionTypes.ts` | `CompletionContextType` union, `ConnectionDeclarationType`, `ComponentCatalogType`, `ScriptLanguageType`, `PerformScopeType`. |
| `performScanner.ts` | Find the enclosing perform head and map parameter names; resolve the enclosing task for code workflows and the enclosing action for custom components. |
| `sourceScanner.ts` | Tolerant scans of object-literal sources: `name` values inside a `properties`, `inputs`, `tasks` or `connections` array, and `"perform": name` task lookup. |
| `memberPath.ts` | Parse the text before the cursor into a member chain: root identifier plus segments (`a.b['c'].d` → `{root: 'a', segments: ['b', 'c', 'd']}`), or null. |
| `workflowDocumentScanner.ts` | Classify a JSON or YAML cursor position: inside a string or not, JSON path to it, enclosing task index and `type`, unescaped script text and cursor offset within it. |
| `scriptCompletions.ts` | Items for the three JavaScript-family contexts given a `PerformScopeType`, member chain and catalog. |
| `workflowDefinitionCompletions.ts` | Items for the JSON/YAML context: `${` nodes, node output properties, functions, `type` values; delegates inline scripts to `scriptCompletions`. |
| `snippets.ts` | Action call snippet builder per language and connection case; function snippet builder. |
| `registerCompletionProviders.ts` | Builds one `languages.CompletionItemProvider` per language from the two completion modules and registers it once. |
| `useCompletionContext.ts` | React hook: takes a model (from `onMount`) and a context, registers on change, unregisters on unmount. |
| `useComponentCatalog.ts` | Builds a `ComponentCatalogType` from the component list and per-component definition queries, with a lazy cache. |
| `useWorkflowNodeOutputsForCompletion.ts` | Lazy, cached whole-workflow node outputs for the sheet. |

Modified files:

- `client/src/shared/components/MonacoEditorWrapper.tsx` registers the providers after `loader.config`.
- `client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogEditor.tsx` and `PropertyCodeEditorDialogRightPanel.tsx` (script).
- `client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx` (custom component).
- `client/src/pages/platform/code-workflow/CodeWorkflowSourceEditor.tsx`, `ProjectCodeWorkflowDetail.tsx`, `AutomationWorkflowProjectCodeWorkflowDetail.tsx`, `IntegrationCodeWorkflowDetail.tsx` (code workflow).
- `client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx` (JSON/YAML).

Tests sit beside each file as `<name>.test.ts`, run with `cd client && npx vitest run <path>`.

---

### Task 1: Types and the context registry

**Files:**
- Create: `client/src/shared/components/monaco/completion/completionTypes.ts`
- Create: `client/src/shared/components/monaco/completion/completionContextRegistry.ts`
- Test: `client/src/shared/components/monaco/completion/completionContextRegistry.test.ts`

**Interfaces:**
- Produces, in `completionTypes.ts`:

```ts
import type {ComponentDefinition, ComponentDefinitionBasic, EvaluatorFunctionDefinition, WorkflowNodeOutput} from '...';

export type ScriptLanguageType = 'javascript' | 'python' | 'ruby';

export interface ConnectionDeclarationType {
    componentName: string;
    componentVersion: number;
    name: string;
}

export interface ComponentCatalogType {
    components: ComponentDefinitionBasic[];
    getDefinition(componentName: string, componentVersion?: number): Promise<ComponentDefinition | undefined>;
}

export interface ScriptCompletionContextI {
    components: ComponentCatalogType;
    connections: ConnectionDeclarationType[];
    input: Record<string, unknown>;
    kind: 'script';
    language: ScriptLanguageType;
}

export interface CustomComponentCompletionContextI {
    kind: 'customComponent';
    language: ScriptLanguageType;
}

export interface CodeWorkflowCompletionContextI {
    components: ComponentCatalogType;
    kind: 'codeWorkflow';
    language: ScriptLanguageType;
}

export interface WorkflowDefinitionCompletionContextI {
    components: ComponentCatalogType;
    format: 'json' | 'yaml';
    functions: EvaluatorFunctionDefinition[];
    getNodeOutputs: () => Promise<WorkflowNodeOutput[]>;
    kind: 'workflowDefinition';
}

export type CompletionContextType =
    | ScriptCompletionContextI
    | CustomComponentCompletionContextI
    | CodeWorkflowCompletionContextI
    | WorkflowDefinitionCompletionContextI;

export interface PerformScopeType {
    connectionName?: string;
    contextName?: string;
    inputName?: string;
    performStart: number;
}
```

  `EvaluatorFunctionDefinition` comes from `@/shared/middleware/graphql`; the other three from `@/shared/middleware/platform/configuration`.

- Produces, in `completionContextRegistry.ts`:

```ts
export function registerCompletionContext(modelUri: string, context: CompletionContextType): void;
export function unregisterCompletionContext(modelUri: string): void;
export function getCompletionContext(modelUri: string): CompletionContextType | undefined;
```

- [ ] **Step 1: Write the failing registry test**

```ts
describe('completionContextRegistry', () => {
    it('returns undefined for an unknown model', () => {
        expect(getCompletionContext('inmemory://model/1')).toBeUndefined();
    });

    it('returns the latest context registered for a model', () => {
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'javascript'});
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'python'});

        expect(getCompletionContext('inmemory://model/1')).toEqual({kind: 'customComponent', language: 'python'});
    });

    it('keeps other models when one is unregistered', () => {
        registerCompletionContext('inmemory://model/1', {kind: 'customComponent', language: 'javascript'});
        registerCompletionContext('inmemory://model/2', {kind: 'customComponent', language: 'ruby'});

        unregisterCompletionContext('inmemory://model/1');

        expect(getCompletionContext('inmemory://model/1')).toBeUndefined();
        expect(getCompletionContext('inmemory://model/2')?.language).toBe('ruby');
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/completionContextRegistry.test.ts`
Expected: FAIL, module not found.

- [ ] **Step 3: Write `completionTypes.ts` and `completionContextRegistry.ts`**

The registry is a module-level `Map<string, CompletionContextType>`.

- [ ] **Step 4: Run the test to verify it passes**

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/completionTypes.ts client/src/shared/components/monaco/completion/completionContextRegistry.ts client/src/shared/components/monaco/completion/completionContextRegistry.test.ts
git commit -m "- client - Add the Monaco completion context types and registry"
```

---

### Task 2: Perform scanner and member path

**Files:**
- Create: `client/src/shared/components/monaco/completion/performScanner.ts`
- Create: `client/src/shared/components/monaco/completion/memberPath.ts`
- Test: `performScanner.test.ts`, `memberPath.test.ts` beside them

**Interfaces:**
- Consumes: `PerformScopeType`, `ScriptLanguageType` from Task 1.
- Produces:

```ts
// performScanner.ts
export function findPerformScope(source: string, cursorOffset: number, language: ScriptLanguageType): PerformScopeType | undefined;
export function findEnclosingFunctionName(source: string, cursorOffset: number, language: ScriptLanguageType): string | undefined;

// memberPath.ts
export interface MemberPathType {
    root: string;
    segments: string[];      // a trailing '' means the cursor sits right after the final separator
    callSegments: boolean[]; // callSegments[i] is true when segment i was written with () e.g. input()
}
export function parseMemberPath(textBeforeCursor: string): MemberPathType | undefined;
```

`findPerformScope` returns the nearest function head **before** the cursor whose body contains the cursor, mapping parameters positionally: three → input, connection, context; two → input, context; one → context. `performStart` is the offset of the head. The heads it recognises:

- javascript: `function perform(a, b, c)`, `function (a, b, c)` after `perform:`, `perform(a, b, c) {`, `perform: (a, b, c) =>`, `perform: a =>`, and any `function name(a, b, c)` or `name: (a, b, c) =>` or `const name = (a, b, c) =>` (for a named function a task refers to);
- python: `def name(a, b, c):`, `lambda a, b, c:`, `lambda *args:` (returns a scope with no names);
- ruby: `def name(a, b, c)`, `lambda { |a, b, c|`, `->(a, b, c) {`, `->(a, b, c) do`.

Body containment: for JavaScript and Ruby, count braces from the head's `{` (or `do`) forward; for Python, the body is the lines indented deeper than the `def` line, and a `lambda` body is the rest of the line. A half-typed body with unclosed braces still contains the cursor.

`findEnclosingFunctionName` returns the name on that head when it has one (`greet` for `def greet(context):`, `perform` for `function perform(...)`), or undefined for anonymous heads.

`parseMemberPath` reads backwards from the cursor over identifier characters, `.`, `()`, `['...']` and `["..."]` and stops at anything else. `input.a.` → `{root: 'input', segments: ['a', ''], callSegments: [false, false]}`. `context.input().` → `{root: 'context', segments: ['input', ''], callSegments: [true, false]}`. `connection['googleMaps'].` → `{root: 'connection', segments: ['googleMaps', '']}`. `foo(bar.` → `{root: 'bar', segments: ['']}`. An empty or non-identifier tail returns undefined.

- [ ] **Step 1: Write the failing scanner tests**

```ts
describe('findPerformScope', () => {
    it('maps three JavaScript parameters positionally', () => {
        const source = 'function perform(input, connection, context) {\n  input.\n}';
        expect(findPerformScope(source, source.indexOf('input.') + 6, 'javascript')).toMatchObject({connectionName: 'connection', contextName: 'context', inputName: 'input'});
    });

    it('maps the older custom component parameter names', () => {
        const source = 'perform: function (inputParameters, connectionParameters, context) {\n  inputParameters.';
        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({inputName: 'inputParameters', connectionName: 'connectionParameters', contextName: 'context'});
    });

    it('maps a two-parameter script as input and context', () => {
        const source = 'function perform(input, context) {\n  context.';
        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({inputName: 'input', contextName: 'context'});
        expect(findPerformScope(source, source.length, 'javascript')?.connectionName).toBeUndefined();
    });

    it('maps an arrow function with no parentheses', () => {
        const source = 'perform: context => {\n  context.';
        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({contextName: 'context'});
    });

    it('maps a method shorthand', () => {
        const source = '{ perform(context) {\n  context.';
        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({contextName: 'context'});
    });

    it('returns undefined outside any function', () => {
        expect(findPerformScope('const x = 1;\nx.', 15, 'javascript')).toBeUndefined();
    });

    it('does not leak an outer scope into a sibling function that already closed', () => {
        const source = 'function perform(input, context) {\n  return 1;\n}\nconst other = 2;\nother.';
        expect(findPerformScope(source, source.length, 'javascript')).toBeUndefined();
    });

    it('prefers the innermost function', () => {
        const source = 'function perform(input, context) {\n  const f = (row) => {\n    row.';
        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({contextName: 'row'});
    });

    it('maps a Python def by indentation', () => {
        const source = 'def greet(context):\n    name = context.';
        expect(findPerformScope(source, source.length, 'python')).toMatchObject({contextName: 'context'});
        expect(findEnclosingFunctionName(source, source.length, 'python')).toBe('greet');
    });

    it('ends a Python def at a dedent', () => {
        const source = 'def greet(context):\n    return 1\n\nproject = {\n  "x": ctx.';
        expect(findPerformScope(source, source.length, 'python')).toBeUndefined();
    });

    it('maps a Python lambda on its own line', () => {
        const source = '"perform": lambda context: context.';
        expect(findPerformScope(source, source.length, 'python')).toMatchObject({contextName: 'context'});
    });

    it('returns a nameless scope for lambda *args', () => {
        const source = '"perform": lambda *args: args[0].';
        expect(findPerformScope(source, source.length, 'python')).toEqual({performStart: source.indexOf('lambda')});
    });

    it('maps a Ruby lambda block', () => {
        const source = '"perform" => lambda { |context| context.';
        expect(findPerformScope(source, source.length, 'ruby')).toMatchObject({contextName: 'context'});
    });

    it('maps a Ruby stabby lambda and def', () => {
        expect(findPerformScope('->(input, connection, context) { input.', 39, 'ruby')).toMatchObject({inputName: 'input'});
        expect(findPerformScope('def perform(input, connection, context)\n  connection.', 52, 'ruby')).toMatchObject({connectionName: 'connection'});
    });
});

describe('parseMemberPath', () => {
    it.each([
        ['input.', {root: 'input', segments: [''], callSegments: [false]}],
        ['input.a.', {root: 'input', segments: ['a', ''], callSegments: [false, false]}],
        ['context.input().', {root: 'context', segments: ['input', ''], callSegments: [true, false]}],
        ["connection['googleMaps'].", {root: 'connection', segments: ['googleMaps', ''], callSegments: [false, false]}],
        ['connection["googleMaps"].api', {root: 'connection', segments: ['googleMaps', 'api'], callSegments: [false, false]}],
        ['foo(bar.', {root: 'bar', segments: [''], callSegments: [false]}],
        ['  con', {root: 'con', segments: [], callSegments: []}],
    ])('parses %s', (text, expected) => {
        expect(parseMemberPath(text)).toEqual(expected);
    });

    it('returns undefined for a non-identifier tail', () => {
        expect(parseMemberPath('x = 1 +')).toBeUndefined();
        expect(parseMemberPath('')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/performScanner.test.ts src/shared/components/monaco/completion/memberPath.test.ts`
Expected: FAIL, modules not found.

- [ ] **Step 3: Implement `performScanner.ts` and `memberPath.ts`**

Scan heads with one regex per language applied with the global flag over the whole source, collect every match before the cursor, then test containment from the innermost (latest) outward. Never throw: wrap nothing, just return undefined when no head contains the cursor.

- [ ] **Step 4: Run the tests to verify they pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/performScanner.ts client/src/shared/components/monaco/completion/performScanner.test.ts client/src/shared/components/monaco/completion/memberPath.ts client/src/shared/components/monaco/completion/memberPath.test.ts
git commit -m "- client - Add the perform signature scanner and member path parser for completions"
```

---

### Task 3: Source scanner for custom component and code workflow sources

**Files:**
- Create: `client/src/shared/components/monaco/completion/sourceScanner.ts`
- Test: `sourceScanner.test.ts`

**Interfaces:**
- Consumes: `ScriptLanguageType`, `ConnectionDeclarationType` from Task 1.
- Produces:

```ts
export interface EnclosingActionType {
    name: string;
    propertyNames: string[];
}

export interface EnclosingTaskType {
    connections: ConnectionDeclarationType[];
    earlierTaskNames: string[];
    inputNames: string[];
    name: string;
}

export function findEnclosingAction(source: string, cursorOffset: number, language: ScriptLanguageType): EnclosingActionType | undefined;
export function findComponentConnectionPropertyNames(source: string, language: ScriptLanguageType): string[];
export function findEnclosingTask(source: string, cursorOffset: number, language: ScriptLanguageType): EnclosingTaskType | undefined;
export function findTaskByPerformName(source: string, functionName: string, language: ScriptLanguageType): EnclosingTaskType | undefined;
```

Keys may be written bare (`name:`), single-quoted, double-quoted, or as Ruby hash rockets (`"name" =>`), in all three languages. "Enclosing" means the nearest object whose open brace precedes the cursor and whose close brace follows it or is missing. `connections` entries may be a list of `{componentName, name}` objects or a map keyed by name whose values carry `componentName`; both are returned as `ConnectionDeclarationType` with `componentVersion` defaulting to `1` when absent. `earlierTaskNames` are the `name` values of tasks in the same `tasks` array that appear before the enclosing task. `inputNames` are the `name` values of the enclosing workflow's `inputs` array.

- [ ] **Step 1: Write the failing tests**

```ts
const CUSTOM_COMPONENT = `({
    name: "acme",
    connection: {
        properties: [
            {name: "accessToken", type: "STRING"},
            {name: "region", type: "STRING"}
        ]
    },
    actions: [
        {
            name: "first",
            properties: [{name: "query", type: "STRING"}],
            perform: function (input, connection, context) { return input.query; }
        },
        {
            name: "second",
            properties: [{name: "id", type: "STRING"}, {name: "body", type: "OBJECT"}],
            perform: function (input, connection, context) {
                input.
            }
        }
    ]
})`;

describe('findEnclosingAction', () => {
    it('returns the second action when the cursor is in its perform', () => {
        const offset = CUSTOM_COMPONENT.indexOf('input.\n') + 6;
        expect(findEnclosingAction(CUSTOM_COMPONENT, offset, 'javascript')).toEqual({name: 'second', propertyNames: ['id', 'body']});
    });

    it('returns undefined above the actions array', () => {
        expect(findEnclosingAction(CUSTOM_COMPONENT, CUSTOM_COMPONENT.indexOf('"acme"'), 'javascript')).toBeUndefined();
    });

    it('tolerates an unclosed action object', () => {
        const source = 'actions: [\n  {\n    name: "x",\n    properties: [{name: "a"}],\n    perform: function (input, connection, context) {\n      input.';
        expect(findEnclosingAction(source, source.length, 'javascript')).toEqual({name: 'x', propertyNames: ['a']});
    });

    it('reads Python dict keys', () => {
        const source = '"actions": [\n  {\n    "name": "x",\n    "properties": [{"name": "a"}],\n    "perform": lambda input, connection, context: input.';
        expect(findEnclosingAction(source, source.length, 'python')).toEqual({name: 'x', propertyNames: ['a']});
    });
});

describe('findComponentConnectionPropertyNames', () => {
    it('returns the component-level connection property names', () => {
        expect(findComponentConnectionPropertyNames(CUSTOM_COMPONENT, 'javascript')).toEqual(['accessToken', 'region']);
    });

    it('returns an empty list when there is no connection block', () => {
        expect(findComponentConnectionPropertyNames('({name: "x", actions: []})', 'javascript')).toEqual([]);
    });
});

const CODE_WORKFLOW = `({
    workflows: [
        {
            name: "wf",
            inputs: [{name: "orderId"}, {name: "customer"}],
            tasks: [
                {name: "fetch", perform: (context) => 1},
                {
                    name: "notify",
                    connections: [{componentName: "slack", name: "slack-prod"}],
                    perform: function (context) {
                        context.
                    }
                },
                {name: "later", perform: (context) => 3}
            ]
        }
    ]
})`;

describe('findEnclosingTask', () => {
    it('returns the task, its connections, inputs and earlier task names', () => {
        const offset = CODE_WORKFLOW.indexOf('context.\n') + 8;
        expect(findEnclosingTask(CODE_WORKFLOW, offset, 'javascript')).toEqual({
            connections: [{componentName: 'slack', componentVersion: 1, name: 'slack-prod'}],
            earlierTaskNames: ['fetch'],
            inputNames: ['orderId', 'customer'],
            name: 'notify',
        });
    });

    it('reads a connections map keyed by name', () => {
        const source = 'tasks: [{name: "t", connections: {"gmail": {componentName: "googleMail", componentVersion: 2}}, perform: (context) => { context.';
        expect(findEnclosingTask(source, source.length, 'javascript')?.connections).toEqual([{componentName: 'googleMail', componentVersion: 2, name: 'gmail'}]);
    });
});

describe('findTaskByPerformName', () => {
    it('finds the Python task that references a module-level function', () => {
        const source = 'def greet(context):\n    return 1\n\nproject = {"workflows": [{"name": "w", "inputs": [{"name": "name"}], "tasks": [{"name": "greet", "perform": greet}]}]}';
        expect(findTaskByPerformName(source, 'greet', 'python')).toEqual({connections: [], earlierTaskNames: [], inputNames: ['name'], name: 'greet'});
    });

    it('returns undefined when no task references the name', () => {
        expect(findTaskByPerformName('def other(context):\n    pass', 'other', 'python')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/sourceScanner.test.ts`
Expected: FAIL.

- [ ] **Step 3: Implement `sourceScanner.ts`**

A small tokenizer over the source that tracks brace and bracket depth and skips strings and comments is enough; the enclosing object is the innermost open brace before the cursor that is not closed before it. Read `name` within the enclosing object at its own depth only.

- [ ] **Step 4: Run the tests to verify they pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/sourceScanner.ts client/src/shared/components/monaco/completion/sourceScanner.test.ts
git commit -m "- client - Add the source scanner that reads actions, tasks and connections for completions"
```

---

### Task 4: Snippets and script completions

**Files:**
- Create: `client/src/shared/components/monaco/completion/snippets.ts`
- Create: `client/src/shared/components/monaco/completion/scriptCompletions.ts`
- Test: `snippets.test.ts`, `scriptCompletions.test.ts`

**Interfaces:**
- Consumes: Tasks 1 to 3.
- Produces:

```ts
// snippets.ts
export function buildActionCallSnippet(actionName: string, language: ScriptLanguageType, matchingConnectionNames: string[], acceptsClusterElements: boolean): string;
export function buildFunctionSnippet(functionName: string): string; // `${name}($1)`

// scriptCompletions.ts
export interface CompletionItemDraftType {
    detail?: string;
    documentation?: string;
    insertText: string;
    insertAsSnippet: boolean;
    kind: 'function' | 'module' | 'method' | 'property' | 'value' | 'variable';
    label: string;
    sortText?: string;
    filterText?: string;
}

export interface ScriptCompletionRequestI {
    context: ScriptCompletionContextI | CustomComponentCompletionContextI | CodeWorkflowCompletionContextI;
    cursorOffset: number;
    source: string;
}

export function getScriptCompletions(request: ScriptCompletionRequestI): Promise<CompletionItemDraftType[]>;
```

Snippet shapes, with `$1`, `$2`, `$3` as Monaco tab stops:

| case | javascript | python | ruby |
|---|---|---|---|
| exactly one matching connection `c` | `name({$1}, 'c')` | `name({$1}, 'c')` | `name({$1}, 'c')` |
| several matching | `name({$1}, '$2')` | same | same |
| none | `name({$1})` | same | same |
| plus cluster elements | append `, {$3}` before `)` (or `, {$2}` when there is no connection stop) | same | same |

`getScriptCompletions` resolves the perform scope, builds the member path from the text before the cursor, and returns:

- root equals `contextName`, one segment `''`: the runtime members for `context.kind` (`component` as module; `log`, `http`, `input`, `parameters`, `connection` as method with `insertText` like `log($1)`).
- root `contextName`, segments `['component', '']`: all `components` as modules, label = `title ?? name`, insert = `name`, returned with the connected ones first and catalog order otherwise, `sortText` `0_<name>` for components with a declared connection (script: `context.connections`; code workflow: enclosing task connections) and `1_<name>` otherwise, detail `connected` for the former.
- root `contextName`, segments `['component', '<comp>', '']`: `getDefinition(comp)` → `actions` as methods with the call snippet; `acceptsClusterElements` is `Boolean(definition.actionClusterElementTypes?.[action.name]?.length)` and only for `codeWorkflow`.
- root `contextName`, segments `['input', '']` with `callSegments[0]` true, codeWorkflow only: the enclosing task's `inputNames` then `earlierTaskNames` as properties.
- root `inputName`, segments `['']`: script → `Object.keys(context.input)`; customComponent → enclosing action `propertyNames`.
- root `inputName`, deeper segments, script only: walk `context.input` by the typed segments; offer keys when the value is a plain object.
- root `connectionName`, segments `['']`: script → `connections[].name`; customComponent → `findComponentConnectionPropertyNames`.
- root `connectionName`, segments `['<name>', '']`, script only: find the declaration by name, `getDefinition(componentName)` → `connection?.properties` names, plus the names of `authorizations[].properties` when present.
- a bare root with no segments: offer `inputName`, `connectionName`, `contextName` as variables when they exist.
- anything else: `[]`.

For the custom component kind, the enclosing action comes from `findEnclosingAction`; for the code workflow kind the task comes from `findEnclosingTask`, falling back to `findTaskByPerformName(findEnclosingFunctionName(...))` when the cursor is in a function outside `tasks`.

- [ ] **Step 1: Write the failing tests**

Build a fake catalog:

```ts
const catalog: ComponentCatalogType = {
    components: [
        {name: 'googleMaps', title: 'Google Maps', version: 1} as ComponentDefinitionBasic,
        {name: 'logger', title: 'Logger', version: 1} as ComponentDefinitionBasic,
        {name: 'aiAgent', title: 'AI Agent', version: 1} as ComponentDefinitionBasic,
    ],
    getDefinition: vi.fn(async (name: string) => ({
        actionClusterElementTypes: name === 'aiAgent' ? {chat: ['MODEL', 'TOOLS']} : {},
        actions: [{name: name === 'aiAgent' ? 'chat' : 'getAddress', title: 'Get Address'}],
        connection: {properties: [{name: 'apiKey'}, {name: 'region'}]},
        name,
        version: 1,
    }) as unknown as ComponentDefinition),
};
```

Tests, each calling `getScriptCompletions` with a `source` whose cursor is at `source.length`:

```ts
describe('snippets', () => {
    it.each([
        [['gm'], false, "getAddress({$1}, 'gm')"],
        [['a', 'b'], false, "getAddress({$1}, '$2')"],
        [[], false, 'getAddress({$1})'],
        [['gm'], true, "getAddress({$1}, 'gm', {$3})"],
        [[], true, 'getAddress({$1}, {$2})'],
    ])('builds %s / cluster=%s', (connections, cluster, expected) => {
        expect(buildActionCallSnippet('getAddress', 'javascript', connections, cluster)).toBe(expected);
    });
});

describe('getScriptCompletions for a script', () => {
    const context: ScriptCompletionContextI = {
        components: catalog,
        connections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}],
        input: {order: {id: 1}, total: 5},
        kind: 'script',
        language: 'javascript',
    };

    it('offers only component on context', async () => {
        const items = await getScriptCompletions({context, cursorOffset: 48, source: 'function perform(input, connection, context) {\ncontext.'});
        expect(items.map((item) => item.label)).toEqual(['component']);
    });

    it('sorts connected components first and marks them', async () => {
        const items = await getScriptCompletions({context, cursorOffset: 58, source: 'function perform(input, connection, context) {\ncontext.component.'});
        expect(items.map((item) => item.insertText)).toEqual(['googleMaps', 'logger', 'aiAgent']);
        expect(items[0].detail).toBe('connected');
        expect(items.map((item) => item.sortText)).toEqual(['0_googleMaps', '1_logger', '1_aiAgent']);
    });

    it('offers actions with the single connection prefilled', async () => {
        const source = 'function perform(input, connection, context) {\ncontext.component.googleMaps.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});
        expect(items[0]).toMatchObject({insertAsSnippet: true, insertText: "getAddress({$1}, 'gm')", label: 'getAddress'});
    });

    it('does not offer cluster elements in a script', async () => {
        const source = 'function perform(input, connection, context) {\ncontext.component.aiAgent.';
        const items = await getScriptCompletions({context, cursorOffset: source.length, source});
        expect(items[0].insertText).toBe('chat({$1})');
    });

    it('offers input keys and nested keys', async () => {
        const base = 'function perform(input, connection, context) {\n';
        expect((await getScriptCompletions({context, cursorOffset: base.length + 6, source: base + 'input.'})).map((item) => item.label)).toEqual(['order', 'total']);
        expect((await getScriptCompletions({context, cursorOffset: base.length + 12, source: base + 'input.order.'})).map((item) => item.label)).toEqual(['id']);
    });

    it('offers connection names then that connection\'s properties, for dot and bracket access', async () => {
        const base = 'function perform(input, connection, context) {\n';
        expect((await getScriptCompletions({context, cursorOffset: base.length + 11, source: base + 'connection.'})).map((item) => item.label)).toEqual(['gm']);
        expect((await getScriptCompletions({context, cursorOffset: base.length + 14, source: base + 'connection.gm.'})).map((item) => item.label)).toEqual(['apiKey', 'region']);
        expect((await getScriptCompletions({context, cursorOffset: base.length + 17, source: base + "connection['gm']."})).map((item) => item.label)).toEqual(['apiKey', 'region']);
    });

    it('returns nothing outside perform', async () => {
        expect(await getScriptCompletions({context, cursorOffset: 8, source: 'context.'})).toEqual([]);
    });
});

describe('getScriptCompletions for a custom component', () => {
    const context: CustomComponentCompletionContextI = {kind: 'customComponent', language: 'javascript'};

    it('offers http and log on context and the enclosing action properties on input', async () => {
        const source = '({connection: {properties: [{name: "token"}]}, actions: [{name: "a", properties: [{name: "q"}], perform: function (input, connection, context) {\ncontext.';
        expect((await getScriptCompletions({context, cursorOffset: source.length, source})).map((item) => item.label)).toEqual(['http', 'log']);
        const inputSource = source.replace(/context\.$/, 'input.');
        expect((await getScriptCompletions({context, cursorOffset: inputSource.length, source: inputSource})).map((item) => item.label)).toEqual(['q']);
        const connectionSource = source.replace(/context\.$/, 'connection.');
        expect((await getScriptCompletions({context, cursorOffset: connectionSource.length, source: connectionSource})).map((item) => item.label)).toEqual(['token']);
    });
});

describe('getScriptCompletions for a code workflow', () => {
    const context: CodeWorkflowCompletionContextI = {components: catalog, kind: 'codeWorkflow', language: 'javascript'};
    const source = '({workflows: [{name: "w", inputs: [{name: "orderId"}], tasks: [{name: "fetch", perform: (context) => 1}, {name: "n", connections: [{componentName: "aiAgent", name: "agent"}], perform: function (context) {\ncontext.';

    it('offers the five runtime members', async () => {
        expect((await getScriptCompletions({context, cursorOffset: source.length, source})).map((item) => item.label)).toEqual(['component', 'connection', 'input', 'log', 'parameters']);
    });

    it('offers inputs and earlier tasks under context.input()', async () => {
        const inputSource = source + 'input().';
        expect((await getScriptCompletions({context, cursorOffset: inputSource.length, source: inputSource})).map((item) => item.label)).toEqual(['orderId', 'fetch']);
    });

    it('adds the cluster elements stop for an action that accepts them', async () => {
        const actionSource = source + 'component.aiAgent.';
        expect((await getScriptCompletions({context, cursorOffset: actionSource.length, source: actionSource}))[0].insertText).toBe("chat({$1}, 'agent', {$3})");
    });

    it('resolves a Python task through its named function', async () => {
        const pythonContext: CodeWorkflowCompletionContextI = {...context, language: 'python'};
        const pythonSource = 'def greet(context):\n    context.input().';
        const fullSource = pythonSource + '\n\nproject = {"workflows": [{"name": "w", "inputs": [{"name": "name"}], "tasks": [{"name": "greet", "perform": greet}]}]}';
        expect((await getScriptCompletions({context: pythonContext, cursorOffset: pythonSource.length, source: fullSource})).map((item) => item.label)).toEqual(['name']);
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/snippets.test.ts src/shared/components/monaco/completion/scriptCompletions.test.ts`
Expected: FAIL.

- [ ] **Step 3: Implement `snippets.ts` and `scriptCompletions.ts`**

Runtime member lists are three constants in `scriptCompletions.ts`: `SCRIPT_CONTEXT_MEMBERS = ['component']`, `CUSTOM_COMPONENT_CONTEXT_MEMBERS = ['http', 'log']`, `CODE_WORKFLOW_CONTEXT_MEMBERS = ['component', 'connection', 'input', 'log', 'parameters']`, with `component` as a module and the rest as methods.

- [ ] **Step 4: Run the tests to verify they pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/snippets.ts client/src/shared/components/monaco/completion/snippets.test.ts client/src/shared/components/monaco/completion/scriptCompletions.ts client/src/shared/components/monaco/completion/scriptCompletions.test.ts
git commit -m "- client - Add the script, custom component and code workflow completion items"
```

---

### Task 5: Provider registration and the context hook

**Files:**
- Create: `client/src/shared/components/monaco/completion/registerCompletionProviders.ts`
- Create: `client/src/shared/components/monaco/completion/useCompletionContext.ts`
- Modify: `client/src/shared/components/MonacoEditorWrapper.tsx:33-45` (`ensureMonacoConfigured`)
- Test: `registerCompletionProviders.test.ts`, `useCompletionContext.test.ts`

**Interfaces:**
- Consumes: registry (Task 1), `getScriptCompletions` (Task 4), `getWorkflowDefinitionCompletions` (Task 7, stubbed here as a function returning `[]` and replaced in Task 7).
- Produces:

```ts
// registerCompletionProviders.ts
export const COMPLETION_LANGUAGES = ['javascript', 'python', 'ruby', 'json', 'yaml'] as const;
export function registerCompletionProviders(monaco: typeof import('monaco-editor')): void; // idempotent
export function buildCompletionProvider(monaco: typeof import('monaco-editor')): languages.CompletionItemProvider; // exported for tests

// useCompletionContext.ts
export function useCompletionContext(model: editor.ITextModel | undefined, context: CompletionContextType | undefined): void;
```

The provider's `triggerCharacters` are `['.', '{', '$', "'", '"', '/']`. `provideCompletionItems(model, position)` reads `getCompletionContext(model.uri.toString())`; when undefined, returns `{suggestions: []}`. Otherwise it computes `source = model.getValue()`, `cursorOffset = model.getOffsetAt(position)`, calls `getScriptCompletions` for the three script kinds or `getWorkflowDefinitionCompletions` for `workflowDefinition`, and maps each `CompletionItemDraftType` to a `languages.CompletionItem` with `range` = the word range at the position (`model.getWordUntilPosition`), `insertTextRules` = `InsertAsSnippet` when `insertAsSnippet`, `kind` mapped through `monaco.languages.CompletionItemKind`. Errors thrown by the completion functions are caught and yield `{suggestions: []}`.

`useCompletionContext` registers the context under `model.uri.toString()` in a `useEffect` keyed on `[model, context]` and unregisters in the cleanup. React StrictMode double-invokes effects, so unregister then re-register must leave the context in place.

- [ ] **Step 1: Write the failing tests**

```ts
const registerCompletionItemProvider = vi.fn(() => ({dispose: vi.fn()}));
const fakeMonaco = {
    languages: {
        CompletionItemInsertTextRule: {InsertAsSnippet: 4},
        CompletionItemKind: {Function: 1, Method: 0, Module: 8, Property: 9, Value: 12, Variable: 4},
        registerCompletionItemProvider,
    },
} as unknown as typeof import('monaco-editor');

describe('registerCompletionProviders', () => {
    it('registers one provider per language and only once', () => {
        registerCompletionProviders(fakeMonaco);
        registerCompletionProviders(fakeMonaco);

        expect(registerCompletionItemProvider).toHaveBeenCalledTimes(5);
        expect(registerCompletionItemProvider.mock.calls.map((call) => call[0])).toEqual(['javascript', 'python', 'ruby', 'json', 'yaml']);
    });
});

describe('buildCompletionProvider', () => {
    const model = (value: string, uri: string) => ({
        getOffsetAt: () => value.length,
        getValue: () => value,
        getWordUntilPosition: () => ({endColumn: 1, startColumn: 1}),
        uri: {toString: () => uri},
    });
    const position = {column: 1, lineNumber: 1};

    it('returns no suggestions for a model without a context', async () => {
        const provider = buildCompletionProvider(fakeMonaco);
        expect(await provider.provideCompletionItems(model('context.', 'inmemory://none') as never, position as never, {} as never, {} as never)).toEqual({suggestions: []});
    });

    it('returns script suggestions for a registered model', async () => {
        registerCompletionContext('inmemory://script', {kind: 'customComponent', language: 'javascript'});
        const provider = buildCompletionProvider(fakeMonaco);
        const result = await provider.provideCompletionItems(model('actions: [{name: "a", perform: function (input, connection, context) {\ncontext.', 'inmemory://script') as never, position as never, {} as never, {} as never);
        expect(result?.suggestions.map((suggestion) => suggestion.label)).toEqual(['http', 'log']);
    });
});

describe('useCompletionContext', () => {
    const makeModel = (uri: string) => ({uri: {toString: () => uri}}) as unknown as editor.ITextModel;

    it('registers on mount and unregisters on unmount', () => {
        const {unmount} = renderHook(() => useCompletionContext(makeModel('inmemory://a'), {kind: 'customComponent', language: 'javascript'}));
        expect(getCompletionContext('inmemory://a')?.kind).toBe('customComponent');
        unmount();
        expect(getCompletionContext('inmemory://a')).toBeUndefined();
    });

    it('keeps two mounted editors apart', () => {
        const first = renderHook(() => useCompletionContext(makeModel('inmemory://a'), {kind: 'customComponent', language: 'javascript'}));
        renderHook(() => useCompletionContext(makeModel('inmemory://b'), {kind: 'customComponent', language: 'ruby'}));
        first.unmount();
        expect(getCompletionContext('inmemory://a')).toBeUndefined();
        expect(getCompletionContext('inmemory://b')?.language).toBe('ruby');
    });

    it('does nothing without a model', () => {
        renderHook(() => useCompletionContext(undefined, {kind: 'customComponent', language: 'javascript'}));
        expect(getCompletionContext('undefined')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/registerCompletionProviders.test.ts src/shared/components/monaco/completion/useCompletionContext.test.ts`
Expected: FAIL.

- [ ] **Step 3: Implement both files and wire the wrapper**

In `MonacoEditorWrapper.tsx` `ensureMonacoConfigured`, after `loader.config({monaco})`, call `registerCompletionProviders(monaco)`. Until Task 7 lands, `registerCompletionProviders.ts` imports `getWorkflowDefinitionCompletions` from a one-line `workflowDefinitionCompletions.ts` that returns `Promise.resolve([])`.

- [ ] **Step 4: Run the tests and the type check**

Run: `cd client && npx vitest run src/shared/components/monaco/completion && npm run typecheck`
Expected: PASS, no type errors.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/registerCompletionProviders.ts client/src/shared/components/monaco/completion/registerCompletionProviders.test.ts client/src/shared/components/monaco/completion/useCompletionContext.ts client/src/shared/components/monaco/completion/useCompletionContext.test.ts client/src/shared/components/monaco/completion/workflowDefinitionCompletions.ts client/src/shared/components/MonacoEditorWrapper.tsx
git commit -m "- client - Register the Monaco completion providers once and attach per-editor contexts"
```

---

### Task 6: Workflow document scanner (JSON and YAML)

**Files:**
- Create: `client/src/shared/components/monaco/completion/workflowDocumentScanner.ts`
- Test: `workflowDocumentScanner.test.ts`

**Interfaces:**
- Produces:

```ts
export interface WorkflowDocumentPositionType {
    enclosingTaskIndex?: number;        // index within the top-level tasks array, when the cursor is inside one
    enclosingTaskType?: string;         // that task's `type` value, when written
    enclosingTaskConnections: ConnectionDeclarationType[];
    enclosingTaskInputKeys: string[];   // keys of that task's parameters.input object
    earlierNodeNames: string[];         // trigger names, then task names before the enclosing task (all tasks when not in one)
    inString: boolean;
    path: string[];                     // e.g. ['tasks', '2', 'parameters', 'script'] or ['triggers', '0', 'type']
    stringValueBeforeCursor?: string;   // unescaped string content from its start to the cursor, when inString
}

export function scanWorkflowDocument(source: string, cursorOffset: number, format: 'json' | 'yaml'): WorkflowDocumentPositionType | undefined;
```

Rules: the JSON scanner walks the text tracking `{`/`[` nesting, the current key, and string state, accepting a trailing comma, an unterminated string and a missing close brace at the end; the YAML scanner uses indentation and `- ` list markers and supports plain, single-quoted, double-quoted and `|`/`>` block scalars. `stringValueBeforeCursor` for a JSON string has `\n`, `\"` and `\\` escapes resolved; for a YAML block scalar it is the de-indented text. Returns undefined only when the position is inside a comment or the format is unknown.

- [ ] **Step 1: Write the failing tests**

```ts
const JSON_DOC = `{
  "label": "x",
  "triggers": [{"name": "trigger_1", "type": "schedule/v1/interval"}],
  "tasks": [
    {"name": "httpClient_1", "type": "httpClient/v1/get", "parameters": {"uri": "https://a"}},
    {
      "name": "script_1",
      "type": "script/v1/javascript",
      "connections": [{"componentName": "googleMaps", "componentVersion": 1, "name": "gm"}],
      "parameters": {
        "input": {"order": "\${httpClient_1.body}", "flag": true},
        "script": "function perform(input, connection, context) {\\n  input."
      }
    }
  ]
}`;

describe('scanWorkflowDocument json', () => {
    it('classifies the cursor inside the inline script with the task context', () => {
        const offset = JSON_DOC.lastIndexOf('input.') + 6;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({
            earlierNodeNames: ['trigger_1', 'httpClient_1'],
            enclosingTaskConnections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}],
            enclosingTaskIndex: 1,
            enclosingTaskInputKeys: ['order', 'flag'],
            enclosingTaskType: 'script/v1/javascript',
            inString: true,
            path: ['tasks', '1', 'parameters', 'script'],
            stringValueBeforeCursor: 'function perform(input, connection, context) {\n  input.',
        });
    });

    it('classifies a type value', () => {
        const offset = JSON_DOC.indexOf('httpClient/v1/get') + 'httpClient/v1/get'.length;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({inString: true, path: ['tasks', '0', 'type'], stringValueBeforeCursor: 'httpClient/v1/get'});
    });

    it('classifies an expression string', () => {
        const offset = JSON_DOC.indexOf('${httpClient_1.body}') + '${httpClient_1.'.length;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({earlierNodeNames: ['trigger_1', 'httpClient_1'], inString: true, path: ['tasks', '1', 'parameters', 'input', 'order'], stringValueBeforeCursor: '${httpClient_1.'});
    });

    it('tolerates an unterminated string and a missing close brace', () => {
        const source = '{"tasks": [{"name": "a", "type": "script/v1/javascript", "parameters": {"script": "function perform(input, connection, context) { input.';
        expect(scanWorkflowDocument(source, source.length, 'json')).toMatchObject({enclosingTaskIndex: 0, inString: true, path: ['tasks', '0', 'parameters', 'script']});
    });

    it('reports not in a string between tokens', () => {
        const offset = JSON_DOC.indexOf('"tasks"') - 1;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({inString: false});
    });
});

const YAML_DOC = `label: x
triggers:
  - name: trigger_1
    type: schedule/v1/interval
tasks:
  - name: httpClient_1
    type: httpClient/v1/get
  - name: script_1
    type: script/v1/python
    parameters:
      input:
        order: \${httpClient_1.body}
      script: |
        def perform(input, connection, context):
            return input.
`;

describe('scanWorkflowDocument yaml', () => {
    it('classifies the cursor inside a block scalar script', () => {
        const offset = YAML_DOC.lastIndexOf('input.') + 6;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({
            earlierNodeNames: ['trigger_1', 'httpClient_1'],
            enclosingTaskIndex: 1,
            enclosingTaskInputKeys: ['order'],
            enclosingTaskType: 'script/v1/python',
            inString: true,
            path: ['tasks', '1', 'parameters', 'script'],
            stringValueBeforeCursor: 'def perform(input, connection, context):\n    return input.',
        });
    });

    it('classifies a plain scalar type value', () => {
        const offset = YAML_DOC.indexOf('httpClient/v1/get') + 'httpClient/v1/get'.length;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({inString: true, path: ['tasks', '0', 'type'], stringValueBeforeCursor: 'httpClient/v1/get'});
    });

    it('classifies a plain scalar expression', () => {
        const offset = YAML_DOC.indexOf('${httpClient_1.body}') + '${httpClient_1.'.length;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({inString: true, path: ['tasks', '1', 'parameters', 'input', 'order'], stringValueBeforeCursor: '${httpClient_1.'});
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/shared/components/monaco/completion/workflowDocumentScanner.test.ts`
Expected: FAIL.

- [ ] **Step 3: Implement `workflowDocumentScanner.ts`**

Two internal functions, `scanJson` and `scanYaml`, each producing a list of `{path, valueStart, valueEnd, rawValue}` for every scalar it can see plus the structural facts (task indices, names, types, connections, input keys), then one shared function picks the scalar containing the cursor and assembles the result. In YAML, plain scalars count as strings. The builder may use the `yaml` package's CST if the indentation walk gets unwieldy, per the spec.

- [ ] **Step 4: Run the tests to verify they pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/workflowDocumentScanner.ts client/src/shared/components/monaco/completion/workflowDocumentScanner.test.ts
git commit -m "- client - Add the JSON and YAML workflow document position scanner for completions"
```

---

### Task 7: Workflow definition completions

**Files:**
- Modify: `client/src/shared/components/monaco/completion/workflowDefinitionCompletions.ts` (replace the Task 5 stub)
- Test: `workflowDefinitionCompletions.test.ts`

**Interfaces:**
- Consumes: Task 6 scanner, Task 4 `getScriptCompletions` and `buildFunctionSnippet`, `WorkflowDefinitionCompletionContextI`, `filterFunctionDefinitions` from `property-mentions-input/functionSuggestionUtils.ts`, `getOutputSchemaFromWorkflowNodeOutput` from `workflow-editor/utils`.
- Produces:

```ts
export interface WorkflowDefinitionCompletionRequestI {
    context: WorkflowDefinitionCompletionContextI;
    cursorOffset: number;
    source: string;
}

export function getWorkflowDefinitionCompletions(request: WorkflowDefinitionCompletionRequestI): Promise<CompletionItemDraftType[]>;
```

Decision table over `scanWorkflowDocument` output and `stringValueBeforeCursor` (`s`):

| condition | items |
|---|---|
| not in a string | `[]` |
| path ends in `parameters`, `script` and `enclosingTaskType` starts with `script/v1/` | `getScriptCompletions` with a synthetic `ScriptCompletionContextI` (`language` from the type suffix: `javascript`, `python`; `java` and `r` → `[]`), `input` built as `Object.fromEntries(enclosingTaskInputKeys.map((key) => [key, null]))`, `connections` = `enclosingTaskConnections`, `source` = `s`, `cursorOffset` = `s.length` |
| path ends in `type` under `tasks` or `triggers`, `s` has no `/` | each component as `name/v<version>/`, kind module, detail title, `insertText` ending in `/`, `command: editor.action.triggerSuggest` so the popup reopens (`CompletionItemDraftType` gains an optional `retrigger: boolean` the provider maps to that command) |
| path ends in `type`, `s` matches `^(\w+)/v(\d+)/` | `getDefinition(name, version)` → `actions` (under `tasks`) or `triggers` (under `triggers`), insert = the action name only, `filterText` = the full `name/vN/action` |
| `s` ends with `${` followed by identifier chars and no `.` | `earlierNodeNames` as variables |
| `s` ends with `${<node>.<a>.<b>.` | `getNodeOutputs()` → the output whose `workflowNodeName` is `<node>`, `getOutputSchemaFromWorkflowNodeOutput` → walk `properties` by `a`, `b`; offer the `name` of each property at that level, detail = its `type` |
| `s` starts with `=` or `s` ends with `${` + word, and the trailing word is non-empty | `filterFunctionDefinitions(functions, word)` → label `name(p1, p2)` via `formatFunctionSignatureParts`, `filterText` = name, `insertText` = `buildFunctionSnippet(name)`, detail = description |

Function and node items may both apply after `${wo`; return the node items first, then the functions.

- [ ] **Step 1: Write the failing tests**

```ts
const functions: EvaluatorFunctionDefinition[] = [
    {category: 'STRING', description: 'Joins', example: '', name: 'join', parameters: [{description: '', name: 'separator', required: true, type: 'STRING'}, {description: '', name: 'list', required: true, type: 'LIST'}], returnType: 'STRING', title: 'Join'},
    {category: 'DATE_TIME', description: 'Now', example: '', name: 'now', parameters: [], returnType: 'DATETIME', title: 'Now'},
];
const nodeOutputs = [{outputResponse: {outputSchema: {properties: [{name: 'body', properties: [{name: 'amount', type: 'NUMBER'}], type: 'OBJECT'}], type: 'OBJECT'}}, workflowNodeName: 'httpClient_1'}] as unknown as WorkflowNodeOutput[];
const context: WorkflowDefinitionCompletionContextI = {components: catalog, format: 'json', functions, getNodeOutputs: vi.fn(async () => nodeOutputs), kind: 'workflowDefinition'};

const request = (source: string, marker: string) => ({context, cursorOffset: source.indexOf(marker) + marker.length, source});

it('offers earlier nodes after ${', async () => {
    const source = '{"triggers": [{"name": "trigger_1"}], "tasks": [{"name": "httpClient_1"}, {"name": "t2", "parameters": {"x": "${"}}]}';
    expect((await getWorkflowDefinitionCompletions(request(source, '"x": "${'))).map((item) => item.label)).toEqual(['trigger_1', 'httpClient_1']);
});

it('offers output properties by dot path', async () => {
    const source = '{"tasks": [{"name": "httpClient_1"}, {"name": "t2", "parameters": {"x": "${httpClient_1.body."}}]}';
    expect((await getWorkflowDefinitionCompletions(request(source, 'body.'))).map((item) => item.label)).toEqual(['amount']);
});

it('offers functions in a formula value', async () => {
    const source = '{"tasks": [{"name": "t", "parameters": {"x": "=jo"}}]}';
    const items = await getWorkflowDefinitionCompletions(request(source, '=jo'));
    expect(items[0]).toMatchObject({filterText: 'join', insertAsSnippet: true, insertText: 'join($1)', label: 'join(separator, list)'});
});

it('offers components then actions for a task type', async () => {
    const first = '{"tasks": [{"name": "t", "type": ""}]}';
    expect((await getWorkflowDefinitionCompletions(request(first, '"type": "'))).map((item) => item.insertText)).toEqual(['googleMaps/v1/', 'logger/v1/', 'aiAgent/v1/']);
    const second = '{"tasks": [{"name": "t", "type": "googleMaps/v1/"}]}';
    expect((await getWorkflowDefinitionCompletions(request(second, 'googleMaps/v1/'))).map((item) => item.filterText)).toEqual(['googleMaps/v1/getAddress']);
});

it('delegates an inline script to the script completions with the task connections', async () => {
    const source = '{"tasks": [{"name": "s", "type": "script/v1/javascript", "connections": [{"componentName": "googleMaps", "name": "gm"}], "parameters": {"input": {"order": 1}, "script": "function perform(input, connection, context) {\\n input."}}]}';
    expect((await getWorkflowDefinitionCompletions(request(source, 'input.'))).map((item) => item.label)).toEqual(['order']);
    const connectionSource = source.replace('input."', 'connection."');
    expect((await getWorkflowDefinitionCompletions(request(connectionSource, 'connection.'))).map((item) => item.label)).toEqual(['gm']);
});

it('returns nothing outside strings', async () => {
    expect(await getWorkflowDefinitionCompletions({context, cursorOffset: 1, source: '{ "tasks": [] }'})).toEqual([]);
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL (the stub returns `[]`).

- [ ] **Step 3: Implement `workflowDefinitionCompletions.ts`; add `retrigger?: boolean` to `CompletionItemDraftType` and map it in `registerCompletionProviders.ts` to `command: {id: 'editor.action.triggerSuggest', title: 'Suggest'}`**

- [ ] **Step 4: Run the whole completion suite**

Run: `cd client && npx vitest run src/shared/components/monaco/completion`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion
git commit -m "- client - Add the workflow definition completions for expressions, functions, types and inline scripts"
```

---

### Task 8: Catalog and node output hooks

**Files:**
- Create: `client/src/shared/components/monaco/completion/useComponentCatalog.ts`
- Create: `client/src/shared/components/monaco/completion/useWorkflowNodeOutputsForCompletion.ts`
- Test: `useComponentCatalog.test.ts`, `useWorkflowNodeOutputsForCompletion.test.ts`

**Interfaces:**
- Produces:

```ts
export function useComponentCatalog(options?: {embedded?: boolean}): ComponentCatalogType;
export function useWorkflowNodeOutputsForCompletion(workflowId: string | undefined, environmentId: number): () => Promise<WorkflowNodeOutput[]>;
```

`useComponentCatalog` reads the component list with `useGetComponentDefinitionsQuery({actionDefinitions: true, triggerDefinitions: true})` from `@/shared/queries/automation/componentDefinitions.queries` (or the embedded twin in `@/ee/shared/queries/embedded/componentDefinitions.queries` when `embedded` is true), and builds `getDefinition` with `queryClient.fetchQuery({queryKey: ComponentDefinitionKeys.componentDefinition(request), queryFn: () => new ComponentDefinitionApi().getComponentDefinition(request), staleTime: DEFINITION_STALE_TIME})`, where `componentVersion` defaults to the version in the list. The returned object is memoised on the list so the completion context does not churn.

`useWorkflowNodeOutputsForCompletion` returns a stable callback that calls `queryClient.fetchQuery` with `WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({environmentId, id: workflowId})` and `new WorkflowNodeOutputApi().getPreviousWorkflowNodeOutputs(...)` with no `lastWorkflowNodeName`, `staleTime: DEFINITION_STALE_TIME`, and resolves `[]` when `workflowId` is undefined. The sheet invalidates that key after a save (Task 9).

- [ ] **Step 1: Write the failing tests**

Mock `@/shared/queries/automation/componentDefinitions.queries` and `@/shared/middleware/platform/configuration` (`ComponentDefinitionApi`, `WorkflowNodeOutputApi`) with `vi.mock`, render each hook inside a `QueryClientProvider`, and assert:

```ts
it('exposes the component list and fetches a definition once per component', async () => {
    // list mock returns [{name: 'slack', version: 1}]
    const {result} = renderHook(() => useComponentCatalog(), {wrapper});
    await waitFor(() => expect(result.current.components).toHaveLength(1));
    await result.current.getDefinition('slack');
    await result.current.getDefinition('slack');
    expect(getComponentDefinitionMock).toHaveBeenCalledTimes(1);
    expect(getComponentDefinitionMock).toHaveBeenCalledWith({componentName: 'slack', componentVersion: 1});
});

it('fetches whole-workflow node outputs lazily and once', async () => {
    const {result} = renderHook(() => useWorkflowNodeOutputsForCompletion('wf-1', 7), {wrapper});
    expect(getPreviousWorkflowNodeOutputsMock).not.toHaveBeenCalled();
    await result.current();
    await result.current();
    expect(getPreviousWorkflowNodeOutputsMock).toHaveBeenCalledTimes(1);
    expect(getPreviousWorkflowNodeOutputsMock).toHaveBeenCalledWith({environmentId: 7, id: 'wf-1'});
});

it('resolves an empty list without a workflow id', async () => {
    const {result} = renderHook(() => useWorkflowNodeOutputsForCompletion(undefined, 7), {wrapper});
    expect(await result.current()).toEqual([]);
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL.

- [ ] **Step 3: Implement both hooks**

- [ ] **Step 4: Run the tests to verify they pass**

Run: `cd client && npx vitest run src/shared/components/monaco/completion`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/monaco/completion/useComponentCatalog.ts client/src/shared/components/monaco/completion/useComponentCatalog.test.ts client/src/shared/components/monaco/completion/useWorkflowNodeOutputsForCompletion.ts client/src/shared/components/monaco/completion/useWorkflowNodeOutputsForCompletion.test.ts
git commit -m "- client - Add the component catalog and workflow node output hooks for completions"
```

---

### Task 9: Wire the four editors

**Files:**
- Modify: `client/src/shared/components/MonacoEditorWrapper.tsx` (no change beyond Task 5; `onMount` already hands the editor over)
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogEditor.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanel.tsx:70-100`
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore.ts`
- Modify: `client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx:180-200`
- Modify: `client/src/pages/platform/code-workflow/CodeWorkflowSourceEditor.tsx`, `ProjectCodeWorkflowDetail.tsx`, `AutomationWorkflowProjectCodeWorkflowDetail.tsx`, `IntegrationCodeWorkflowDetail.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx:205-225` and `hooks/useWorkflowCodeEditorSheet.ts`
- Test: extend `PropertyCodeEditorDialogEditor.test.tsx`, `CodeWorkflowSourceEditor.test.tsx`, `CustomComponentDetail` tests and `WorkflowCodeEditorSheet` tests (existing files beside each component)

**Interfaces:**
- Consumes: `useCompletionContext`, `useComponentCatalog`, `useWorkflowNodeOutputsForCompletion`, `useEvaluatorFunctionDefinitions` from `property-mentions-input/useEvaluatorFunctionDefinitions.ts`.

Per editor:

1. **Script dialog.** `PropertyCodeEditorDialogRightPanel` already computes `componentConnections` (GraphQL rows with `key`, `componentName`, `componentVersion`) and `input`; it stores them in the dialog store under two new fields `componentConnections: ConnectionDeclarationType[]` and `scriptInput: Record<string, unknown>` (mapping `key` → `name`). `PropertyCodeEditorDialogEditor` keeps the mounted editor's model in `useState<editor.ITextModel>()` set from `onMount` (`editor.getModel() ?? undefined`), reads those two fields plus `inputParameters` from the store (the edited Input panel JSON wins over `scriptInput` when set), builds the catalog with `useComponentCatalog()`, and calls `useCompletionContext(model, {components, connections, input, kind: 'script', language})` where `language` is `'javascript' | 'python'` from the dialog's `language` prop lowercased, and no context (`undefined`) for `java` or `R`.
2. **Custom component.** `CustomComponentSourceEditor` gains the same `useState` model and calls `useCompletionContext(model, {kind: 'customComponent', language: monacoLanguage})` for the three script languages, `undefined` for `java`.
3. **Code workflow.** `CodeWorkflowSourceEditor` takes a new optional prop `embedded?: boolean`; `IntegrationCodeWorkflowDetail` and `AutomationWorkflowProjectCodeWorkflowDetail` pass `embedded`. Inside, `useComponentCatalog({embedded})` and `useCompletionContext(model, {components, kind: 'codeWorkflow', language})` for the three script languages.
4. **Workflow sheet.** `WorkflowCodeEditorSheet` keeps the model from `onMount` (`editor.focus()` stays), builds `functions` with `useEvaluatorFunctionDefinitions()`, `components` with `useComponentCatalog()`, `getNodeOutputs` with `useWorkflowNodeOutputsForCompletion(workflow.id, currentEnvironmentId)`, and calls `useCompletionContext(model, {components, format, functions, getNodeOutputs, kind: 'workflowDefinition'})` with `format` = `workflow.format?.toLowerCase() === 'yaml' ? 'yaml' : 'json'`. After a successful save in `useWorkflowCodeEditorSheet`, invalidate `WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({environmentId: currentEnvironmentId, id: workflow.id!})`.

- [ ] **Step 1: Write the failing wiring tests**

Each existing editor test file already mocks `MonacoEditorWrapper`; extend that mock to invoke `onMount` with a fake editor whose `getModel()` returns `{uri: {toString: () => 'inmemory://test'}}`. Mock `useComponentCatalog`, `useEvaluatorFunctionDefinitions` and `useWorkflowNodeOutputsForCompletion` to return fixed values. Assert through the real registry:

```ts
// PropertyCodeEditorDialogEditor.test.tsx
it('registers a script completion context with the dialog connections and input', () => {
    usePropertyCodeEditorDialogStore.setState({componentConnections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}], scriptInput: {order: 1}});
    render(<PropertyCodeEditorDialogEditor language="javascript" />);
    expect(getCompletionContext('inmemory://test')).toMatchObject({connections: [{name: 'gm'}], input: {order: 1}, kind: 'script', language: 'javascript'});
});

it('registers no context for a Java script', () => {
    render(<PropertyCodeEditorDialogEditor language="java" />);
    expect(getCompletionContext('inmemory://test')).toBeUndefined();
});

// CustomComponentDetail test
it('registers a custom component completion context for a Ruby component', ...); // expect {kind: 'customComponent', language: 'ruby'}

// CodeWorkflowSourceEditor.test.tsx
it('registers a code workflow completion context', ...); // expect {kind: 'codeWorkflow', language: 'python'}

// WorkflowCodeEditorSheet test
it('registers a workflow definition completion context in the workflow format', ...); // expect {format: 'yaml', kind: 'workflowDefinition'} for a YAML workflow
```

And in `PropertyCodeEditorDialogRightPanel.test.tsx`:

```ts
it('stores the connections and input for completions', async () => {
    // existing GraphQL mocks return one connection {key: 'gm', componentName: 'googleMaps', componentVersion: 1} and input {order: 1}
    render(<PropertyCodeEditorDialogRightPanel ... />);
    await waitFor(() => expect(usePropertyCodeEditorDialogStore.getState().componentConnections).toEqual([{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}]));
    expect(usePropertyCodeEditorDialogStore.getState().scriptInput).toEqual({order: 1});
});
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-code-editor src/ee/pages/settings/platform/custom-components src/pages/platform/code-workflow src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet`
Expected: the new tests FAIL, the rest still PASS.

- [ ] **Step 3: Wire the four editors as described**

Keep the hook order rule: `useState` for the model first, then store reads, then the catalog and context hooks, `useEffect` last.

- [ ] **Step 4: Run the full client check**

Run: `cd client && npm run format && npm run check`
Expected: lint, typecheck and tests all PASS.

- [ ] **Step 5: Manual check on a running instance**

Start the server and client (`./gradlew -p server/apps/server-app bootRun`, `cd client && npm run dev`), then in each editor confirm one completion from the spec's success paragraph: `context.component.goo` in the script dialog, `input.` in a custom component action, `context.input().` in a code workflow task, `=jo` and `${` in the JSON sheet, and `input.` inside an inline script string in the YAML sheet. Record what you saw in the final report.

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-code-editor client/src/ee/pages/settings/platform/custom-components client/src/pages/platform/code-workflow client/src/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet.tsx client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts
git commit -m "- client - Attach completion contexts to the script, custom component, code workflow and workflow code editors"
```
