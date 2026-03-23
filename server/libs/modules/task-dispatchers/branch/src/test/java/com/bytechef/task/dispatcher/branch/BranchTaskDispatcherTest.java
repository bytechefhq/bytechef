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

package com.bytechef.task.dispatcher.branch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.DeferredEvaluationParameterKeys;
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
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class BranchTaskDispatcherTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final ContextService contextService = mock(ContextService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(base64FileStorageService);

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        DeferredEvaluationParameterKeys.register("branch/", "cases", "default");
    }

    @Test
    public void test1() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(base64FileStorageService.storeFileContent("", "", CompressionUtils.compress("{}")));
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(1L)
                    .build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
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
                                    List.of(
                                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))),
                            "expression", "k1"))))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);

        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(
                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "print")))
                    .build());

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher, times(1)).dispatch(argument.capture());

        taskExecution = argument.getValue();

        Assertions.assertEquals("print", taskExecution.getType());
    }

    @Test
    public void test2() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(base64FileStorageService.storeFileContent("", "", "{}"));

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(
                new WorkflowTask(
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
                                    List.of(
                                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "print"))))),
                            "expression", "k2"))))
            .build();

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        verify(taskDispatcher, times(0)).dispatch(any());
    }

    @Test
    public void test3() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(1L)
                    .build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "cases", Arrays.asList(
                                Map.of(
                                    "key", "k1",
                                    "tasks", List.of(
                                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "print")))),
                                Map.of(
                                    "key", "k2",
                                    "tasks", List.of(
                                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "sleep"))))),
                            "expression", "k2"))))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", "type", "sleep")))
                    .build());

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher, times(1)).dispatch(argument.capture());

        TaskExecution value = argument.getValue();

        Assertions.assertEquals("sleep", value.getType());
    }

    @Test
    public void testDeferredEvaluationDoesNotCorruptSubTaskExpressions() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(
                taskFileStorage.storeContextValue(
                    1, Context.Classname.TASK_EXECUTION, Map.of("selectedCase", "caseA")));
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(
                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "print")))
                    .build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage);

        // Use branch/v1 type so deferred evaluation kicks in.
        // The key "${selectedCase}" should be evaluated to "caseA" by the dispatcher,
        // while the tasks inside cases should NOT be pre-evaluated.
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "branchTask",
                        WorkflowConstants.TYPE, "branch/v1",
                        WorkflowConstants.PARAMETERS,
                        Map.of(
                            "cases", List.of(
                                Map.of(
                                    "key", "${selectedCase}",
                                    "tasks", List.of(
                                        Map.of(
                                            WorkflowConstants.NAME, "matchedTask",
                                            WorkflowConstants.TYPE, "print",
                                            WorkflowConstants.PARAMETERS, Map.of("msg", "${someVar}")))),
                                Map.of(
                                    "key", "caseB",
                                    "tasks", List.of(
                                        Map.of(
                                            WorkflowConstants.NAME, "unmatchedTask",
                                            WorkflowConstants.TYPE, "sleep",
                                            WorkflowConstants.PARAMETERS, Map.of("msg", "${otherVar}"))))),
                            "expression", "${selectedCase}"))))
            .build();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);

        // Pre-evaluate with context (simulates what JobExecutor does)
        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(1L, Context.Classname.TASK_EXECUTION));

        taskExecution.evaluate(context, EVALUATOR);

        // Verify that 'cases' sub-task expressions are NOT resolved
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> cases = (List<Map<String, ?>>) taskExecution.getParameters()
            .get("cases");

        @SuppressWarnings("unchecked")
        List<Map<String, ?>> firstCaseTasks = (List<Map<String, ?>>) cases.get(0)
            .get("tasks");

        @SuppressWarnings("unchecked")
        Map<String, ?> firstTaskParams = (Map<String, ?>) firstCaseTasks.get(0)
            .get(WorkflowConstants.PARAMETERS);

        Assertions.assertEquals(
            "${someVar}", firstTaskParams.get("msg"),
            "Sub-task expressions inside deferred cases should NOT be pre-evaluated");

        // But the expression parameter (non-deferred) should be evaluated
        Assertions.assertEquals("caseA", taskExecution.getParameters()
            .get("expression"));

        // Now verify dispatch still works — the dispatcher evaluates keys itself
        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher, times(1)).dispatch(argument.capture());

        Assertions.assertEquals("print", argument.getValue()
            .getType());
    }

    @Test
    public void test4() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(base64FileStorageService.storeFileContent("", "", CompressionUtils.compress("{}")));
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(1L)
                    .build());

        BranchTaskDispatcher branchTaskDispatcher = new BranchTaskDispatcher(
            contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);
        TaskExecution taskExecution = TaskExecution.builder()
            .id(1L)
            .workflowTask(
                new WorkflowTask(
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
                            "default", List.of(),
                            "expression", "k99"))))
            .build();

        when(taskExecutionService.update(any()))
            .thenReturn(taskExecution);

        branchTaskDispatcher.dispatch(taskExecution);

        ArgumentCaptor<TaskExecutionCompleteEvent> taskExecutionCompleteEventArgumentCaptor = ArgumentCaptor.forClass(
            TaskExecutionCompleteEvent.class);

        verify(eventPublisher, times(1)).publishEvent(taskExecutionCompleteEventArgumentCaptor.capture());

        TaskExecutionCompleteEvent taskExecutionCompleteEvent = taskExecutionCompleteEventArgumentCaptor.getValue();

        Assertions.assertNotNull(taskExecutionCompleteEvent);
    }
}
