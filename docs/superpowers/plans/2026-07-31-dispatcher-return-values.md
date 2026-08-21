# Dispatcher Return Values Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Condition and branch task dispatchers return the output of the last node of the executed branch; fork/join returns a `Map<String, Object>` keyed `branch_0..N` — both at runtime and in the editor's design-time output preview.

**Architecture:** Runtime: each completion handler copies the last child's output onto the parent task execution at sequence exhaustion (map's exact write point); fork/join aggregates with map's read-modify-write pattern under `getTaskExecutionForUpdate` locking. Design-time: a new shared `TaskListOutputDataSource` SPI (mirroring `MapDataSource`) feeds `.output(...)` functions on the three definition factories.

**Tech Stack:** Java 25 / Spring Boot, atlas engine (`TaskFileStorage`, `CounterService`, `ContextService`), TaskDispatcherDsl, JsonFileAssert snapshots.

**Spec:** `docs/superpowers/specs/2026-07-31-dispatcher-return-values-design.md` (approved; decisions log binds: synthetic `branch_0..N` keys, NO DSL change; empty case/branch → null; branch literal-`value` path unchanged; caseTrue/first-case design-time approximation).

## Global Constraints

- CE code, Apache 2.0 headers. No Claude trailers in commits; server commit convention (`<description>`, no ticket).
- CLAUDE.md Java conventions: blank line before control statements, blank line after variable modification before use, no chained calls outside the allowed list, descriptive variable names.
- Gradle discipline: redirect to file, `echo "exit=$?"` on its own line, grep `'^> Task .* FAILED'`.
- Snapshot regen: delete the stale JSON from BOTH `src/test/resources/definition/` and `build/resources/test/definition/`, then rerun the definition-factory test.
- Existing tests must pass unmodified unless a task explicitly says otherwise.

---

### Task 1: Condition runtime output

**Files:**
- Modify: `server/libs/modules/task-dispatchers/condition/src/main/java/com/bytechef/task/dispatcher/condition/completion/ConditionTaskCompletionHandler.java` (~line 147, the exhaustion `else`)
- Test: extend `server/libs/modules/task-dispatchers/condition/src/test/java/.../ConditionTaskDispatcherIntTest.java` (find exact path/name first)

**Interfaces:**
- Consumes: `TaskFileStorage.storeTaskExecutionOutput(jobId, taskExecutionId, value)` / `readTaskExecutionOutput(FileEntry)`; `TaskExecution.setOutput(FileEntry)`.
- Produces: `${conditionNodeName}` resolves to the executed case's last-task output downstream.

- [ ] **Step 1: Write the failing IntTest case** — a workflow where a condition's caseTrue ends in a task with a known output, followed by a task referencing `${conditionNodeName}`; assert the reference resolves to that output (follow the existing IntTest's var-collector pattern). Also a case pinning the empty-case path: condition whose selected case has zero tasks → downstream `${conditionNodeName}` is null.
- [ ] **Step 2: Run it, verify it fails** (reference currently resolves to null).
- [ ] **Step 3: Implement** — in the exhaustion `else` (currently `conditionTaskExecution.setEndDate(...)` then update+handle), copy the completed child's output onto the parent BEFORE the update:

```java
} else {
    if (taskExecution.getOutput() != null) {
        long jobId = Objects.requireNonNull(conditionTaskExecution.getJobId());

        Object outputValue = taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());

        conditionTaskExecution.setOutput(taskFileStorage.storeTaskExecutionOutput(jobId, id, outputValue));
    }

    conditionTaskExecution.setEndDate(Instant.now());
    ...
```

  The dispatcher's empty-case fast path (`ConditionTaskDispatcher` ~116-122) stays untouched — that IS the null-output contract.
- [ ] **Step 4: Run the module's `test` + `testIntegration`** — new cases pass, existing pass unmodified.
- [ ] **Step 5: Commit** `"Return the executed case's last-node output from the condition dispatcher"`.

---

### Task 2: Branch runtime output

**Files:**
- Modify: `server/libs/modules/task-dispatchers/branch/src/main/java/com/bytechef/task/dispatcher/branch/completion/BranchTaskCompletionHandler.java` (exhaustion `else`, ~line 150)
- Test: the branch module's IntTest (find it; if none exists, add one modeled on condition's)

**Interfaces:** same as Task 1; additionally must NOT disturb `BranchTaskDispatcher`'s existing literal-`value` path (~134-139), which already sets output when the selected case has `value` instead of `tasks`.

- [ ] **Step 1: Failing test cases** — (a) selected case's task sequence ends in a known output → `${branchNodeName}` resolves to it; (b) regression-pin the literal-`value` case still returns the literal; (c) default-case fallback returns the default sequence's last output.
- [ ] **Step 2: Run, verify (a) and (c) fail, (b) passes.**
- [ ] **Step 3: Implement** — same copy-on-exhaustion block as Task 1, in branch's completion handler (parent variable name differs; read the file). Dispatcher untouched.
- [ ] **Step 4: Run module `test` + `testIntegration`.**
- [ ] **Step 5: Commit** `"Return the selected case's last-node output from the branch dispatcher"`.

---

### Task 3: Fork/join runtime output map

**Files:**
- Modify: `server/libs/modules/task-dispatchers/fork-join/src/main/java/com/bytechef/task/dispatcher/fork/join/completion/ForkJoinTaskCompletionHandler.java` (the `else` at ~166)
- Test: `ForkJoinTaskDispatcherIntTest`

**Interfaces:**
- Produces: parent output `Map<String, Object>` with keys `"branch_" + i` (declaration order), value = branch i's last-task output (null-valued entry for an output-less last task; entry simply absent until a branch completes).
- CONCURRENCY: branch completions run in parallel — the parent mutation MUST use `taskExecutionService.getTaskExecutionForUpdate(parentId)` (map's pattern, `MapTaskCompletionHandler:160`) instead of the plain `getTaskExecution` fetched earlier in the method, and the parent must be `update`d even when `branchesLeft != 0` (map's `else` at :200).

- [ ] **Step 1: Failing IntTest** — two branches with distinct known last outputs; downstream task references `${forkJoinName.branch_0}` and `${forkJoinName.branch_1}`; assert both. Include a branch whose last task produces no output → key present with null (or absent — match what the implementation does and pin it explicitly).
- [ ] **Step 2: Run, verify failure.**
- [ ] **Step 3: Implement** in the exhaustion `else`:

```java
} else {
    TaskExecution forkJoinTaskExecutionForUpdate =
        taskExecutionService.getTaskExecutionForUpdate(taskExecutionParentId);

    int branch = MapUtils.getInteger(taskExecution.getParameters(), BRANCH);
    long jobId = Objects.requireNonNull(forkJoinTaskExecutionForUpdate.getJobId());

    Map<String, Object> branchOutputs;

    if (forkJoinTaskExecutionForUpdate.getOutput() != null) {
        branchOutputs = new HashMap<>(
            (Map<String, Object>) taskFileStorage.readTaskExecutionOutput(
                forkJoinTaskExecutionForUpdate.getOutput()));
    } else {
        branchOutputs = new HashMap<>();
    }

    Object branchValue = taskExecution.getOutput() == null
        ? null
        : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());

    branchOutputs.put("branch_" + branch, branchValue);

    forkJoinTaskExecutionForUpdate.setOutput(
        taskFileStorage.storeTaskExecutionOutput(jobId, taskExecutionParentId, branchOutputs));

    long branchesLeft = counterService.decrement(taskExecutionParentId);

    if (branchesLeft == 0) {
        forkJoinTaskExecutionForUpdate.setEndDate(Instant.now());

        forkJoinTaskExecutionForUpdate = taskExecutionService.update(forkJoinTaskExecutionForUpdate);

        taskCompletionHandler.handle(forkJoinTaskExecutionForUpdate);
    } else {
        taskExecutionService.update(forkJoinTaskExecutionForUpdate);
    }
}
```

  Put the key prefix in `ForkJoinTaskDispatcherConstants` (e.g. `BRANCH_OUTPUT_KEY_PREFIX = "branch_"`).
- [ ] **Step 4: Run module `test` + `testIntegration`.**
- [ ] **Step 5: Commit** `"Return a branch-keyed output map from the fork/join dispatcher"`.

---

### Task 4: TaskListOutputDataSource SPI + condition design-time output

**Files:**
- Create: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-api/src/main/java/com/bytechef/platform/workflow/task/dispatcher/output/TaskListOutputDataSource.java`
- Create: impl in `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/output/TaskListOutputDataSourceImpl.java`
- Modify: `.../condition/ConditionTaskDispatcherDefinitionFactory.java`
- Test: factory snapshot test + an impl unit test mirroring `MapDataSourceImpl`'s

**Interfaces:**
- Produces (consumed by Tasks 4-5's factories):

```java
public interface TaskListOutputDataSource {

    String WORKFLOW_ID = "workflowId";
    String ENVIRONMENT_ID = "environmentId";

    @Nullable
    OutputResponse getLastTaskOutput(String workflowId, String lastTaskName, String lastTaskType, long environmentId);
}
```

  (Same contract as `MapDataSource.getLastIterateeTaskOutput` — READ `MapDataSource` + `MapDataSourceImpl` first and mirror them exactly, including how the constants get injected into `inputParameters` by the caller of the output function; the impl reuses the identical fallback chain: `WorkflowNodeTestOutput` sample → static component output → static dispatcher output → live `WorkflowNodeOutputFacade` computation. If the impl would be a line-for-line copy of `MapDataSourceImpl`, extract the shared logic instead of duplicating — e.g. `MapDataSourceImpl` delegates to the new impl.)

- [ ] **Step 1: Write the SPI + impl + impl unit test** (mirror `MapDataSourceImpl`'s test).
- [ ] **Step 2: Condition factory** — inject `Optional<TaskListOutputDataSource>`, add:

```java
.output(inputParameters -> taskListOutputDataSource
    .map(dataSource -> output(inputParameters, dataSource))
    .orElse(null))
```

  where `output(...)` reads the `caseTrue` task list (constant from `ConditionTaskDispatcherConstants`), takes its last task's `name`/`type`, calls `getLastTaskOutput`, and returns the schema UNWRAPPED (single value, not `array()`): `OutputResponse.of(lastTaskSchema, lastTaskSampleOutput)`. Empty caseTrue → null. Mirror `MapTaskDispatcherDefinitionFactory.output(...)` structure (:80-116).
- [ ] **Step 3: Regenerate `condition_v1.json`** (delete both copies, rerun the factory test) and eyeball the new `outputDefinition` block.
- [ ] **Step 4: Run `platform-configuration-service` + condition module `test`.**
- [ ] **Step 5: Commit** `"Add the last-task output data source and the condition design-time output"`.

---

### Task 5: Branch + fork/join design-time outputs and editor wiring

**Files:**
- Modify: `.../branch/BranchTaskDispatcherDefinitionFactory.java`, `.../fork/join/ForkJoinTaskDispatcherDefinitionFactory.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/WorkflowNodeOutputFacadeImpl.java` (~292-330 — the `loop`/`each`/`map` dispatcher special-case list)
- Test: both factory snapshot tests; a facade test if the special-case list has one (check)

**Interfaces:** Consumes Task 4's `TaskListOutputDataSource` verbatim.

- [ ] **Step 1: Branch factory** — `.output(...)`: resolve the FIRST case's `tasks` list (read `BranchTaskDispatcherConstants` for the `cases`/`tasks`/`default` keys and the exact shape), fall back to the `default` list when there are no cases; last task → `getLastTaskOutput` → unwrapped `OutputResponse`. A first case carrying a literal `value` instead of `tasks` → return null (no schema claim).
- [ ] **Step 2: Fork/join factory** — `.output(...)`: for each branch list `i`, resolve last-task schema; build `object().properties(...)` with one property named `"branch_" + i` per branch (use the Task 3 constant). Branches with unresolvable last-task schema get an untyped `object("branch_" + i)` property rather than being dropped.
- [ ] **Step 3: Read `WorkflowNodeOutputFacadeImpl`'s dispatcher special-case handling** (`loop`/`each`/`map` list) and add `condition`/`branch`/`fork-join` with the SAME self-reference exclusion semantics: a task nested inside a case/branch must not see its own enclosing dispatcher's aggregate output in autocomplete. Understand before editing — if the existing mechanism is name-list-based, extend the list; if type-based, extend the match. Pin with a test if the file has one for map.
- [ ] **Step 4: Regenerate `branch_v1.json` + `fork-join_v1.json`** (delete both copies each, rerun).
- [ ] **Step 5: Run both modules' `test` + `platform-configuration-service:test`.**
- [ ] **Step 6: Commit** `"Add branch and fork/join design-time outputs and editor self-reference exclusion"`.

---

### Task 6: Docs, spec close-out, final gates

**Files:**
- Modify: `docs/superpowers/specs/2026-07-31-dispatcher-return-values-design.md` (Status → Implemented; decisions-log additions if implementation forced any)
- Check/modify: user docs for flow controls (`grep -ril "fork" docs/content/docs` — find where condition/branch/fork-join are documented; add output sections per released-version convention: these are unreleased → Coming Soon note per `docs/content` sibling patterns)
- CLAUDE.md: one short bullet under an appropriate existing section noting the three dispatchers now return values (shape + `branch_i` keys), if CLAUDE.md documents map's behavior anywhere (check; skip if it doesn't).

- [ ] **Step 1: Docs updates as above.**
- [ ] **Step 2: Final gates, in order** — `./gradlew spotlessApply`; `./gradlew compileJava compileTestJava --continue`; targeted `:check` on the three dispatcher modules + `platform-workflow-task-dispatcher-api` + `platform-configuration-service`; targeted `testIntegration` on the three dispatcher modules; `cd docs && npm run types:check` if docs/content changed.
- [ ] **Step 3: Commit** `"Document the dispatcher return values and mark the design implemented"`.
