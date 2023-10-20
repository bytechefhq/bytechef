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

package com.bytechef.task.dispatcher.fork;

import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.test.workflow.WorkflowExecutor;
import com.bytechef.hermes.test.task.handler.TestVarTaskHandler;
import com.bytechef.task.dispatcher.fork.completion.ForkJoinTaskCompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class ForkJoinTaskDispatcherIntTest {

    private TestVarTaskHandler<List<Object>, Object> testVarTaskHandler;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        testVarTaskHandler = new TestVarTaskHandler<>((valueMap, name, value) ->
                valueMap.computeIfAbsent(name, key -> new ArrayList<>()).add(value));
    }

    @Disabled
    @Test
    public void testDispatch() {
        workflowExecutor.execute(
                "fork-join_v1",
                (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        List.of(new ForkJoinTaskCompletionHandler(
                                taskExecutionService,
                                taskCompletionHandler,
                                counterService,
                                taskDispatcher,
                                contextService,
                                taskEvaluator)),
                (contextService, counterService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        List.of(new ForkJoinTaskDispatcher(
                                contextService,
                                counterService,
                                messageBroker,
                                taskDispatcher,
                                taskEvaluator,
                                taskExecutionService)),
                () -> Map.of("var", testVarTaskHandler));

        // TODO
    }
}
