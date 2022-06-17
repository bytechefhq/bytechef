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
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.bytechef.task.handler.core.Var;
import com.bytechef.test.support.task.BaseTaskIntTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@SpringBootTest
public class IfTaskDispatcherIntTest extends BaseTaskIntTest {

    private TestVar testVar;

    @BeforeEach
    void setUp() {
        testVar = new TestVar();
    }

    @Test
    public void testIfTaskDispatcher_Boolean() {
        startJob("samples/if-conditions-boolean.yaml", Map.of("value1", "true", "value2", "false"));

        Assertions.assertEquals("false branch", testVar.get("equalsResult"));
        Assertions.assertEquals("true branch", testVar.get("notEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_DateTime() {
        startJob(
                "samples/if-conditions-dateTime.yaml",
                Map.of("value1", "2022-01-01T00:00:00", "value2", "2022-01-01T00:00:01"));

        Assertions.assertEquals("false branch", testVar.get("afterResult"));
        Assertions.assertEquals("true branch", testVar.get("beforeResult"));
    }

    @Test
    public void testIfTaskDispatcher_Expression() {
        startJob("samples/if-conditions-expression.yaml", Map.of("value1", 100, "value2", 200));

        Assertions.assertEquals("false branch", testVar.get("equalsResult"));
    }

    @Test
    public void testIfTaskDispatcher_Number() {
        startJob("samples/if-conditions-number.yaml", Map.of("value1", 100, "value2", 200));

        Assertions.assertEquals("false branch", testVar.get("equalsResult"));
        Assertions.assertEquals("true branch", testVar.get("notEqualsResult"));
        Assertions.assertEquals("false branch", testVar.get("greaterResult"));
        Assertions.assertEquals("true branch", testVar.get("lessResult"));
        Assertions.assertEquals("false branch", testVar.get("greaterEqualsResult"));
        Assertions.assertEquals("true branch", testVar.get("lessEqualsResult"));
    }

    @Test
    public void testIfTaskDispatcher_String() {
        startJob("samples/if-conditions-string.yaml", Map.of("value1", "Hello World", "value2", "Hello"));

        Assertions.assertEquals("false branch", testVar.get("equalsResult"));
        Assertions.assertEquals("true branch", testVar.get("notEqualsResult"));
        Assertions.assertEquals("true branch", testVar.get("containsResult"));
        Assertions.assertEquals("false branch", testVar.get("notContainsResult"));
        Assertions.assertEquals("true branch", testVar.get("startsWithResult"));
        Assertions.assertEquals("false branch", testVar.get("endsWithResult"));
        Assertions.assertEquals("false branch", testVar.get("isEmptyResult"));
        Assertions.assertEquals("false branch", testVar.get("regexResult"));
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
            TaskCompletionHandler taskCompletionHandler, TaskDispatcher taskDispatcher) {
        return List.of(new IfTaskCompletionHandler(
                contextService,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionService));
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
            MessageBroker coordinatorMessageBroker, TaskDispatcher taskDispatcher) {
        return List.of(new IfTaskDispatcher(
                contextService,
                coordinatorMessageBroker,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionService));
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("core/var", testVar);
    }

    private static class TestVar extends Var {

        private final Map<String, String> valuesMap = new HashMap<>();

        @Override
        public Object handle(TaskExecution taskExecution) {
            Object value = super.handle(taskExecution);

            valuesMap.computeIfAbsent(taskExecution.getName(), key -> (String) value);

            return value;
        }

        public String get(String key) {
            return valuesMap.get(key);
        }
    }
}
