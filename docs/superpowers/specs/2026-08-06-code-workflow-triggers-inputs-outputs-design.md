# Triggers, inputs and outputs for code workflows

Status: implemented (2026-08-06)
Date: 2026-08-06

## Problem

A code workflow cannot declare a trigger, an input, or an output. Not "declares them awkwardly" —
the declarations are silently discarded:

```java
// CodeWorkflowContainerFacadeImpl.getDefinition
OptionalUtils.ifPresent(workflowDefinition.getInputs(),   inputs   -> objectNode.set("inputs",   objectMapper.createObjectNode()));
OptionalUtils.ifPresent(workflowDefinition.getOutputs(),  outputs  -> objectNode.set("outputs",  objectMapper.createObjectNode()));
OptionalUtils.ifPresent(workflowDefinition.getTriggers(), triggers -> objectNode.set("triggers", objectMapper.createArrayNode()));
```

The presence of a declaration is used only to decide whether to emit an empty container. The SDK
side matches: `Input`, `Output` and `Parameter` are **empty marker interfaces** — no name, no type,
nothing to carry — and `TriggerDefinition` exposes a name and a type but its parameters are a list
of those empty `Parameter`s.

The consequences are user-visible:

- A code workflow **never starts on its own**. No schedule, no webhook, no polling. It runs only
  when something calls it, which also means it cannot satisfy the embedded bridge's
  invocable-trigger requirement or be exposed through MCP (`workflow/newWorkflowCall`).
- Its **inputs are the empty set**, so the Workflow Inputs dialog has nothing to show, a caller has
  no declared contract, and test configuration cannot prompt for values.
- Its **outputs are empty**, so a synchronous caller gets the raw last-task shape rather than a
  declared response.

There is a live bug alongside: `inputs` and `outputs` are emitted as **objects** where the workflow
model reads **lists**, and the editor's Workflow Inputs dialog writes into the same definition — so
an input added there survives until the next source save silently wipes it.

## Design

### Inputs

`Workflow.Input(name, label, type, required, extensions)`. The SDK's `Input` marker grows the same
four fields, and the DSL reads like the rest of it:

```java
WorkflowDsl.workflow("orders")
    .inputs(
        input("orderId").label("Order ID").type("STRING").required(true),
        input("dryRun").type("BOOLEAN"))
```

```javascript
inputs: [
    {name: "orderId", label: "Order ID", type: "STRING", required: true},
    {name: "dryRun", type: "BOOLEAN"}
]
```

Emitted as a JSON **array**, fixing the shape bug. At run time they arrive where they already do —
in `context.input()`, each under its own name — so nothing about the perform contract changes; what
changes is
that the platform now knows they exist.

**The editor's Workflow Inputs dialog becomes read-only for code workflows.** The source owns them;
letting the dialog write inputs the next save deletes is the divergence trap the name-mismatch check
already guards against elsewhere.

**Corrected at implementation:** blanket read-only was wrong. That sheet is not only a declaration
editor — it also carries each input's **test value** (`WorkflowInputsTable`'s Test Value column,
`testValue` in the edit dialog), and for a code workflow it is the *only* place to set one. Locking
the whole sheet would have made code workflows untestable. What is locked is the declaration alone:
`New Input` and `Delete` disappear, and `type` / `name` / `label` / `required` / `internalOnly` go
read-only inside the edit dialog. The dialog still opens, and `testValue` stays editable. The empty
state says to declare inputs in the source and set their test values here.

### Outputs

`Workflow.Output(name, value)`, where `value` is evaluated against the job context when the workflow
completes. Two forms, because the obvious one has the hyphen trap:

```javascript
outputs: [
    {name: "orderId", value: "${fetchOrder.body.id}"},   // plain identifier task name
    {name: "customer", task: "fetch-customer"}           // any task name, including hyphenated
]
```

The `task` form emits `=#root['fetch-customer']` — the same formula the input snapshot already
relies on. Without it, a code workflow whose task names contain hyphens could declare no output at
all, which is exactly the trap that made `context.input()` necessary in the first place.

**How a declared output is read back** (unchanged by this design — it is the platform's existing
behavior, and the reason declaring outputs is worth anything):

- `DefaultTaskCompletionHandler.complete` evaluates the name→value map against the final job context
  and stores the result as the job's outputs.
- **A synchronous call returns them as the response body.** `WebhookWorkflowExecutorImpl` reads
  `readJobOutputs(job.getOutputs())` and returns that, unless a `response` action set an explicit
  webhook response — which wins. The MCP and A2A sync surfaces read the same job outputs. With no
  declared outputs today, a sync caller gets an empty map, which is precisely the gap.
- **An asynchronous run exposes them through the public API**: `GET /workflow-executions/{id}`
  returns "inputs, outputs, error and task executions" (the list endpoint deliberately omits them),
  and the CLI wraps it as `bytechef automation execution get --id <id>`. The caller needs the
  execution id; the list endpoint filtered by workflow and date is the way to find one when the
  async dispatch did not hand it back.

### Triggers

A trigger is **not user code**. It is a component trigger node — `schedule/v1/interval`,
`webhook/v1/newWebhook`, `workflow/v1/newWorkflowCall` — with parameters. So the SDK's existing
`trigger(name, type)` has the right shape and only needs parameters that can hold values:

```java
.triggers(
    trigger("daily", "schedule/v1/interval")
        .parameters(Map.of("interval", 1, "unit", "DAY")))
```

```javascript
triggers: [
    {name: "daily", type: "schedule/v1/interval", parameters: {interval: 1, unit: "DAY"}},
    {name: "onCall", type: "workflow/v1/newWorkflowCall"}
]
```

Emitted as `{name, label?, description?, type, parameters}` entries, which is what `WorkflowTrigger`
already parses out of the definition's `triggers` extension.

**Every trigger type is allowed.** A code workflow is a normal workflow whose *tasks* happen to be
code; there is no reason its trigger should be more restricted than a visual one's. Note the
contrast with script custom components, where the supported set is bounded (POLLING, STATIC_WEBHOOK
and DYNAMIC_WEBHOOK — an earlier draft of this line said POLLING alone, which was already wrong when
written) because there the trigger's `poll` / `webhookRequest` / `webhookEnable` are guest code we
must run; here the trigger is a platform component that already exists.

**The component and trigger name are validated at save time**, resolving the version the same way
declared connections already do (`resolveLatestComponentVersion`). A typo should fail the save, not
produce a workflow that never fires.

### Task parameters

~~`CodeWorkflowContainerFacadeImpl` carries a `// TODO taskDefinition.getParameters()` and the SDK's
`Parameter` is the third empty marker. **Out of scope, deliberately**: a code task's inputs come
from its `perform` reading `context.input()`, so task-level parameters have no consumer today.
Deleting the marker is a bigger conversation than this change needs; leave it and the TODO alone.~~

**Stale as written — task parameters were already wired a day earlier** by the perform-context work
(`2026-08-05-code-perform-context-design.md`), which gave them the consumer this section says they
lack: `context.parameters()`. The TODO and the empty `Parameter` marker are both gone.
`CodeWorkflowContainerFacadeImpl` emits `taskDefinition.getParameters()` into the task's parameters
node first, so the engine evaluates any `${...}` in them against the job context before the perform
action forwards them to `CodeWorkflowTaskContext`. They stay deliberately separate from
`context.input()`: one is the task's configuration, the other is the run's data.

## Work

1. **SDK** (`workflow-api`) — fields on `Input` and `Output`, `parameters(Map)` on
   `ModifiableTriggerDefinition`, DSL builders, and the `output(name).task(...)` form.
2. **Loaders** — both polyglot engines parse `inputs` / `outputs` / `triggers`; the Java paths get
   them through the SDK types. Validation (unknown trigger component, duplicate input name) lives
   here so it fails at save time.
3. **Deploy** — real emission in `getDefinition`, as arrays; trigger component/version resolution.
4. ~~**Client** — the Workflow Inputs dialog goes read-only for code workflows, with a line saying the
   source owns them.~~ **Done**, in the corrected shape above: the declaration locks, the test value
   does not.
5. **Docs, prompts, starters** — the three new members, and the hyphen caveat on output values.

## Risks

- **A code workflow with a `request` or App Event trigger becomes embedded-servable**, which is what
  the bridge's invocable-trigger rule wants — but it also means a source edit can now change whether
  connected users can reach a workflow. Worth a line in the embedded docs.
- **Trigger parameters are evaluated** (`WorkflowTrigger.evaluateParameters`), so a `${...}` in a
  parameter behaves as it does for visual workflows — including rejecting hyphenated references.
- **Existing code workflows are unaffected**: they declare none of the three today, and a source
  that declares nothing still emits nothing.
