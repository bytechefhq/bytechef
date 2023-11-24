/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.hermes.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.hermes.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.hermes.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
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
            EncodingUtils.encodeBase64ToString("sequence_v1"),
            (counterService, taskExecutionService) -> List.of(
                (taskCompletionHandler, taskDispatcher) -> new SequenceTaskCompletionHandler(
                    contextService, taskCompletionHandler, taskDispatcher, taskExecutionService,
                    taskFileStorage)),
            (messageBroker, contextService, counterService, taskExecutionService) -> List.of(
                (taskDispatcher) -> new SequenceTaskDispatcher(
                    messageBroker, contextService, taskDispatcher, taskExecutionService, taskFileStorage)),
            () -> Map.of("var", testVarTaskHandler));

        Assertions.assertEquals(1, (Integer) testVarTaskHandler.get("value1"));
        Assertions.assertEquals(2, (Integer) testVarTaskHandler.get("value2"));
        Assertions.assertEquals(3, (Integer) testVarTaskHandler.get("value3"));
        Assertions.assertEquals("${value3}", testVarTaskHandler.get("value4"));
        Assertions.assertEquals(5, testVarTaskHandler.get("value5"));
        Assertions.assertEquals("${value5}", testVarTaskHandler.get("value6"));
    }
}
