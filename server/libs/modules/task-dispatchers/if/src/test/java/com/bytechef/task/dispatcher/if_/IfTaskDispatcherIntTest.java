
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

import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.sync.executor.WorkflowExecutor;
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

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatchBoolean() {
        workflowExecutor.execute(
            Base64.getEncoder()
                .encodeToString("if_v1-conditions-boolean".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "true", "value2", "false"),
            getGetTaskCompletionHandlers(),
            getGetTaskDispatcherResolvers(),
            getTaskHandlerMap());

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
    }

    @Test
    public void testDispatchDateTime() {
        workflowExecutor.execute(
            Base64.getEncoder()
                .encodeToString("if_v1-conditions-dateTime".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01"),
            getGetTaskCompletionHandlers(),
            getGetTaskDispatcherResolvers(),
            getTaskHandlerMap());

        Assertions.assertEquals("false branch", testVarTaskHandler.get("afterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("beforeResult"));
    }

    @Test
    public void testDispatchExpression() {
        workflowExecutor.execute(
            Base64.getEncoder()
                .encodeToString("if_v1-conditions-expression".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            getGetTaskCompletionHandlers(),
            getGetTaskDispatcherResolvers(),
            getTaskHandlerMap());

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
    }

    @Test
    public void testDispatchNumber() {
        workflowExecutor.execute(
            Base64.getEncoder()
                .encodeToString("if_v1-conditions-number".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", 100, "value2", 200),
            getGetTaskCompletionHandlers(),
            getGetTaskDispatcherResolvers(),
            getTaskHandlerMap());

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("lessEqualsResult"));
    }

    @Test
    public void testDispatchString() {
        workflowExecutor.execute(
            Base64.getEncoder()
                .encodeToString("if_v1-conditions-string".getBytes(StandardCharsets.UTF_8)),
            Map.of("value1", "Hello World", "value2", "Hello"),
            getGetTaskCompletionHandlers(),
            getGetTaskDispatcherResolvers(),
            getTaskHandlerMap());

        Assertions.assertEquals("false branch", testVarTaskHandler.get("equalsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("notEqualsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("containsResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("notContainsResult"));
        Assertions.assertEquals("true branch", testVarTaskHandler.get("startsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("endsWithResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("isEmptyResult"));
        Assertions.assertEquals("false branch", testVarTaskHandler.get("regexResult"));
    }

    private WorkflowExecutor.GetTaskHandlerMapSupplier getTaskHandlerMap() {
        return () -> Map.of("var", testVarTaskHandler);
    }

    private static WorkflowExecutor.GetTaskDispatcherResolversFunction getGetTaskDispatcherResolvers() {
        return (
            contextService, counterService, messageBroker, taskDispatcher, taskEvaluator,
            taskExecutionService) -> List.of(new IfTaskDispatcher(
                contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private WorkflowExecutor.GetTaskCompletionHandlersFunction getGetTaskCompletionHandlers() {
        return (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) -> List
            .of(new IfTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService));
    }
}
