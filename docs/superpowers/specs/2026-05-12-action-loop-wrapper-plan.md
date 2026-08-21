# Action Loop Wrapper — Implementation Notes

Spec: [2026-05-12-action-loop-wrapper-design.md](./2026-05-12-action-loop-wrapper-design.md)

This file captures the as-built implementation. The implementation landed in three files in
`platform-component-api` with no SDK or registry changes.

## Files

| File | Purpose |
| --- | --- |
| `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/loop/LoopActions.java` | `loopable(action)` factory + iterating `PerformFunction` |
| `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/loop/LoopSchemaLifter.java` | Pure schema lifter: scalar → array, broadcast pass-through, output wrap |
| `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/loop/LoopActionsTest.java` | 9 unit tests covering the contract |

No changes to:

- The SDK (`sdks/backend/java/component-api`) — `ActionDefinition` interface untouched
- `ComponentDefinitionRegistry` — looped actions are normal actions registered by the component handler
- `ActionDefinitionService` / `ActionDefinitionFacade` — no fan-out logic at the service layer
- Task dispatchers, completion handlers, workflow executor — entirely unaffected
- `client/` — no UI changes

## What `loopable(action)` produces

A `ModifiableActionDefinition` built via the normal `ComponentDsl.action(name)` chain, with:

- `name`: `wrapped.getName() + "__loop"` (or an explicit override)
- `title`: `wrapped.getTitle() + " (Loop)"` if the original had a title, else the new name
- `description`: synthesized boilerplate naming the wrapped action
- `properties`: result of `LoopSchemaLifter.liftInputProperties(...)` — top-level scalars wrapped in arrays,
  already-arrays passed through
- `output`: if the wrapped action has a static output schema, the schema is wrapped in `array(...)` and a sample
  output (if present) is wrapped in `List.of(sample)`; otherwise no static output
- `perform`: a `LoopPerform` instance (private inner class) that fans out per index

`LoopPerform` captures three things from the factory call:

- The wrapped action's `PerformFunction` (cast-validated at factory time; non-`PerformFunction` variants are
  rejected with `IllegalArgumentException` mentioning the unsupported variant)
- The set of lifted-scalar property names (the only keys that get indexed per iteration)
- The wrapped action's name (for failure messages)

At call time `LoopPerform.apply`:

1. Computes iteration count from lifted-scalar inputs; validates all matching lengths; returns `List.of()` if
   `N == 0`.
2. For each `i`, builds a per-iteration `LinkedHashMap` (preserving the original input order — useful for
   deterministic output ordering in tests/snapshots), wraps with `ParametersFactory.create(map)`, calls
   `wrappedPerform.apply(...)`, collects.
3. Catches any `Exception` and rethrows wrapped in `IllegalStateException` whose message contains the failing
   index, the wrapped action name, and the cause's message. The original is preserved as `getCause()`.

## Verification

- `./gradlew :server:libs:platform:platform-component:platform-component-api:check` → SUCCESS (37 tests pass,
  9 new + 28 existing)
- `./gradlew :sdks:backend:java:component-api:check` → SUCCESS (no SDK changes)
- `./gradlew spotlessApply` → no manual fixups needed
- Downstream consumers compile: `platform-component-service`, `platform-custom-component-loader`

## Test coverage (9 tests, all passing)

| Test | What it pins |
| --- | --- |
| `testLoopableProducesArrayLiftedInputProperties` | Top-level scalar → array; already-array pass-through; required flag propagation |
| `testLoopableLiftsOutputSchemaToArray` | Output schema wrapped in array |
| `testLoopableIteratesScalarInputsAndAggregatesOutputs` | Happy path: `[1,2,3]` × double → `[2,4,6]` |
| `testLoopableEmptyInputsProduceEmptyOutput` | `N == 0` → `List.of()` with no perform calls |
| `testLoopableMismatchedArrayLengthsRejected` | Length mismatch surfaces both property names |
| `testLoopableBroadcastsArrayInputUnchanged` | Already-array property reused per iteration |
| `testLoopableIterationFailureSurfacesIndex` | Iteration-i exception → message contains `"iteration 1"`, cause preserved |
| `testLoopableRejectsActionWithoutPerformFunction` | No-perform actions rejected at factory time |
| `testLoopableWithoutOutputDefinitionLeavesNoStaticSchema` | No-schema actions don't synthesize a schema |

`@BeforeAll setObjectMapper(new ObjectMapper())` is required because `ParametersImpl`'s typed getters delegate
to `MapUtils.objectMapper`, which is static-initialized in production by the Spring context but unset in unit
tests. This matches the existing pattern in `OpenApiClientUtilsTest`.

## Known constraints carried from the spec

- Only `PerformFunction` is supported (no streaming/webhook/resume).
- Sequential per-iteration only.
- Zip semantics only.
- First-failure abort.
- No workflow editor UX changes.
- Shallow lifting: nested object properties keep their inner shape.

## Follow-up issues to file

1. Concurrency policy via `loopable(action, LoopOptions.concurrency(n))`.
2. Cartesian-product mode over multiple lifted-scalar inputs.
3. Partial-success mode returning `{outputs[], errors[]}`.
4. Workflow editor UX: paired badge/array-input affordance for looped variants.
5. Decide whether `.batch(true)` actions should be rejected at factory time (currently silently passes the
   `batch` flag through, which is semantically muddled).
