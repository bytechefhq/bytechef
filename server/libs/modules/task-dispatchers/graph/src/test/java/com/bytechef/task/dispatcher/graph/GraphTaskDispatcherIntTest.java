/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.task.dispatcher.graph;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.graph.completion.GraphTaskCompletionHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pins the {@code graph/v1} runtime contract through the real atlas engine (as opposed to the mock-based unit tests in
 * {@link GraphTaskDispatcherTest} and {@code GraphTaskCompletionHandlerTest}).
 *
 * <p>
 * Unlike production wiring, this harness never loads {@code GraphTaskDispatcherConfiguration} as a Spring bean (its
 * dispatcher/handler are constructed directly, mirroring every sibling {@code *TaskDispatcherIntTest}) -- but
 * {@code TaskDispatcherIntTestConfiguration} force-loads that configuration class (and every other
 * {@code *TaskDispatcherConfiguration} on the classpath) via {@code DeferredEvaluationParameterKeysLoader}, so its
 * static {@code DeferredEvaluationParameterKeys.register(...)} call for {@code graph/}'s {@code nodes} key still runs.
 * Without it, a graph's own {@code TaskExecution#evaluate} would eagerly resolve every expression nested inside
 * {@code nodes} (task parameter values AND {@code next} transitions) against whatever context exists at the graph's OWN
 * dispatch time, baking a stale evaluated value into node definitions that are meant to be evaluated once per visit --
 * see {@code testDispatchBackJumpCycle}, which would otherwise loop forever on a frozen {@code next}.
 * </p>
 *
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class GraphTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatchForwardFlow() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-forward"), this::getGraphTaskCompletionHandlerFactories,
            this::getGraphTaskDispatcherResolverFactories, this::getTaskHandlerMap);

        Assertions.assertEquals("A output", testVarTaskHandler.get("aTask"));
        Assertions.assertEquals("B output", testVarTaskHandler.get("bTask"));

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("B output", outputs.get("result"));
    }

    @Test
    public void testDispatchBackJumpCycle() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-backJumpCycle"), Map.of("counter", 0),
            this::getGraphTaskCompletionHandlerFactories, this::getGraphTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        // node A is revisited: two "counter" executions (1st hop: 0 -> 1, 2nd hop: 1 -> 2)
        long counterExecutionCount = jobExecution.taskExecutions()
            .stream()
            .filter(taskExecution -> "counter".equals(taskExecution.getName()))
            .count();

        Assertions.assertEquals(2, counterExecutionCount, "node A must have been revisited exactly once");

        // node B is likewise revisited: two "bVisit" executions
        long bVisitExecutionCount = jobExecution.taskExecutions()
            .stream()
            .filter(taskExecution -> "bVisit".equals(taskExecution.getName()))
            .count();

        Assertions.assertEquals(2, bVisitExecutionCount, "node B must have been revisited exactly once");

        // accumulated context, last-write-wins: the final "counter" value reflects both increments
        Assertions.assertEquals(2, testVarTaskHandler.get("counter"));
        Assertions.assertEquals(2, testVarTaskHandler.get("bVisit"));
        Assertions.assertEquals(2, testVarTaskHandler.get("cResult"));

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals(2, outputs.get("result"));
    }

    @Test
    public void testDispatchBudgetExhaustion() {
        // TaskDispatcherJobTestExecutor always runs with checkForError=true, so a job/task failure surfaces
        // as a thrown ExecutionException rather than a normally-returned failed TaskDispatcherJobExecution --
        // which also means the in-memory repositories backing that execution are discarded with the throw.
        // The exact permitted-transition count is therefore pinned via real handler invocation counts
        // (recorded as each xTask/yTask actually runs), not via a post-hoc repository query.
        Map<String, Integer> executionCounts = new ConcurrentHashMap<>();
        TestVarTaskHandler<Object, Object> countingVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> {
                valueMap.put(name, value);
                executionCounts.merge(name, 1, Integer::sum);
            });

        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class,
            () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString("graph_v1-budgetExhausted"),
                this::getGraphTaskCompletionHandlerFactories, this::getGraphTaskDispatcherResolverFactories,
                () -> Map.of("var/v1/set", countingVarTaskHandler)));

        Assertions.assertEquals(
            "graph transition budget exhausted (maxTransitions=3)", executionException.getMessage());

        // maxTransitions=3 -- exactly 3 transitions are permitted (X->Y, Y->X, X->Y), so exactly 2 X
        // executions and 2 Y executions happen before the 4th transition (Y->X) is rejected
        Assertions.assertEquals(2, executionCounts.get("xTask"), "exactly 2 executions of node X are permitted");
        Assertions.assertEquals(2, executionCounts.get("yTask"), "exactly 2 executions of node Y are permitted");
    }

    @Test
    public void testDispatchRouterNodes() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-router"), this::getGraphTaskCompletionHandlerFactories,
            this::getGraphTaskDispatcherResolverFactories, this::getTaskHandlerMap);

        // the router-as-start-node hand-off reached the real node behind it
        Assertions.assertEquals("reached afterStart", testVarTaskHandler.get("afterStartTask"));

        // the mid-graph router hand-off chained through to the final node
        Assertions.assertEquals("reached finalNode", testVarTaskHandler.get("finalTask"));

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("reached finalNode", outputs.get("result"));
    }

    /**
     * A top-level graph whose entire router chain resolves to terminal WITHOUT ever dispatching a real task (both
     * {@code r1} and {@code r2} are empty router nodes; {@code r2} has no {@code next}) is a legal shape per the spec
     * (empty node = router; a terminal resolution with no completed task output resolves null).
     * {@code maxTransitions: 1} is exactly enough for the single {@code r1 -> r2} hop -- pinning that the router
     * hand-off's terminal resolution does not re-trigger itself (the degenerate-shape recursion this regression-tests):
     * if it did, a second budget decrement would be attempted and the job would fail with budget exhaustion instead of
     * completing.
     */
    @Test
    public void testDispatchPureRouterGraphCompletesCleanly() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-pureRouter"), this::getGraphTaskCompletionHandlerFactories,
            this::getGraphTaskDispatcherResolverFactories, this::getTaskHandlerMap);

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertTrue(outputs.containsKey("result"), "workflow output key must be present, value null");
        Assertions.assertNull(outputs.get("result"));
    }

    @Test
    public void testDispatchUnknownTargetFails() {
        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class,
            () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString("graph_v1-unknownTarget"),
                this::getGraphTaskCompletionHandlerFactories, this::getGraphTaskDispatcherResolverFactories,
                this::getTaskHandlerMap));

        Assertions.assertEquals(
            "Unknown graph transition target node: 'missingNode' resolved from node 'A'",
            executionException.getMessage());
    }

    @Test
    public void testDispatchNestedConditionComposesOutput() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-nestedCondition"),
            this::getGraphAndConditionTaskCompletionHandlerFactories,
            this::getGraphAndConditionTaskDispatcherResolverFactories, this::getTaskHandlerMap);

        Assertions.assertEquals("nested condition output", testVarTaskHandler.get("lastTask"));

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("nested condition output", outputs.get("result"));
    }

    @Test
    public void testDispatchGraphInsideForkJoinBranch() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-forkJoinBranch"),
            this::getGraphAndForkJoinTaskCompletionHandlerFactories,
            this::getGraphAndForkJoinTaskDispatcherResolverFactories, this::getTaskHandlerMap);

        Assertions.assertEquals("graph branch output", testVarTaskHandler.get("graphTask"));
        Assertions.assertEquals("plain branch output", testVarTaskHandler.get("lastTask1"));

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("graph branch output", outputs.get("branch0Result"));
        Assertions.assertEquals("plain branch output", outputs.get("branch1Result"));
    }

    @Test
    public void testDispatchNestedGraphWithNonEmptyInnerStart() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-nestedGraphNonEmptyStart"),
            this::getGraphTaskCompletionHandlerFactories, this::getGraphTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("inner graph output", testVarTaskHandler.get("innerTask"));

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("inner graph output", outputs.get("result"));
    }

    /**
     * The inner graph's own start node ({@code innerRouterStart}) is empty, so its {@code __routerNode} hand-off fires.
     * This pins that the hand-off's deliberately-not-persisted self-stamp does not corrupt the outer graph's own
     * {@code __node} stamp: the composed output still reaches the outer graph's output correctly.
     */
    @Test
    public void testDispatchNestedGraphWithEmptyInnerStart() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("graph_v1-nestedGraphEmptyStart"),
            this::getGraphTaskCompletionHandlerFactories, this::getGraphTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("inner graph via router output", testVarTaskHandler.get("innerTask"));

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertEquals("inner graph via router output", outputs.get("result"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getGraphTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new GraphTaskCompletionHandler(
                contextService, counterService, EVALUATOR, taskCompletionHandler, taskDispatcher,
                taskExecutionService, taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getGraphTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new GraphTaskDispatcher(
                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getGraphAndConditionTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new GraphTaskCompletionHandler(
                contextService, counterService, EVALUATOR, taskCompletionHandler, taskDispatcher,
                taskExecutionService, taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getGraphAndConditionTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskDispatcher) -> new GraphTaskDispatcher(
                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getGraphAndForkJoinTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                contextService, counterService, EVALUATOR, taskExecutionService, taskCompletionHandler,
                taskDispatcher, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new GraphTaskCompletionHandler(
                contextService, counterService, EVALUATOR, taskCompletionHandler, taskDispatcher,
                taskExecutionService, taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getGraphAndForkJoinTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ForkJoinTaskDispatcher(
                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new GraphTaskDispatcher(
                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var/v1/set", testVarTaskHandler);
    }
}
