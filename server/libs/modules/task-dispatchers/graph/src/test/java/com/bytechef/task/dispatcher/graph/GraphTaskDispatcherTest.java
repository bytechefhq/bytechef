/*
 * Copyright 2025 ByteChef
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

package com.bytechef.task.dispatcher.graph;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.START_NODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.DeferredEvaluationParameterKeys;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class GraphTaskDispatcherTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final ContextService contextService = mock(ContextService.class);
    private final CounterService counterService = mock(CounterService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    @SuppressWarnings("unchecked")
    private final TaskDispatcher<? super Task> taskDispatcher = mock(TaskDispatcher.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(base64FileStorageService);

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    public void testDispatchStartsExplicitStartNode() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));

        TaskExecution graphTaskExecution = graphTaskExecution(
            List.of(printTask("classify"), printTask("approve")), "approve", null);

        when(taskExecutionService.update(any()))
            .thenReturn(graphTaskExecution);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approve", "type", "print")))
                    .build());

        GraphTaskDispatcher dispatcher = new GraphTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);

        dispatcher.dispatch(graphTaskExecution);

        verify(counterService, times(1)).set(1L, 100);
        verify(eventPublisher, never()).publishEvent(any());

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        TaskExecution createdSubTaskExecution = createCaptor.getValue();

        Assertions.assertEquals("approve", createdSubTaskExecution.getParameters()
            .get(NODE));
        Assertions.assertEquals("approve", createdSubTaskExecution.getName());
        Assertions.assertEquals(1L, createdSubTaskExecution.getParentId());
        Assertions.assertEquals(1, createdSubTaskExecution.getTaskNumber());

        verify(taskDispatcher, times(1)).dispatch(any());
    }

    @Test
    public void testDispatchDefaultsToFirstDeclaredNodeWhenStartNodeAbsent() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));

        TaskExecution graphTaskExecution = graphTaskExecution(
            List.of(printTask("classify"), printTask("approve")), null, null);

        when(taskExecutionService.update(any()))
            .thenReturn(graphTaskExecution);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "classify", "type", "print")))
                    .build());

        GraphTaskDispatcher dispatcher = new GraphTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);

        dispatcher.dispatch(graphTaskExecution);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        TaskExecution createdSubTaskExecution = createCaptor.getValue();

        Assertions.assertEquals("classify", createdSubTaskExecution.getParameters()
            .get(NODE));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testDispatchSeedsCounterWithDefaultMaxTransitions() {
        when(contextService.peek(anyLong(), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));

        TaskExecution graphTaskExecution = graphTaskExecution(List.of(printTask("classify")), null, 7);

        when(taskExecutionService.update(any()))
            .thenReturn(graphTaskExecution);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(2L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "classify", "type", "print")))
                    .build());

        GraphTaskDispatcher dispatcher = new GraphTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);

        dispatcher.dispatch(graphTaskExecution);

        verify(counterService, times(1)).set(1L, 7);
    }

    @Test
    public void testDispatchWhenStartNodeUnknownFails() {
        TaskExecution graphTaskExecution = graphTaskExecution(List.of(printTask("classify")), "missing", null);

        dispatcher().dispatch(graphTaskExecution);

        ArgumentCaptor<TaskExecutionErrorEvent> errorCaptor = ArgumentCaptor.forClass(TaskExecutionErrorEvent.class);

        verify(eventPublisher, times(1)).publishEvent(errorCaptor.capture());

        TaskExecutionErrorEvent taskExecutionErrorEvent = errorCaptor.getValue();

        TaskExecution erroredTaskExecution = taskExecutionErrorEvent.getTaskExecution();

        Assertions.assertTrue(erroredTaskExecution.getError()
            .getMessage()
            .contains("missing"));

        verify(taskDispatcher, never()).dispatch(any());
        verify(counterService, never()).set(anyLong(), anyLong());
    }

    @Test
    public void testDispatchWhenDuplicateNodeNamesFails() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            List.of(printTask("classify"), printTask("classify")), null, null);

        dispatcher().dispatch(graphTaskExecution);

        ArgumentCaptor<TaskExecutionErrorEvent> errorCaptor = ArgumentCaptor.forClass(TaskExecutionErrorEvent.class);

        verify(eventPublisher, times(1)).publishEvent(errorCaptor.capture());

        TaskExecutionErrorEvent taskExecutionErrorEvent = errorCaptor.getValue();

        TaskExecution erroredTaskExecution = taskExecutionErrorEvent.getTaskExecution();

        Assertions.assertTrue(erroredTaskExecution.getError()
            .getMessage()
            .contains("classify"));

        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    public void testDispatchWhenNodesEmptyFails() {
        TaskExecution graphTaskExecution = graphTaskExecution(List.of(), null, null);

        dispatcher().dispatch(graphTaskExecution);

        verify(eventPublisher, times(1)).publishEvent(any(TaskExecutionErrorEvent.class));
        verify(taskDispatcher, never()).dispatch(any());
    }

    @Test
    public void testDeferredKeysCoverNodesAndTransitions() throws ClassNotFoundException {
        // force the static initializer
        Class.forName("com.bytechef.task.dispatcher.graph.config.GraphTaskDispatcherConfiguration");

        Set<String> deferredKeys = DeferredEvaluationParameterKeys.forTaskType("graph/v1");

        Assertions.assertTrue(deferredKeys.contains("nodes"));
        Assertions.assertTrue(deferredKeys.contains("transitions"));
    }

    private GraphTaskDispatcher dispatcher() {
        return new GraphTaskDispatcher(
            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }

    private static TaskExecution graphTaskExecution(
        List<Map<String, ?>> nodes, String startNode, Integer maxTransitions) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(NODES, nodes);

        if (startNode != null) {
            parameters.put(START_NODE, startNode);
        }

        if (maxTransitions != null) {
            parameters.put(MAX_TRANSITIONS, maxTransitions);
        }

        return TaskExecution.builder()
            .id(1L)
            .jobId(2L)
            .priority(5)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "graphTask",
                        WorkflowConstants.TYPE, "graph/v1",
                        WorkflowConstants.PARAMETERS, parameters)))
            .build();
    }

    private static Map<String, ?> printTask(String name) {
        return Map.of(WorkflowConstants.NAME, name, WorkflowConstants.TYPE, "print");
    }
}
