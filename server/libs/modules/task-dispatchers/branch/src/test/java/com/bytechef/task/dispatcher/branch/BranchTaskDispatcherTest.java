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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.task.dispatcher.branch;

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
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BranchTaskDispatcherTest {

    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final ContextService contextService = mock(ContextService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(base64FileStorageService);

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class JsonUtilsMock extends JsonUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new JsonUtilsMock();

        class MapUtilsMock extends MapUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new MapUtilsMock();
    }

    @Test
    public void test1() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(base64FileStorageService.storeFileContent("", "", CompressionUtils.compress("{}")));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage);
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
            eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage);
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
            taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.create(any())).thenReturn(TaskExecution.builder().id(1L).build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage);
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
            eventPublisher, contextService, taskDispatcher, taskExecutionService, taskFileStorage);
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

        ArgumentCaptor<TaskExecutionCompleteEvent> taskExecutionCompleteEventArgumentCaptor = ArgumentCaptor.forClass(
            TaskExecutionCompleteEvent.class);

        verify(eventPublisher, times(1)).publishEvent(taskExecutionCompleteEventArgumentCaptor.capture());

        TaskExecutionCompleteEvent taskExecutionCompleteEvent = taskExecutionCompleteEventArgumentCaptor.getValue();

        taskExecution = taskExecutionCompleteEvent.getTaskExecution();

        Assertions.assertEquals("1234", taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
    }
}
