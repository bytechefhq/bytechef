# Action Loop Wrapper

**Date:** 2026-05-12
**Author:** Ivica Cardic (with Claude)
**Branch:** `0_732`

## Summary

A static factory `LoopActions.loopable(action)` that takes an existing `ActionDefinition` and returns a sibling
looped action. The sibling has the same shape as any other action — input properties, output schema, and a
`perform` function — except that every top-level scalar input is lifted to an array, the output schema is wrapped
in an array, and the `perform` runs the wrapped action's `perform` once per array index.

This mirrors the cluster-element pattern (`action → tool`): a separate definition produced from an existing
action by a static factory, declared explicitly by the component author, registered alongside the original.

## Motivation

ByteChef already has workflow-level iteration via task dispatchers (`each`, `loop`, `map`, `parallel`). All of
them require authoring an iteratee sub-workflow. That ceremony is heavy when the only thing being iterated is a
single action.

The cluster-element tool wrapper shows the pattern that fits better: the wrapper is **another definition**
produced from the original, not a workflow construct. Component authors opt in per action and get a new action
that runs in any normal workflow slot without any executor changes.

## Public API

```java
package com.bytechef.platform.component.loop;

public final class LoopActions {
    public static final String LOOP_SUFFIX = "__loop";

    public static ActionDefinition loopable(ActionDefinition wrapped);
    public static ActionDefinition loopable(ActionDefinition wrapped, String name);
}
```

Usage from a component handler:

```java
import static com.bytechef.platform.component.loop.LoopActions.loopable;

private static final ActionDefinition APPEND_ROW = action("appendRow")
    .properties(string("spreadsheetId"), string("range"), object("values"))
    .output(outputSchema(object().properties(string("updatedRange"))))
    .perform(GoogleSheetsAppendRowAction::perform);

private static final ActionDefinition APPEND_ROW_LOOP = loopable(APPEND_ROW);
// or with custom name:
// private static final ActionDefinition APPEND_ROW_BULK = loopable(APPEND_ROW, "appendRowsBulk");

// register both in the component definition:
component("googleSheets").actions(APPEND_ROW, APPEND_ROW_LOOP);
```

The returned action behaves like any other action: registry lookup, dispatch, executePerform, suspend/resume,
etc. all "just work" because it IS a real action.

## Where the code lives

| Unit | Location |
| --- | --- |
| `LoopActions.loopable(...)` | `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/loop/LoopActions.java` |
| `LoopSchemaLifter` (helper) | `…/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/loop/LoopSchemaLifter.java` |
| Tests | `…/platform-component-api/src/test/java/com/bytechef/platform/component/loop/LoopActionsTest.java` |

`platform-component-api` — same layer as `ParametersFactory`, which the iterating perform uses to build
per-iteration `Parameters`. Component authors already import platform-side helpers when declaring cluster
elements (e.g. `ScriptPythonTool` imports `com.bytechef.platform.ai.tool.constant.ToolConstants`), so the layer
is consistent with existing patterns.

The SDK (`component-api`) is left untouched — no new `ActionDefinition` interface methods, no flags. The
synthesized action is a normal `ModifiableActionDefinition` built by composing the existing DSL.

## Schema lifting rules

| Original | Loop variant |
| --- | --- |
| `string("spreadsheetId").required(true)` | `array("spreadsheetId").items(string()).required(true)` |
| `integer("count")` | `array("count").items(integer())` |
| `object("values").properties(...)` | `array("values").items(object().properties(...))` |
| `array("rows").items(...)` (already array) | unchanged — **broadcast** per iteration |
| `output: object(...)` | `output: array(object(...))` |
| `output: sampleOutput(x)` | `output: sampleOutput([x])` |
| no output | no output (List returned at runtime, no static schema) |

Rules:
1. **Lift**: every top-level *scalar* property `p: T` becomes `p: array(T)`. Top-level constraints
   (`required`, `label`, `description`) are copied to the lifted array property. The original property becomes
   the array's `items` template.
2. **Broadcast**: top-level properties that are already `ARRAY` pass through unchanged. A broadcast value is
   reused for every iteration. Rationale: lifting `T[]` to `T[][]` would be confusing; broadcast covers the
   common case of "the same list of allowed values for every row."
3. **Output**: the entire output schema (when present) is wrapped in `array(...)`. Sample output is wrapped in
   a single-element list. Actions without a static output schema produce no static schema on the loop variant —
   the runtime output is still a `List`, just without a schema.
4. **Lifting is shallow**: nested properties inside an object pass through unchanged. The analogy is "run this
   action N times — each call gets one element of every lifted-scalar argument."

## Execution semantics

The synthesized `perform` (`LoopActions.LoopPerform`):

1. Reads the input `Parameters` (with array values for every lifted-scalar input).
2. Determines iteration count `N` from the lifted-scalar inputs. All non-null lifted-scalar inputs must agree
   on length (zip semantics). Mismatched lengths throw `IllegalArgumentException` naming both keys.
3. If `N == 0`, returns `List.of()` — no `perform` invocations.
4. For each `i ∈ [0, N)`:
   - Builds a per-iteration `Map<String, Object>` where each scalar key maps to its `i`-th element and each
     broadcast key maps to its full value.
   - Wraps it via `ParametersFactory.create(map)` to produce a real `Parameters` (so the wrapped action's typed
     getters like `getString`/`getInteger` work normally).
   - Invokes `wrappedPerform.apply(perIteration, connectionParameters, context)`.
   - Wraps any thrown exception with `IllegalStateException("Loop iteration <i> of '<name>' failed: ...", cause)`.
5. Returns the aggregated `List<Object>`.

Connection and context are passed through unchanged — each iteration sees the same connection.

## Constraints / non-goals (MVP)

- Only `PerformFunction` is supported. Streaming, webhook-response, and resume perform variants throw at
  factory time with a clear message.
- Sequential per-iteration only. Concurrency policy can be added later via an overload (`loopable(action, opts)`).
- Zip semantics only — no cartesian product over multiple array inputs. (Could be a `LoopOptions.cartesian(true)`
  later.)
- First failure aborts. No partial-success / collect-errors mode in MVP.
- No workflow-editor / palette UI changes.
- No EE remote-client mirroring needed (the factory produces a plain `ActionDefinition` that crosses RPC
  boundaries via the normal definition serialization).

## Why not a task dispatcher

A task-dispatcher loop would:
- Spawn `N` `TaskExecution` rows per call (storage + counter cost).
- Require a completion handler to aggregate outputs (more code paths, more race surface).
- Force the user to model iteration in workflow JSON (the heavy ceremony we're trying to remove).
- Require Atlas-level changes to suspend/resume semantics if the wrapped action suspends.

Putting iteration *inside* the synthesized `perform` keeps the executor and dispatchers untouched. The looped
action is a normal action from every other layer's perspective.

## Why not a flag on `ActionDefinition` + registry synthesis

We considered `.loopable(true)` on the SDK builder + auto-synthesizing a `<name>__loop` variant in the
component registry. Rejected because:
- It puts the loop logic at a layer (component registry) that has no good reason to know about iteration.
- It makes the "do I exist?" question for a loop variant implicit (synthesized on demand vs. authored).
- It requires teaching every layer that fans out actions (introspection, EE remote clients, agentic tools)
  about the implicit variant.

The factory approach makes the loop variant an explicit, authored object. The component handler returns both
actions from `getDefinition().getActions()` and every layer sees them identically.

## Open questions (post-MVP, captured for follow-up)

- Concurrency: should we accept `loopable(action, LoopOptions.concurrency(5))`?
- Cartesian product over multiple lifted-scalar inputs.
- Partial-success mode returning `{outputs[], errors[]}`.
- Workflow editor UX: paired badge/UI for entering array values when the looped variant is selected.
- Make `loopable(action)` reject `.batch(true)` actions explicitly (batch + loop are semantically muddled).
