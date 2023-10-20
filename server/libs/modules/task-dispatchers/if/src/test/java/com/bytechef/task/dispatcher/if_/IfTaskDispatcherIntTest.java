
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.dispatcher.if_;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.task.dispatcher.test.workflow.TaskDispatcherWorkflowTestSupport;
import com.bytechef.hermes.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.hermes.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class IfTaskDispatcherIntTest {

    public static final Base64.Encoder ENCODER = Base64.getEncoder();
    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    private TaskDispatcherWorkflowTestSupport taskDispatcherWorkflowTestSupport;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatchBoolean() {
        taskDispatcherWorkflowTestSupport.execute(
            ENCODER.encodeToString("if_v1-conditions-boolean".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "true", "value2", "false"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchDateTime() {
        taskDispatcherWorkflowTestSupport.execute(
            ENCODER.encodeToString("if_v1-conditions-dateTime".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01"),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("afterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("beforeResult"));
    }

    @Test
    public void testDispatchExpression() {
        taskDispatcherWorkflowTestSupport.execute(
            ENCODER.encodeToString("if_v1-conditions-expression".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
    }

    @Test
    public void testDispatchNumber() {
        taskDispatcherWorkflowTestSupport.execute(
            ENCODER.encodeToString("if_v1-conditions-number".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            this::getTaskCompletionHandlerFactories,
            this::getTaskDispatcherResolverFactories,
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
        taskDispatcherWorkflowTestSupport.execute(
            ENCODER.encodeToString("if_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
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
    }

    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        CounterService counterService, TaskEvaluator taskEvaluator, TaskExecutionService taskExecutionService) {

        return List.of(
            (
                TaskCompletionHandler taskCompletionHandler,
                TaskDispatcher<? super Task> taskDispatcher) -> new IfTaskCompletionHandler(
                    contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ContextService contextService, CounterService counterService, MessageBroker messageBroker,
        TaskEvaluator taskEvaluator, TaskExecutionService taskExecutionService) {
        return List.of(
            (taskDispatcher) -> new IfTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var", testVarTaskHandler);
    }
}
