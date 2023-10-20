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

package com.bytechef.task.dispatcher.sequence;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.bytechef.task.handler.core.Var;
import com.bytechef.test.task.BaseTaskIntTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class SequenceTaskDispatcherIntTest extends BaseTaskIntTest {

    private TestVar testVar;

    @BeforeEach
    void setUp() {
        testVar = new TestVar();
    }

    @Test
    public void testSequence() {
        startJob("samples/sequence.yaml", Map.of());

        Assertions.assertEquals(1, (Integer) testVar.get("value1"));
        Assertions.assertEquals(2, (Integer) testVar.get("value2"));
        Assertions.assertEquals(3, (Integer) testVar.get("value3"));
        Assertions.assertEquals("${value3}", testVar.get("value4"));
        Assertions.assertEquals(5, testVar.get("value5"));
        Assertions.assertEquals("${value5}", testVar.get("value6"));
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
            TaskCompletionHandler taskCompletionHandler, TaskDispatcher taskDispatcher) {
        return List.of(new SequenceTaskCompletionHandler(
                contextService,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionService));
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
            MessageBroker coordinatorMessageBroker, TaskDispatcher taskDispatcher) {
        return List.of(new SequenceTaskDispatcher(
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

        private final Map<String, Object> valueMap = new HashMap<>();

        @Override
        public Object handle(TaskExecution taskExecution) {
            Object value = super.handle(taskExecution);

            valueMap.put(taskExecution.getName(), value);

            return value;
        }

        public Object get(String key) {
            return valueMap.get(key);
        }
    }
}
