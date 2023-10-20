/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.task.dispatcher.each;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.spel.SpelTaskEvaluator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class EachTaskDispatcherTest {

    private final ContextService contextService = mock(ContextService.class);
    private final CounterService counterService = mock(CounterService.class);
    private final MessageBroker messageBroker = mock(MessageBroker.class);
    private final TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    @Test
    public void testDispatch1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                    taskDispatcher,
                    taskExecutionService,
                    messageBroker,
                    contextService,
                    counterService,
                    SpelTaskEvaluator.create());
            dispatcher.dispatch(new TaskExecution());
        });
    }

    @Test
    public void testDispatch2() {
        when(contextService.peek(any())).thenReturn(new Context());
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                taskDispatcher,
                taskExecutionService,
                messageBroker,
                contextService,
                counterService,
                SpelTaskEvaluator.create());
        TaskExecution taskExecution = TaskExecution.of(new WorkflowTask(Map.of(
                "list", Arrays.asList(1, 2, 3),
                "iteratee", Collections.singletonMap("type", "print"))));

        taskExecution.setId("id");
        taskExecution.setJobId("jobId");

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(3)).dispatch(any());
        verify(messageBroker, times(0)).send(any(), any());
    }

    @Test
    public void testDispatch3() {
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                taskDispatcher,
                taskExecutionService,
                messageBroker,
                contextService,
                counterService,
                SpelTaskEvaluator.create());
        TaskExecution taskExecution = TaskExecution.of(new WorkflowTask(Map.of(
                "list", List.of(),
                "iteratee", Collections.singletonMap("type", "print"))));

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
        verify(messageBroker, times(1)).send(any(), any());
    }
}
