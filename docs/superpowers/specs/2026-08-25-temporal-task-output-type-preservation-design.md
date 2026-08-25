# Task-output value type preservation

**Status:** design only — nothing implemented
**Edition:** Community
**Ticket:** follow-up to #5575
**Date:** 2026-08-25

## Problem

A component that returns a genuine date returns a `String` to every expression that reads it.

The Oracle SQL query action reads columns with `rs.getObject(i)`
(`server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/jdbc/operation/QueryJdbcOperation.java:50`),
so an Oracle `DATE` column arrives as a `java.sql.Timestamp`. That object survives the worker
untouched — `AbstractTaskHandler` returns `ActionDefinitionFacade.executePerform`'s value directly,
`ActionDefinitionServiceImpl` applies no output coercion, and the `TaskExecutionPostOutputProcessor`
chain handles webhook/SSE/suspend concerns only
(`server/libs/atlas/atlas-worker/atlas-worker-impl/src/main/java/com/bytechef/atlas/worker/TaskWorker.java:257-268`).

The type is lost at storage. `TaskFileStorageImpl.storeTaskExecutionOutput` writes with plain
`JsonUtils.write(output)` and no type information
(`server/libs/atlas/atlas-file-storage/atlas-file-storage-impl/src/main/java/com/bytechef/atlas/file/storage/TaskFileStorageImpl.java:103`),
producing:

```json
[{"APPLYDATE":"2026-08-26T00:00:00.000Z"}]
```

`readTaskExecutionOutput` reads it back as `JsonUtils.read(json, Object.class)`
(`TaskFileStorageImpl.java:74`), and `readContextValue` as `Map<String, ?>`
(`TaskFileStorageImpl.java:60`). With `Object` as the target type Jackson's untyped binding can only
produce `String`, `Number`, `Boolean`, `List`, `Map`. `DefaultTaskCompletionHandler` then puts that
untyped value into the job context and re-serializes the whole context
(`server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/task/completion/DefaultTaskCompletionHandler.java:119-137`),
so every task output crosses JSON at least twice before an expression sees it.

The user-visible consequence, from the reported workflow:

```
=${oracleSql_1[0].APPLYDATE} > parseDate(${liferay_2.latestModifyDate}, "yyyy-MM-dd'T'HH:mm:ssX")
```

`parseDate` with two arguments returns a `ZonedDateTime`
(`server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/Parse.java:49`),
the left operand is a `String`, and SpEL raises
`EL1013E: Cannot compare instances of class java.lang.String and class java.time.ZonedDateTime`
(verified). The workflow author is forced to call `parseDate` on a value that was never a string,
guessing a format pattern that is an artifact of Jackson's serialization rather than of their data.

**End state this design aims at:** a value keeps the type the task returned it as — temporal or
numeric — when a later expression reads it, in raw expressions and in the condition builder alike. A
value that was always a string stays a string.

## Why a type tag is unavoidable

Oracle's `2026-08-26T00:00:00.000Z` (a serialized `Timestamp`) and Liferay's
`2026-08-24T22:00:00Z` (a genuine JSON string from the REST API) are indistinguishable by shape.
Heuristic rehydration — "parse anything that looks like ISO-8601" — cannot separate them, and would
convert the Liferay string too, breaking the legitimate `parseDate` call on it. Only recorded type
information can make the distinction, and that distinction is exactly the requirement.

## Design

### Boundary

All reads and writes of task outputs and context values go through `TaskFileStorage` — 69 call sites
across roughly 30 consumers, with two implementations: `TaskFileStorageImpl` (durable) and
`InMemoryTaskFileStorage` (sync jobs, which delegates to the durable one). The change is confined to
that codec; no caller changes its call.

### Encoding

Temporal values serialize as a tagged object rather than a bare string:

```json
{"@bytechefType": "ZONED_DATE_TIME", "@bytechefValue": "2026-08-26T00:00:00Z"}
```

Tagged types: `java.util.Date` and its `java.sql` subclasses (`Date`, `Time`, `Timestamp`),
`java.time.Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`,
`ZonedDateTime`.

Deserialization is defensive: a JSON object is treated as a tag only when it has exactly those two
keys, `@bytechefType` names a known type, and `@bytechefValue` parses. Anything else is data.

### What comes back

Reconstruction is by semantics, not by declared class. A type is *instant-bearing* when it fixes a
point on the timeline — it carries both a date and a zone or offset:

| Stored type | Reconstructed as |
|---|---|
| `java.util.Date`, `java.sql.Timestamp`, `Instant`, `OffsetDateTime`, `ZonedDateTime` | `ZonedDateTime` at UTC |
| `java.sql.Date`, `LocalDate` | `LocalDate` |
| `java.sql.Time`, `LocalTime` | `LocalTime` |
| `LocalDateTime` | `LocalDateTime` |
| `OffsetTime` | `OffsetTime` |

`java.sql.Date` and `java.sql.Time` extend `java.util.Date` but represent a date-only and a
time-only value respectively, so they map to `LocalDate` and `LocalTime` rather than being swept
into the instant-bearing row with their superclass.

Instant-bearing types reconstruct as **`ZonedDateTime` at UTC**, not as their original class.

This is the load-bearing decision. Preserving the exact class would not fix the reported bug: a
`Timestamp` is `Comparable<Date>` only, so `Timestamp > ZonedDateTime` raises a `ClassCastException`
rather than `EL1013E` — the failure moves rather than disappears. The same applies to `Instant`,
which is `Comparable<Instant>` only; `Instant.compareTo(ZonedDateTime)` throws
`ClassCastException: class java.time.ZonedDateTime cannot be cast to class java.time.Instant`
(verified). `ZonedDateTime` is `Comparable<ChronoZonedDateTime<?>>`, so two of them compare
correctly, and `parseDate(x, pattern)` already returns one. Normalizing to `ZonedDateTime` is what
makes the reported expression evaluate.

UTC is the reconstruction zone because the platform already serializes every task output to UTC, so
it is the existing implicit convention, and unlike the JVM default zone it is identical across dev,
staging and production.

Everything not instant-bearing round-trips as itself. Those values genuinely fix no point on the
timeline, and inventing a zone or a date inside the storage layer would assert something the data
does not say.

**Known limitation, accepted:** a component returning a `LocalDateTime` still will not compare
against a `parseDate` result, because `LocalDateTime` and `ZonedDateTime` are not mutually
`Comparable`. The workaround is the existing `atZone` function. This is documented rather than
silently papered over; closing it belongs to a comparison-layer change, not to storage.

### The synchronous cache path

`InMemoryTaskFileStorage` is a read-through cache over the durable storage that caches the **original
object** on write and returns it on read
(`server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/file/storage/InMemoryTaskFileStorage.java:96-108,140-147`).
Synchronous executions — webhook sync, subflow sync, MCP sync, and workflow **test mode** — therefore
never cross JSON at all, and never lose the type in the first place.

They are broken today for a different reason: a `java.sql.Timestamp` is `Comparable<Date>` only, so
`Timestamp > ZonedDateTime` fails with the same `EL1013E` the string case produces. The symptom is
identical, the cause is not.

The consequence for this design is that tagging alone would make the two paths disagree — async
would yield a `ZonedDateTime` while sync yielded a `Timestamp`, so a workflow could behave one way
under the editor's Test button and another way in production. That is a worse failure than the one
being fixed, because it is intermittent by deployment mode rather than reproducible.

The codec therefore exposes normalization as an operation in its own right, applied on both paths:
the durable path normalizes while untagging, and `InMemoryTaskFileStorage` normalizes the value it
caches. Sync and async then hand expressions the same type for the same data, which is the property
the sample flows assert.

### `parseDate` / `parseDateTime` passthrough

`Parse.execute` casts its first argument with `(String) arguments[0]`
(`Parse.java:49,51,56,58`). Once the Oracle value is a real `ZonedDateTime`, an existing workflow
written against the string form — `parseDate(${oracleSql_1[0].APPLYDATE}, "yyyy-MM-dd'T'HH:mm:ss.SSSX")`,
the documented workaround for #5575 — would throw `ClassCastException`.

`Parse` therefore accepts an already-temporal argument and converts it to the requested target type
instead of casting. This keeps every workflow written against the old behaviour working.

### Backward compatibility

Untagged JSON reads exactly as it does today: strings stay strings. Task outputs already on disk,
job contexts already stored, and jobs in flight across an upgrade are unaffected. There is no
migration and no dual-write window.

## Numeric values

The same erasure destroys numeric types, and for one of them it destroys data rather than just type
information. Measured through the real storage path:

| Stored | Read back | `.equals()` |
|---|---|---|
| `BigDecimal("7.5345")` | `Double(7.5345)` | false |
| `BigDecimal("0.1")` | `Double(0.1)` | false |
| `Long(42)` | `Integer(42)` | false |
| `Float(1.1f)` | `Double(1.1)` | false |
| `Long(9007199254740993)` | `Long(...)` | true |

`BigDecimal` is the serious one. Oracle `NUMBER` columns arrive from `rs.getObject` as `BigDecimal`,
and once one becomes a `Double` its exactness is gone: arithmetic drifts and comparisons follow it.
Measured in the evaluator, `(0.1d + 0.2d)` compares as **greater than** `BigDecimal("0.3")`.

**What is NOT broken, contrary to an early reading of this:** SpEL compares numbers numerically, not
by `.equals()`. `Long(42) == Integer(42)` and `BigDecimal("0.1") == Double(0.1)` both evaluate true
in a workflow expression. Box-type drift therefore does not corrupt workflow comparisons. It still
matters for Java `.equals()` — map keys, `contains`, data-table lookups — which is why the decision
below covers integral types too rather than only the lossy ones.

`BigDecimal`, `BigInteger`, `Long`, `Float`, `Short` and `Byte` are tagged, so each round-trips as
exactly the type the component returned. `Integer`, `Double` and `Boolean` are left alone: they
already round-trip as themselves, and tagging them would grow every payload for nothing.

Numbers are tagged but NOT normalized. Normalization exists to make temporal values mutually
comparable; numbers already are, so the sync cache path and `Parse` are untouched by this. Both
paths still end up handing expressions the exact type the component produced.

### Codec shape

`tag`/`untag` move out of `TemporalValueUtils` into their own class in the same package, leaving
`TemporalValueUtils.normalize` as the temporal-only operation that `InMemoryTaskFileStorage` and
`Parse` consume. A class named for temporal values that also encodes numbers would misdescribe
itself, and the two concerns have different consumers.

### Consequence for the consumer audit

The audit that cleared the temporal change does not cover this one. Tagging changes what consumers
receive in the opposite direction: a `Long` that previously arrived as `Integer` now arrives as
`Long`, and a `BigDecimal` that arrived as `Double` now arrives as `BigDecimal`. Any consumer
casting to `Integer` or `Double`, or calling a `Double`-specific method, breaks where it did not
before. The audit is re-run against numeric types as its own task.

## The structured `dateTime` condition operand

The same requirement is broken a second time, on a different code path, and the two cannot be fixed
independently.

`ConditionTaskUtils.getConditionExpressions` parses `dateTime` operands with `LocalDateTime.parse`
(`server/libs/modules/task-dispatchers/condition/src/main/java/com/bytechef/task/dispatcher/condition/util/ConditionTaskUtils.java:169-170`),
which throws on every offset-bearing value the platform emits — verified:
`LocalDateTime.parse("2026-08-26T00:00:00.000Z")` and `LocalDateTime.parse("2026-08-24T22:00:00Z")`
both raise `DateTimeParseException`. So the editor's **Date Time → After/Before** condition already
fails for any date that came out of a task output, with or without the storage change.

**Why it belongs in this change rather than a follow-up.** The operand is read with
`MapUtils.getString(condition, VALUE_1, "")` (`ConditionTaskUtils.java:163-164`), which coerces
whatever it is given to a `String`. Once the storage fix lands, `${oracleSql_1[0].APPLYDATE}`
evaluates to a `ZonedDateTime` and `getString` immediately flattens it straight back to text. The
storage fix alone therefore leaves this path exactly as broken as it is today, while making the
flattening harder to see. Shipping one without the other produces a platform where the same date
works in a raw expression and fails in the condition builder.

### Fix

Read the operand untyped with `MapUtils.get(condition, VALUE_1)`
(`server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/MapUtils.java:107`)
and normalize both operands to `ZonedDateTime` at UTC through a single parser:

- already a `ZonedDateTime` — used as is
- another instant-bearing temporal — converted per the reconstruction table above
- a `String` — parsed leniently: offset-bearing forms keep their own offset; zone-less date-times
  and date-only values are read as UTC, per the zone rule this design already adopts
- anything else, or a string that does not parse — fails naming the operand (`value1` / `value2`)
  and showing the offending value, rather than surfacing a bare `DateTimeParseException`

The two SpEL templates are unchanged — `${value1}.isAfter(${value2})` and the `isBefore`
counterpart — because `ZonedDateTime` has both methods and they remain instance methods. The
`SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods()` context and the #5081
injection hardening are untouched.

### No regression for existing conditions

Today two zone-less datepicker operands compare as `LocalDateTime`. Afterwards both become
`ZonedDateTime` at the same UTC offset, so the ordering between them is unchanged — the shift is
identical on both sides and cancels. Every condition that passes today still passes. What changes is
that inputs which previously threw now evaluate.

## Consumer audit

Roughly 30 classes read through `readTaskExecutionOutput` / `readContextValue` and may now receive a
`ZonedDateTime` where they previously saw a `String`. The implementation plan carries an explicit
per-consumer check rather than an assumption. Expected outcomes:

- **Evaluation context** (`DefaultTaskCompletionHandler`, the task dispatchers) — receives the
  richer type. This is the point of the change.
- **HTTP/DTO paths** (`ProjectWorkflowExecutionFacadeImpl`, `IntegrationWorkflowExecutionFacadeImpl`,
  `SseStreamApplicationEventListener`) and **tool-result paths** (`AutomationMcpToolFacade`,
  `AutomationA2AServerFacade`, `EmbeddedMcpToolFacade`) hand the value to Jackson, which renders
  `ZonedDateTime` as an ISO-8601 string, so nothing crashes and no consumer needs a code change. But
  the rendered text itself is not always the same text a client received before this branch — see
  "The client payload does change" below, which replaces an earlier, false claim on this point that
  the change is invisible to API clients.
- **Anything casting to `String` or calling `String` methods on a context value** — must be found
  and fixed. This is the real risk the audit exists to surface.

### The client payload does change

An earlier version of this document claimed the change is invisible to API clients. That is false.
Measured directly at the storage boundary — store BEFORE this branch (`JsonUtils.write`, no codec)
versus AFTER (`ValueTagUtils.tag` on write, `ValueTagUtils.untag` on read), each side then
re-serialized with `JsonUtils.write` the way an HTTP/DTO or tool-result path would render it to a
client:

| Value | Before | After |
|---|---|---|
| `Timestamp`, whole second (`2026-08-26T00:00:00Z`) | `"2026-08-26T00:00:00.000Z"` | `"2026-08-26T00:00:00Z"` |
| `Timestamp`, nanosecond precision (`...123456789Z`) | `"2026-08-26T00:00:00.123Z"` | `"2026-08-26T00:00:00.123456789Z"` |
| `java.sql.Date` (`2026-08-26`) | `"2026-08-25T22:00:00.000Z"` | `"2026-08-26"` |
| `BigDecimal("7.5345")` | `7.5345` | `7.5345` |
| `Long(42)` | `42` | `42` |
| Plain ISO-8601 `String` | `"2026-08-24T22:00:00Z"` | `"2026-08-24T22:00:00Z"` |

Two of these are not cosmetic:

- **Every instant-bearing temporal changes its rendered precision.** A whole-second `Timestamp`
  drops its trailing `.000`; a nanosecond-precision `Timestamp` — previously truncated to
  milliseconds by Jackson's default `java.util.Date` serialization, which is itself a silent loss of
  precision — now renders with all nine fractional digits it actually carries. A client parsing that
  field with a fixed-width format string, or comparing the raw text for equality, sees different
  output than before.
- **`java.sql.Date` changes shape entirely**, from an ISO instant string shifted by the server's
  default JVM time zone (`2026-08-25T22:00:00.000Z` for a value that is `2026-08-26` local, in a
  server running two hours ahead of UTC) to a bare date string. This is arguably a correctness fix —
  the old value was wrong, asserting a time-of-day and an offset the data never had — but it is
  nonetheless a breaking payload change: a running workflow that pipes such a value into an outbound
  HTTP request, or a client that parses the field as a full timestamp, receives a different string
  after upgrade, not merely a differently-typed one.

Numeric and plain-string values, by contrast, measured identical before and after in this pass: the
`BigDecimal`/`Long` cases render the same digits either way, because the same digits round-trip
through `Double`/`Integer` in this range. That is not a general guarantee for every numeric value —
only what the consumer audit for numeric types (a separate task, see "Consequence for the consumer
audit" below) is responsible for checking.

## Testing

### Unit

- **Codec round-trip** — one case per tagged type; nested inside `List` and `Map`; a `null` value;
  a map that merely resembles a tag (`@bytechefType` with an unknown name, or with extra keys) is
  read back as plain data.
- **Legacy read** — untagged JSON produced by the current writer reads back exactly as today, with
  ISO-8601 strings remaining strings.
- **Reconstruction targets** — `Timestamp`, `Date`, `Instant`, `OffsetDateTime` and `ZonedDateTime`
  all return `ZonedDateTime` at UTC and compare correctly against each other and against a
  `parseDate` result; `LocalDate`, `LocalDateTime` and `LocalTime` return their own types.
- **`dateTime` operand parser** — offset-bearing strings with and without milliseconds; a
  zone-less date-time; a date-only value; an operand that is already a `ZonedDateTime`; an empty
  string and an unparseable string, both of which must fail naming the operand and the value.
- **Existing `dateTime` conditions** — the current `ConditionTaskUtilsTest` cases keep passing
  unchanged, pinning the no-regression claim above.
- **`Parse` passthrough** — `parseDate` / `parseDateTime` given a `ZonedDateTime`, `Instant` or
  `Timestamp` return the requested type rather than throwing `ClassCastException`; given a `String`
  they behave exactly as before.

### Integration — sample flows

Both run on the real atlas engine through `TaskDispatcherJobTestExecutor`, so the JSON round-trip
that erases the type actually happens. A test-only task handler returning a `java.sql.Timestamp`
stands in for the Oracle SQL action, keeping the fixture free of a database.

- **`condition_v1-temporalOutput-noParseDate.yaml`** — the reported workflow. A task returns a
  `Timestamp`; the condition compares it against `parseDate` of an ISO-8601 **string** with no
  `parseDate` on the temporal side:

  ```
  =${dbDate} > parseDate(${restDate}, "yyyy-MM-dd'T'HH:mm:ssX")
  ```

  Asserted on both branches — a `dbDate` after `restDate` takes `caseTrue`, one before it takes
  `caseFalse` — so the test pins the comparison's direction, not merely that it stopped throwing.

- **`condition_v1-temporalOutput-stringStaysString.yaml`** — the regression guard for the other half
  of the requirement. A task returns an ISO-8601 **string**; the condition compares it with
  `parseDate` on both sides. It must keep working, proving the tag did not leak into values that
  were always strings.

- **`condition_v1-dateTime-temporalOutput.yaml`** — the structured path. A `dateTime` condition
  using the **After** operation whose two operands come from prior task outputs: one task returning
  a `Timestamp`, one returning an ISO-8601 string. This is the editor's condition builder rather
  than a raw expression, and it must work without the author touching a format pattern. Asserted on
  both branches.

The measured payload change from "The client payload does change" above is pinned at the codec
instead of through a flow, because the assertion concerns serialization rather than dispatch: a
stored `Timestamp` and a stored `java.sql.Date` are read back and re-serialized, and the resulting
JSON text is asserted against its actual post-change form — not against what a client received
before this branch, which for these two cases it no longer matches. Running that through a workflow
would add dispatch machinery without strengthening the assertion.
`TaskFileStorageSerializationTest.testSerializedOutputNeverLeaksTheValueTag` is that pin.

## Where evaluation happens, and what this covers

Expressions are evaluated in more than one place, so it is worth stating exactly which of them this
design reaches.

- **Coordinator.** `JobExecutor.executeNextTask` calls `TaskExecution.evaluate` against the job
  context read through `TaskFileStorage.readContextValue`
  (`server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/job/JobExecutor.java:144`).
  This is where `${someTask.field}` resolves against another task's output, and it is what this
  design fixes.
- **Task dispatchers** — condition, loop, each, fork-join and the rest — evaluate coordinator-side
  through the same context. Also covered.
- **Worker.** `TaskWorker` evaluates too
  (`server/libs/atlas/atlas-worker/atlas-worker-impl/src/main/java/com/bytechef/atlas/worker/TaskWorker.java:253,296,326`),
  but against a **fresh in-memory context**: `new HashMap<>()` populated solely by the outputs of
  `pre` / `post` / `finalize` sub-tasks, held as live Java objects and never serialized. Nothing was
  ever erased there, so there is nothing to fix.

### Known gap: evaluated parameters crossing the message broker

Once the coordinator has substituted `${...}`, the resolved parameters travel to the worker inside
the `TaskExecution` message. The memory broker used in mono deployments passes objects in-JVM, so
types survive. The distributed brokers do not: AMQP converts through `jacksonAmqpMessageConverter`
and Redis through `RedisMessageSerializer`, both Jackson. A `ZonedDateTime` the coordinator resolved
therefore reaches a distributed worker as a `String` again.

This is deliberately not addressed here. Evaluation has already happened by that point, and
components read parameters through converting accessors (`MapUtils.get(map, key, Class)` runs Jackson
`convertValue`), so a component that asks for a date still receives one. It would matter to a
component that inspects a parameter's runtime type rather than requesting a conversion. Closing it
means applying the same codec at the broker boundary, which is a different change with a different
blast radius.

## Known limitations

Two gaps in this branch's scope were accepted deliberately rather than discovered late, but the
documentation that recorded the decision was never written until this correction.

### `Parse`'s date-only return is not "the requested target type"

Earlier in this document, `Parse` is described as converting its argument "to the requested target
type." That is not literally true for a date-only input. `Parse.execute`
(`server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/Parse.java:60-63`)
handles a normalized `LocalDate` argument like this:

```java
if (argument instanceof LocalDate localDate) {
    return new TypedValue(
        type == Type.DATE ? localDate : localDate.atStartOfDay());
}
```

`parseDate(LocalDate)` returns the `LocalDate` unchanged — not a `ZonedDateTime`, even though
`parseDate` with a string argument and no format pattern returns a `ZonedDateTime`-comparable type
in every other branch of this method. This was an accepted decision: converting a date-only value to
a `ZonedDateTime` would invent a midnight and a UTC offset the data never asserted, exactly the kind
of fabrication this design otherwise refuses to do for `java.sql.Date`. The consequence is that a
workflow calling `parseDate` on a value that started as a `LocalDate` gets a `LocalDate` back, and
must not assume it is comparable to a `ZonedDateTime`-returning call elsewhere in the same
expression.

`parseDateTime(LocalDate)`, on the same lines, does the opposite — it calls `localDate.atStartOfDay()`
and returns a `LocalDateTime`. That is defensible on its own terms, because `parseDateTime`'s
contract is to return a `LocalDateTime`, and a `LocalDate` has no time component to preserve as-is.
But it means the rationale above — "don't invent a midnight the data never asserted" — does not
apply uniformly across both methods: `parseDate` refuses to invent one, `parseDateTime` invents one
by design. A workflow author moving a date-only value between the two functions sees different
fabrication behavior depending on which one they call, not a single consistent rule.

### The typed-key branch dispatcher can silently take the wrong branch

`BranchTaskDispatcher.resolveCase`
(`server/libs/modules/task-dispatchers/branch/src/main/java/com/bytechef/task/dispatcher/branch/BranchTaskDispatcher.java:164`)
and `BranchTaskCompletionHandler`
(`server/libs/modules/task-dispatchers/branch/src/main/java/com/bytechef/task/dispatcher/branch/completion/BranchTaskCompletionHandler.java:181`)
both select a case with `key.equals(expression)`, comparing the case's evaluated `key` against the
branch's evaluated `expression` by Java `.equals()`, not by numeric value.

Before this branch, a component returning `Long(42)` arrived at this comparison as `Integer(42)`
after the untyped-JSON round-trip erased its type. After this branch, tagging preserves it as
`Long(42)`. `Integer(42).equals(Long(42))` is `false` in both directions — `Integer.equals` and
`Long.equals` both check the argument's runtime class before comparing value — so a branch case
whose key was authored as a bare numeric literal that used to match a component's `Long` output by
accident (both having collapsed to `Integer`) no longer matches, and evaluation silently falls
through to the `default` case instead.

Both `KEY` and `EXPRESSION` are declared `string(...)` in
`BranchTaskDispatcherDefinitionFactory` (lines 60 and 74), so a workflow authored through the editor
is unaffected — the editor always produces string case keys, and `evaluator.evaluate` on a
`string(...)`-typed parameter yields a `String`, which continues to compare correctly. The exposure
is a hand-authored or copilot-generated YAML workflow carrying a numeric literal as a case key
(`key: 42` rather than `key: "42"`), where the YAML parser produces a boxed `Long` or `Integer`
directly, bypassing the string coercion the editor would have applied.

This is documented as a known limitation, not fixed in this branch: fixing it changes `Branch`'s
comparison semantics, which is production behavior out of scope for this documentation-only wave. The
failure mode to be clear about is not an exception or a validation error — it is **taking the wrong
branch silently**, because an unmatched case falls through to `default` rather than failing loudly.

## Out of scope

- **Comparison-layer coercion.** A custom SpEL `TypeComparator` that parses ISO-8601 strings during
  comparison would make the reported expression work without touching storage, but leaves the value
  a `String` — `plusDays(${oracleSql_1[0].APPLYDATE}, 1)` would still fail — and changes `>`, `<`,
  `>=`, `<=` for every formula in the product. Rejected in favour of fixing the type itself.
- **Typed JSON generally.** Only temporal values are tagged. Extending polymorphic typing to
  arbitrary objects is a much larger change with no current demand.
