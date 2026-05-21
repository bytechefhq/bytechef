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
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
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

    // An ActionContext that records the Suspend it received. The real context implements both
    // ActionContext and ActionContextAware; the test double mirrors that.
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

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(null);

        doAnswer(invocation -> {
            suspendRef.set(invocation.getArgument(0));

            return null;
        }).when(agentContext)
            .suspend(any());

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Parameters inputParameters = MockParametersFactory.create(
            Map.of("workflowUuid", "uuid-1", "inputs", Map.of("amount", 5)));

        Object result = toolFunction.apply(
            inputParameters, MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(ToolSuspendConstants.SUSPENDED_SENTINEL, result);

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

        when(context.getAgentActionContext()).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-1")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        // Assert by equality against the package-private constant rather than substring matching: prevents future
        // reword from leaking past, and prevents this test from passing against an unrelated error path (S1).

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

    /**
     * C1 regression test: the spec calls out the agent-is-itself-a-sub-workflow case as the v1 edge case that
     * <strong>must never silently swallow</strong>, because silent swallowing is the exact bug #5055. If the tool
     * suspends in this case, the eventual {@code resumeJob} on the agent hits {@code JobServiceImpl
     * .resumeToStatusStarted}'s {@code parentTaskExecutionId == null} assertion and the agent is parked forever. The
     * tool must detect this and return an LLM-readable error <em>without</em> suspending.
     */
    @Test
    void testToolReturnsErrorWhenAgentIsItselfASubflow() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(42L);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(subflowDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("workflowUuid", "uuid-1")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(WorkflowCallWorkflowTool.ERROR_AGENT_IS_SUBFLOW, result);

        // Critical: the tool MUST NOT call suspend in this case. If it does, the agent parks forever (silent-park
        // failure), which is the exact regression class #5055 was filed against.

        verify(agentContext, never()).suspend(any());
    }

    /**
     * I4: when {@code SubflowResolver.resolveSubflow} throws (workflow missing / unpublished / trigger removed), the
     * tool must surface this to the LLM as a tool-result error rather than letting the exception escape to Spring AI.
     * Per the spec's Error-handling table.
     */
    @Test
    void testToolReturnsErrorWhenResolutionFails() throws Exception {
        SubflowDataSource subflowDataSource = mock(SubflowDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(subflowResolver.resolveSubflow("uuid-missing", "newWorkflowCall", false))
            .thenThrow(new IllegalStateException("workflow not found"));

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(null);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);
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

    // Reaches the package-private tool function under test.
    private static ToolFunction getToolFunction(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {

        ClusterElementDefinition<ToolFunction> definition = WorkflowCallWorkflowTool.of(
            subflowDataSource, subflowResolver);

        return definition.getElement();
    }
}
