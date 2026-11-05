# Autocomplete in the four code editors

## Problem

Four places in the client let a user write code or a workflow definition in a Monaco editor, and none of them
suggests anything beyond what Monaco's own JavaScript language service knows:

1. **Script component editor**: the dialog behind a Script task's `script` property in the visual workflow editor.
   The script is `perform(input, connection, context)` in JavaScript, Python, Java or R.
   R is out of scope: the dialog hands Monaco the server's `R` language id, which Monaco does not know, so the
   editor has no language there today and this work does not change that.
2. **Custom component editor**: the source editor of a JavaScript, Python or Ruby custom component under
   Settings > Platform > Custom Components. Each action is `perform(input, connection, context)`.
3. **Code workflow editor**: the source editor of a project, embedded workflow project or integration code
   workflow in JavaScript, Python or Ruby. Each task is `perform(context)`.
4. **Workflow code editor**: the JSON or YAML sheet for a visual workflow's definition, where tasks and triggers are
   written by hand and string values carry `${...}` expressions.

The user has to remember the `context` members, the input keys, the connection names, every component and action
name, and the built-in expression functions, and gets no help when they mistype one. The property fields of the
visual editor already suggest built-in functions and data pills; the code editors should offer the same knowledge
in the same places.

## Goal

Each editor suggests the names that are valid at the cursor, drawn from what the platform actually exposes there:

- the `context` members of that runtime,
- the keys of `input`,
- the declared connection names and, under each, that connection's parameter names,
- every component under `context.component`, and every action under a component, with the connected components
  first,
- the built-in expression functions and the earlier tasks' outputs inside `${...}` in the JSON and YAML editor,
- the same script completions inside an inline script string in the JSON and YAML editor.

Success is: a user typing `context.component.goo` in any of the three JavaScript editors sees Google Mail, Google
Maps and the rest, picks one, types `.`, sees that component's actions, and lands on a call with the declared
connection name already filled in. A user typing `=jo` in a JSON string sees `join(separator, list)` with its
description.

## Non-goals

- Hover documentation and signature-help popups. Completion items carry a description; nothing more.
- Completing a task's `parameters` keys in the JSON and YAML editor from the action's property schema.
- Type checking or red squiggles for unknown members.
- Java custom components and Java code workflows. Their source is read-only in the client.
- Injecting TypeScript declaration files into Monaco. The sources are dynamic and per-editor, and Monaco's
  JavaScript service already covers the language itself.

## Anti-goals

- A popup that opens on every keystroke in ordinary code. Completions fire only at the positions below and
  otherwise return nothing, so Monaco's default behaviour is untouched.
- A provider registered more than once. Monaco keeps providers globally per language, and React remounts editors
  freely; a second registration doubles every list.
- Suggestions that lie: a member the runtime does not expose in that editor must not appear there.

## What each runtime exposes

Read from the server on this branch. The builder does not need to re-verify, but the sources are named so the
lists can be kept in step.

| Editor | Signature | `context` members | Source |
|---|---|---|---|
| Script component | `perform(input, connection, context)` | `component` (components and their actions), nothing else | `PolyglotEngine` in `platform-component-runner-impl`, `ContextProxyObject` in `platform-component-polyglot` |
| Custom component | `perform(input, connection, context)` | `http(request)`, `log(level, message)` | `ComponentHandlerPolyglotEngine.toGuestContext` in `platform-custom-component-loader` |
| Code workflow | `perform(context)` | `component`, `input()`, `parameters()`, `connection()`, `log(level, message)` | `ProjectHandlerPolyglotEngine` and `IntegrationHandlerPolyglotEngine` in the code workflow loaders |

Script and custom component `perform` used to differ in parameter names; the custom component rename to
`input, connection, context` already landed on this branch. The provider reads the parameter names from the
signature anyway (see below), so an older source with `inputParameters` keeps working.

An action call under `context.component` is `component.action(parameters, connectionName?)` in scripts, and
`component.action(parameters, connectionName?, clusterElements?)` in code workflows, where the third argument is
accepted by actions that take cluster elements, above all the AI Agent.

## Design

### One provider module, one registration, per-editor context

A new module under `client/src/shared/components/monaco/completion/` owns all completion logic. It registers one
Monaco `CompletionItemProvider` per language it serves (`javascript`, `python`, `ruby`, `json`, `yaml`) exactly
once per page load, from `MonacoEditorWrapper`'s `ensureMonacoConfigured` step, which already runs once and is the
only place that touches the global Monaco instance.

A provider cannot know which editor asked. Each editor therefore attaches a **completion context** to its Monaco
model when it mounts and detaches it on unmount, through a small registry keyed by the model's URI. The provider
looks the context up by `model.uri` and returns nothing when there is none, so every other Monaco editor in the
app (sample data dialogs, the API connector YAML editor, and so on) keeps today's behaviour.

The completion context is a plain object the editor builds from React state and refreshes whenever that state
changes:

```ts
type CompletionContextType =
    | {kind: 'script'; language: 'javascript' | 'python' | 'java' | 'r'; input: Record<string, unknown>;
       connections: ConnectionDeclarationType[]; components: ComponentCatalogType}
    | {kind: 'customComponent'; language: 'javascript' | 'python' | 'ruby'}
    | {kind: 'codeWorkflow'; language: 'javascript' | 'python' | 'ruby'; components: ComponentCatalogType}
    | {kind: 'workflowDefinition'; format: 'json' | 'yaml'; components: ComponentCatalogType;
       functions: EvaluatorFunctionDefinition[]; nodeOutputs: WorkflowNodeOutput[]};

interface ConnectionDeclarationType {
    name: string;            // the name the script refers to, e.g. "googleMaps2"
    componentName: string;   // e.g. "googleMaps"
    componentVersion: number;
}

interface ComponentCatalogType {
    components: ComponentDefinitionBasic[];                       // every component the client already loads
    getDefinition(componentName: string): Promise<ComponentDefinition>; // lazy, cached per component: actions,
                                                                        // triggers, connection properties and
                                                                        // actionClusterElementTypes all live here
}
```

The catalog is built from the queries the client already uses: the component definitions list that feeds the
editor palette, the per-component definition query for actions and connection properties. Nothing new is added on
the server.

### Where completions fire

The provider works from the text before the cursor on the current line, plus a light scan of the whole source
when it needs structure. It never throws on half-typed code: a scan that cannot find what it wants returns an empty
list.

Trigger characters are `.`, `{` and `$`, plus Monaco's normal word-triggered completion, so typing `con` at a
statement start also offers `connection` and `context`.

**JavaScript editors (script, custom component, code workflow)**

| Text before cursor ends with | Offer |
|---|---|
| `<ctx>.` | the `context` members of this editor's runtime, as functions or as a namespace for `component` |
| `<ctx>.component.` | every component, label = title, insert = `name`; components with a declared connection sorted first and marked "connected" in the detail text |
| `<ctx>.component.<name>.` | that component's actions, insert = a call snippet (see below) |
| `<input>.` | the input keys; in the code workflow editor the equivalent is `<ctx>.input().`, offering the workflow's `inputs` names and the names of tasks earlier in the same `tasks` array |
| `<input>.<key>.` | nested keys when the input value at `<key>` is an object |
| `<conn>.` | the declared connection names |
| `<conn>.<name>.` | the connection parameter names of that connection's component, from its connection definition |
| `<conn>['<name>'].` | same, for bracket access |

`<ctx>`, `<input>` and `<conn>` are the actual parameter names of the **enclosing** perform function, found by
scanning backwards from the cursor for the nearest function head in the file's language and mapping its parameters
positionally: three parameters are input, connection, context; two are input, context; one is context. The heads
that occur in the documented and tested sources, and that the scanner must recognise:

- JavaScript: `function perform(a, b, c)`, `perform: function (a, b, c)`, `perform(a, b, c) {` as a method
  shorthand, and `perform: (a, b, c) =>` with or without parentheses around a single parameter;
- Python: `def perform(a, b, c):`, a module-level `def greet(context):` that a task refers to by name
  (`"perform": greet`), and `lambda context:` or `lambda *args:` inline in the task dict;
- Ruby: `def perform(a, b, c)`, `lambda { |context| ... }`, and `->(context) { ... }`.

Outside any such function, nothing fires. A `lambda *args:` form has no named parameters, so only the
`context`-rooted completions are offered there once the user has written `args[0].`; this is a corner the tests
note and the provider need not handle beyond not throwing.

When the enclosing function sits outside the `tasks` array, which is the documented Python shape, the scanner
resolves the task by finding the `"perform": <functionName>` entry that names it. If no task names it, the
runtime members and the component catalog are still offered, but task-scoped names (the task's `connections`, the
earlier tasks, the workflow `inputs`) are not, and action snippets omit the connection name.

Sources per editor:

- **Script**: `input` keys come from the Input panel on the right of the dialog, which already keeps the parsed
  JSON in the dialog store. Connections come from the same list the Connections panel renders. Components come from
  the catalog.
- **Custom component**: `input` keys are the `name` values inside the `properties` array of the **enclosing
  action** object (the nearest `name:`-bearing object between the cursor and the `actions:` array). Connection
  parameter names are the `name` values inside the component-level `connection.properties` array. Both are read
  from the source being edited, so the lists change as the user types. There is no `context.component` here.
- **Code workflow**: components come from the catalog. The connection name offered in an action snippet is the
  `name` of the enclosing task's `connections` entry whose `componentName` matches; the `inputs` names and earlier
  task names come from scanning the enclosing `workflows[]` entry.

**Action call snippet**

Picking an action inserts a snippet, with the first tab stop inside the parameters object:

- script: `getAddress({$1}, '<connection>')` when exactly one declared connection matches the component,
  `getAddress({$1}, '$2')` when several match, `getAddress({$1})` when none does;
- code workflow: the same, plus a third `, {$3}` tab stop when the action definition says it accepts cluster
  elements;
- Python and Ruby use the same shape with that language's string quotes and `{}` dict or hash literal.

The item's detail shows the action title and its description.

**JSON and YAML workflow editor**

The provider first establishes where the cursor is in the document: inside a string value or not, and the path to
it (for example `tasks[2].parameters.script`, or `tasks[2].type`). For JSON this is a tolerant scan that
accepts the trailing comma and unclosed brace of a document mid-edit; for YAML the same from the indentation
structure. If the position cannot be classified, nothing fires.

| Position | Offer |
|---|---|
| in a string, after `${` and before any `.` | the trigger and every task defined **earlier in the file** than the enclosing task, by name; detail = component title |
| in a string, after `${<name>.` | the output properties of that node from `nodeOutputs`, walked by the dot path already typed, the same properties the data pill panel shows |
| in a string that starts with `=`, or after `${` followed by a word | the built-in functions; label = `join(separator, list)`, detail = description, insert = `join($1)` with the caret on the first argument, filtered by the typed prefix exactly as the property-field suggestion filters them |
| value of a `type` key on a task or trigger, before the first `/` | every component as `componentName/v<version>/`, detail = the component title; picking one leaves the caret after the slash and reopens the popup |
| value of a `type` key, after `componentName/v<version>/` | that component's actions under `tasks`, or its triggers under `triggers`, fetched lazily for that one component; detail = the action or trigger title |
| in the string value of `parameters.script` on a task whose `type` starts with `script/v1/` | the **script** completions above, with `input` keys from that task's `parameters.input` object, connection names from that task's `connections` array, and components from the catalog |

Inside the inline script the text is one line with `\n` escapes in JSON and a block scalar in YAML. The provider
treats the string's unescaped content as the source for the `perform` scan, and maps the cursor into it. The
language for the scan is the script action named by the `type` (`script/v1/javascript`, `.../python`, and so on).

`nodeOutputs` is **not** the data pill panel's list: that one is fetched only while a node is selected and is
relative to that node, and the sheet usually opens with no node selected. The sheet fetches the whole workflow's
node outputs itself, through the same query the panel uses with no `lastWorkflowNodeName`, lazily on the first
`${` completion request after the sheet opens, and caches the result until the sheet closes or the workflow is
saved from it. The list is matched to the text by node name: which nodes are "earlier" comes from the unsaved text
in the editor, and the properties under each come from the server's view of the saved workflow. A task that exists
in the text but not yet on the server is offered by name with no properties beneath it.

### Ordering and filtering

Monaco filters by the typed prefix itself. The provider only sets `sortText` so that connected components precede
the rest and otherwise keeps catalog order, and sets `filterText` on function items to the bare name so that
typing `jo` matches `join(separator, list)`.

### Keeping the context fresh

Each editor owns a `useCompletionContext(modelUri, context)` hook that writes the latest object into the registry
on every render where it changed and removes it on unmount. The provider reads it at request time, so a connection
added in the Connections panel, or input JSON edited on the right, shows up on the next `.` without remounting the
editor.

### Tests

Unit tests cover the pure parts, which is nearly all of it:

- the perform signature scanner for each language and each head form listed above, including the two-parameter
  and one-parameter shapes, nested functions, a cursor outside any function, and the Python named-function case
  where the task is resolved through `"perform": name`;
- the custom component source scan for the enclosing action's properties and the connection properties, on a
  complete source, a source with an unclosed brace at the cursor, and a source with two actions;
- the code workflow scan for inputs and earlier task names, with the cursor in the first task and in a later one;
- the JSON and YAML position classifier for each row in the table above, including an unterminated string and a
  missing closing brace;
- the snippet builder for the four connection cases per language;
- the provider as a whole against a fake registry and catalog, asserting that a model without a context returns
  nothing and that each context kind returns the right list at a representative position.

No browser test is required. A manual check of each editor on a running instance closes the work.

## Constraints

- No server change. Everything reads from the GraphQL and REST queries the client already has.
- No new dependency for the JavaScript scans; they are hand-written tolerant scanners, not an AST parser. For JSON
  and YAML position mapping the builder may add `jsonc-parser` and may use the `yaml` package that `monaco-yaml`
  already brings in, if the hand-written classifier gets unwieldy; this is the builder's call.
- Ruby appears only in custom components and code workflows. Providers are registered for `javascript`,
  `python`, `ruby`, `json` and `yaml`; no provider is registered for R.
- The provider must not register twice under React StrictMode or when the Monaco wrapper remounts; a module-level
  flag guards it, the same way `monacoConfigured` does today.
- Existing Monaco language services stay on. JavaScript keyword and local-variable completions still appear
  alongside ours.

## Left to the builder

- The exact shape of the tolerant scanners, as long as they meet the test list above.
- Whether the component catalog lives in a small store or is built in each editor's hook from the existing queries.
  The embedded integration code workflow page uses the embedded component list query its surrounding pages use.
- Reading the model from `onMount` rather than a URI prop, since the wrapper never passes `path` to Monaco.
- Monaco item kinds and icons per entry type.
- How the YAML block scalar's cursor offset is mapped into the unescaped script text.
