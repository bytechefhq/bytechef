/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.integri.atlas.engine.coordinator.task.SwitchTaskDispatcher;
import com.integri.atlas.engine.core.context.ContextRepository;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.messagebroker.MessageBroker;
import java.util.Arrays;
import java.util.Collections;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskDispatcher;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskExecutionRepository;
import com.integri.atlas.engine.core.task.spel.SpelTaskEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SwitchTaskDispatcherTests {

    private TaskExecutionRepository taskRepo = mock(TaskExecutionRepository.class);
    private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
    private MessageBroker messageBroker = mock(MessageBroker.class);
    private ContextRepository contextRepository = mock(ContextRepository.class);

    @Test
    public void test1() {
        when(contextRepository.peek(any())).thenReturn(new MapContext());
        SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(
            taskDispatcher,
            taskRepo,
            messageBroker,
            contextRepository,
            SpelTaskEvaluator.create()
        );
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set(
            "cases",
            Arrays.asList(ImmutableMap.of("key", "k1", "tasks", Arrays.asList(ImmutableMap.of("type", "print"))))
        );
        task.set("expression", "k1");
        dispatcher.dispatch(task);
        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);
        verify(taskDispatcher, times(1)).dispatch(argument.capture());
        Assertions.assertEquals("print", argument.getValue().getType());
    }

    @Test
    public void test2() {
        when(contextRepository.peek(any())).thenReturn(new MapContext());
        SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(
            taskDispatcher,
            taskRepo,
            messageBroker,
            contextRepository,
            SpelTaskEvaluator.create()
        );
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set(
            "cases",
            Arrays.asList(ImmutableMap.of("key", "k1", "tasks", Arrays.asList(ImmutableMap.of("type", "print"))))
        );
        task.set("expression", "k2");
        dispatcher.dispatch(task);
        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test3() {
        when(contextRepository.peek(any())).thenReturn(new MapContext());
        SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(
            taskDispatcher,
            taskRepo,
            messageBroker,
            contextRepository,
            SpelTaskEvaluator.create()
        );
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set(
            "cases",
            Arrays.asList(
                ImmutableMap.of("key", "k1", "tasks", Arrays.asList(ImmutableMap.of("type", "print"))),
                ImmutableMap.of("key", "k2", "tasks", Arrays.asList(ImmutableMap.of("type", "sleep")))
            )
        );
        task.set("expression", "k2");
        dispatcher.dispatch(task);
        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);
        verify(taskDispatcher, times(1)).dispatch(argument.capture());
        Assertions.assertEquals("sleep", argument.getValue().getType());
    }

    @Test
    public void test4() {
        when(contextRepository.peek(any())).thenReturn(new MapContext());
        SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(
            taskDispatcher,
            taskRepo,
            messageBroker,
            contextRepository,
            SpelTaskEvaluator.create()
        );
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set(
            "cases",
            Arrays.asList(
                ImmutableMap.of("key", "k1", "tasks", Arrays.asList(ImmutableMap.of("type", "print"))),
                ImmutableMap.of("key", "k2", "tasks", Arrays.asList(ImmutableMap.of("type", "sleep")))
            )
        );
        task.set("default", Collections.singletonMap("value", "1234"));
        task.set("expression", "k99");
        dispatcher.dispatch(task);
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TaskExecution> arg2 = ArgumentCaptor.forClass(TaskExecution.class);
        verify(messageBroker, times(1)).send(arg1.capture(), arg2.capture());
        Assertions.assertEquals("1234", arg2.getValue().getOutput());
    }
}
