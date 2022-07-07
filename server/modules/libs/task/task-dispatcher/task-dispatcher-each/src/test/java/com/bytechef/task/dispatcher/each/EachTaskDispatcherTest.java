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

import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.counter.CounterService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class EachTaskDispatcherTest {

    private TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
    private MessageBroker messageBroker = mock(MessageBroker.class);
    private ContextService contextService = mock(ContextService.class);
    private CounterService counterService = mock(CounterService.class);

    @Test
    public void test1() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EachTaskDispatcher dispatcher =
                    new EachTaskDispatcher(null, null, null, null, null, SpelTaskEvaluator.create());
            dispatcher.dispatch(new SimpleTaskExecution());
        });
    }

    @Test
    public void test2() {
        when(contextService.peek(any())).thenReturn(new MapContext());
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                taskDispatcher,
                taskExecutionService,
                messageBroker,
                contextService,
                counterService,
                SpelTaskEvaluator.create());
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set("list", Arrays.asList(1, 2, 3));
        task.set("iteratee", Collections.singletonMap("type", "print"));
        dispatcher.dispatch(task);
        verify(taskDispatcher, times(3)).dispatch(any());
        verify(messageBroker, times(0)).send(any(), any());
    }

    @Test
    public void test3() {
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                taskDispatcher,
                taskExecutionService,
                messageBroker,
                contextService,
                counterService,
                SpelTaskEvaluator.create());
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set("list", Arrays.asList());
        task.set("iteratee", Collections.singletonMap("type", "print"));
        dispatcher.dispatch(task);
        verify(taskDispatcher, times(0)).dispatch(any());
        verify(messageBroker, times(1)).send(any(), any());
    }
}
