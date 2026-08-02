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

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NEXT;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.ROUTER_NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TASKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    public void testCanHandleReturnsTrueForNormalNodeSubTaskCompletion() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(node("classify", List.of(printTask("classifyTask")), null)), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        assertTrue(handler().canHandle(completedChild));
    }

    @Test
    public void testCanHandleReturnsTrueForRouterHandOff() {
        TaskExecution routerTaskExecution = routerHandOffTaskExecution(
            1L, List.of(node("route", List.of(), "approve")), "route", null);

        assertTrue(handler().canHandle(routerTaskExecution));

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    public void testCanHandleReturnsFalseWhenNodeStampAbsent() {
        TaskExecution unstampedChild = TaskExecution.builder()
            .id(2L)
            .jobId(100L)
            .parentId(1L)
            .priority(5)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "classifyTask", "type", "print")))
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

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        assertFalse(handler().canHandle(completedChild));
    }

    // (b) mid-node advancement

    @Test
    public void testHandleDispatchesNextTaskWithinSameNode() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(node("classify", List.of(printTask("task1"), printTask("task2")), "approve")),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(3L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "task2", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "task1", null);

        handler().handle(completedChild);

        assertEquals(TaskExecution.Status.COMPLETED, completedChild.getStatus());

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        TaskExecution createdSubTaskExecution = createCaptor.getValue();

        assertEquals("classify", createdSubTaskExecution.getParameters()
            .get(NODE));
        assertEquals(2, createdSubTaskExecution.getTaskNumber());
        assertEquals(1L, createdSubTaskExecution.getParentId());

        verify(taskDispatcher, times(1)).dispatch(any());
        verify(counterService, never()).decrement(anyLong());
        verify(taskCompletionHandler, never()).handle(any());
    }

    // (c) node exhaustion + next -> target's first task dispatched with new __node, counter decremented

    @Test
    public void testHandleTransitionsToTargetNodesFirstTaskAndDecrementsCounter() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(
                node("classify", List.of(printTask("classifyTask")), "approve"),
                node("approve", List.of(printTask("approveTask")), null)),
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
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approveTask", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

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

    // (d) budget exhausted -> run fails with the pinned message

    @Test
    public void testHandleThrowsWhenTransitionBudgetExhausted() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(
                node("classify", List.of(printTask("classifyTask")), "approve"),
                node("approve", List.of(printTask("approveTask")), null)),
            5);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(-1L);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> handler().handle(completedChild));

        assertEquals("graph transition budget exhausted (maxTransitions=5)", exception.getMessage());

        verify(taskExecutionService, never()).create(any());
        verify(taskDispatcher, never()).dispatch(any());
        verify(taskCompletionHandler, never()).handle(any());
    }

    // (e) unknown target -> fails naming node + resolved value

    @Test
    public void testHandleThrowsWhenTransitionTargetUnknown() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(node("classify", List.of(printTask("classifyTask")), "missingNode")), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> handler().handle(completedChild));

        assertTrue(exception.getMessage()
            .contains("missingNode"));
        assertTrue(exception.getMessage()
            .contains("classify"));

        verify(counterService, never()).decrement(anyLong());
        verify(taskDispatcher, never()).dispatch(any());
    }

    // (f) terminal -> parent gets the child's output re-stored under its own id

    @Test
    public void testHandleCompletesGraphWithTerminalNodeOutput() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L, List.of(node("approve", List.of(printTask("approveTask")), null)), null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        FileEntry childOutput = taskFileStorage.storeTaskExecutionOutput(100L, 2L, "approved!");

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "approve", "approveTask", childOutput);

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

    @Test
    public void testHandleRouterHandOffTransitionsToTargetNode() {
        TaskExecution routerTaskExecution = routerHandOffTaskExecution(
            1L,
            List.of(
                node("route", List.of(), "approve"),
                node("approve", List.of(printTask("approveTask")), null)),
            "route", null);

        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(6L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approveTask", "type", "print")))
                    .build());

        handler().handle(routerTaskExecution);

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
        verify(counterService, times(1)).decrement(1L);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "approve", createCaptor.getValue()
                .getParameters()
                .get(NODE));

        verify(taskDispatcher, times(1)).dispatch(any());
    }

    @Test
    public void testHandleNestedRouterHandOffFiresWithInnerStartNodeAndPreservesOuterNodeStamp() {
        TaskExecution nestedRouterTaskExecution = nestedRouterHandOffTaskExecution(
            1L,
            List.of(
                node("innerRoute", List.of(), "innerApprove"),
                node("innerApprove", List.of(printTask("innerApproveTask")), null)),
            "outerNodeA", "innerRoute", null);

        // the router branch is detected on __routerNode alone -- no parent-type lookup, even though this graph's
        // parent (id 50L) IS itself a graph/v1, which the old "parent is not a graph" heuristic would have rejected
        assertTrue(handler().canHandle(nestedRouterTaskExecution));

        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(counterService.decrement(1L))
            .thenReturn(99L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(7L)
                    .workflowTask(
                        new WorkflowTask(Map.of(WorkflowConstants.NAME, "innerApproveTask", "type", "print")))
                    .build());

        handler().handle(nestedRouterTaskExecution);

        // fired directly off the event-carried execution -- never consulted taskExecutionService for the parent
        verify(taskExecutionService, never()).getTaskExecution(anyLong());
        verify(counterService, times(1)).decrement(1L);

        // the outer graph's own __node stamp survived the inner router hand-off untouched
        assertEquals(
            "outerNodeA", nestedRouterTaskExecution.getParameters()
                .get(NODE));
        assertEquals(
            "innerRoute", nestedRouterTaskExecution.getParameters()
                .get(ROUTER_NODE));

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        // transitioned using the INNER graph's own node ("innerApprove"), resolved from its own start node
        assertEquals(
            "innerApprove", createCaptor.getValue()
                .getParameters()
                .get(NODE));

        verify(taskDispatcher, times(1)).dispatch(any());
    }

    // (g) router chain: empty nodes hop in a loop, one budget unit per hop

    @Test
    public void testHandleRouterChainHopsThroughEmptyNodesToRealNode() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(
                node("classify", List.of(printTask("classifyTask")), "r1"),
                node("r1", List.of(), "r2"),
                node("r2", List.of(), "approve"),
                node("approve", List.of(printTask("approveTask")), null)),
            null);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(2L, 1L, 0L);
        when(taskExecutionService.create(any()))
            .thenReturn(
                TaskExecution.builder()
                    .id(9L)
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "approveTask", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        handler().handle(completedChild);

        verify(counterService, times(3)).decrement(1L);

        ArgumentCaptor<TaskExecution> createCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskExecutionService, times(1)).create(createCaptor.capture());

        assertEquals(
            "approve", createCaptor.getValue()
                .getParameters()
                .get(NODE));

        verify(taskDispatcher, times(1)).dispatch(any());
        verify(taskCompletionHandler, never()).handle(any());
    }

    @Test
    public void testHandlePureRouterCycleDiesByBudgetInsteadOfHanging() {
        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(
                node("classify", List.of(printTask("classifyTask")), "loopA"),
                node("loopA", List.of(), "loopB"),
                node("loopB", List.of(), "loopA")),
            3);

        when(taskExecutionService.getTaskExecution(1L))
            .thenReturn(graphTaskExecution);
        when(contextService.peek(eq(1L), any()))
            .thenReturn(taskFileStorage.storeContextValue(1, Context.Classname.TASK_EXECUTION, Map.of()));
        when(taskExecutionService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.decrement(1L))
            .thenReturn(2L, 1L, 0L, -1L);

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> handler().handle(completedChild));

        assertEquals("graph transition budget exhausted (maxTransitions=3)", exception.getMessage());

        // one decrement per hop: classify->loopA, loopA->loopB, loopB->loopA, loopA->loopB(exhausted)
        verify(counterService, times(4)).decrement(1L);
        verify(taskExecutionService, never()).create(any());
        verify(taskDispatcher, never()).dispatch(any());
    }

    // (h) `next` evaluation reuses the hardened, injected Evaluator

    @Test
    public void testHandleEvaluatesNextThroughInjectedEvaluator() {
        Evaluator spiedEvaluator = spy(EVALUATOR);

        TaskExecution graphTaskExecution = graphTaskExecution(
            1L,
            List.of(
                node("classify", List.of(printTask("classifyTask")), "=${score} > 0.5 ? 'review' : 'approve'"),
                node("review", List.of(printTask("reviewTask")), null),
                node("approve", List.of(printTask("approveTask")), null)),
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
                    .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "reviewTask", "type", "print")))
                    .build());

        TaskExecution completedChild = childTaskExecution(2L, 1L, 1, "classify", "classifyTask", null);

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

    private GraphTaskCompletionHandler handler() {
        return new GraphTaskCompletionHandler(
            contextService, counterService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }

    private static TaskExecution graphTaskExecution(long id, List<Map<String, ?>> nodes, Integer maxTransitions) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(NODES, nodes);

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

    private static TaskExecution routerHandOffTaskExecution(
        long id, List<Map<String, ?>> nodes, String routerNodeName, Integer maxTransitions) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(NODES, nodes);
        parameters.put(ROUTER_NODE, routerNodeName);

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

    /**
     * A router hand-off for a graph nested inside another graph's node: {@code __node} (stamped by the OUTER graph when
     * it dispatched this graph as one of its node's tasks) and {@code __routerNode} (self-stamped by THIS graph's own
     * empty-start-node router hand-off) both live on the same parameters map, at the same time.
     */
    private static TaskExecution nestedRouterHandOffTaskExecution(
        long id, List<Map<String, ?>> nodes, String outerNodeName, String routerNodeName, Integer maxTransitions) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(NODES, nodes);
        parameters.put(NODE, outerNodeName);
        parameters.put(ROUTER_NODE, routerNodeName);

        if (maxTransitions != null) {
            parameters.put(MAX_TRANSITIONS, maxTransitions);
        }

        return TaskExecution.builder()
            .id(id)
            .jobId(100L)
            .parentId(50L)
            .priority(5)
            .status(TaskExecution.Status.STARTED)
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "innerGraphTask",
                        WorkflowConstants.TYPE, "graph/v1",
                        WorkflowConstants.PARAMETERS, parameters)))
            .build();
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

    private static Map<String, ?> node(String name, List<Map<String, ?>> tasks, String next) {
        Map<String, Object> node = new LinkedHashMap<>();

        node.put(NAME, name);
        node.put(TASKS, tasks);

        if (next != null) {
            node.put(NEXT, next);
        }

        return node;
    }

    private static Map<String, ?> printTask(String name) {
        return Map.of(WorkflowConstants.NAME, name, WorkflowConstants.TYPE, "print");
    }
}
