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
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowCallAiAgentToolTest {

    // An ActionContext that records the Suspend it received. The real context implements both ActionContext and
    // ActionContextAware; the test double mirrors that (same convention as WorkflowCallWorkflowToolTest).
    interface TestAgentContext extends ActionContext, ActionContextAware {
    }

    @Test
    void testToolSuspendsWithPendingSubflowRequestCarryingMessageAndConversationId() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(callableAgentDataSource.resolveAgent("agent-uuid-1", false))
            .thenReturn(new ResolvedAiAgent("workflow-uuid-1", "Support Agent", "Handles support questions."));
        when(subflowResolver.resolveSubflow("workflow-uuid-1", "newWorkflowCall", false))
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

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Parameters inputParameters = MockParametersFactory.create(
            Map.of(
                "agentUuid", "agent-uuid-1", "message", "Please look into this.", "conversationId", "conv-1"));

        Object result = toolFunction.apply(
            inputParameters, MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(ToolSuspendConstants.SUSPENDED_SENTINEL, result);

        ActionContext.Suspend suspend = suspendRef.get();

        assertNotNull(suspend);

        Object pending = suspend.continueParameters()
            .get(SubflowRequestConstants.PENDING_SUBFLOW);

        assertInstanceOf(PendingSubflowRequest.class, pending);

        PendingSubflowRequest pendingSubflowRequest = (PendingSubflowRequest) pending;

        assertEquals("wf-99", pendingSubflowRequest.workflowId());
        assertEquals(
            Map.of("message", "Please look into this.", "conversationId", "conv-1"),
            pendingSubflowRequest.inputs());
    }

    @Test
    void testToolSuspendsWithoutConversationIdWhenBlank() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(callableAgentDataSource.resolveAgent("agent-uuid-1", false))
            .thenReturn(new ResolvedAiAgent("workflow-uuid-1", "Support Agent", "Handles support questions."));
        when(subflowResolver.resolveSubflow("workflow-uuid-1", "newWorkflowCall", false))
            .thenReturn(new Subflow("wf-99", "newWorkflowCall"));

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(null);

        AtomicReference<ActionContext.Suspend> suspendRef = new AtomicReference<>();

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

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Parameters inputParameters = MockParametersFactory.create(
            Map.of("agentUuid", "agent-uuid-1", "message", "Please look into this."));

        toolFunction.apply(inputParameters, MockParametersFactory.create(Map.of()), (ClusterElementContext) context);

        PendingSubflowRequest pendingSubflowRequest = (PendingSubflowRequest) suspendRef.get()
            .continueParameters()
            .get(SubflowRequestConstants.PENDING_SUBFLOW);

        assertEquals(Map.of("message", "Please look into this."), pendingSubflowRequest.inputs());
    }

    @Test
    void testToolReturnsErrorWhenSuspendAlreadyPending() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), null));

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("agentUuid", "agent-uuid-1", "message", "hi")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        // Assert by equality, not substring: prevents a future reword from leaking past, and prevents this test from
        // passing against an unrelated error path.

        assertEquals(WorkflowCallAiAgentTool.ERROR_ALREADY_SUSPENDED, result);
        verify(agentContext, never()).suspend(any());
    }

    @Test
    void testToolReturnsErrorWhenContextIsNotClusterElementContextAware() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        ClusterElementContext context = mock(ClusterElementContext.class);

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("agentUuid", "agent-uuid-1", "message", "hi")),
            MockParametersFactory.create(Map.of()),
            context);

        assertEquals(WorkflowCallAiAgentTool.ERROR_NOT_AGENT_CONTEXT, result);
    }

    /**
     * Same C1-class regression coverage as {@code WorkflowCallWorkflowToolTest}: the tool must detect that the calling
     * agent is itself a sub-workflow and return an LLM-readable error WITHOUT suspending, or the eventual
     * {@code resumeJob} on the agent parks it forever (bug class #5055).
     */
    @Test
    void testToolReturnsErrorWhenAgentIsItselfASubflow() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(42L);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("agentUuid", "agent-uuid-1", "message", "hi")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertEquals(WorkflowCallAiAgentTool.ERROR_AGENT_IS_SUBFLOW, result);

        verify(agentContext, never()).suspend(any());
    }

    @Test
    void testToolReturnsErrorWhenAgentResolutionFails() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(callableAgentDataSource.resolveAgent("missing-agent-uuid", false))
            .thenThrow(new IllegalArgumentException("agent has no published version"));

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(null);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("agentUuid", "missing-agent-uuid", "message", "hi")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertInstanceOf(String.class, result);
        assertTrue(
            ((String) result).startsWith(WorkflowCallAiAgentTool.ERROR_AGENT_RESOLVE_FAILED_PREFIX),
            "Expected agent-resolve error prefix, got: " + result);
        assertTrue(
            ((String) result).contains("agent has no published version"),
            "Expected underlying exception message in the tool-result error, got: " + result);

        verify(agentContext, never()).suspend(any());
    }

    @Test
    void testToolReturnsErrorWhenSubflowResolutionFails() throws Exception {
        CallableAiAgentDataSource callableAgentDataSource = mock(CallableAiAgentDataSource.class);
        SubflowResolver subflowResolver = mock(SubflowResolver.class);

        when(callableAgentDataSource.resolveAgent("agent-uuid-1", false))
            .thenReturn(new ResolvedAiAgent("workflow-uuid-1", "Support Agent", "Handles support questions."));
        when(subflowResolver.resolveSubflow("workflow-uuid-1", "newWorkflowCall", false))
            .thenThrow(new IllegalStateException("no callable trigger found"));

        TestAgentContext agentContext = mock(TestAgentContext.class);

        when(agentContext.getSuspend()).thenReturn(null);
        when(agentContext.getParentTaskExecutionId()).thenReturn(null);

        ClusterElementContextAware context = mock(
            ClusterElementContextAware.class,
            withSettings().extraInterfaces(ClusterElementContext.class));

        when(context.getAgentActionContext()).thenReturn(agentContext);
        when(((ClusterElementContext) context).isEditorEnvironment()).thenReturn(false);

        ToolFunction toolFunction = getToolFunction(callableAgentDataSource, subflowResolver);

        Object result = toolFunction.apply(
            MockParametersFactory.create(Map.of("agentUuid", "agent-uuid-1", "message", "hi")),
            MockParametersFactory.create(Map.of()),
            (ClusterElementContext) context);

        assertInstanceOf(String.class, result);
        assertTrue(
            ((String) result).startsWith(WorkflowCallAiAgentTool.ERROR_RESOLVE_FAILED_PREFIX),
            "Expected sub-workflow resolve error prefix, got: " + result);
        assertTrue(
            ((String) result).contains("no callable trigger found"),
            "Expected underlying exception message in the tool-result error, got: " + result);

        verify(agentContext, never()).suspend(any());
    }

    // Reaches the package-private tool function under test.
    private static ToolFunction getToolFunction(
        CallableAiAgentDataSource callableAgentDataSource, SubflowResolver subflowResolver) {

        ClusterElementDefinition<ToolFunction> definition = WorkflowCallAiAgentTool.of(
            callableAgentDataSource, subflowResolver);

        return definition.getElement();
    }
}
