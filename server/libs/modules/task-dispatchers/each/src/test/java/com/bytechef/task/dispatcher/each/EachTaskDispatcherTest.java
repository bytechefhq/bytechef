
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 */
public class EachTaskDispatcherTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final RemoteContextService contextService = mock(RemoteContextService.class);
    private final RemoteCounterService counterService = mock(RemoteCounterService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final RemoteTaskExecutionService taskExecutionService = mock(RemoteTaskExecutionService.class);
    private final WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
        new Base64FileStorageService(), new ObjectMapper());

    @Test
    public void testDispatch1() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            EachTaskDispatcher dispatcher = new EachTaskDispatcher(
                eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
                workflowFileStorageFacade);

            dispatcher.dispatch(TaskExecution.builder()
                .workflowTask(WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "type")))
                .build());
        });
    }

    @Test
    public void testDispatch2() {
        when(contextService.peek(anyLong(), any())).thenReturn(
            workflowFileStorageFacade.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
            eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
            workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder().workflowTask(
            WorkflowTask.of(
                Map.of(
                    WorkflowConstants.NAME, "name",
                    WorkflowConstants.TYPE, "type",
                    WorkflowConstants.PARAMETERS,
                    Map.of(
                        "list", Arrays.asList(1, 2, 3),
                        "iteratee", WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(1L);

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(3)).dispatch(any());
        verify(eventPublisher, times(0)).publishEvent(any());
    }

    @Test
    public void testDispatch3() {
        EachTaskDispatcher dispatcher = new EachTaskDispatcher(
            eventPublisher, contextService, counterService, taskDispatcher, taskExecutionService,
            workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(
                1L)
            .workflowTask(
                WorkflowTask.of(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "list", List.of(),
                            "iteratee", WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))))
            .build();

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        dispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
        verify(eventPublisher, times(1)).publishEvent(any(TaskExecutionCompleteEvent.class));
    }
}
