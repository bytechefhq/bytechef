# Graph Dispatcher Edge-List DSL Implementation Plan (Plan A — server)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `graph/v1`'s shape so `nodes` is a plain task list and transitions are an explicit `transitions: [{from, to, condition?}]` edge list, evaluated "conditional edges in declared order, then the unconditional default", while keeping every engine-level guarantee (budget, accumulated context, `__node` stamp, nothing under `server/libs/atlas/`).

**Architecture:** All changes live in `server/libs/modules/task-dispatchers/graph` plus one arm of `WorkflowNodeOutputFacadeImpl` and the user docs. The dispatcher dispatches one task per node; the completion handler evaluates the completed node's outgoing transitions; `GraphTaskUtils.resolveTransition` is the single place the evaluation rule exists. Router hand-off machinery (`__routerNode`) is deleted with the router concept.

**Tech Stack:** Java 25, Spring Boot 4, Atlas coordinator dispatcher SPI (`ErrorHandlingTaskDispatcher`, `TaskCompletionHandler`), `SpelEvaluator`, JUnit 5 + Mockito, `@TaskDispatcherIntTest` harness, Spotless.

**Spec:** `docs/superpowers/specs/2026-08-17-graph-dispatcher-freeform-canvas-design.md` (sections "DSL", "Server changes", "Design-time output").

## Global Constraints

- `graph/v1` is unreleased: replace the shape in place, keep the type name/version, no migration.
- Nothing under `server/libs/atlas/` changes.
- Evaluation rule (verbatim from spec): conditional transitions of the completed node in declared order, first truthy `condition` wins; if none matched take the first unconditional one; none → terminal. A dynamic `to` that resolves blank counts as "did not match" and evaluation continues.
- Blank-line-before-control-statement and blank-line-after-variable-modification Java style rules (CLAUDE.md); Checkstyle test names camelCase without underscores; no `TODO` comments.
- Run `./gradlew spotlessApply` before every commit; verify Gradle results by redirecting to a file and checking `$?`, never through a pipe.
- Commit messages: `732 <description>` (server-side convention).

Module path used throughout: `MODULE=server/libs/modules/task-dispatchers/graph`.
Gradle project used throughout: `:server:libs:modules:task-dispatchers:graph`.

---

### Task 1: Constants and definition factory

**Files:**
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/constant/GraphTaskDispatcherConstants.java`
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherDefinitionFactory.java`
- Delete then regenerate: `$MODULE/src/test/resources/definition/graph_v1.json` (and `$MODULE/build/resources/test/definition/graph_v1.json`)
- Test: `$MODULE/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherDefinitionFactoryTest.java` (existing snapshot test — unchanged)

**Interfaces:**
- Produces constants used by every later task: `NODES = "nodes"`, `TRANSITIONS = "transitions"`, `FROM = "from"`, `TO = "to"`, `CONDITION = "condition"`, `START_NODE`, `MAX_TRANSITIONS`, `NODE = "__node"`, `DEFAULT_MAX_TRANSITIONS = 100`. Removes `TASKS`, `NEXT`, `ROUTER_NODE`.

- [ ] **Step 1: Rewrite the constants class**

```java
public class GraphTaskDispatcherConstants {

    public static final String GRAPH = "graph";
    public static final String NODES = "nodes";
    public static final String NAME = "name";
    public static final String TRANSITIONS = "transitions";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String CONDITION = "condition";
    public static final String START_NODE = "startNode";
    public static final String MAX_TRANSITIONS = "maxTransitions";
    public static final String NODE = "__node";

    public static final int DEFAULT_MAX_TRANSITIONS = 100;
}
```

- [ ] **Step 2: Rewrite the definition factory's properties**

Replace the `taskDispatcher(GRAPH)` builder body and delete `nodeProperty()`; keep `output(...)` but change terminal detection to "no outgoing transition":

```java
this.taskDispatcherDefinition = taskDispatcher(GRAPH)
    .title("Graph")
    .description(
        "Runs a set of task nodes wired by transitions: after a node completes, its conditional transitions are checked in order, then its default one; a node with no matching transition ends the graph. Cycles are allowed and bounded by a transition budget.")
    .icon("path:assets/graph.svg")
    .properties(
        string(START_NODE)
            .label("Start Node")
            .description(
                "The name of the node execution begins from. Defaults to the first declared node when left empty."),
        integer(MAX_TRANSITIONS)
            .label("Max Transitions")
            .description(
                "The maximum number of node-to-node transitions allowed before the graph is halted, to guard against infinite loops.")
            .defaultValue(DEFAULT_MAX_TRANSITIONS),
        array(TRANSITIONS)
            .label("Transitions")
            .description("The edges between nodes. Conditional transitions are checked in declared order, then the unconditional one.")
            .items(
                object()
                    .properties(
                        string(FROM)
                            .label("From")
                            .description("The name of the node this transition leaves.")
                            .required(true),
                        string(TO)
                            .label("To")
                            .description(
                                "The name of the node to transition to, or an expression resolving to one.")
                            .required(true),
                        string(CONDITION)
                            .label("Condition")
                            .description(
                                "An expression that must evaluate to true for this transition to be taken. Absent or blank means unconditional.")
                            .controlType(Property.ControlType.FORMULA_MODE))))
    .output(inputParameters -> taskListOutputDataSource
        .map(dataSource -> output(inputParameters, dataSource))
        .orElse(null))
    .taskProperties(
        array(NODES)
            .description("The task nodes that make up the graph; each entry is one task.")
            .items(task()));
```

Replace `output(...)` and `findFirstTerminalNode(...)`:

```java
protected static OutputResponse output(
    Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

    String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
    long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

    List<Map<String, ?>> nodes = MapUtils.getList(inputParameters, NODES, new TypeReference<>() {}, List.of());
    List<Map<String, ?>> transitions = MapUtils.getList(
        inputParameters, TRANSITIONS, new TypeReference<>() {}, List.of());

    Map<String, ?> terminalNode = findFirstTerminalNode(nodes, transitions);

    if (terminalNode == null) {
        return null;
    }

    String terminalNodeName = MapUtils.getString(terminalNode, "name");
    String terminalNodeType = MapUtils.getString(terminalNode, "type");

    if (terminalNodeType == null) {
        return null;
    }

    OutputResponse terminalNodeOutput = taskListOutputDataSource.getLastTaskOutput(
        workflowId, terminalNodeName, terminalNodeType, environmentId);

    if (terminalNodeOutput == null) {
        return null;
    }

    ModifiableValueProperty<?, ?> terminalNodeSchema =
        (ModifiableValueProperty<?, ?>) terminalNodeOutput.getOutputSchema();

    Object terminalNodeSampleOutput = terminalNodeOutput.getSampleOutput();

    if (terminalNodeSampleOutput != null) {
        return OutputResponse.of(terminalNodeSchema, terminalNodeSampleOutput);
    }

    return OutputResponse.of(terminalNodeSchema);
}

/**
 * Returns the first declared node that has no outgoing transition (no {@code transitions[].from} equal to its
 * name), or {@code null} when every node has one -- a documented approximation, since which node actually ends a
 * run is undecidable statically.
 */
private static Map<String, ?> findFirstTerminalNode(
    List<Map<String, ?>> nodes, List<Map<String, ?>> transitions) {

    Set<String> sourceNodeNames = transitions.stream()
        .map(transition -> MapUtils.getString(transition, FROM))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    for (Map<String, ?> node : nodes) {
        String name = MapUtils.getString(node, "name");

        if (name != null && !sourceNodeNames.contains(name)) {
            return node;
        }
    }

    return null;
}
```

Adjust imports: add `FROM`, `TRANSITIONS`, `CONDITION`, `TO` static imports, `java.util.Objects`, `java.util.Set`, `java.util.stream.Collectors`; remove `NAME`, `NEXT`, `TASKS`, `ModifiableObjectProperty`.

- [ ] **Step 3: Delete the snapshot and regenerate it**

```bash
rm -f server/libs/modules/task-dispatchers/graph/src/test/resources/definition/graph_v1.json server/libs/modules/task-dispatchers/graph/build/resources/test/definition/graph_v1.json
./gradlew :server:libs:modules:task-dispatchers:graph:test --tests '*GraphTaskDispatcherDefinitionFactoryTest*' > /tmp/gradle-a1.log 2>&1; echo "exit=$?"
```

Expected: `exit=0`; `graph_v1.json` regenerated containing `"transitions"` under `properties` and `"nodes"` under `taskProperties` whose `items` is a single `task` entry (no nested `tasks`).

- [ ] **Step 4: Inspect the snapshot** — `grep -c '"name" : "next"' graph_v1.json` prints `0`; `grep -c '"name" : "condition"' graph_v1.json` prints `1`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply > /tmp/spotless.log 2>&1; echo "exit=$?"
git add server/libs/modules/task-dispatchers/graph/src/main/java/com/bytechef/task/dispatcher/graph/constant/GraphTaskDispatcherConstants.java server/libs/modules/task-dispatchers/graph/src/main/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherDefinitionFactory.java server/libs/modules/task-dispatchers/graph/src/test/resources/definition/graph_v1.json
git commit -m "732 Redefine graph/v1 as task nodes wired by an explicit transitions list"
```

(The module does not compile yet — the dispatcher/handler/utils still reference removed constants. That is fine for a definition-only commit? **No** — keep the build green: do Tasks 1–4 on one branch and commit each, but run the compile check at the end of Task 4. If your workflow requires green at every commit, squash Tasks 1–4 into one commit at Task 4.)

---

### Task 2: `GraphTaskUtils` — nodes as tasks, transition resolution

**Files:**
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/util/GraphTaskUtils.java`
- Create: `$MODULE/src/test/java/com/bytechef/task/dispatcher/graph/util/GraphTaskUtilsTest.java`

**Interfaces:**
- Produces:
  - `static List<WorkflowTask> getNodes(Map<String, ?> parameters)`
  - `static Optional<WorkflowTask> findNode(List<WorkflowTask> nodes, String name)`
  - `static List<Map<String, ?>> getTransitions(Map<String, ?> parameters)`
  - `static Optional<String> resolveTransition(Evaluator evaluator, List<Map<String, ?>> transitions, String fromNodeName, Map<String, ?> context)` — the evaluation rule; returns the resolved target node name (unvalidated) or empty for terminal.
  - `static WorkflowTask stampNode(Map<String, ?> workflowTaskMap, String nodeName)` (kept)
  - `static void dispatchNodeTask(ContextService, Evaluator, TaskDispatcher<? super Task>, TaskExecutionService, TaskFileStorage, TaskExecution graphTaskExecution, WorkflowTask nodeTask, String nodeName)` — always `taskNumber = 1`.
- Removes: `stampRouterNode`, `stripRouterNode`, `getNodeWorkflowTasks`, `resolveNext`.

- [ ] **Step 1: Write the failing unit test**

```java
package com.bytechef.task.dispatcher.graph.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class GraphTaskUtilsTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    @Test
    public void testResolveTransitionTakesFirstTruthyConditionalInDeclaredOrder() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "b", "=score > 10"),
            transition("a", "c", "=score > 5"),
            transition("a", "d", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 7));

        assertEquals(Optional.of("c"), target);
    }

    @Test
    public void testResolveTransitionFallsBackToUnconditionalWhenNoConditionMatches() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "d", null),
            transition("a", "b", "=score > 10"));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 1));

        // the unconditional edge is the fallback even though it is declared first
        assertEquals(Optional.of("d"), target);
    }

    @Test
    public void testResolveTransitionPrefersConditionalOverEarlierUnconditional() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "d", null),
            transition("a", "b", "=score > 10"));

        Optional<String> target = GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 11));

        assertEquals(Optional.of("b"), target);
    }

    @Test
    public void testResolveTransitionIsTerminalWhenNothingMatches() {
        List<Map<String, ?>> transitions = List.of(transition("a", "b", "=score > 10"));

        assertTrue(GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of("score", 1))
            .isEmpty());
    }

    @Test
    public void testResolveTransitionIgnoresOtherNodesTransitions() {
        List<Map<String, ?>> transitions = List.of(transition("x", "b", null));

        assertTrue(GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of())
            .isEmpty());
    }

    @Test
    public void testResolveTransitionResolvesDynamicTarget() {
        List<Map<String, ?>> transitions = List.of(transition("a", "=nextNode", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(
            EVALUATOR, transitions, "a", Map.of("nextNode", "review"));

        assertEquals(Optional.of("review"), target);
    }

    @Test
    public void testResolveTransitionSkipsDynamicTargetThatResolvesBlank() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "=nextNode", null),
            transition("a", "fallback", null));

        Optional<String> target = GraphTaskUtils.resolveTransition(
            EVALUATOR, transitions, "a", Map.of("nextNode", ""));

        assertEquals(Optional.of("fallback"), target);
    }

    @Test
    public void testResolveTransitionTakesFirstOfSeveralUnconditional() {
        List<Map<String, ?>> transitions = List.of(
            transition("a", "first", null),
            transition("a", "second", ""));

        assertEquals(Optional.of("first"), GraphTaskUtils.resolveTransition(EVALUATOR, transitions, "a", Map.of()));
    }

    private static Map<String, ?> transition(String from, String to, String condition) {
        if (condition == null) {
            return Map.of("from", from, "to", to);
        }

        return Map.of("from", from, "to", to, "condition", condition);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:modules:task-dispatchers:graph:test --tests '*GraphTaskUtilsTest*' > /tmp/gradle-a2.log 2>&1; echo "exit=$?"
```
Expected: `exit=1` (compilation error: `resolveTransition` undefined).

- [ ] **Step 3: Rewrite `GraphTaskUtils`**

Keep `stampNode` and the class Javadoc (reworded: "node lookup, transition resolution and node dispatch"). Replace everything from `stampRouterNode` down with:

```java
/**
 * Reads the {@code nodes} task list out of a graph task's (already resolved) parameters, without validation --
 * the dispatcher is the only place non-empty/unique-name validation is enforced, at dispatch time.
 */
public static List<WorkflowTask> getNodes(Map<String, ?> parameters) {
    return MapUtils.getList(parameters, NODES, new TypeReference<Map<String, ?>>() {}, List.of())
        .stream()
        .map(WorkflowTask::new)
        .toList();
}

public static Optional<WorkflowTask> findNode(List<WorkflowTask> nodes, String name) {
    return nodes.stream()
        .filter(node -> Objects.equals(node.getName(), name))
        .findFirst();
}

public static List<Map<String, ?>> getTransitions(Map<String, ?> parameters) {
    return MapUtils.getList(parameters, TRANSITIONS, new TypeReference<Map<String, ?>>() {}, List.of());
}

/**
 * Resolves which node, if any, {@code fromNodeName} transitions to: its CONDITIONAL transitions in declared order,
 * the first whose {@code condition} evaluates truthy wins; if none matched, its first UNCONDITIONAL transition; if
 * there is none the node is terminal. A {@code to} that is an expression is evaluated against the context; one that
 * resolves to null/blank counts as "did not match" and evaluation continues with the next candidate. The returned
 * name is NOT validated against the node list -- the completion handler does that so the error can name the source.
 */
public static Optional<String> resolveTransition(
    Evaluator evaluator, List<Map<String, ?>> transitions, String fromNodeName, Map<String, ?> context) {

    List<Map<String, ?>> outgoingTransitions = transitions.stream()
        .filter(transition -> Objects.equals(MapUtils.getString(transition, FROM), fromNodeName))
        .toList();

    for (Map<String, ?> transition : outgoingTransitions) {
        if (!isConditional(transition)) {
            continue;
        }

        if (evaluatesTruthy(evaluator, MapUtils.getRequiredString(transition, CONDITION), context)) {
            String target = resolveTarget(evaluator, transition, context);

            if (target != null) {
                return Optional.of(target);
            }
        }
    }

    for (Map<String, ?> transition : outgoingTransitions) {
        if (isConditional(transition)) {
            continue;
        }

        String target = resolveTarget(evaluator, transition, context);

        if (target != null) {
            return Optional.of(target);
        }
    }

    return Optional.empty();
}

/**
 * Creates, evaluates, persists, and dispatches {@code nodeTask} as the single task of {@code nodeName}, stamped
 * with {@code __node} -- the one dispatch path shared by the dispatcher's start-node dispatch and the completion
 * handler's post-transition dispatch.
 */
public static void dispatchNodeTask(
    ContextService contextService, Evaluator evaluator, TaskDispatcher<? super Task> taskDispatcher,
    TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
    TaskExecution graphTaskExecution, WorkflowTask nodeTask, String nodeName) {

    long graphTaskExecutionId = Validate.notNull(graphTaskExecution.getId(), "id");

    TaskExecution subTaskExecution = TaskExecution.builder()
        .jobId(graphTaskExecution.getJobId())
        .maxRetries(nodeTask.getMaxRetries())
        .parentId(graphTaskExecutionId)
        .priority(graphTaskExecution.getPriority())
        .taskNumber(1)
        .workflowTask(stampNode(nodeTask.toMap(), nodeName))
        .build();

    Map<String, ?> context = taskFileStorage.readContextValue(
        contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION));

    subTaskExecution.evaluate(context, evaluator);

    subTaskExecution = taskExecutionService.create(subTaskExecution);

    long subTaskExecutionId = Validate.notNull(subTaskExecution.getId(), "id");

    contextService.push(
        subTaskExecutionId, Context.Classname.TASK_EXECUTION,
        taskFileStorage.storeContextValue(subTaskExecutionId, Context.Classname.TASK_EXECUTION, context));

    taskDispatcher.dispatch(subTaskExecution);
}

private static boolean isConditional(Map<String, ?> transition) {
    String condition = MapUtils.getString(transition, CONDITION);

    return condition != null && !condition.isBlank();
}

private static boolean evaluatesTruthy(Evaluator evaluator, String conditionExpression, Map<String, ?> context) {
    Map<String, ?> evaluated = evaluator.evaluate(Map.of(CONDITION, conditionExpression), context);

    Object value = evaluated.get(CONDITION);

    if (value instanceof Boolean booleanValue) {
        return booleanValue;
    }

    return value != null && Boolean.parseBoolean(String.valueOf(value));
}

private static String resolveTarget(Evaluator evaluator, Map<String, ?> transition, Map<String, ?> context) {
    String toExpression = MapUtils.getString(transition, TO);

    if (toExpression == null || toExpression.isBlank()) {
        return null;
    }

    Map<String, ?> evaluated = evaluator.evaluate(Map.of(TO, toExpression), context);

    Object toValue = evaluated.get(TO);

    if (toValue == null) {
        return null;
    }

    String target = String.valueOf(toValue)
        .trim();

    return target.isEmpty() ? null : target;
}
```

Imports: replace `NEXT`, `ROUTER_NODE`, `TASKS`, `NAME` static imports with `CONDITION`, `FROM`, `TO`, `TRANSITIONS`; drop `LinkedHashMap`.

- [ ] **Step 4: Run the test to verify it passes**

Same command as Step 2. Expected: `exit=0` (only this test class compiles yet if the dispatcher/handler still fail — if the module does not compile because Tasks 3–4 are pending, temporarily run after Task 4; the plan's ordering assumes you proceed straight to Task 3).

- [ ] **Step 5: Commit** (with Task 4, see note in Task 1) — message `732 Resolve graph transitions from the edge list in GraphTaskUtils`.

---

### Task 3: `GraphTaskDispatcher` — one task per node, no router

**Files:**
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcher.java`
- Modify: `$MODULE/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherTest.java`

**Interfaces:**
- Consumes `GraphTaskUtils.getNodes/findNode/dispatchNodeTask` from Task 2.
- Constructor unchanged: `(ContextService, CounterService, Evaluator, ApplicationEventPublisher, TaskDispatcher<? super Task>, TaskExecutionService, TaskFileStorage)` — the `eventPublisher` stays only because `ErrorHandlingTaskDispatcher` needs it.

- [ ] **Step 1: Rewrite the tests**

Delete `testDispatchStartsRouterHandOffForEmptyStartNode`. Change the two remaining "start" tests and the helpers so a node IS a task. Replace the helper section at the bottom of the file with:

```java
private static TaskExecution graphTaskExecution(List<Map<String, ?>> nodes, String startNode, Integer maxTransitions) {
    Map<String, Object> parameters = new HashMap<>();

    parameters.put(NODES, nodes);

    if (startNode != null) {
        parameters.put(START_NODE, startNode);
    }

    if (maxTransitions != null) {
        parameters.put(MAX_TRANSITIONS, maxTransitions);
    }

    return TaskExecution.builder()
        .id(1L)
        .jobId(2L)
        .priority(5)
        .workflowTask(
            new WorkflowTask(
                Map.of(
                    WorkflowConstants.NAME, "graphTask",
                    WorkflowConstants.TYPE, "graph/v1",
                    WorkflowConstants.PARAMETERS, parameters)))
        .build();
}

private static Map<String, ?> printTask(String name) {
    return Map.of(WorkflowConstants.NAME, name, WorkflowConstants.TYPE, "print");
}
```

and rewrite the test bodies to pass tasks directly, e.g.:

```java
@Test
public void testDispatchStartsExplicitStartNode() {
    when(contextService.peek(anyLong(), any()))
        .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));

    TaskExecution graphTaskExecution = graphTaskExecution(
        List.of(printTask("classify"), printTask("approve")), "approve", null);

    when(taskExecutionService.update(any()))
        .thenReturn(graphTaskExecution);
    when(taskExecutionService.create(any()))
        .thenReturn(
            TaskExecution.builder()
                .id(2L)
                .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approve", "type", "print")))
                .build());

    GraphTaskDispatcher dispatcher = new GraphTaskDispatcher(
        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
        taskFileStorage);

    dispatcher.dispatch(graphTaskExecution);

    verify(counterService, times(1)).set(1L, 100);
    verify(eventPublisher, never()).publishEvent(any());

    ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

    verify(taskExecutionService, times(1)).create(createCaptor.capture());

    TaskExecution createdSubTaskExecution = createCaptor.getValue();

    Assertions.assertEquals("approve", createdSubTaskExecution.getParameters().get(NODE));
    Assertions.assertEquals("approve", createdSubTaskExecution.getName());
    Assertions.assertEquals(1L, createdSubTaskExecution.getParentId());
    Assertions.assertEquals(1, createdSubTaskExecution.getTaskNumber());

    verify(taskDispatcher, times(1)).dispatch(any());
}
```

Keep (adapted to `printTask(...)` nodes): `testDispatchDefaultsToFirstDeclaredNodeWhenStartNodeAbsent` (asserts `__node == "classify"`, the first declared), `testDispatchSeedsCounterWithDefaultMaxTransitions`, `testDispatchWhenStartNodeUnknownFails` (message `Unknown graph start node: 'ghost'`), `testDispatchWhenDuplicateNodeNamesFails` (message `Duplicate graph node name: 'classify'`). Add:

```java
@Test
public void testDispatchWhenNodesEmptyFails() {
    TaskExecution graphTaskExecution = graphTaskExecution(List.of(), null, null);

    GraphTaskDispatcher dispatcher = new GraphTaskDispatcher(
        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
        taskFileStorage);

    IllegalArgumentException exception = Assertions.assertThrows(
        IllegalArgumentException.class, () -> dispatcher.dispatch(graphTaskExecution));

    Assertions.assertEquals("graph must define at least one node", exception.getMessage());
}
```

(`ErrorHandlingTaskDispatcher.dispatch` may wrap/publish instead of rethrowing — check the existing `testDispatchWhenStartNodeUnknownFails` for how failures are asserted in this file and mirror it exactly.)

- [ ] **Step 2: Run to verify failure** — `./gradlew :server:libs:modules:task-dispatchers:graph:test --tests '*GraphTaskDispatcherTest*' > /tmp/gradle-a3.log 2>&1; echo "exit=$?"` → `exit=1`.

- [ ] **Step 3: Rewrite the dispatcher**

```java
public class GraphTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public GraphTaskDispatcher(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        ApplicationEventPublisher eventPublisher, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        super(eventPublisher);

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void doDispatch(TaskExecution taskExecution) {
        List<WorkflowTask> nodes = GraphTaskUtils.getNodes(taskExecution.getParameters());

        Validate.isTrue(!nodes.isEmpty(), "graph must define at least one node");

        validateUniqueNodeNames(nodes);

        WorkflowTask startNode = resolveStartNode(taskExecution, nodes);

        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

        int maxTransitions = MapUtils.getInteger(
            taskExecution.getParameters(), MAX_TRANSITIONS, DEFAULT_MAX_TRANSITIONS);

        counterService.set(taskExecutionId, maxTransitions);

        GraphTaskUtils.dispatchNodeTask(
            contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage, taskExecution,
            startNode, startNode.getName());
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), GRAPH + "/v1")) {
            return this;
        }

        return null;
    }

    private static void validateUniqueNodeNames(List<WorkflowTask> nodes) {
        Set<String> nodeNames = new HashSet<>();

        for (WorkflowTask node : nodes) {
            String name = Validate.notBlank(node.getName(), "graph node name");

            if (!nodeNames.add(name)) {
                throw new IllegalArgumentException("Duplicate graph node name: '" + name + "'");
            }
        }
    }

    private static WorkflowTask resolveStartNode(TaskExecution taskExecution, List<WorkflowTask> nodes) {
        String startNodeName = MapUtils.getString(taskExecution.getParameters(), START_NODE);

        if (startNodeName == null || startNodeName.isBlank()) {
            return nodes.getFirst();
        }

        return GraphTaskUtils.findNode(nodes, startNodeName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown graph start node: '" + startNodeName + "'"));
    }
}
```

Class Javadoc: describe "dispatches the start node's task stamped `__node`; transitions are owned by `GraphTaskCompletionHandler`". Remove the `TaskExecutionCompleteEvent`, `NAME`, `NODES`, `TASKS`, `TypeReference`, `HashSet` (keep), `eventPublisher` field.

- [ ] **Step 4: Run tests** — same command → `exit=0` (after Task 4 compiles the handler).

- [ ] **Step 5: Commit** — `732 Dispatch one task per graph node and drop router hand-off`.

---

### Task 4: `GraphTaskCompletionHandler` — evaluate transitions

**Files:**
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/completion/GraphTaskCompletionHandler.java`
- Modify: `$MODULE/src/test/java/com/bytechef/task/dispatcher/graph/completion/GraphTaskCompletionHandlerTest.java`

**Interfaces:**
- Consumes `GraphTaskUtils.getNodes/findNode/getTransitions/resolveTransition/dispatchNodeTask`.
- Constructor unchanged.

- [ ] **Step 1: Rewrite the tests**

Delete every router test (`testCanHandleReturnsTrueForRouterHandOff`, `testHandleRouterHandOffTransitionsToTargetNode`, `testHandleNestedRouterHandOff…`, `testHandleRouterChainHopsThroughEmptyNodesToRealNode`, `testHandlePureRouterCycleDiesByBudgetInsteadOfHanging`, `testHandleDispatchesNextTaskWithinSameNode`) and the `routerHandOffTaskExecution` / `nestedRouterHandOffTaskExecution` helpers. Replace `graphTaskExecution(id, nodes, maxTransitions)` with:

```java
private static TaskExecution graphTaskExecution(
    long id, List<Map<String, ?>> nodes, List<Map<String, ?>> transitions, Integer maxTransitions) {

    Map<String, Object> parameters = new HashMap<>();

    parameters.put(NODES, nodes);
    parameters.put(TRANSITIONS, transitions);

    if (maxTransitions != null) {
        parameters.put(MAX_TRANSITIONS, maxTransitions);
    }

    return TaskExecution.builder()
        .id(id)
        .jobId(100L)
        .priority(5)
        .status(TaskExecution.Status.STARTED)
        .workflowTask(
            new WorkflowTask(
                Map.of(
                    WorkflowConstants.NAME, "graphTask",
                    WorkflowConstants.TYPE, "graph/v1",
                    WorkflowConstants.PARAMETERS, parameters)))
        .build();
}

private static Map<String, ?> transition(String from, String to, String condition) {
    Map<String, Object> transition = new HashMap<>();

    transition.put(FROM, from);
    transition.put(TO, to);

    if (condition != null) {
        transition.put(CONDITION, condition);
    }

    return transition;
}
```

`childTaskExecution(id, parentId, taskNumber, nodeName, name, output)` stays (a completed node task carries `__node = nodeName` and `name = nodeName`). Tests to keep/adapt (nodes are `printTask("classify")` etc.; `nodeName == task name`):

- `testCanHandleReturnsTrueForNodeCompletion`, `testCanHandleReturnsFalseWhenNodeStampAbsent`, `testCanHandleReturnsFalseWhenParentIsNotGraph`.
- `testHandleTransitionsToTargetNodeAndDecrementsCounter`: transitions `[transition("classify","approve",null)]`; assert `create` captured `__node == "approve"`, `counterService.decrement(1L)` called once, `taskDispatcher.dispatch` once.
- `testHandleTakesFirstTruthyConditionalBeforeDefault`: transitions `[("classify","reject","=false"), ("classify","review","=true"), ("classify","approve",null)]` → `__node == "review"`.
- `testHandleFallsBackToDefaultWhenNoConditionMatches`: `[("classify","review","=false"), ("classify","approve",null)]` → `"approve"`.
- `testHandleThrowsWhenTransitionBudgetExhausted` (message pinned: `graph transition budget exhausted (maxTransitions=1)` when `counterService.decrement` returns `-1` and `maxTransitions = 1`).
- `testHandleThrowsWhenTransitionTargetUnknown`: `[("classify","missingNode",null)]` → `IllegalArgumentException` message `Unknown graph transition target node: 'missingNode' resolved from node 'classify'`.
- `testHandleCompletesGraphWithTerminalNodeOutput` (no transitions from `approve`) — unchanged assertions.
- `testHandleEvaluatesConditionThroughInjectedEvaluator` (spy evaluator, `atLeastOnce()`).
- New `testHandleResolvesDynamicTargetExpression`: `[("classify","=${score} > 0.5 ? 'review' : 'approve'",null)]` with context `score = 0.9` pushed via `contextService.peek` → `__node == "review"`.

- [ ] **Step 2: Run to verify failure** — `--tests '*GraphTaskCompletionHandlerTest*'` → `exit=1`.

- [ ] **Step 3: Rewrite the handler**

```java
public class GraphTaskCompletionHandler implements TaskCompletionHandler {

    // fields + constructor unchanged

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        if (MapUtils.getString(taskExecution.getParameters(), NODE) == null) {
            return false;
        }

        Long parentId = taskExecution.getParentId();

        if (parentId == null) {
            return false;
        }

        TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

        return Objects.equals(parentTaskExecution.getType(), GRAPH + "/v1");
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution graphTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        long graphTaskExecutionId = Objects.requireNonNull(graphTaskExecution.getId());
        String nodeName = MapUtils.getRequiredString(taskExecution.getParameters(), NODE);

        pushCompletedNodeOutputToContext(taskExecution, graphTaskExecutionId);

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION));

        Map<String, ?> graphParameters = graphTaskExecution.getParameters();

        Optional<String> targetNodeName = GraphTaskUtils.resolveTransition(
            evaluator, GraphTaskUtils.getTransitions(graphParameters), nodeName, context);

        if (targetNodeName.isEmpty()) {
            completeGraph(graphTaskExecution, taskExecution);

            return;
        }

        String resolvedTargetNodeName = targetNodeName.get();

        WorkflowTask targetNode = GraphTaskUtils.findNode(GraphTaskUtils.getNodes(graphParameters), resolvedTargetNodeName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown graph transition target node: '" + resolvedTargetNodeName + "' resolved from node '"
                    + nodeName + "'"));

        decrementTransitionBudget(graphTaskExecution, graphTaskExecutionId);

        GraphTaskUtils.dispatchNodeTask(
            contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage, graphTaskExecution,
            targetNode, resolvedTargetNodeName);
    }

    // pushCompletedNodeOutputToContext == old pushCompletedTaskOutputToContext (renamed)
    // decrementTransitionBudget unchanged
    // completeGraph: keep the output copy + endDate + update + taskCompletionHandler.handle;
    //                DELETE the router-stamp stripping block and hasRouterNodeStamp/isRouterHandOff/isGraphType/
    //                hasNodeStamp/isNodeSubTaskCompletion/requireNode helpers.
}
```

Class Javadoc: "Completes a `graph/v1` node: pushes the node's output to the accumulated context, resolves its transition per the conditional-then-default rule (`GraphTaskUtils.resolveTransition`), enforces the budget, dispatches the target or completes the graph." Imports: `Optional`, `WorkflowTask`; drop `ROUTER_NODE`, `List`, `HashMap` if unused.

- [ ] **Step 4: Compile and run all three unit test classes**

```bash
./gradlew :server:libs:modules:task-dispatchers:graph:test --tests '*GraphTaskUtilsTest*' --tests '*GraphTaskDispatcherTest*' --tests '*GraphTaskCompletionHandlerTest*' > /tmp/gradle-a4.log 2>&1; echo "exit=$?"
```
Expected: `exit=0`.

- [ ] **Step 5: Commit** — `732 Evaluate graph transitions conditional-first then default on node completion`.

---

### Task 5: Configuration — defer `transitions`

**Files:**
- Modify: `$MODULE/src/main/java/com/bytechef/task/dispatcher/graph/config/GraphTaskDispatcherConfiguration.java:51`
- Verify: `server/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-test-int-support/src/main/java/com/bytechef/platform/workflow/task/dispatcher/test/config/DeferredEvaluationParameterKeysLoader.java` (force-loads every `*TaskDispatcherConfiguration` — no change expected)

- [ ] **Step 1: Register both keys**

```java
static {
    DeferredEvaluationParameterKeys.register(GRAPH + "/", NODES, TRANSITIONS);
}
```

Update the class Javadoc: `nodes` AND `transitions` are deferred — evaluating a transition's `condition`/`to` at graph dispatch would resolve it against a context that lacks the node outputs it references; the handler evaluates them at transition time. Add the `TRANSITIONS` static import.

- [ ] **Step 2: Add a registration test** to `GraphTaskDispatcherTest`:

```java
@Test
public void testDeferredKeysCoverNodesAndTransitions() {
    // force the static initializer
    Assertions.assertNotNull(GraphTaskDispatcherConfiguration.class.getName());

    Set<String> deferredKeys = DeferredEvaluationParameterKeys.forTaskType("graph/v1");

    Assertions.assertTrue(deferredKeys.contains("nodes"));
    Assertions.assertTrue(deferredKeys.contains("transitions"));
}
```
(Use `Class.forName("com.bytechef.task.dispatcher.graph.config.GraphTaskDispatcherConfiguration")` if referencing the class does not trigger the static block.)

- [ ] **Step 3: Run** the dispatcher test class → `exit=0`. **Commit** — `732 Defer graph transitions evaluation until node completion`.

---

### Task 6: IntTests and YAML fixtures

**Files:**
- Modify: `$MODULE/src/test/java/com/bytechef/task/dispatcher/graph/GraphTaskDispatcherIntTest.java`
- Replace fixtures under `$MODULE/src/test/resources/workflows/` (delete `graph_v1-router.yaml`, `graph_v1-pureRouter.yaml`, `graph_v1-nestedGraphEmptyStart.yaml`; rewrite the rest; add `graph_v1-conditionalFanOut.yaml`, `graph_v1-dynamicTarget.yaml`, `graph_v1-duplicateDefault.yaml`)

The harness (`TaskDispatcherJobTestExecutor`, `TestVarTaskHandler`, `var/v1/set`) is unchanged.

- [ ] **Step 1: Write the fixtures**

`graph_v1-forward.yaml`:
```yaml
label: "Graph Forward"
outputs:
- name: "result"
  value: "${graph_1}"
tasks:
- name: "graph_1"
  type: "graph/v1"
  parameters:
    nodes:
    - name: "aTask"
      type: "var/v1/set"
      parameters:
        value: "A output"
    - name: "bTask"
      type: "var/v1/set"
      parameters:
        value: "B output"
    transitions:
    - from: "aTask"
      to: "bTask"
```

`graph_v1-backJumpCycle.yaml`:
```yaml
label: "Graph Back-Jump Cycle"
inputs:
- name: "counter"
  type: "integer"
  required: true
outputs:
- name: "result"
  value: "${graph_1}"
tasks:
- name: "graph_1"
  type: "graph/v1"
  parameters:
    nodes:
    - name: "counter"
      type: "var/v1/set"
      parameters:
        value: "=counter + 1"
    - name: "bVisit"
      type: "var/v1/set"
      parameters:
        value: "=counter"
    - name: "cResult"
      type: "var/v1/set"
      parameters:
        value: "=counter"
    transitions:
    - from: "counter"
      to: "bVisit"
    - from: "bVisit"
      to: "counter"
      condition: "=counter < 2"
    - from: "bVisit"
      to: "cResult"
```

`graph_v1-budgetExhausted.yaml` (two nodes `x`/`y` looping unconditionally, `maxTransitions: 3`).

`graph_v1-unknownTarget.yaml` (one node with `transitions: [{from: aTask, to: missingNode}]`).

`graph_v1-conditionalFanOut.yaml`:
```yaml
label: "Graph Conditional Fan-Out"
inputs:
- name: "score"
  type: "integer"
  required: true
outputs:
- name: "result"
  value: "${graph_1}"
tasks:
- name: "graph_1"
  type: "graph/v1"
  parameters:
    nodes:
    - name: "classify"
      type: "var/v1/set"
      parameters:
        value: "=score"
    - name: "high"
      type: "var/v1/set"
      parameters:
        value: "high"
    - name: "medium"
      type: "var/v1/set"
      parameters:
        value: "medium"
    - name: "low"
      type: "var/v1/set"
      parameters:
        value: "low"
    transitions:
    - from: "classify"
      to: "high"
      condition: "=classify > 80"
    - from: "classify"
      to: "medium"
      condition: "=classify > 40"
    - from: "classify"
      to: "low"
```

`graph_v1-dynamicTarget.yaml` (input `nextNode` string; node `router` sets `value: "=nextNode"`; transitions `[{from: router, to: "=router"}]`; nodes `review`/`approve` each set a literal).

`graph_v1-duplicateDefault.yaml` (node `a` with two unconditional transitions to `first` and `second`; expects `first`).

`graph_v1-nestedCondition.yaml` (a `condition/v1` task as the single node, output composes as before).

`graph_v1-forkJoinBranch.yaml` (a graph inside a fork-join branch — keep, adapt shape).

`graph_v1-nestedGraph.yaml` (outer graph whose node is an inner `graph/v1`; inner has two nodes + one transition; outer transition from the inner graph to a final node; asserts both ran and the outer result).

- [ ] **Step 2: Rewrite the IntTest methods** — one per fixture, keeping the existing assertion style (`testVarTaskHandler.get(...)`, `jobExecution.taskExecutions()` counts, `taskFileStorage.readJobOutputs`). Budget-exhaustion and unknown-target keep the `ExecutionException` assertion pattern with the pinned messages. `testDispatchConditionalFanOut` runs three times with `score = 90/50/10` and asserts `high/medium/low`. `testDispatchDynamicTarget` runs with `nextNode = "review"` and asserts `review` ran and `approve` did not. `testDispatchDuplicateDefaultTakesFirstDeclared` asserts `first` ran.

- [ ] **Step 3: Run the IntTests**

```bash
./gradlew :server:libs:modules:task-dispatchers:graph:testIntegration > /tmp/gradle-a6.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|tests completed" /tmp/gradle-a6.log
```
Expected: `exit=0`.

- [ ] **Step 4: Commit** — `732 Pin graph edge-list semantics end-to-end through the atlas engine`.

---

### Task 7: Output facade arm, docs, full check

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/WorkflowNodeOutputFacadeImpl.java:314-321`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/facade/WorkflowNodeOutputFacadeTest.java:396-450` (graph fixture shape)
- Modify: `docs/content/docs/platform/automation/build/workflows/flow-controls.mdx` (Graph section, lines 35-110)
- Regenerate: `docs/content/docs/reference/flow-controls/graph_v1.mdx` via `./gradlew generateDocumentation`

- [ ] **Step 1: Simplify the facade arm**

```java
} else if (Objects.equals(workflowNodeType.name(), "graph")) {
    return getWorkflowTaskList(workflowTask.getParameters(), "nodes");
} else {
```

- [ ] **Step 2: Update the facade test's graph fixture** so `parameters.nodes` is a task list (`List.of(Map.of("name","nested1","type","var/v1/set",...))`) — the assertion "a task nested inside a graph must not see the enclosing graph's aggregate output" is unchanged. Run:

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*WorkflowNodeOutputFacadeTest*' > /tmp/gradle-a7.log 2>&1; echo "exit=$?"
```

- [ ] **Step 3: Docs** — rewrite the Graph section of `flow-controls.mdx`: nodes are tasks; transitions block with `from/to/condition`; the evaluation rule sentence verbatim from the spec; dynamic `to` for the LLM-routed example (`to: "=${classify.nextNode}"`); "In the editor" paragraph describes the free-form box (drag nodes, drag from a node's side handle to connect, click an edge to set its condition, Auto-arrange) and drops the "experimental engine only" caveat. Keep the *coming soon* banner. Then `./gradlew generateDocumentation > /tmp/gendoc.log 2>&1; echo "exit=$?"` and `cd docs && npm run types:check` if the docs project defines it (check `docs/package.json`).

- [ ] **Step 4: Full module check**

```bash
./gradlew spotlessApply :server:libs:modules:task-dispatchers:graph:check :server:libs:platform:platform-configuration:platform-configuration-service:check --continue > /tmp/gradle-a7b.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/gradle-a7b.log
```
Expected: `exit=0`, no FAILED lines.

- [ ] **Step 5: Commit** — `732 Read graph nodes as a task list in the output facade and document the edge-list DSL`.

---

## Self-review

- Spec coverage: DSL shape (T1), evaluation rule (T2/T4), start node + budget + unique names (T3), deferred keys (T5), design-time output (T1), output facade arm (T7), IntTest list from the spec — forward, conditional fan-out, default, dynamic `to`, cycle+budget, budget exhaustion, unknown target, terminal output/downstream, nested dispatcher, graph-in-graph, duplicate default (T6), docs (T7). Router removal is spread across T1–T4.
- Placeholders: none — every step names concrete code or exact fixture content; where a fixture is described in prose (budgetExhausted, unknownTarget, dynamicTarget, duplicateDefault, nestedGraph) the node/transition lists are stated explicitly.
- Type consistency: `resolveTransition(Evaluator, List<Map<String,?>>, String, Map<String,?>) -> Optional<String>` used identically in T2 test, T2 impl, T4 handler; `dispatchNodeTask(..., WorkflowTask nodeTask, String nodeName)` (8 args, no taskNumber) used in T3 and T4; `getNodes(...) -> List<WorkflowTask>` and `findNode(List<WorkflowTask>, String)` consistent across T2–T4.
