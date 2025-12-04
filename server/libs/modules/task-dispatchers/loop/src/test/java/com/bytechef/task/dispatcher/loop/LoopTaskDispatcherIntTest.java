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
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
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
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

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
        assertNoTaskErrors(taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_1"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap()));

        Assertions.assertEquals(
            IntStream.rangeClosed(2, 11)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar1"));
    }

    @Test
    public void testDispatch2() {
        taskDispatcherJobTestExecutor.execute(
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
        assertNoTaskErrors(
            taskDispatcherJobTestExecutor.execute(
                EncodingUtils.base64EncodeToString("loop_v1_3"), this::getTaskCompletionHandlerFactories,
                this::getTaskDispatcherResolverFactories, getTaskHandlerMap()));

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 13)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch4() {
        taskDispatcherJobTestExecutor.execute(
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
        taskDispatcherJobTestExecutor.execute(
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
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_6"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Disabled
    @RepeatedTest(10)
    void testLoopForeverCappedNoResidualStarted() {
        // Use an existing workflow with loopForever=true and a condition that triggers loopBreak after index > 5
        Job job = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_6"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated(job);

        TaskExecution parentTaskExecution = findParentLoopTask(Objects.requireNonNull(job.getId()));

        Assertions.assertNotNull(parentTaskExecution.getEndDate(), "Loop parent must have endDate");

        TaskExecution.Status status = parentTaskExecution.getStatus();

        Assertions.assertTrue(status.isTerminated(), "Loop parent must be in a terminated state");
    }

    @Disabled
    @RepeatedTest(10)
    void testLoopEmptyItemsParentCompletesImmediately() {
        Job job = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_empty"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated(job);

        TaskExecution parentTaskExecution = findParentLoopTask(Objects.requireNonNull(job.getId()));

        Assertions.assertNotNull(parentTaskExecution.getEndDate(), "Loop parent must have endDate");

        TaskExecution.Status status = parentTaskExecution.getStatus();

        Assertions.assertTrue(status.isTerminated());
    }

    @Disabled
    @RepeatedTest(10)
    void testLoopOverSmallItemsNoResidualStarted() {
        Job job = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("loop_v1_1"), this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        assertAllTasksTerminated(job);

        // Parent loop should be COMPLETED with endDate
        TaskExecution parent = findParentLoopTask(Objects.requireNonNull(job.getId()));

        Assertions.assertNotNull(parent.getEndDate(), "Loop parent must have endDate");
        Assertions.assertEquals(TaskExecution.Status.COMPLETED, parent.getStatus());
    }

    private void assertNoTaskErrors(Job job) {
        if (job.getId() == null) {
            Assertions.fail("Job must not be null");

            return;
        }

        List<ExecutionError> executionErrors = taskDispatcherJobTestExecutor.getExecutionErrors(
            Validate.notNull(job.getId(), "id"));

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

    private void assertAllTasksTerminated(Job job) {
        long jobId = Objects.requireNonNull(job.getId(), "job id");

        long timeoutMillis = TimeUnit.SECONDS.toMillis(30);
        long pollDelayMillis = 50L;
        long start = System.currentTimeMillis();

        while (true) {
            List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

            boolean allTerminated = true;

            for (TaskExecution taskExecution : taskExecutions) {
                TaskExecution.Status status = taskExecution.getStatus();

                if (!(status == null || status.isTerminated())) {
                    allTerminated = false;

                    break;
                }
            }

            if (allTerminated) {
                return;
            }

            if (System.currentTimeMillis() - start > timeoutMillis) {
                List<TaskExecution> currentExecutions = taskExecutionService.getJobTaskExecutions(jobId);

                String message = currentExecutions.stream()
                    .map(te -> "TaskExecution " + te.getId() + " (" + te.getName() + ") status=" + te.getStatus())
                    .collect(Collectors.joining(System.lineSeparator()));

                Assertions.fail("Not all TaskExecutions terminated within timeout. Current statuses:\n" + message);
            }

            try {
                Thread.sleep(pollDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                Assertions.fail("Interrupted while waiting for task terminations");
            }
        }
    }

    private TaskExecution findParentLoopTask(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(te -> te.getParentId() == null)
            .filter(te -> "loop/v1".equals(te.getType()))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        CounterService counterService, TaskExecutionService taskExecutionService) {

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
