
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

package com.bytechef.task.dispatcher.branch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bytechef.message.broker.MessageRoute;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BranchTaskDispatcherTest {

    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final ContextService contextService = mock(ContextService.class);
    private final MessageBroker messageBroker = mock(MessageBroker.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final WorkflowFileStorageFacade workflowFileStorageFacade = new WorkflowFileStorageFacadeImpl(
        base64FileStorageService, new ObjectMapper());

    @Test
    public void test1() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(base64FileStorageService.storeFileContent("", "", CompressionUtils.compress("{}")));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder().workflowTask(
            WorkflowTask.of(
                Map.of(
                    WorkflowConstants.NAME, "name",
                    WorkflowConstants.TYPE, "type",
                    WorkflowConstants.PARAMETERS,
                    Map.of(
                        "cases",
                        List.of(
                            Map.of(
                                "key", "k1",
                                "tasks",
                                List.of(WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))),
                        "expression", "k1")
                )
            )
        ).build();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);

        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(
                        WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "print")))
                    .build());

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher, times(1)).dispatch(argument.capture());

        taskExecution = argument.getValue();

        Assertions.assertEquals("print", taskExecution.getType());
    }

    @Test
    public void test2() {
        when(contextService.peek(anyLong(), any())).thenReturn(
            base64FileStorageService.storeFileContent("", "", "{}"));

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(
                WorkflowTask.of(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "cases",
                            List.of(
                                Map.of(
                                    "key", "k1",
                                    "tasks",
                                    List.of(WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))),
                            "expression", "k2"))))
            .build();

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test3() {
        when(contextService.peek(anyLong(), any())).thenReturn(
            workflowFileStorageFacade.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder().workflowTask(
            WorkflowTask.of(
                Map.of(
                    WorkflowConstants.NAME, "name",
                    WorkflowConstants.TYPE, "type",
                    WorkflowConstants.PARAMETERS,
                    Map.of(
                        "cases", Arrays.asList(
                            Map.of(
                                "key", "k1",
                                "tasks", List.of(
                                    WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "print")))),
                            Map.of(
                                "key", "k2",
                                "tasks", List.of(
                                    WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "sleep"))))),
                        "expression", "k2")))).build();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(WorkflowTask.of(Map.of(WorkflowConstants.NAME, "name", "type", "sleep"))).build());

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher, times(1)).dispatch(argument.capture());

        Assertions.assertEquals("sleep", argument.getValue()
            .getType());
    }

    @Test
    public void test4() {
        when(contextService.peek(anyLong(), any())).thenReturn(
            base64FileStorageService.storeFileContent("", "", "{}"));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, messageBroker, taskDispatcher, taskExecutionService, workflowFileStorageFacade);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(
                WorkflowTask.of(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "cases", Arrays.asList(
                                Map.of(
                                    "key", "k1",
                                    "tasks", List.of(Map.of(WorkflowConstants.NAME, "name", "type", "print"))),
                                Map.of(
                                    "key", "k2",
                                    "tasks",
                                    List.of(Map.of(WorkflowConstants.NAME, "name", "type", "sleep")))),
                            "default", Collections.singletonMap("value", "1234"),
                            "expression", "k99"))))
            .build();

        when(taskExecutionService.update(any())).thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<MessageRoute> arg1 = ArgumentCaptor.forClass(MessageRoute.class);
        ArgumentCaptor<TaskExecution> arg2 = ArgumentCaptor.forClass(TaskExecution.class);

        verify(messageBroker, times(1)).send(arg1.capture(), arg2.capture());

        TaskExecution value = arg2.getValue();

        Assertions.assertEquals(
            "1234", workflowFileStorageFacade.readTaskExecutionOutput(value.getOutput()));
    }
}
