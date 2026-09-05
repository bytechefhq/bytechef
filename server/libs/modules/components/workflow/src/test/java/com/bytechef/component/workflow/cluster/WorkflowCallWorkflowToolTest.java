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

package com.bytechef.component.workflow.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowCallWorkflowToolTest {

    interface TestAgentContext extends ActionContext, ActionContextAware {
    }

    @Test
    void testToolSuspendsWithPendingSubflowRequest() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(subflowResolver.resolveSubflow("uuid-1", "newWorkflowCall", false))
            .thenReturn(new Subflow("wf-99", "newWorkflowCall"));

        AtomicReference<ActionContext.Suspend> suspendRef = new AtomicReference<>();

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenAnswer(invocation -> suspendRef.get());

        doAnswer(invocation -> {
            suspendRef.set(invocation.getArgument(0));

            return null;
        }).when(agentContext)
            .suspend(any());

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.toActionContext("workflow", 1, "callWorkflow", null)).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Parameters inputParameters = MockParametersFactory.create(
            Map.of("workflowUuid", "uuid-1", "inputs", Map.of("amount", 5)));

        Object result = toolFunction.apply(
            inputParameters, MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertTrue(ToolSuspension.isSuspendedToolResult((String) result, suspendRef.get()));

        ActionContext.Suspend suspend = suspendRef.get();

        assertNotNull(suspend);

        Object pending = suspend.continueParameters()
            .get(SubflowRequestConstants.PENDING_SUBFLOW);

        assertInstanceOf(PendingSubflowRequest.class, pending);
        assertEquals("wf-99", ((PendingSubflowRequest) pending).workflowId());
    }

    @Test
    void testToolReturnsErrorWhenSuspendAlreadyPending() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), null));

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.toActionContext("workflow", 1, "callWorkflow", null)).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-1")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(WorkflowCallWorkflowTool.ERROR_ALREADY_SUSPENDED, result);
        verify(agentContext, never()).suspend(any());
    }

    @Test
    void testToolReturnsErrorWhenContextIsNotClusterElementContextAware() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        ClusterElementContext context = mock(ClusterElementContext.class);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-1")),
            MockParametersFactory.create(Map.of()),
            context);

        assertEquals(WorkflowCallWorkflowTool.ERROR_NOT_AGENT_CONTEXT, result);
    }

    @Test
    void testToolRunsTheSubflowSynchronouslyInTheEditor() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);
        SubflowSyncExecutor subflowSyncExecutor = mock(SubflowSyncExecutor.class);

        when(subflowSyncExecutor.execute("uuid-1", "newWorkflowCall", Map.of("amount", 5), true))
            .thenReturn(Map.of("total", 10));

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(true);

        Object result = getToolFunction(subflowDataSource, subflowResolver, subflowSyncExecutor).apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-1", "inputs", Map.of("amount", 5))),
            MockParametersFactory.create(Map.of()), (ClusterElementContext) context);

        assertEquals(Map.of("total", 10), result);
        verify(context, never()).toActionContext(any(), anyInt(), any(), any());
    }

    @Test
    void testToolReturnsErrorWhenResolutionFails() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(subflowResolver.resolveSubflow("uuid-missing", "newWorkflowCall", false))
            .thenThrow(new IllegalStateException("workflow not found"));

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.toActionContext("workflow", 1, "callWorkflow", null)).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-missing")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertInstanceOf(String.class, result);
        assertTrue(
            ((String) result).startsWith(WorkflowCallWorkflowTool.ERROR_RESOLVE_FAILED_PREFIX),
            "Expected resolution-failure error prefix, got: " + result);
        assertTrue(
            ((String) result).contains("workflow not found"),
            "Expected underlying exception message in the tool-result error, got: " + result);

        verify(agentContext, never()).suspend(any());
    }

    private static ToolFunction getToolFunction(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {

        return getToolFunction(subflowDataSource, subflowResolver, mock(SubflowSyncExecutor.class));
    }

    private static ToolFunction getToolFunction(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver,
        SubflowSyncExecutor subflowSyncExecutor) {

        ClusterElementDefinition<ToolFunction> definition = WorkflowCallWorkflowTool.of(
            subflowDataSource, subflowResolver, subflowSyncExecutor);

        return definition.getElement();
    }
}
