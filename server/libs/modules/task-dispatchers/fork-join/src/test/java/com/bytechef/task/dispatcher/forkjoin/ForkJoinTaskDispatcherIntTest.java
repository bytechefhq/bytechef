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

package com.bytechef.task.dispatcher.forkjoin;

import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class ForkJoinTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatch() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1"),
            (
                counterService, taskExecutionService) -> List.of(
                    (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                        contextService, counterService, EVALUATOR, taskExecutionService,
                        taskCompletionHandler, taskDispatcher, taskFileStorage)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    (taskDispatcher) -> new ForkJoinTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        Assertions.assertEquals(85, testVarTaskHandler.get("sumVar1"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }
}
