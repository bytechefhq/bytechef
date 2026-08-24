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

package com.bytechef.task.dispatcher.loop;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class LoopTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<List<Object>, Object> testVarTaskHandler;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    private TaskDispatcherJobExecution taskDispatcherJobExecution;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> {
                List<Object> list = valueMap.computeIfAbsent(name, k -> new ArrayList<>());

                list.add(value);
            });
    }

    @Test
    public void testDispatch1() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_1"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertNoTaskErrors(taskDispatcherJobExecution);

        Assertions.assertEquals(
            IntStream.rangeClosed(2, 11)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar1"));
    }

    @Test
    public void testDispatch2() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_2"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(1, 10)
                .boxed()
                .flatMap(item1 -> IntStream.rangeClosed(1, item1)
                    .mapToObj(item2 -> item1 + "_" + item2))
                .collect(Collectors.toList()),
            testVarTaskHandler.get("var1"));
    }

    @Test
    public void testDispatch3() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_3"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertNoTaskErrors(taskDispatcherJobExecution);

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 13)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch4() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_4"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch5() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_5"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch6() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_6"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @RepeatedTest(10)
    void testLoopForeverCappedNoResidualStarted() {
        // Use an existing workflow with loopForever=true and a condition that triggers loopBreak after index > 5
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_6"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated();

        TaskExecution parentTaskExecution = findParentLoopTask();

        Assertions.assertNotNull(parentTaskExecution.getEndDate(), "Loop parent must have endDate");

        TaskExecution.Status status = parentTaskExecution.getStatus();

        Assertions.assertTrue(status.isTerminated(), "Loop parent must be in a terminated state");
    }

    @RepeatedTest(10)
    void testLoopEmptyItemsParentCompletesImmediately() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_empty"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated();

        TaskExecution parentTaskExecution = findParentLoopTask();

        Assertions.assertNotNull(parentTaskExecution.getEndDate(), "Loop parent must have endDate");

        TaskExecution.Status status = parentTaskExecution.getStatus();

        Assertions.assertTrue(status.isTerminated());
    }

    @Test
    void testLoopWithDisabledIterateeCompletesInsteadOfFailing() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_disabled_iteratee"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertNoTaskErrors(taskDispatcherJobExecution);

        Job job = taskDispatcherJobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());

        Assertions.assertNull(testVarTaskHandler.get("skippedVar"));
        Assertions.assertEquals(List.of("done"), testVarTaskHandler.get("afterLoop"));

        TaskExecution parentTaskExecution = findParentLoopTask();

        Assertions.assertNotNull(parentTaskExecution.getEndDate(), "Loop parent must have endDate");
        Assertions.assertEquals(TaskExecution.Status.COMPLETED, parentTaskExecution.getStatus());
    }

    @Test
    @Timeout(30)
    void testWorkflowWithEveryTaskDisabledCompletes() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_all_disabled"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertNoTaskErrors(taskDispatcherJobExecution);

        Job job = taskDispatcherJobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());

        Assertions.assertNull(testVarTaskHandler.get("innerVar"));
        Assertions.assertNull(testVarTaskHandler.get("afterLoop"));
    }

    @Test
    void testLoopWithEmptyIterateeFails() {
        ExecutionException executionException = Assertions.assertThrows(
            ExecutionException.class,
            () -> taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString("loop_v1_empty_iteratee"), this::getTaskCompletionHandlerFactories,
                this::getTaskDispatcherResolverFactories, getTaskHandlerMap()));

        Assertions.assertEquals("'iteratee' must contain at least one task", executionException.getMessage());
    }

    @RepeatedTest(10)
    void testLoopOverSmallItemsNoResidualStarted() {
        taskDispatcherJobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_1"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated();

        // Parent loop should be COMPLETED with endDate
        TaskExecution parent = findParentLoopTask();

        Assertions.assertNotNull(parent.getEndDate(), "Loop parent must have endDate");
        Assertions.assertEquals(TaskExecution.Status.COMPLETED, parent.getStatus());
    }

    private void assertNoTaskErrors(TaskDispatcherJobExecution jobExecution) {
        Job job = jobExecution.job();

        if (job.getId() == null) {
            Assertions.fail("Job must not be null");

            return;
        }

        List<ExecutionError> executionErrors = jobExecution.getExecutionErrors();

        if (!executionErrors.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();

            executionErrors.forEach(executionError -> {
                stringBuilder.append(executionError.getMessage());
                stringBuilder.append(System.lineSeparator());

                executionError.getStackTrace()
                    .forEach(s -> {
                        stringBuilder.append(s);
                        stringBuilder.append(System.lineSeparator());
                    });
            });

            Assertions.fail(stringBuilder.toString());
        }
    }

    private void assertAllTasksTerminated() {
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofMillis(50))
            .untilAsserted(() -> {
                List<TaskExecution> nonTerminatedTaskExecutions = taskDispatcherJobExecution.jobTaskExecutions()
                    .stream()
                    .filter(taskExecution -> {
                        TaskExecution.Status status = taskExecution.getStatus();

                        return status != null && !status.isTerminated();
                    })
                    .toList();

                Assertions.assertTrue(
                    nonTerminatedTaskExecutions.isEmpty(),
                    () -> "Not all TaskExecutions terminated within timeout. Current statuses:\n" +
                        nonTerminatedTaskExecutions.stream()
                            .map(taskExecution -> "TaskExecution " + taskExecution.getId() + " (" +
                                taskExecution.getName() + ") status=" + taskExecution.getStatus())
                            .collect(Collectors.joining(System.lineSeparator())));
            });
    }

    private TaskExecution findParentLoopTask() {
        return taskDispatcherJobExecution.jobTaskExecutions()
            .stream()
            .filter(te -> te.getParentId() == null)
            .filter(te -> "loop/v1".equals(te.getType()))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService, CounterService counterService,
        TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private TaskDispatcherJobTestExecutor.TaskHandlerMapSupplier getTaskHandlerMap() {
        return () -> Map.of("var/v1/set", testVarTaskHandler);
    }
}
