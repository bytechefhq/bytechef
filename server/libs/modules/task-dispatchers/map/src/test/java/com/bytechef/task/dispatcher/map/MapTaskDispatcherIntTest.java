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

package com.bytechef.task.dispatcher.map;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class MapTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<List<Object>, Object> testVarTaskHandler;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> valueMap.computeIfAbsent(
                name,
                key -> java.util.Collections.synchronizedList(new ArrayList<>()))
                .add(value));
    }

    @Test
    public void testDispatch1() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("map_v1_1"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(jobExecution.job()
            .getOutputs());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 12)
                .boxed()
                .collect(Collectors.toList()),
            outputs.get("map"));
    }

    @Test
    public void testDispatch2() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("map_v1_2"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        List<?> actual = (List<?>) outputs.get("map");

        List<?> expected = IntStream.rangeClosed(1, 10)
            .boxed()
            .map(item1 -> IntStream.rangeClosed(1, item1)
                .mapToObj(item2 -> item1 + "_" + item2 + 1)
                .collect(Collectors.toList()))
            .collect(Collectors.toList());

        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testDispatch3() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("map_v1_3"),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories, getTaskHandlerMap());

        Job job = jobExecution.job();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertNull(outputs.get("map"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskCompletionHandler, taskDispatcher) -> new MapTaskCompletionHandler(
                contextService, counterService, EVALUATOR, taskDispatcher, taskCompletionHandler,
                taskExecutionService, taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService, CounterService counterService,
        TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage),
            (taskDispatcher) -> new MapTaskDispatcher(
                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                taskExecutionService, taskFileStorage));
    }

    private TaskDispatcherJobTestExecutor.TaskHandlerMapSupplier getTaskHandlerMap() {
        return () -> Map.of("var/v1/set", testVarTaskHandler);
    }
}
