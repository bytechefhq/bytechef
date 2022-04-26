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

package com.integri.atlas.task.dispatcher.loop;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.YAMLWorkflowMapper;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.dispatcher.if_.IfTaskDispatcher;
import com.integri.atlas.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.integri.atlas.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.integri.atlas.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.integri.atlas.task.handler.core.Var;
import com.integri.atlas.test.task.handler.BaseTaskIntTest;
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
            IntStream.rangeClosed(2, 11).boxed().collect(Collectors.toList()),
            testVar.get("sumVar1")
        );
    }

    @Test
    public void testLoopTaskDispatcher2() {
        startJob("samples/loop2.yaml", Map.of());

        Assertions.assertEquals(
            IntStream
                .rangeClosed(1, 10)
                .boxed()
                .flatMap(item1 -> IntStream.rangeClosed(1, item1).mapToObj(item2 -> item1 + "_" + item2))
                .collect(Collectors.toList()),
            testVar.get("sumVar1")
        );
    }

    @Test
    public void testLoopTaskDispatcher3() {
        startJob("samples/loop3.yaml", Map.of());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 13).boxed().collect(Collectors.toList()),
            testVar.get("sumVar2")
        );
    }

    @Test
    public void testLoopTaskDispatcher4() {
        startJob("samples/loop4.yaml", Map.of());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8).boxed().collect(Collectors.toList()),
            testVar.get("sumVar2")
        );
    }

    @Test
    public void testLoopTaskDispatcher5() {
        startJob("samples/loop5.yaml", Map.of());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8).boxed().collect(Collectors.toList()),
            testVar.get("sumVar2")
        );
    }

    @Test
    public void testLoopTaskDispatcher6() {
        startJob("samples/loop6.yaml", Map.of());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 8).boxed().collect(Collectors.toList()),
            testVar.get("sumVar2")
        );
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<?> taskDispatcher
    ) {
        return List.of(
            new IfTaskCompletionHandler(
                contextRepository,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            ),
            new LoopTaskCompletionHandler(
                contextRepository,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            ),
            new SequenceTaskCompletionHandler(
                contextRepository,
                taskCompletionHandler,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            )
        );
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
        MessageBroker coordinatorMessageBroker,
        TaskDispatcher<?> taskDispatcher
    ) {
        return List.of(
            new IfTaskDispatcher(
                contextRepository,
                coordinatorMessageBroker,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            ),
            new LoopBreakTaskDispatcher(coordinatorMessageBroker, taskExecutionRepository),
            new LoopTaskDispatcher(
                contextRepository,
                coordinatorMessageBroker,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            ),
            new SequenceTaskDispatcher(
                contextRepository,
                coordinatorMessageBroker,
                taskDispatcher,
                SpelTaskEvaluator.create(),
                taskExecutionRepository
            )
        );
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of("core/var", testVar);
    }

    @Override
    protected WorkflowMapper getWorkflowMapper() {
        return new YAMLWorkflowMapper();
    }

    private static class TestVar extends Var {

        private final Map<String, List<Object>> valuesMap = new HashMap<>();

        @Override
        public Object handle(TaskExecution taskExecution) {
            Object value = super.handle(taskExecution);

            valuesMap.computeIfAbsent(taskExecution.getName(), key -> new ArrayList<>()).add(value);

            return value;
        }

        public List<Object> get(String key) {
            return valuesMap.get(key);
        }
    }
}
