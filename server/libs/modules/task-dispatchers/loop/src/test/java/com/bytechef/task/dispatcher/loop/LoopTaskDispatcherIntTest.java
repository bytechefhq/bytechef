/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class LoopTaskDispatcherIntTest {

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
            (valueMap, name, value) -> valueMap.computeIfAbsent(name, key -> new ArrayList<>())
                .add(value));
    }

    @Test
    public void testDispatch1() {
        assertNoTaskErrors(taskDispatcherJobTestExecutor.execute(
            EncodingUtils.encodeBase64ToString("loop_v1_1"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap()));

        Assertions.assertEquals(
            IntStream.rangeClosed(2, 11)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar1"));
    }

    private void assertNoTaskErrors(@Nonnull Job job) {
        if (taskDispatcherJobTestExecutor == null) {
            Assertions.fail("Executor must not be null");

            return;
        }

        if ((job != null) && job.getId() == null) {
            Assertions.fail("Job must not be null");

            return;
        }

        long jobId = job.getId();

        List<ExecutionError> executionErrors = taskDispatcherJobTestExecutor.getExecutionErrors(jobId);

        if (executionErrors == null) {
            return;
        }

        if (!executionErrors.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();

            executionErrors.stream()
                .forEachOrdered(executionError -> {
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

    @Test
    public void testDispatch2() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.encodeBase64ToString("loop_v1_2"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

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
                EncodingUtils.encodeBase64ToString("loop_v1_3"),
                this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
                getTaskHandlerMap()));

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 13)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch4() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.encodeBase64ToString("loop_v1_4"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch5() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.encodeBase64ToString("loop_v1_5"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch6() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.encodeBase64ToString("loop_v1_6"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskExecutionService, taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new LoopBreakTaskDispatcher(eventPublisher, taskExecutionService),
            (taskDispatcher) -> new LoopTaskDispatcher(
                eventPublisher, contextService, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private TaskDispatcherJobTestExecutor.TaskHandlerMapSupplier getTaskHandlerMap() {
        return () -> Map.of("var", testVarTaskHandler);
    }
}
