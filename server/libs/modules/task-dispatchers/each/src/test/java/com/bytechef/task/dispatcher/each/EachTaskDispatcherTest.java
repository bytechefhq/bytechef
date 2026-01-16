/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.task.dispatcher.each;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Arik Cohen
 */
public class EachTaskDispatcherTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ContextService contextService = mock(ContextService.class);
    private final CounterService counterService = mock(CounterService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    public void testEachTaskDispatcherWhenMissingRequiredParameter() {
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);

        dispatcher.dispatch(
            TaskExecution.builder()
                .workflowTask(
                    new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "type")))
                .build());

        verify(eventPublisher, times(1)).publishEvent(any(TaskExecutionErrorEvent.class));
    }

    @Test
    public void testDispatch2() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.create(any()))
            .thenReturn(TaskExecution.builder()
                .id(1L)
                .build());

        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
            taskExecutionService, taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "items", Arrays.asList(1, 2, 3),
                            "iteratee", new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(1L);

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(3)).dispatch(any());
        verify(eventPublisher, times(0)).publishEvent(any());
    }

    @Test
    public void testDispatch3() {
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
            taskExecutionService, taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(
                1L)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "items", List.of(),
                            "iteratee", new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))))
            .build();

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
        verify(eventPublisher, times(1)).publishEvent(any(TaskExecutionCompleteEvent.class));
    }
}
