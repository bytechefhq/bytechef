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

package com.bytechef.task.dispatcher.graph.completion;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.CONDITION;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.FROM;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TO;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
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
import com.bytechef.file.storage.domain.FileEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class GraphTaskCompletionHandlerTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private final Base64FileStorageService base64FileStorageService = new Base64FileStorageService();
    private final ContextService contextService = mock(ContextService.class);
    private final CounterService counterService = mock(CounterService.class);
    private final TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);
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

    // (a) canHandle

    @Test
    public void testCanHandleReturnsTrueForNodeCompletion() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(printTask("classify")), List.of(), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        assertTrue(handler().canHandle(completedChild));
    }

    @Test
    public void testCanHandleReturnsFalseWhenNodeStampAbsent() {
        TaskExecution unstampedChild = TaskExecution.builder()
            .id(2L)
            .jobId(100L)
            .parentId(1L)
            .priority(5)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "classify", "type", "print")))
            .build();

        assertFalse(handler().canHandle(unstampedChild));

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    public void testCanHandleReturnsFalseWhenParentIsNotGraph() {
        TaskExecution nonGraphParent = TaskExecution.builder()
            .id(1L)
            .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "cond", "type", "condition/v1")))
            .build();

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(nonGraphParent);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        assertFalse(handler().canHandle(completedChild));
    }

    // (b) transition to target node

    @Test
    public void testHandleTransitionsToTargetNodeAndDecrementsCounter() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("approve")),
            List.of(transition("classify", "approve", null)),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(5L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approve", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        handler().handle(completedChild);

        verify(counterService, times(1)).decrement(1L);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        TaskExecution createdSubTaskExecution = createCaptor.getValue();

        assertEquals("approve", createdSubTaskExecution.getParameters()
            .get(NODE));
        assertEquals(1, createdSubTaskExecution.getTaskNumber());

        verify(taskDispatcher, times(1)).dispatch(any());
        verify(taskCompletionHandler, never()).handle(any());
    }

    @Test
    public void testHandleTakesFirstTruthyConditionalBeforeDefault() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("reject"), printTask("review"), printTask("approve")),
            List.of(
                transition("classify", "reject", "=false"),
                transition("classify", "review", "=true"),
                transition("classify", "approve", null)),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(5L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "review", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        handler().handle(completedChild);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "review", createCaptor.getValue()
                .getParameters()
                .get(NODE));
    }

    @Test
    public void testHandleFallsBackToDefaultWhenNoConditionMatches() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("review"), printTask("approve")),
            List.of(
                transition("classify", "review", "=false"),
                transition("classify", "approve", null)),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(5L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approve", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        handler().handle(completedChild);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "approve", createCaptor.getValue()
                .getParameters()
                .get(NODE));
    }

    // (c) budget exhausted -> run fails with the pinned message

    @Test
    public void testHandleThrowsWhenTransitionBudgetExhausted() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("approve")),
            List.of(transition("classify", "approve", null)),
            1);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(-1L);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> handler().handle(completedChild));

        assertEquals("graph transition budget exhausted (maxTransitions=1)", exception.getMessage());

        verify(taskExecutionService, never()).create(any());
        verify(taskDispatcher, never()).dispatch(any());
        verify(taskCompletionHandler, never()).handle(any());
    }

    // (d) unknown target -> fails naming node + resolved value

    @Test
    public void testHandleThrowsWhenTransitionTargetUnknown() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(printTask("classify")), List.of(transition("classify", "missingNode", null)), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> handler().handle(completedChild));

        assertEquals(
            "Unknown graph transition target node: 'missingNode' resolved from node 'classify'",
            exception.getMessage());

        verify(counterService, never()).decrement(anyLong());
        verify(taskDispatcher, never()).dispatch(any());
    }

    // (e) terminal -> parent gets the child's output re-stored under its own id

    @Test
    public void testHandleCompletesGraphWithTerminalNodeOutput() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(printTask("approve")), List.of(), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        FileEntry childOutput = taskFileStorage.storeTaskExecutionOutput(100L, 2L, "approved!");

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "approve", "approve", childOutput);

        handler().handle(completedChild);

        ArgumentCaptor<TaskExecution> completionCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskCompletionHandler, times(1)).handle(completionCaptor.capture());

        TaskExecution completedGraphTaskExecution = completionCaptor.getValue();

        assertNotNull(completedGraphTaskExecution.getEndDate());
        assertNotNull(completedGraphTaskExecution.getOutput());
        assertEquals(
            "approved!", taskFileStorage.readTaskExecutionOutput(completedGraphTaskExecution.getOutput()));

        verify(taskDispatcher, never()).dispatch(any());
    }

    // (e2) terminal -> the graph's own transition-budget counter row is released

    @Test
    public void testHandleReleasesTransitionBudgetCounterOnCompletion() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(printTask("approve")), List.of(), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "approve", "approve", null);

        handler().handle(completedChild);

        // The row is keyed by the GRAPH's own execution id, and is released before the parent chain
        // is re-entered: a nested graph's completion advances the OUTER graph through that chain,
        // and the outer graph's budget is a different counter row entirely.
        InOrder inOrder = inOrder(counterService, taskCompletionHandler);

        inOrder.verify(counterService)
            .delete(1L);
        inOrder.verify(taskCompletionHandler)
            .handle(any());

        verify(counterService, never()).delete(2L);
    }

    // (f) condition evaluation reuses the hardened, injected Evaluator

    @Test
    public void testHandleEvaluatesConditionThroughInjectedEvaluator() {
        Evaluator spiedEvaluator = spy(EVALUATOR);

        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("review"), printTask("approve")),
            List.of(transition("classify", "review", "=${score} > 0.5")),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(
                taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of("score", 0.9)));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(9L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "review", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        GraphTaskCompletionHandler handler = new GraphTaskCompletionHandler(
            contextService, counterService, spiedEvaluator, taskCompletionHandler, taskDispatcher,
            taskExecutionService, taskFileStorage);

        handler.handle(completedChild);

        verify(spiedEvaluator, atLeastOnce()).evaluate(any(), any());

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "review", createCaptor.getValue()
                .getParameters()
                .get(NODE));
    }

    // (g) dynamic `to` expression resolves the target node name

    @Test
    public void testHandleResolvesDynamicTargetExpression() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(printTask("classify"), printTask("review"), printTask("approve")),
            List.of(transition("classify", "=${score} > 0.5 ? 'review' : 'approve'", null)),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(
                taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of("score", 0.9)));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(9L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "review", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classify", null);

        handler().handle(completedChild);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "review", createCaptor.getValue()
                .getParameters()
                .get(NODE));
    }

    private GraphTaskCompletionHandler handler() {
        return new GraphTaskCompletionHandler(
            contextService, counterService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }

    private static TaskExecution graphTaskExecution(
        long id, List<Map<String, ?>> nodes, List<Map<String, ?>> transitions, Integer maxTransitions) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(NODES, nodes);
        parameters.put(TRANSITIONS, transitions);

        if (maxTransitions != null) {
            parameters.put(MAX_TRANSITIONS, maxTransitions);
        }

        return TaskExecution.builder()
            .id(id)
            .jobId(100L)
            .priority(5)
            .status(TaskExecution.Status.STARTED)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "graphTask",
                        WorkflowConstants.TYPE, "graph/v1",
                        WorkflowConstants.PARAMETERS, parameters)))
            .build();
    }

    private static Map<String, ?> transition(String from, String to, String condition) {
        Map<String, Object> transition = new HashMap<>();

        transition.put(FROM, from);
        transition.put(TO, to);

        if (condition != null) {
            transition.put(CONDITION, condition);
        }

        return transition;
    }

    private static TaskExecution childTaskExecution(
        long id, long parentId, int taskNumber, String nodeName, String name, FileEntry output) {

        TaskExecution.Builder builder = TaskExecution.builder()
            .id(id)
            .jobId(100L)
            .parentId(parentId)
            .priority(5)
            .status(TaskExecution.Status.STARTED)
            .taskNumber(taskNumber)
            .workflowTask(stampedWorkflowTask(name, "print", nodeName));

        if (output != null) {
            builder.output(output);
        }

        return builder.build();
    }

    private static WorkflowTask stampedWorkflowTask(String name, String type, String nodeName) {
        return new WorkflowTask(
            Map.of(
                WorkflowConstants.NAME, name,
                WorkflowConstants.TYPE, type,
                WorkflowConstants.PARAMETERS, Map.of(NODE, nodeName)));
    }

    private static Map<String, ?> printTask(String name) {
        return Map.of(WorkflowConstants.NAME, name, WorkflowConstants.TYPE, "print");
    }
}
