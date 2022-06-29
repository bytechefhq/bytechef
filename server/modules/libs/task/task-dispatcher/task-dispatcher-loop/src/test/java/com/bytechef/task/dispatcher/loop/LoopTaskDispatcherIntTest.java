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

package com.bytechef.task.dispatcher.loop;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.dispatcher.if_.IfTaskDispatcher;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.bytechef.task.handler.core.Var;
import com.bytechef.test.task.BaseTaskIntTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class LoopTaskDispatcherIntTest extends BaseTaskIntTest {

    private TestVar testVar;

    @BeforeEach
    void setUp() {
        testVar = new TestVar();
    }

    @Test
    public void testLoopTaskDispatcher1() {
        startJob("samples/loop1.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(2, 11).boxed().collect(Collectors.toList()), testVar.get("sumVar1"));
    }

    @Test
    public void testLoopTaskDispatcher2() {
        startJob("samples/loop2.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(1, 10)
                        .boxed()
                        .flatMap(item1 -> IntStream.rangeClosed(1, item1).mapToObj(item2 -> item1 + "_" + item2))
                        .collect(Collectors.toList()),
                testVar.get("sumVar1"));
    }

    @Test
    public void testLoopTaskDispatcher3() {
        startJob("samples/loop3.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(4, 13).boxed().collect(Collectors.toList()), testVar.get("sumVar2"));
    }

    @Test
    public void testLoopTaskDispatcher4() {
        startJob("samples/loop4.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(4, 8).boxed().collect(Collectors.toList()), testVar.get("sumVar2"));
    }

    @Test
    public void testLoopTaskDispatcher5() {
        startJob("samples/loop5.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(4, 8).boxed().collect(Collectors.toList()), testVar.get("sumVar2"));
    }

    @Test
    public void testLoopTaskDispatcher6() {
        startJob("samples/loop6.yaml", Map.of());

        Assertions.assertEquals(
                IntStream.rangeClosed(3, 8).boxed().collect(Collectors.toList()), testVar.get("sumVar2"));
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
            TaskCompletionHandler taskCompletionHandler, TaskDispatcher<?> taskDispatcher) {
        return List.of(
                new IfTaskCompletionHandler(
                        contextService,
                        taskCompletionHandler,
                        taskDispatcher,
                        SpelTaskEvaluator.create(),
                        taskExecutionService),
                new LoopTaskCompletionHandler(
                        contextService,
                        taskCompletionHandler,
                        taskDispatcher,
                        SpelTaskEvaluator.create(),
                        taskExecutionService),
                new SequenceTaskCompletionHandler(
                        contextService,
                        taskCompletionHandler,
                        taskDispatcher,
                        SpelTaskEvaluator.create(),
                        taskExecutionService));
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
            MessageBroker coordinatorMessageBroker, TaskDispatcher<?> taskDispatcher) {
        return List.of(
                new IfTaskDispatcher(
                        contextService,
                        coordinatorMessageBroker,
                        taskDispatcher,
                        SpelTaskEvaluator.create(),
                        taskExecutionService),
                new LoopBreakTaskDispatcher(coordinatorMessageBroker, taskExecutionService),
                new LoopTaskDispatcher(
                        contextService,
                        coordinatorMessageBroker,
                        taskDispatcher,
                        SpelTaskEvaluator.create(),
                        taskExecutionService),
                new SequenceTaskDispatcher(
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

        private final Map<String, List<Object>> valuesMap = new HashMap<>();

        @Override
        public Object handle(TaskExecution taskExecution) {
            Object value = super.handle(taskExecution);

            valuesMap
                    .computeIfAbsent(taskExecution.getName(), key -> new ArrayList<>())
                    .add(value);

            return value;
        }

        public List<Object> get(String key) {
            return valuesMap.get(key);
        }
    }
}
