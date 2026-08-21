# Graph Task Dispatcher — Phase 1 (Engine) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A working `graph/v1` task dispatcher — named nodes, expression `next` transitions, cycles bounded by a transition budget, terminal-node output — usable via code workflows and the API (editor rendering is phase 2).

**Architecture:** Fourth instance of the container-dispatcher pattern: `__node`-stamped children, taskNumber advancement, `CounterService` budget, context-stack pushes, and the condition/branch output contract. One dispatcher + one completion handler own all graph semantics; nothing under `server/libs/atlas/` changes.

**Tech Stack:** Java 25 / Spring Boot, atlas primitives (`TaskFileStorage`, `CounterService`, `ContextService`, `Evaluator`), TaskDispatcherDsl, JsonFileAssert snapshots.

**Spec:** `docs/superpowers/specs/2026-08-02-graph-task-dispatcher-design.md` (decisions bind: expression `next`; total budget `maxTransitions` default 100; accumulated last-write-wins context; empty node = router consuming budget; unknown target → clean run failure; terminal output = last task's output; design-time preview = first declared terminal node).

**WORKTREE:** ALL work happens in `/Volumes/Data/bytechef/bc-graph-wt` (branch `graph-dispatcher`). Never touch `/Volumes/Data/bytechef/bytechef`. Commit on `graph-dispatcher`; the merge back to `0_732` happens after the phase completes.

## Global Constraints

- CE code, Apache 2.0 headers. Server commit convention. No Claude trailers.
- CLAUDE.md Java conventions (blank-line rules, descriptive names, no `Impl` in test names, camelCase test methods).
- Gradle discipline: redirect to file, `echo "exit=$?"` own line, grep `'^> Task .* FAILED'`.
- Snapshot regen: delete stale JSON from BOTH `src/test/resources/definition/` and `build/resources/test/definition/`.
- MIRROR THE CONDITION MODULE (`server/libs/modules/task-dispatchers/condition/`) for structure, registration, build wiring, and test idioms — it is the closest sibling (sequential task list + expression evaluation) and just went through the return-values work, so it embodies every current convention.
- `next`-expression evaluation MUST reuse the same hardened evaluation path condition's raw expression uses (read `ConditionTaskUtils` — the SpEL hardening from #5081 is load-bearing; do not evaluate via a fresh SpEL parser).

---

### Task 1: Module scaffold + GraphTaskDispatcher

**Files:**
- Create: `server/libs/modules/task-dispatchers/graph/build.gradle.kts` (copy condition's, adjust)
- Create: `.../graph/src/main/java/com/bytechef/task/dispatcher/graph/constant/GraphTaskDispatcherConstants.java`
- Create: `.../graph/src/main/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcher.java`
- Modify: `settings.gradle.kts` (module entry) + every build file that lists the condition module as a dependency (grep `task-dispatchers:condition` across `server/` — server-app, coordinator/worker apps, test-int-support etc. — add graph to the SAME lists)
- Test: `.../graph/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherTest.java`

**Interfaces:**
- Produces (consumed by Tasks 2-4):

```java
public class GraphTaskDispatcherConstants {

    public static final String GRAPH = "graph";
    public static final String NODES = "nodes";
    public static final String NAME = "name";
    public static final String TASKS = "tasks";
    public static final String NEXT = "next";
    public static final String START_NODE = "startNode";
    public static final String MAX_TRANSITIONS = "maxTransitions";
    public static final String NODE = "__node";
    public static final int DEFAULT_MAX_TRANSITIONS = 100;
}
```

- [ ] **Step 1: Read the condition module end-to-end** — dispatcher, completion handler, both factories, build file, and FIND its registration mechanism (how `ConditionTaskDispatcher`/`ConditionTaskCompletionHandler` become beans/factories the coordinator discovers — configuration classes, `*Factory` interfaces, AutoConfiguration imports). List every registration site in your report.
- [ ] **Step 2: Failing unit test** for `doDispatch`: given a graph task with two nodes, the START node's first task is created with `parameters.__node = "<startName>"`, priority/jobId inherited, context pushed, `counterService.set(graphTaskExecutionId, maxTransitions)` called; `startNode` absent → first declared node; unknown `startNode` → fails with a message naming it; duplicate node names → fails naming the duplicate.
- [ ] **Step 3: Implement `GraphTaskDispatcher`** mirroring `ConditionTaskDispatcher.doDispatch`'s shape (validation first, then dispatch task #1 of the start node's list). Router start node (empty `tasks`): do NOT resolve transitions in the dispatcher — dispatch nothing and emit the same immediate-completion event the condition dispatcher uses for an empty case, but ONLY after seeding the counter; the completion handler owns router-chaining (Task 2) so the logic lives in exactly one place. For an empty start node this means the parent completes through the handler path with `__node` context carried in the event — follow how `ConditionTaskDispatcher` fast-paths an empty case (`TaskExecutionCompleteEvent`) and adapt: stamp the synthetic completion with `__node` so Task 2's handler picks it up and runs the router chain.
- [ ] **Step 4: Run the module test task; green.**
- [ ] **Step 5: Commit** `"Add the graph task dispatcher module and start-node dispatch"`.

---

### Task 2: GraphTaskCompletionHandler — transitions, budget, terminal output

**Files:**
- Create: `.../graph/src/main/java/com/bytechef/task/dispatcher/graph/completion/GraphTaskCompletionHandler.java`
- Test: `.../graph/src/test/java/com/bytechef/task/dispatcher/graph/completion/GraphTaskCompletionHandlerTest.java`

**Interfaces:**
- Consumes Task 1's constants verbatim.
- Produces the runtime contract Tasks 3-4 test against: transition = evaluate `next` → validate target → `counterService.decrement` (already-zero → fail "graph transition budget exhausted (maxTransitions=N)") → dispatch target's task #1 with fresh `__node` stamp; terminal (`next` absent OR evaluates null/blank) → copy last child's output onto the parent (the condition/branch write point: read value, re-store under parent id, `setOutput` before `update`) → `setEndDate` → outer `taskCompletionHandler.handle(parent)`.

- [ ] **Step 1: Failing unit tests**: (a) `canHandle` = parent is `graph/v1` AND `__node` present; (b) mid-node advancement dispatches taskNumber+1 within the same node; (c) node exhaustion + `next` → target's first task dispatched with new `__node`, counter decremented; (d) budget exhausted → run fails with the pinned message; (e) unknown target → fails naming node + resolved value; (f) terminal → parent gets the child's output re-stored under its own id; (g) ROUTER CHAIN: transition into an empty node evaluates that node's `next` immediately, consuming one budget unit per hop, looping until a non-empty node or terminal (a pure-router cycle must die by budget, not hang); (h) `next` evaluation reuses the hardened expression path (structure the code so the test can pin which utility evaluates it).
- [ ] **Step 2: Implement**, mirroring `ConditionTaskCompletionHandler.handle`'s skeleton (context push for named children first, then advance-or-complete) with the transition block replacing the complete-only else. Router chaining is a loop INSIDE the handler's transition step, not recursion through dispatch.
- [ ] **Step 3: Module tests green.**
- [ ] **Step 4: Commit** `"Add graph transitions with budget enforcement and terminal output"`.

---

### Task 3: Integration tests

**Files:**
- Create: `.../graph/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherIntTest.java`
- Create: workflow YAMLs under `.../graph/src/test/resources/workflows/` (one per scenario, following the condition module's fixtures + `TaskDispatcherJobTestExecutor` idiom)

**Interfaces:** none new — this pins the whole runtime contract through the real engine.

- [ ] **Step 1: Fixtures + tests** (workflow-level `outputs: [{name: result, value: "${graph_1}"}]` + `readJobOutputs`, the established idiom):
  - forward flow A→B→terminal, `${graph_1}` = B's last-task output
  - back-jump cycle A→B→A→C driven by a var-task counter expression; asserts the revisit executed (accumulated context, last-write-wins pinned via the final value)
  - budget exhaustion: two nodes ping-ponging with `maxTransitions: 3` → job fails, error message pinned
  - router node (empty `tasks` with `next`) forwards correctly
  - unknown target → job fails, message pinned
  - nested condition as a node's last task → composed output propagates (the return-values composition)
  - graph inside a fork/join branch → `branch_i` carries the graph's output
- [ ] **Step 2: Run module `testIntegration`; green.** If the graph module needs registration in the shared test-int support to be discovered, mirror how condition's IntTest wires its dispatcher/handler into `TaskDispatcherJobTestExecutor` (read that test's setup — registration is likely per-test, not global).
- [ ] **Step 3: Commit** `"Pin the graph dispatcher runtime contract with integration tests"`.

---

### Task 4: Definition factory, design-time output, editor exclusion

**Files:**
- Create: `.../graph/src/main/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherDefinitionFactory.java`
- Create: `.../graph/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherDefinitionFactoryTest.java` (JsonFileAssert snapshot)
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/.../facade/WorkflowNodeOutputFacadeImpl.java` (`getChildWorkflowTasks` gains a `graph` arm flattening `nodes[*].tasks`)
- Test: extend `WorkflowNodeOutputFacadeTest` with the graph exclusion test (mirror the condition/branch/fork-join per-type tests added in the return-values feature)

**Interfaces:**
- Consumes `TaskListOutputDataSource` verbatim (constants alias `MapDataSource`'s `"__workflowId"`/`"__environmentId"` literals — see the return-values spec decisions log; do NOT invent new keys).

- [ ] **Step 1: Factory** — `taskDispatcher(GRAPH)` with `.title("Graph")`, description, icon (add `graph.svg` asset following condition's), properties: `string(START_NODE)` (optional), `integer(MAX_TRANSITIONS)` (optional, default 100), and the `nodes` structure as `taskProperties` (READ how fork/join declares `array(BRANCHES).items(array().items(task()))` and shape `array(NODES).items(object().properties(string(NAME), string(NEXT), array(TASKS).items(task())))` — verify the DSL supports object-wrapped task lists in taskProperties; if it does not, document the closest representable shape in your report and match what the runtime actually reads).
- [ ] **Step 2: `.output(...)`** via `Optional<TaskListOutputDataSource>`: find the FIRST node whose `next` is absent/blank (the first declared terminal), resolve its last task via `getLastTaskOutput`, return UNWRAPPED `OutputResponse`; no terminal or empty tasks → null. Mirror the condition factory's `output(...)` structure (post-MethodLength-fix shape — keep the constructor short).
- [ ] **Step 3: Snapshot** — run the factory test to generate `graph_v1.json`; verify the dynamic-marker `outputDefinition` form.
- [ ] **Step 4: Facade exclusion** — `graph` arm returning `nodes[*].tasks` flattened; per-type exclusion test (the divergent-extraction lesson: every type gets its own test).
- [ ] **Step 5: Run graph module `test` + `platform-configuration-service:test`; green.**
- [ ] **Step 6: Commit** `"Add the graph dispatcher definition with design-time output and editor exclusion"`.

---

### Task 5: Gates + spec close-out

- [ ] **Step 1: Spec** — mark Phase 1 implemented in the spec's phase list (keep overall Status: Draft until all phases land; add a per-phase status line), append any implementation-forced decisions to the decisions log.
- [ ] **Step 2: Gates in order** (in the WORKTREE): `./gradlew spotlessApply`; `./gradlew compileJava compileTestJava --continue`; `:server:libs:modules:task-dispatchers:graph:check` + `:testIntegration`; `platform-configuration-service:check`; re-run condition/branch/fork-join/map `:test` (regression sweep over the shared patterns).
- [ ] **Step 3: Commit** `"Mark graph dispatcher phase 1 implemented"`.
