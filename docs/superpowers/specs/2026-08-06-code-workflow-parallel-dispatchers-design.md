# Parallel and fork/join tasks in the code workflow SDK

Status: implemented (2026-08-06)
Date: 2026-08-06

## Problem

A code workflow's `WorkflowDsl.workflow(name).tasks(...)` list becomes a flat, strictly sequential
run of `codeWorkflow/v1/perform` nodes. Every task waits for the previous one even when nothing
connects them, so a workflow that fans out to five independent APIs takes the sum of their latencies
instead of the maximum. A visually built workflow does not have this limitation — it can drop a
`parallel/v1` or `fork-join/v1` node — so today code workflows are strictly less capable than the
canvas on the one axis where code is normally the stronger tool.

Doing the fan-out inside a single `perform` is not an answer. Guest tasks run in a strict GraalVM
sandbox: JavaScript has no worker threads there, `allowCreateThread` is off for every language except
Ruby (and that carve-out exists only so TruffleRuby's fiber-backed enumerators work), and a Java task
spawning its own pool would run work the engine cannot see, checkpoint, retry, or account for.
Parallelism has to come from the engine.

## What the engine already offers

Two dispatchers, both already registered and rendered by the canvas:

- `parallel/v1` — `parameters.tasks` is a list of task nodes, all dispatched at once. Flat: each
  entry is one task, not a chain.
- `fork-join/v1` — `parameters.branches` is a list of task lists. Branches run concurrently; the
  tasks inside a branch run in sequence. Branch outputs are keyed `branch_<n>`.

Neither needs a server-side change. The work is entirely in expressing them from the SDK, parsing
them in the loaders, and emitting them in the deploy path.

## Proposed SDK shape

```java
WorkflowDsl.workflow("orders")
    .tasks(
        task("fetch-order")
            .perform(context -> ...),
        parallel("enrich")
            .tasks(
                task("fetch-customer").perform(context -> ...),
                task("fetch-inventory").perform(context -> ...)),
        forkJoin("notify")
            .branches(
                branch(task("post-slack").perform(...), task("record-slack").perform(...)),
                branch(task("send-email").perform(...))),
        task("summarize")
            .perform(context -> context.input("fetch-customer")));
```

```javascript
tasks: [
    {name: "fetch-order", perform: (context) => ...},
    {name: "enrich", type: "parallel", tasks: [
        {name: "fetch-customer", perform: (context) => ...},
        {name: "fetch-inventory", perform: (context) => ...}
    ]},
    {name: "notify", type: "forkJoin", branches: [
        [{name: "post-slack", perform: ...}, {name: "record-slack", perform: ...}],
        [{name: "send-email", perform: ...}]
    ]},
    {name: "summarize", perform: (context) => context.input("fetch-customer")}
]
```

A task entry with no `type` stays what it is today, so every existing source keeps working
unchanged.

## Design decisions

**Composite tasks are containers, not performs.** `parallel`/`forkJoin` entries carry no `perform`
of their own — they name a group. `TaskDefinition` gains a sibling rather than an option: a
`CompositeTaskDefinition` (name, label, description, and either nested tasks or nested branches),
with `WorkflowDefinition.getTasks()` widened to the common supertype. A composite that also declares
a `perform`, or a leaf that declares nested tasks, is rejected at load time — silently ignoring one
half would make a task look like it ran.

**Leaf tasks keep their own names and their own connections.** Each nested leaf still emits a
`codeWorkflow/v1/perform` node carrying its own `taskName`, its own declared `connections`, and its
own `input` formula. Nothing about connection wiring or the job-context snapshot changes; a nested
task simply sees whatever had completed when it was dispatched.

**Names stay flat and must stay unique across the whole workflow.** `CodeWorkflowTaskExecutor`
resolves a perform by `(workflowName, taskName)`, and the job context keys outputs by task name.
Nesting does not namespace either. The loader therefore validates uniqueness across the flattened
task set, including composite names, and the executor's lookup flattens before searching. Scoping
names per branch would be a larger change to how `context.input(name)` reads, for no gain a distinct
name does not already give.

**No nesting.** A parallel inside a parallel, or a fork/join inside a branch, is rejected. The
dispatchers support it, but the failure modes (a branch that fans out again while its siblings are
still running) are not worth the complexity for code workflows. Decided 2026-08-06: not a v1 gap to
close later — a decision to leave it out.

## Work

1. **SDK** (`sdks/backend/java/workflow-api`) — `CompositeTaskDefinition`, `BranchDefinition`,
   `WorkflowDsl.parallel/forkJoin/branch`, widened `WorkflowDefinition.getTasks()`.
2. **Loaders** — `ProjectHandlerPolyglotEngine` and `IntegrationHandlerPolyglotEngine` parse the
   `type` discriminator plus nested `tasks`/`branches`; the Espresso and class-loader Java paths get
   the same through the SDK types. Validation (uniqueness, no double-declaration, no deep nesting)
   lives here so it fails at save time, not at run time.
3. **Deploy** — `CodeWorkflowContainerFacadeImpl.toArrayNode` emits `parallel/v1` /
   `fork-join/v1` nodes whose `parameters.tasks` / `parameters.branches` hold the leaf perform
   nodes.
4. **Execution** — `CodeWorkflowTaskExecutor` flattens composites when resolving a perform by name.
5. **Docs and prompts** — `code-workflows.mdx`, both `prompt_code_workflow_*build.txt`, and a
   starter-template comment.

## Risks

- **A parallel group's tasks cannot read each other.** They are dispatched together, so a sibling's
  output is not in the snapshot and `context.input(siblingName)` throws. Decided (2026-08-06):
  throwing is correct — returning null would let a task compute on a missing value and write a wrong
  output, which is worse than stopping.

  Done: the deploy path records each leaf's concurrent siblings in a `concurrentTaskNames` parameter
  (for a fork/join leaf, the other branches' tasks — its own branch runs in sequence), and the
  failure names the timing rather than the spelling: "Task X runs at the same time as this task, so
  its output is not available; move it before the group to read it here". The Espresso path resolves
  `input(name)` on the host through the bridge so the same message crosses to guest Java. The
  prompts and docs state the restriction too, since it is the one way the feature can surprise
  someone.
- **Editor rendering.** The canvas already lays out both dispatchers, and the code editor shows
  source rather than a graph, so no client work is expected — but the generated definition should be
  opened once in the canvas to confirm the read-only rendering is sane.
- **Test configuration** is keyed per workflow, not per task, so it is unaffected.
