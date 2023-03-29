
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

import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.hermes.task.dispatcher.test.workflow.TaskDispatcherWorkflowTestSupport;
import com.bytechef.hermes.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.hermes.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.task.dispatcher.if_.IfTaskDispatcher;
import com.bytechef.task.dispatcher.if_.completion.IfTaskCompletionHandler;
import com.bytechef.task.dispatcher.loop.completion.LoopTaskCompletionHandler;
import com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    private TaskDispatcherWorkflowTestSupport taskDispatcherWorkflowTestSupport;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(
            (valueMap, name, value) -> valueMap.computeIfAbsent(name, key -> new ArrayList<>())
                .add(value));
    }

    @Test
    public void testDispatch1() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_1".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(2, 11)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar1"));
    }

    @Test
    public void testDispatch2() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_2".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

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
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_3".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 13)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch4() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_4".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch5() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_5".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(4, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    @Test
    public void testDispatch6() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("loop_v1_6".getBytes(StandardCharsets.UTF_8)),
            getTaskCompletionHandlers(), getTaskDispatcherResolvers(), getTaskHandlerMap());

        Assertions.assertEquals(
            IntStream.rangeClosed(3, 8)
                .boxed()
                .collect(Collectors.toList()),
            testVarTaskHandler.get("sumVar2"));
    }

    private TaskDispatcherWorkflowTestSupport.TaskCompletionHandlersFunction getTaskCompletionHandlers() {
        return (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) -> List.of(
            new IfTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService),
            new LoopTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService),
            new SequenceTaskCompletionHandler(
                contextService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private static TaskDispatcherWorkflowTestSupport.TaskDispatcherResolversFunction getTaskDispatcherResolvers() {
        return (
            contextService, counterService, messageBroker, taskDispatcher, taskEvaluator,
            taskExecutionService) -> List.of(
                new IfTaskDispatcher(
                    contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
                new LoopBreakTaskDispatcher(messageBroker, taskExecutionService),
                new LoopTaskDispatcher(
                    contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService),
                new SequenceTaskDispatcher(
                    contextService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService));
    }

    private TaskDispatcherWorkflowTestSupport.TaskHandlerMapSupplier getTaskHandlerMap() {
        return () -> Map.of("var", testVarTaskHandler);
    }
}
