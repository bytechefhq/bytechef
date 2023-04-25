
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

import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.hermes.task.dispatcher.test.workflow.TaskDispatcherWorkflowTestSupport;
import com.bytechef.hermes.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.hermes.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public class SequenceTaskDispatcherIntTest {

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
    public void testDispatch() {
        taskDispatcherWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("sequence_v1".getBytes(StandardCharsets.UTF_8)),
            (counterService, taskExecutionService) -> List.of(
                (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                    contextService, taskCompletionHandler, taskDispatcher, taskExecutionService)),
            (contextService, counterService, messageBroker, taskExecutionService) -> List.of(
                (taskDispatcher) -> new SequenceTaskDispatcher(
                    contextService, messageBroker, taskDispatcher, taskExecutionService)),
            () -> Map.of("var", testVarTaskHandler));

        Assertions.assertEquals(1, (Integer) testVarTaskHandler.get("value1"));
        Assertions.assertEquals(2, (Integer) testVarTaskHandler.get("value2"));
        Assertions.assertEquals(3, (Integer) testVarTaskHandler.get("value3"));
        Assertions.assertEquals("${value3}", testVarTaskHandler.get("value4"));
        Assertions.assertEquals(5, testVarTaskHandler.get("value5"));
        Assertions.assertEquals("${value5}", testVarTaskHandler.get("value6"));
    }
}
