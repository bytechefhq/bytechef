# Workflow Node Disable/Enable Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Users can disable individual workflow nodes (top-level and nested) from the editor; disabled nodes are skipped at execution with their names resolving to null downstream.

**Architecture:** A first-class `disabled` boolean on the task definition JSON; one generic recursive strip (`WorkflowTaskUtils.removeDisabledTasks`) applied where the coordinator materializes tasks for execution; job context seeded with `name → null` for skipped names; a null-propagating SpEL property accessor so `${disabledTask.field}` resolves null; editor toggle in the node three-dots menu with muted rendering and advisory reference warnings.

**Tech Stack:** Java 25 / Spring Boot (atlas engine, SpEL evaluator, MapStruct, openapi-generator), React 19 + TypeScript (workflow editor, Zustand, Vitest).

**Spec:** `docs/superpowers/specs/2026-08-24-workflow-node-disable-design.md`

## Global Constraints

- Absent flag = enabled; only `"disabled": true` is ever serialized. Existing workflows must stay byte-identical.
- No changes under `server/libs/atlas/` beyond engine-core skip semantics (this feature IS engine-core; notifications/approvals rules are unaffected).
- Java style: blank line before control statements; blank line after variable modification before use; no trailing blank line in class body; no `_` prefixes; descriptive variable names.
- Client style: ESLint sort-keys (manual fix), interface names end `I`/`Props`, lucide icons imported with `Icon` suffix, `twMerge` (never `cn()`), hook ordering (useState → useRef → stores → custom hooks → derived → useEffect → return).
- Test naming: camelCase without underscores; unit classes end `Test`, integration classes end `IntTest`.
- Gradle verification: redirect output to a file, check `$?` on its own line, grep the file for `^> Task .* FAILED`. Never judge a piped Gradle run.
- Commit messages: server `<description>`, client `client - <description>` (no ticket number for this branch), each ending with the Claude co-author trailer.

---

### Task 1: `disabled` field on WorkflowTask (server)

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/constant/WorkflowConstants.java`
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/domain/WorkflowTask.java`
- Test: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/domain/WorkflowTaskTest.java` (create if absent; extend if present)

**Interfaces:**
- Consumes: nothing new.
- Produces: `WorkflowConstants.DISABLED` (= `"disabled"`); `boolean WorkflowTask#isDisabled()`; `WorkflowTask.toMap()` contains key `"disabled"` → `true` only when disabled.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.atlas.configuration.domain;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class WorkflowTaskTest {

    @Test
    public void testDisabledParsedFromSource() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "task1", "type", "component/v1/action", "disabled", true));

        Assertions.assertTrue(workflowTask.isDisabled());
        Assertions.assertEquals(Boolean.TRUE, workflowTask.toMap()
            .get("disabled"));
    }

    @Test
    public void testDisabledDefaultsToFalseAndIsOmittedFromMap() {
        WorkflowTask workflowTask = new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action"));

        Assertions.assertFalse(workflowTask.isDisabled());
        Assertions.assertFalse(workflowTask.toMap()
            .containsKey("disabled"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.domain.WorkflowTaskTest" > /tmp/t1.log 2>&1; echo EXIT=$?; grep -c "isDisabled" /tmp/t1.log
```
Expected: compilation failure — `isDisabled` not defined.

- [ ] **Step 3: Implement**

In `WorkflowConstants.java` (alphabetical order, after `DESCRIPTION`):

```java
public static final String DISABLED = "disabled";
```

Also add `DISABLED` to the `WORKFLOW_DEFINITION_CONSTANTS` list, after `DESCRIPTION`.

In `WorkflowTask.java`:
1. Field, placed after `description` (match existing field grouping): `private boolean disabled;`
2. Parse branch in the map constructor, after the `DESCRIPTION` branch:

```java
} else if (WorkflowConstants.DISABLED.equals(entry.getKey())) {
    this.disabled = MapUtils.getBoolean(source, WorkflowConstants.DISABLED, false);
```

3. Getter (near `getDescription()`):

```java
public boolean isDisabled() {
    return disabled;
}
```

4. In `toMap()`, after the `description` block:

```java
if (disabled) {
    map.put(WorkflowConstants.DISABLED, true);
}
```

5. Add `", disabled=" + disabled +` to `toString()`.
6. Do NOT touch `equals`/`hashCode` — they deliberately exclude presentation-adjacent attributes (`description`, `metadata`); follow that precedent.

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: EXIT=0, BUILD SUCCESSFUL in the log.

- [ ] **Step 5: Commit**

```bash
git add server/libs/atlas/atlas-configuration
git commit -m "Add disabled flag to WorkflowTask definition model

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: Recursive strip + disabled-name collection utilities

**Files:**
- Modify: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/main/java/com/bytechef/atlas/configuration/util/WorkflowTaskUtils.java`
- Test: `server/libs/atlas/atlas-configuration/atlas-configuration-api/src/test/java/com/bytechef/atlas/configuration/util/WorkflowTaskUtilsTest.java` (exists — extend)

**Interfaces:**
- Consumes: `WorkflowTask#isDisabled()`, `WorkflowTask#toMap()`, existing private `isWorkflowTaskMap(Map)` (structural task detection).
- Produces:
  - `public static List<WorkflowTask> removeDisabledTasks(List<WorkflowTask> workflowTasks)` — disabled tasks removed at ALL depths (top level, nested parameter lists like `caseTrue`/`iteratee`, `cases[].tasks`, fork/join list-of-lists, single-map subtasks like each's `iteratee`, plus `pre`/`post`/`finalize`); surviving tasks rebuilt only when something beneath them changed.
  - `public static List<String> getDisabledTaskNames(List<WorkflowTask> workflowTasks)` — names of every removed task, including all descendants of a disabled dispatcher.

- [ ] **Step 1: Write the failing tests** (add to `WorkflowTaskUtilsTest`; reuse the test's existing task-map helpers if present, otherwise use inline `Map.of`/`new HashMap`)

Cover these cases (one test method each, camelCase names):
1. `testRemoveDisabledTasksTopLevel` — 3 tasks, middle disabled → 2 remain, order preserved.
2. `testRemoveDisabledTasksInsideConditionCases` — condition task with `parameters.caseTrue`/`caseFalse` lists each holding one enabled + one disabled task → disabled ones gone from surviving condition's parameters.
3. `testRemoveDisabledTasksInsideBranchCases` — branch with `parameters.cases: [{key: "k1", tasks: [...]}]` shape.
4. `testRemoveDisabledSingleMapSubtask` — task with `parameters.iteratee` being a single disabled task map → the `iteratee` KEY is removed from parameters.
5. `testRemoveDisabledTasksReturnsSameListWhenNothingDisabled` — no `disabled` anywhere → result equals input list (assert same task maps).
6. `testGetDisabledTaskNamesIncludesDescendants` — a DISABLED condition whose `caseTrue` holds task `inner1` → names contain the condition's name AND `inner1`.
7. `testGetDisabledTaskNamesForNestedDisabledUnderEnabledParent` — enabled loop with disabled subtask → only the subtask's name.
8. `testRemoveDisabledTasksInsideFinalize` — task whose `finalize` list holds one enabled + one disabled task → disabled one gone from the surviving task's `finalize` (covers the `pre`/`post`/`finalize` walk; `toMap()` carries all three, so one shape suffices).

Example for case 1 (repeat the pattern, do not reference other tests):

```java
@Test
public void testRemoveDisabledTasksTopLevel() {
    List<WorkflowTask> workflowTasks = List.of(
        new WorkflowTask(Map.of("name", "task1", "type", "component/v1/action")),
        new WorkflowTask(Map.of("name", "task2", "type", "component/v1/action", "disabled", true)),
        new WorkflowTask(Map.of("name", "task3", "type", "component/v1/action")));

    List<WorkflowTask> resultWorkflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflowTasks);

    Assertions.assertEquals(
        List.of("task1", "task3"),
        resultWorkflowTasks.stream()
            .map(WorkflowTask::getName)
            .toList());
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test --tests "com.bytechef.atlas.configuration.util.WorkflowTaskUtilsTest" > /tmp/t2.log 2>&1; echo EXIT=$?
```
Expected: compilation failure — `removeDisabledTasks` not defined.

- [ ] **Step 3: Implement** (in `WorkflowTaskUtils`; walk the full task map so `pre`/`post`/`finalize` are covered by the same recursion as parameters)

```java
private static final Object REMOVED = new Object();

public static List<WorkflowTask> removeDisabledTasks(List<WorkflowTask> workflowTasks) {
    List<WorkflowTask> resultWorkflowTasks = new ArrayList<>();

    for (WorkflowTask workflowTask : workflowTasks) {
        if (workflowTask.isDisabled()) {
            continue;
        }

        Map<String, ?> taskMap = workflowTask.toMap();
        Map<String, ?> strippedTaskMap = removeDisabledTasksFromMap(taskMap);

        if (strippedTaskMap.equals(taskMap)) {
            resultWorkflowTasks.add(workflowTask);
        } else {
            resultWorkflowTasks.add(new WorkflowTask(strippedTaskMap));
        }
    }

    return resultWorkflowTasks;
}

public static List<String> getDisabledTaskNames(List<WorkflowTask> workflowTasks) {
    List<String> disabledTaskNames = new ArrayList<>();

    for (WorkflowTask workflowTask : workflowTasks) {
        collectDisabledTaskNames(workflowTask.toMap(), workflowTask.isDisabled(), disabledTaskNames);
    }

    return disabledTaskNames;
}

@SuppressWarnings("unchecked")
private static Map<String, ?> removeDisabledTasksFromMap(Map<String, ?> taskMap) {
    Map<String, Object> resultMap = new LinkedHashMap<>();

    for (Map.Entry<String, ?> entry : taskMap.entrySet()) {
        Object strippedValue = removeDisabledTasksFromValue(entry.getValue());

        if (strippedValue == REMOVED) {
            continue;
        }

        resultMap.put(entry.getKey(), strippedValue);
    }

    return resultMap;
}

@SuppressWarnings("unchecked")
private static Object removeDisabledTasksFromValue(Object value) {
    if (value instanceof List<?> list) {
        List<Object> resultList = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> itemMap && isWorkflowTaskMap(itemMap)) {
                if (Boolean.TRUE.equals(itemMap.get(WorkflowConstants.DISABLED))) {
                    continue;
                }

                resultList.add(removeDisabledTasksFromMap((Map<String, ?>) itemMap));
            } else {
                Object strippedItem = removeDisabledTasksFromValue(item);

                if (strippedItem != REMOVED) {
                    resultList.add(strippedItem);
                }
            }
        }

        return resultList;
    } else if (value instanceof Map<?, ?> map) {
        if (isWorkflowTaskMap(map)) {
            if (Boolean.TRUE.equals(map.get(WorkflowConstants.DISABLED))) {
                return REMOVED;
            }

            return removeDisabledTasksFromMap((Map<String, ?>) map);
        }

        Map<Object, Object> resultMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object strippedValue = removeDisabledTasksFromValue(entry.getValue());

            if (strippedValue != REMOVED) {
                resultMap.put(entry.getKey(), strippedValue);
            }
        }

        return resultMap;
    }

    return value;
}

@SuppressWarnings("unchecked")
private static void collectDisabledTaskNames(
    Map<?, ?> taskMap, boolean ancestorDisabled, List<String> disabledTaskNames) {

    boolean disabled = ancestorDisabled || Boolean.TRUE.equals(taskMap.get(WorkflowConstants.DISABLED));

    if (disabled && taskMap.get(WorkflowConstants.NAME) instanceof String name) {
        disabledTaskNames.add(name);
    }

    for (Object value : taskMap.values()) {
        collectDisabledTaskNamesFromValue(value, disabled, disabledTaskNames);
    }
}

private static void collectDisabledTaskNamesFromValue(
    Object value, boolean ancestorDisabled, List<String> disabledTaskNames) {

    if (value instanceof List<?> list) {
        for (Object item : list) {
            collectDisabledTaskNamesFromValue(item, ancestorDisabled, disabledTaskNames);
        }
    } else if (value instanceof Map<?, ?> map) {
        if (isWorkflowTaskMap(map)) {
            collectDisabledTaskNames(map, ancestorDisabled, disabledTaskNames);
        } else {
            for (Object mapValue : map.values()) {
                collectDisabledTaskNamesFromValue(mapValue, ancestorDisabled, disabledTaskNames);
            }
        }
    }
}
```

Note: `WorkflowTask.toMap()` returns an unmodifiable map — the strip builds new maps, never mutates. `LinkedHashMap` preserves key order for the equality fast path. Adjust imports (`LinkedHashMap`).

- [ ] **Step 4: Run tests to verify they pass**

Same command as Step 2. Expected: EXIT=0.

- [ ] **Step 5: Commit**

```bash
git add server/libs/atlas/atlas-configuration
git commit -m "Add recursive disabled-task strip and name collection to WorkflowTaskUtils

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Null-propagating property access in the evaluator

**Files:**
- Create: `server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/NullSafePropertyAccessor.java`
- Modify: `server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/SpelEvaluator.java:165-179` (`createEvaluationContext`)
- Modify (record outcome): `docs/superpowers/specs/2026-08-24-workflow-node-disable-design.md` (the "Implementation checkpoint" blockquote)
- Test: `server/libs/core/evaluator/evaluator-impl/src/test/java/com/bytechef/evaluator/SpelEvaluatorTest.java` (exists — extend)

**Interfaces:**
- Produces: property access on a `null` target evaluates to `null` (so `${disabledTask.field}` and `${disabledTask.field.sub}` resolve to `null` when the context maps `disabledTask` → `null`). Behavior note: previously such expressions returned the raw expression string (the evaluator catches `SpelEvaluationException`); null is the deliberate improvement chosen in the spec.

**Background for the implementer:** SpEL consults generic property accessors (those whose `getSpecificTargetClasses()` returns `null`) for null evaluation targets. `MapPropertyAccessor` only handles `Map` targets, so today `null.field` throws inside SpEL and `SpelEvaluator` swallows it, returning the original string.

- [ ] **Step 1: Write the failing tests** (add to `SpelEvaluatorTest`, following its existing style of `evaluator.evaluate(Map.of(...), Map.of(...))` assertions — read a nearby test first and match its helpers)

```java
@Test
public void testPropertyAccessOnNullValueEvaluatesToNull() {
    Map<String, Object> context = new HashMap<>();

    context.put("disabledTask", null);

    Map<String, Object> result = new HashMap<>(
        evaluator.evaluate(Map.of("value", "${disabledTask.field}"), context));

    Assertions.assertNull(result.get("value"));
}

@Test
public void testNestedPropertyAccessOnNullValueEvaluatesToNull() {
    Map<String, Object> context = new HashMap<>();

    context.put("disabledTask", null);

    Map<String, Object> result = new HashMap<>(
        evaluator.evaluate(Map.of("value", "${disabledTask.field.subField}"), context));

    Assertions.assertNull(result.get("value"));
}

@Test
public void testPropertyAccessOnMissingNameStillReturnsOriginalExpression() {
    Map<String, Object> result = new HashMap<>(
        evaluator.evaluate(Map.of("value", "${unknownTask.field}"), Map.of()));

    Assertions.assertEquals("${unknownTask.field}", result.get("value"));
}
```

(Adapt `evaluator` construction to how the existing test class builds it — likely `SpelEvaluator.create()`.)

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :server:libs:core:evaluator:evaluator-impl:test --tests "com.bytechef.evaluator.SpelEvaluatorTest" > /tmp/t3.log 2>&1; echo EXIT=$?
```
Expected: the two null tests fail (value equals the raw string, not null); the third passes.

- [ ] **Step 3: Implement**

`NullSafePropertyAccessor.java` (Apache 2.0 header like `MapPropertyAccessor`):

```java
package com.bytechef.evaluator;

import org.jspecify.annotations.Nullable;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * Resolves any property read on a {@code null} target to {@code null} instead of failing. This makes references into
 * skipped (disabled) workflow tasks — seeded into the context as {@code name -> null} — resolve to {@code null} at any
 * property depth, e.g. {@code ${disabledTask.field.subField}}.
 *
 * @author Ivica Cardic
 */
class NullSafePropertyAccessor implements PropertyAccessor {

    @Override
    @Nullable
    public Class<?>[] getSpecificTargetClasses() {
        return null;
    }

    @Override
    public boolean canRead(EvaluationContext evaluationContext, @Nullable Object target, String name) {
        return target == null;
    }

    @Override
    public TypedValue read(EvaluationContext evaluationContext, @Nullable Object target, String name)
        throws AccessException {

        return TypedValue.NULL;
    }

    @Override
    public boolean canWrite(EvaluationContext evaluationContext, @Nullable Object target, String name) {
        return false;
    }

    @Override
    public void write(
        EvaluationContext evaluationContext, @Nullable Object target, String name, @Nullable Object newValue)
        throws AccessException {

        throw new UnsupportedOperationException();
    }
}
```

In `SpelEvaluator.createEvaluationContext`, after `addPropertyAccessor(new MapPropertyAccessor())`:

```java
evaluationContext.addPropertyAccessor(new NullSafePropertyAccessor());
```

- [ ] **Step 4: Run tests to verify they pass** — same command as Step 2, EXIT=0. Also run the whole evaluator module test suite to catch regressions:

```bash
./gradlew :server:libs:core:evaluator:evaluator-impl:test > /tmp/t3b.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t3b.log
```

- [ ] **Step 5: Update the spec checkpoint** — replace the "Implementation checkpoint" blockquote in the spec with a short resolution note: null propagation implemented via `NullSafePropertyAccessor` (generic accessor for null targets); prior behavior for property-on-null was raw-string passthrough, now null.

- [ ] **Step 6: Commit**

```bash
git add server/libs/core/evaluator docs/superpowers/specs/2026-08-24-workflow-node-disable-design.md
git commit -m "Resolve property access on null evaluation targets to null

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Engine skip semantics (strip, seed, empty-job completion, each guard)

**Files:**
- Modify: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/job/JobExecutor.java`
- Modify: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/main/java/com/bytechef/atlas/coordinator/task/completion/DefaultTaskCompletionHandler.java:238-244` (`hasMoreTasks`)
- Modify: `server/libs/atlas/atlas-coordinator/atlas-coordinator-config/src/main/java/com/bytechef/atlas/coordinator/config/TaskCoordinatorConfiguration.java:122-127` (`jobExecutor()` bean)
- Modify: `server/libs/platform/platform-job-sync/src/main/java/com/bytechef/platform/job/sync/executor/JobSyncExecutor.java:213-214` (`new JobExecutor(...)`)
- Modify: `server/libs/atlas/atlas-execution/atlas-execution-service/src/main/java/com/bytechef/atlas/execution/facade/JobFacadeImpl.java:83-96` (`createJob`)
- Modify: `server/libs/modules/task-dispatchers/each/src/main/java/com/bytechef/task/dispatcher/each/EachTaskDispatcher.java:87-88`
- Create: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/test/resources/workflows/hello3.json`, `hello4.json`
- Test: `server/libs/atlas/atlas-coordinator/atlas-coordinator-impl/src/test/java/com/bytechef/atlas/coordinator/TaskCoordinatorIntTest.java` (extend)

**Interfaces:**
- Consumes: `WorkflowTaskUtils.removeDisabledTasks(List<WorkflowTask>)`, `WorkflowTaskUtils.getDisabledTaskNames(List<WorkflowTask>)` (Task 2).
- Produces: `JobExecutor` constructor becomes `JobExecutor(ContextService, Evaluator, ApplicationEventPublisher, JobService, TaskDispatcher<? super TaskExecution>, TaskExecutionService, TaskFileStorage, WorkflowService)`. Behavioral contract: disabled tasks produce no `TaskExecution`; a workflow whose effective task list is empty completes as `Job.Status.COMPLETED` with zero task executions (previously threw `IllegalStateException("No tasks to execute!")`).

- [ ] **Step 1: Add integration-test workflows**

`hello3.json` — middle task disabled, outputs reference the disabled task's name AND a property path on it:

```json
{
  "label": "Disabled Node Demo",
  "outputs": [
    {"name": "skippedValue", "value": "${skippedNumber}"},
    {"name": "skippedField", "value": "${skippedNumber.someField}"}
  ],
  "tasks": [
    {
      "name": "firstNumber",
      "type": "randomHelper/v1/randomInt",
      "parameters": {"startInclusive": 1, "endInclusive": 2}
    },
    {
      "name": "skippedNumber",
      "type": "randomHelper/v1/randomInt",
      "disabled": true,
      "parameters": {"startInclusive": 1, "endInclusive": 2}
    },
    {
      "name": "lastNumber",
      "type": "randomHelper/v1/randomInt",
      "parameters": {"startInclusive": 1, "endInclusive": 2}
    }
  ]
}
```

`hello4.json` — single task, disabled:

```json
{
  "label": "All Disabled Demo",
  "tasks": [
    {
      "name": "onlyTask",
      "type": "randomHelper/v1/randomInt",
      "disabled": true,
      "parameters": {"startInclusive": 1, "endInclusive": 2}
    }
  ]
}
```

- [ ] **Step 2: Write the failing integration tests** (in `TaskCoordinatorIntTest`; workflow ids are base64 of the file basename: `hello3` → `aGVsbG8z`, `hello4` → `aGVsbG80`. The existing `executeWorkflow` helper hardcodes inputs `{yourName: me}` — that is fine; these workflows declare no required inputs. Mirror the existing helper if a variant without inputs is cleaner.)

```java
@Test
public void testPerformWorkflowWithDisabledTask() {
    Job completedJob = executeWorkflow("aGVsbG8z");

    Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());

    List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
        Objects.requireNonNull(completedJob.getId()));

    Assertions.assertEquals(
        List.of("firstNumber", "lastNumber"),
        taskExecutions.stream()
            .map(TaskExecution::getName)
            .toList());

    Map<String, ?> outputs = TASK_FILE_STORAGE.readJobOutputs(completedJob.getOutputs());

    Assertions.assertTrue(outputs.containsKey("skippedValue"));
    Assertions.assertNull(outputs.get("skippedValue"));
    Assertions.assertNull(outputs.get("skippedField"));
}

@Test
public void testPerformWorkflowWithAllTasksDisabled() {
    Job completedJob = executeWorkflow("aGVsbG80");

    Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());

    List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
        Objects.requireNonNull(completedJob.getId()));

    Assertions.assertTrue(taskExecutions.isEmpty());
}
```

(Verify the exact `TaskFileStorage` read method name for job outputs — `readJobOutputs` — against the interface before writing; adjust if it differs.)

- [ ] **Step 3: Run to verify they fail**

Integration tests need Docker (OrbStack socket on this machine — see memory note; if `/var/run/docker.sock` is dangling, export `DOCKER_HOST` to the OrbStack socket):

```bash
./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:testIntegration --tests "com.bytechef.atlas.coordinator.TaskCoordinatorIntTest" > /tmp/t4.log 2>&1; echo EXIT=$?
```
Expected: FAIL — hello3 executes 3 tasks (disabled one runs) or hello4 throws "No tasks to execute!".

- [ ] **Step 4: Implement the engine changes**

**4a. `JobExecutor`** — add `ApplicationEventPublisher eventPublisher` and `JobService jobService` fields + constructor params (order: `contextService, evaluator, eventPublisher, jobService, taskDispatcher, taskExecutionService, taskFileStorage, workflowService`); rewrite `execute`, thread the filtered list through, and add empty-completion:

```java
public void execute(Job job) {
    Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

    List<WorkflowTask> workflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflow.getTasks());

    if (job.getStatus() != Job.Status.STARTED) {
        throw new IllegalStateException("Should not be here");
    } else if (job.getCurrentTask() < workflowTasks.size()) {
        executeNextTask(job, workflow, workflowTasks);
    } else {
        completeJob(job, workflow);
    }
}
```

- `executeNextTask(Job job, Workflow workflow, List<WorkflowTask> workflowTasks)` — same body as today, but `nextTaskExecution` receives `workflowTasks` (filtered) instead of calling `workflow.getTasks()`; `workflow` stays a parameter for `getMaxRetries()`.
- Delete `hasMoreTasks` (inlined above).
- New `completeJob(Job job, Workflow workflow)` mirroring `DefaultTaskCompletionHandler.complete` (read current job context, evaluate `workflow.getOutputs()` against it, set `currentTask = -1`, `endDate`, `Status.COMPLETED`, store outputs, `jobService.update`, publish `JobStatusApplicationEvent`). Copy the exact statements from `DefaultTaskCompletionHandler.complete` (lines 164-191), adapting field names; include a comment that this path only fires when the effective (disabled-stripped) task list is exhausted at dispatch time — i.e. an empty or fully-disabled workflow.
- Note: `TaskCoordinator.onStartJobEvent` publishes a `STARTED` `JobStatusApplicationEvent` after `execute` returns, so an empty job emits COMPLETED then STARTED. Listeners key off job state transitions idempotently; do not try to reorder — just verify the sync test completes (the `JobCompletionAwaiter` keys on the COMPLETED event).

**4b. `DefaultTaskCompletionHandler.hasMoreTasks`:**

```java
private boolean hasMoreTasks(Job job) {
    Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

    List<WorkflowTask> workflowTasks = WorkflowTaskUtils.removeDisabledTasks(workflow.getTasks());

    return job.getCurrentTask() + 1 < workflowTasks.size();
}
```

**4c. Construction sites** — update both:
- `TaskCoordinatorConfiguration.jobExecutor()`: `new JobExecutor(contextService, evaluator, eventPublisher, jobService, taskDispatcher(), taskExecutionService, taskFileStorage, workflowService)`.
- `JobSyncExecutor` (line ~213): `new JobExecutor(contextService, evaluator, coordinatorEventPublisher, jobService, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService)`.

**4d. `JobFacadeImpl.createJob` seeding:**

```java
Workflow workflow = workflowService.getWorkflow(jobParametersDTO.getWorkflowId());

Job job = jobService.create(jobParametersDTO, workflow);

long jobId = Validate.notNull(job.getId(), "id");

log.debug("Job id={}, label='{}' created", jobId, job.getLabel());

Map<String, Object> initialContext = new HashMap<>(job.getInputs());

for (String disabledTaskName : WorkflowTaskUtils.getDisabledTaskNames(workflow.getTasks())) {
    initialContext.put(disabledTaskName, null);
}

contextService.push(
    jobId, Context.Classname.JOB,
    taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, initialContext));
```

(Add `HashMap`/`WorkflowTaskUtils` imports. `HashMap` accepts null values; verify `taskFileStorage.storeContextValue` JSON-serializes nulls — it does, Jackson writes null entries.)

**4e. `EachTaskDispatcher` tolerance guard** — the strip removes a disabled single-map `iteratee`, and `MapUtils.getRequired` would throw. Change lines 87-88 to a nullable read and treat an absent iteratee like an empty items list (find the existing "no items" branch in the same method and route to it):

```java
Map<String, ?> iteratee = MapUtils.getMap(taskExecution.getParameters(), ITERATEE, (Map<String, ?>) null);
```

then guard `if (iteratee == null || items.isEmpty())` on the existing empty-completion branch. Read the method first; keep its completion semantics (it must still mark the each task complete). `MapTaskDispatcher` and `LoopTaskDispatcher` already default to empty — no change.

- [ ] **Step 5: Run the failing tests to verify they pass** — Step 3 command, EXIT=0. Then run the module unit tests and each-dispatcher tests:

```bash
./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:test :server:libs:modules:task-dispatchers:each:test --continue > /tmp/t4b.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t4b.log
```

(`DefaultTaskCompletionHandlerTest` constructs the completion handler — its signature is unchanged. If any hand-assembled `*IntTestConfiguration` assembles `JobExecutor` directly, fix the construction there too; find with `grep -rn "new JobExecutor(" server --include="*.java" | grep -v /build/`.)

- [ ] **Step 6: Commit**

```bash
git add server/libs/atlas server/libs/platform/platform-job-sync server/libs/modules/task-dispatchers/each
git commit -m "Skip disabled workflow tasks during execution

Disabled tasks are stripped recursively before the engine sees them, their
names resolve to null in the job context, and a fully-disabled workflow
completes with zero task executions.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: REST surface — expose `disabled` to the client

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml:3298-3360` (WorkflowTask schema)
- Modify: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/WorkflowTaskDTO.java`
- Generated (by command, then committed): `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-api/generated/**`, `client/src/shared/middleware/platform/configuration/**`

**Interfaces:**
- Produces: `WorkflowTaskModel#getDisabled()` (generated), `WorkflowTaskDTO#isDisabled()`, and the generated TS `WorkflowTask` interface gains `disabled?: boolean` — consumed by Task 6.
- MapStruct maps `disabled` automatically by name in both `WorkflowTaskMapper` mappers once model + DTO have the property.

- [ ] **Step 1: Edit the OpenAPI schema** — in the `WorkflowTask:` schema's `properties`, after `description`:

```yaml
        disabled:
          description: "When true, the task is skipped during workflow execution."
          type: "boolean"
```

- [ ] **Step 2: Add the DTO property** — in `WorkflowTaskDTO`: field `private final boolean disabled;` (after `description`), constructor parameter `boolean disabled` after `String description` in the canonical constructor, assignment, getter `public boolean isDisabled()`, and pass `workflowTask.isDisabled()` in the delegating constructor. Find any other callers of the canonical constructor first:

```bash
grep -rn "new WorkflowTaskDTO(" server --include="*.java" | grep -v /build/
```

Update each call site for the new parameter.

- [ ] **Step 3: Regenerate models (server Spring + client typescript-fetch in one task):**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:generateOpenAPI > /tmp/t5.log 2>&1; echo EXIT=$?
```

Then inspect `git status`: keep the `WorkflowTaskModel` changes and the client `WorkflowTask.ts` change; if the generator churned unrelated files with cosmetic-only diffs, restore them (`git checkout -- <path>`). Note: this module vendors `openapi-templates/pojo.mustache`; the `verifyOpenApiPojoTemplate` task guards it — do not touch the template.

- [ ] **Step 4: Compile server + typecheck client:**

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:compileJava :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api:compileJava :server:libs:platform:platform-configuration:platform-configuration-api:compileJava --continue > /tmp/t5b.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t5b.log
```
```bash
cd client && npm run typecheck
```

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-configuration client/src/shared/middleware/platform/configuration
git commit -m "Expose disabled task flag through the configuration REST API

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: Client types and persistence plumbing

**Files:**
- Modify: `client/src/shared/types.ts` (`NodeDataType` ~line 194, `WorkflowTaskType` ~line 318)
- Modify: `client/src/pages/platform/workflow-editor/utils/saveWorkflowDefinition.ts:55-120`
- Verify (modify only where task fields are enumerated): other task-rebuild sites

**Interfaces:**
- Consumes: generated `WorkflowTask.disabled` (Task 5).
- Produces: `NodeDataType.disabled?: boolean`, `WorkflowTaskType.disabled?: boolean`; `saveWorkflowDefinition` preserves `disabled` when rebuilding a task from node data. `convertTaskToNode` (layoutUtils) already spreads `...task`, so node data picks the flag up with no change there.

- [ ] **Step 1: Add the type fields** (sort-keys: `disabled` goes after `description` in `NodeDataType`, and after `connections` in `WorkflowTaskType`):

```ts
disabled?: boolean;
```

- [ ] **Step 2: Preserve the flag in `saveWorkflowDefinition`** — add `disabled` to the `nodeData` destructure (alphabetical, after `description`) and to the `newTask` literal (after `description`):

```ts
const newTask: WorkflowTask = {
    clusterElements,
    description,
    disabled,
    label,
    maxRetries,
    metadata,
    name: name!,
    parameters,
    type: type ?? `${componentName}/v${version}/${operationName}`,
};
```

- [ ] **Step 3: Audit other rebuild sites** — the danger is any code that constructs a task object by enumerating fields (spread-based updates are safe). Check each hit of:

```bash
cd client && grep -rn "metadata," src/pages/platform/workflow-editor/utils --include="*.ts" -l
```

Read `getTask.ts`, `getRecursivelyUpdatedTasks.ts`, `saveTaskDispatcherSubtaskFieldChange.ts`, `pasteNode.ts`, `insertTaskDispatcherSubtask.ts`: where a task literal enumerates `label/maxRetries/metadata`-style fields, add `disabled` in the same pattern; where objects are spread (`{...task, x}`), no change. List in the commit message which files needed it.

- [ ] **Step 4: Verify:**

```bash
cd client && npm run typecheck && npm run lint
```

- [ ] **Step 5: Commit**

```bash
git add client/src
git commit -m "client - Carry disabled flag through workflow task types and saves

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: Toggle — util, menu item, node wiring

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/toggleNodeDisabled.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/toggleNodeDisabled.test.ts`
- Modify: `client/src/pages/platform/workflow-editor/utils/getWorkflowNodeMenuItems.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu.tsx`
- Modify: `client/src/pages/platform/workflow-editor/nodes/WorkflowNode.tsx`

**Interfaces:**
- Produces: `toggleNodeDisabled({updateWorkflowMutation, workflowNodeName}): void` — flips `disabled` on the named task anywhere in the definition (top level, nested lists, `cases[].tasks`, single-map subtasks, `pre`/`post`/`finalize`), deleting the key when re-enabling, persisting via the same store-update + mutation-guard funnel as `saveStickyNotes`.
- Menu contract: new optional props `onToggleDisabled?: () => void` and `showDisableAction?: boolean` on `WorkflowNodeDropdownMenu` and `getWorkflowNodeMenuItems`; item label `data.disabled ? 'Enable' : 'Disable'`, key `'toggle-disabled'`, icons `PlayIcon` (enable) / `BanIcon` (disable), placed in the second group next to Rename; never shown for triggers (`data.trigger`).

- [ ] **Step 1: Write the failing util test** — mirror the structure of `stickyNoteUtils.test.ts` "should write sticky notes under metadata.ui and fire the mutation" (same store setup + mutation mock; read that test first and copy its scaffolding, including `vi.hoisted` if it uses factory-injected mocks). Cases:
1. toggling a top-level task writes `"disabled": true` into the parsed definition passed to the mutation;
2. toggling the same task again removes the key entirely (not `false`);
3. toggling a task nested in `parameters.caseTrue` finds and flips it;
4. toggling a task nested in `parameters.cases[0].tasks` finds and flips it;
5. unknown node name → store definition unchanged, mutation not fired.

- [ ] **Step 2: Run to verify it fails**

```bash
cd client && npx vitest run src/pages/platform/workflow-editor/utils/toggleNodeDisabled.test.ts
```

- [ ] **Step 3: Implement `toggleNodeDisabled.ts`** — persistence funnel copied from `saveStickyNotes` (`stickyNoteUtils.ts:264-340`): parse `workflow.definition`, apply the recursive toggle, bail if unchanged, `JSON.stringify(..., null, SPACE)`, optimistic `useWorkflowDataStore.setState`, honor `isWorkflowMutating`/`setPendingDefinition`, else fire `updateWorkflowMutation`. Recursive toggle:

```ts
/* eslint-disable @typescript-eslint/no-explicit-any */
function toggleInTasks(tasks: Array<any>, workflowNodeName: string): boolean {
    for (const task of tasks) {
        if (task && task.name === workflowNodeName && typeof task.type === 'string') {
            if (task.disabled) {
                delete task.disabled;
            } else {
                task.disabled = true;
            }

            return true;
        }

        for (const nestedTasks of [task?.pre, task?.post, task?.finalize]) {
            if (Array.isArray(nestedTasks) && toggleInTasks(nestedTasks, workflowNodeName)) {
                return true;
            }
        }

        if (task?.parameters && toggleInValue(task.parameters, workflowNodeName)) {
            return true;
        }
    }

    return false;
}

function toggleInValue(value: any, workflowNodeName: string): boolean {
    if (Array.isArray(value)) {
        if (value.some((item) => isTaskShaped(item))) {
            return toggleInTasks(value, workflowNodeName);
        }

        return value.some((item) => toggleInValue(item, workflowNodeName));
    }

    if (value && typeof value === 'object') {
        if (isTaskShaped(value)) {
            return toggleInTasks([value], workflowNodeName);
        }

        return Object.values(value).some((nestedValue) => toggleInValue(nestedValue, workflowNodeName));
    }

    return false;
}

function isTaskShaped(value: any): boolean {
    return Boolean(value) && typeof value === 'object' && typeof value.name === 'string' && typeof value.type === 'string';
}
```

(Reuse `flattenDefinitionTasks.ts`'s `isWorkflowTask` helper instead of a local `isTaskShaped` if it is exported; check first.)

- [ ] **Step 4: Menu item** — in `getWorkflowNodeMenuItems.tsx` add props `onToggleDisabled?: () => void; showDisableAction: boolean;` and, in the non-trigger path immediately after the `canRename` rename item:

```tsx
if (showDisableAction && onToggleDisabled) {
    menuItems.push({
        icon: data.disabled ? <PlayIcon className="size-4 shrink-0" /> : <BanIcon className="size-4 shrink-0" />,
        key: 'toggle-disabled',
        label: data.disabled ? 'Enable' : 'Disable',
        onSelect: onToggleDisabled,
        type: 'item',
    });
}
```

Include `showDisableAction` in the `hasSecondGroup` computation so the separator logic stays right. Import `BanIcon`, `PlayIcon` from lucide (sorted). Thread the two props through `WorkflowNodeDropdownMenu` (props interface, destructure, `getWorkflowNodeMenuItems` call, `useMemo` deps).

- [ ] **Step 5: Wire in `WorkflowNode.tsx`** — next to `handleDeleteNodeClick` (~line 695), add:

```tsx
const handleToggleDisabledClick = useCallback(() => {
    toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: data.workflowNodeName});
}, [data.workflowNodeName, updateWorkflowMutation]);
```

(Use whatever `updateWorkflowMutation` reference the existing delete handler uses in this component — locate it there.) Pass `onToggleDisabled={handleToggleDisabledClick}` and `showDisableAction={!data.trigger}` to the `<WorkflowNodeDropdownMenu>` usages for task nodes.

- [ ] **Step 6: Verify**

```bash
cd client && npx vitest run src/pages/platform/workflow-editor/utils/toggleNodeDisabled.test.ts && npm run typecheck && npm run lint
```

- [ ] **Step 7: Commit**

```bash
git add client/src
git commit -m "client - Add Disable/Enable toggle to workflow node menu

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 8: Rendering — muted nodes, derived ancestor state, reference warnings

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/getEffectivelyDisabledTaskNames.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/getEffectivelyDisabledTaskNames.test.ts`
- Create: `client/src/pages/platform/workflow-editor/utils/getDisabledNodeReferences.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/getDisabledNodeReferences.test.ts`
- Create: `client/src/pages/platform/workflow-editor/hooks/useDisabledTaskNames.ts`
- Modify: `client/src/pages/platform/workflow-editor/nodes/WorkflowNode.tsx`

**Interfaces:**
- Produces:
  - `getEffectivelyDisabledTaskNames(tasks: Array<WorkflowTask>): Set<string>` — names of every task that is disabled OR sits under a disabled ancestor at any depth (client mirror of the server walk; reuse the nested-extraction shapes from `flattenDefinitionTasks.ts`, but carry an `ancestorDisabled` flag through the recursion).
  - `getDisabledNodeReferences(parameters: unknown, disabledTaskNames: Set<string>): Array<string>` — distinct disabled names referenced inside any `${...}` expression found in string values of `parameters` (recursive over objects/arrays). Match a name inside an expression body with a word-boundary regex: for each expression body, `new RegExp('(^|[^\\w])' + escapedName + '([^\\w]|$)')`. Escape regex metacharacters in names.
  - `useDisabledTaskNames(): Set<string>` — `useWorkflowDataStore((state) => state.workflow.definition)` + `useMemo` that parses the definition (empty set on parse failure) and returns `getEffectivelyDisabledTaskNames(parsed.tasks ?? [])`.

- [ ] **Step 1: Write the failing util tests**

`getEffectivelyDisabledTaskNames`: (1) flat list with one disabled → that name only; (2) disabled condition with `caseTrue` subtask → both names; (3) enabled loop with disabled `iteratee` list entry → subtask name only; (4) `cases[].tasks` shape; (5) no disabled → empty set.

`getDisabledNodeReferences`: (1) `{value: '${skippedTask.field}'}` with `skippedTask` disabled → `['skippedTask']`; (2) name appearing only as a substring of another identifier (`${skippedTask2.field}`, disabled set has `skippedTask`) → `[]`; (3) reference in a nested array/object → found; (4) plain string mentioning the name outside `${...}` → `[]`; (5) multiple hits reported once.

- [ ] **Step 2: Run to verify they fail**

```bash
cd client && npx vitest run src/pages/platform/workflow-editor/utils/getEffectivelyDisabledTaskNames.test.ts src/pages/platform/workflow-editor/utils/getDisabledNodeReferences.test.ts
```

- [ ] **Step 3: Implement the two utils and the hook** (hook ordering rule: store hook, then `useMemo`).

- [ ] **Step 4: Render in `WorkflowNode.tsx`:**
1. `const disabledTaskNames = useDisabledTaskNames();` near the other store hooks.
2. Derived values (before the effects):

```tsx
const isEffectivelyDisabled = Boolean(data.disabled) || disabledTaskNames.has(data.workflowNodeName);

const referencedDisabledNames = useMemo(
    () => (isEffectivelyDisabled ? [] : getDisabledNodeReferences(data.parameters, disabledTaskNames)),
    [data.parameters, disabledTaskNames, isEffectivelyDisabled]
);
```

3. On the node's main container `className`, merge `isEffectivelyDisabled && 'opacity-50 grayscale'` via `twMerge`.
4. Badge on the node header when `data.disabled` (the explicit flag, not derived): a small `BanIcon` with `title="Disabled — skipped during execution"`.
5. Warning badge when `referencedDisabledNames.length > 0`: a `TriangleAlertIcon` (amber, e.g. `text-warning` if the token exists, else `text-amber-500`) whose `title` is `` `References disabled node ${referencedDisabledNames[0]} — it will not run, so this value will not resolve` `` for one name, and `` `References disabled nodes ${referencedDisabledNames.join(', ')} — they will not run, so this value will not resolve` `` for several.
   Place both near where the node renders its label/handles — inspect the header JSX (~lines 320-380 render label editing) and attach to the existing icon/label row.
6. Check the execution read-only canvas renders through this same component (search for the component used by the execution detail workflow view); if it does, the muted state is automatic — note the finding in the commit message. Do not build a separate read-only treatment.

- [ ] **Step 5: Verify**

```bash
cd client && npm run check
```

(`npm run check` runs prettier check + lint + typecheck + vitest with coverage; fix formatting with `npm run format` if prettier complains.)

- [ ] **Step 6: Commit**

```bash
git add client/src
git commit -m "client - Render disabled workflow nodes muted with reference warnings

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 9: Full verification sweep

**Files:** none new — verification only (fix regressions where found).

- [ ] **Step 1: Server format + static checks**

```bash
./gradlew spotlessApply > /tmp/t9a.log 2>&1; echo EXIT=$?
```

```bash
./gradlew compileJava compileTestJava --continue > /tmp/t9b.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t9b.log
```

- [ ] **Step 2: Targeted server test suites** (touched modules):

```bash
./gradlew :server:libs:atlas:atlas-configuration:atlas-configuration-api:test :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:test :server:libs:atlas:atlas-execution:atlas-execution-service:test :server:libs:core:evaluator:evaluator-impl:test :server:libs:modules:task-dispatchers:each:test :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:test --continue > /tmp/t9c.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t9c.log
```

- [ ] **Step 3: Integration tests** (Docker required):

```bash
./gradlew :server:libs:atlas:atlas-coordinator:atlas-coordinator-impl:testIntegration > /tmp/t9d.log 2>&1; echo EXIT=$?; grep "^> Task .* FAILED" /tmp/t9d.log
```

- [ ] **Step 4: Client full check**

```bash
cd client && npm run check
```

- [ ] **Step 5: Spec conformance checklist** — walk the spec section by section and confirm each item maps to landed code; record any deliberate deviation in the spec file:
  - `disabled` parsed/serialized only-when-true (Task 1); recursive strip incl. `pre`/`post`/`finalize` + name collection (Task 2); null resolution incl. property paths (Task 3); no-trace skipping, context seeding, all-disabled graceful completion, sync path covered (Task 4); REST + generated models (Task 5); flag survives editor saves (Task 6); toggle on top-level and nested nodes, key deleted on re-enable (Task 7); muted rendering, derived ancestor state, advisory reference warnings, execution-view check (Task 8).
- [ ] **Step 6: Commit any verification fixes** (server and client separately, following the message convention). If spotless reformatted files, fold those into the relevant fix commit or a dedicated `Apply spotless formatting` commit.
