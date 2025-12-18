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

package com.bytechef.task.dispatcher.condition;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class ConditionTaskDispatcherIntTest {

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
    public void testDispatchBoolean() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-boolean".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "true", "value2", "false"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchDateTime() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-dateTime".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("afterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("beforeResult"));
    }

    @Test
    public void testDispatchExpression() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-expression".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
    }

    @Test
    public void testDispatchMultipleConditions() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString(
                "condition_v1-conditions-multiple-conditions".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World", "value2", "Hello"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchNumber() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-number".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessEqualsResult"));
    }

    @Test
    public void testDispatchString() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World", "value2", "Hello"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("containsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("notContainsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("startsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("endsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("isEmptyResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("regexResult"));

        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("condition_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World's", "value2", "Hello World's"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("true branch", testVarTaskHandler.get("equalsResult"));
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new ConditionTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var/v1/set", testVarTaskHandler);
    }
}
