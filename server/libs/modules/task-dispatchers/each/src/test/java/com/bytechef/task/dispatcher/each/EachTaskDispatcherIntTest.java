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

package com.bytechef.task.dispatcher.each;

import com.bytechef.atlas.sync.executor.WorkflowExecutor;
import com.bytechef.hermes.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.each.completion.EachTaskCompletionHandler;
import com.bytechef.test.task.handler.TestVarTaskHandler;
import java.util.ArrayList;
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
public class EachTaskDispatcherIntTest {

    private TestVarTaskHandler<List<String>, String> testVarTaskHandler;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>((valueMap, name, value) ->
                valueMap.computeIfAbsent(name, key -> new ArrayList<>()).add(value));
    }

    @Test
    public void testEachTaskDispatcher() {
        workflowExecutor.execute(
                "each_v1",
                (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) -> List.of(
                        new EachTaskCompletionHandler(taskExecutionService, taskCompletionHandler, counterService)),
                (contextService, counterService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        List.of(new EachTaskDispatcher(
                                taskDispatcher,
                                taskExecutionService,
                                messageBroker,
                                contextService,
                                counterService,
                                taskEvaluator)),
                () -> Map.of("var", testVarTaskHandler));

        Assertions.assertEquals(
                IntStream.rangeClosed(1, 25)
                        .boxed()
                        .flatMap(item1 -> IntStream.rangeClosed(1, 25).mapToObj(item2 -> item1 + "_" + item2))
                        .collect(Collectors.toList()),
                testVarTaskHandler.get("var1"));
    }
}
