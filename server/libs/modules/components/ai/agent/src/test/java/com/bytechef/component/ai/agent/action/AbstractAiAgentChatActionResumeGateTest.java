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

package com.bytechef.component.ai.agent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Dedicated coverage for the approval-gate resume branch ({@code resolveGatedToolResumeData}): approval executes the
 * gated tool with the originally captured arguments through the RAW callback, rejection feeds a denial back into the
 * loop without executing anything, a vanished tool fails loudly, and a tool failure after approval is reported as an
 * error result instead of propagating.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AbstractAiAgentChatActionResumeGateTest {

    private static final String GATED_TOOL_INPUT = "{\"channel\": \"#general\", \"text\": \"hi\"}";
    private static final String GATED_TOOL_NAME = "SLACK_SEND_MESSAGE";

    private static final Parameters NO_SIMULATION_INPUT = ParametersFactory.create(Map.of());

    private static final Parameters EXTENSIONS = ParametersFactory.create(
        Map.of(
            "clusterElements",
            Map.of(
                "tools",
                List.of(
                    Map.of(
                        "name", "slack_1",
                        "type", "slack/v1/sendMessage",
                        "parameters", Map.of())))));

    private final AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService = mock(
        ClusterElementDefinitionService.class);
    private final ToolCallback toolCallback = mock(ToolCallback.class);

    private final AbstractAiAgentChatAction action = new AbstractAiAgentChatAction(
        aiAgentToolFacade, clusterElementDefinitionService,
        new AgentToolCallingManagers(mock(ToolCallingManager.class))) {};

    private final ActionContextAware context = mock(
        ActionContextAware.class, withSettings().extraInterfaces(ActionContext.class));

    @BeforeEach
    void setUp() {
        when(context.isEditorEnvironment()).thenReturn(false);
        when(clusterElementDefinitionService.getClusterElement("slack", 1, "sendMessage"))
            .thenReturn(mock(MultipleConnectionsToolFunction.class));
        when(toolCallback.getToolDefinition()).thenReturn(
            DefaultToolDefinition.builder()
                .name(GATED_TOOL_NAME)
                .description("Send a Slack message")
                .inputSchema("{}")
                .build());
        when(aiAgentToolFacade.getFunctionToolCallback(any(), anyMap(), eq(false))).thenReturn(toolCallback);
    }

    @Test
    void testApprovedExecutesToolWithOriginalArgumentsAndReportsResult() throws Exception {
        when(toolCallback.call(eq(GATED_TOOL_INPUT), any(ToolContext.class))).thenReturn("message sent: ts=1721");

        String resumeData = action.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            data(true, "ship it"), Map.of(), EXTENSIONS, context);

        // The RAW callback must execute the originally captured arguments — not whatever the human typed.
        verify(toolCallback).call(eq(GATED_TOOL_INPUT), any(ToolContext.class));

        assertThat(resumeData)
            .contains("approvedByReviewer")
            .contains("message sent: ts=1721")
            .contains("ship it");
    }

    @Test
    void testRejectedFeedsDenialWithCommentWithoutExecutingTheTool() throws Exception {
        String resumeData = action.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            data(false, "not now"), Map.of(), EXTENSIONS, context);

        assertThat(resumeData)
            .contains("denied")
            .contains("Denied by reviewer: not now");

        // Rejection must short-circuit before any tool machinery is touched.
        verifyNoInteractions(aiAgentToolFacade);
        verifyNoInteractions(toolCallback);
    }

    @Test
    void testApprovedCarriesVerifiedReviewerIdentity() throws Exception {
        when(toolCallback.call(eq(GATED_TOOL_INPUT), any(ToolContext.class))).thenReturn("message sent: ts=1721");

        String resumeData = action.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            dataWithApprovedBy(true, "@jane"), Map.of(), EXTENSIONS, context);

        assertThat(resumeData)
            .contains("approvedByReviewer")
            .contains("\"reviewer\"")
            .contains("@jane");
    }

    @Test
    void testRejectedCarriesVerifiedReviewerIdentity() throws Exception {
        String resumeData = action.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            dataWithApprovedBy(false, "@jane"), Map.of(), EXTENSIONS, context);

        assertThat(resumeData)
            .contains("denied")
            .contains("\"deniedBy\"")
            .contains("@jane");

        verifyNoInteractions(toolCallback);
    }

    @Test
    void testApprovedButToolNoLongerConfiguredFailsLoudly() {
        assertThatThrownBy(
            () -> action.resolveGatedToolResumeData(
                NO_SIMULATION_INPUT, continueParameters("REMOVED_TOOL"),
                data(true, null), Map.of(), EXTENSIONS, context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REMOVED_TOOL");
    }

    @Test
    void testApprovedToolFailureIsReportedAsErrorInsteadOfPropagating() throws Exception {
        when(toolCallback.call(eq(GATED_TOOL_INPUT), any(ToolContext.class)))
            .thenThrow(new RuntimeException("connection refused"));

        String resumeData = action.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            data(true, null), Map.of(), EXTENSIONS, context);

        assertThat(resumeData)
            .contains("approvedByReviewer")
            .contains("connection refused")
            .doesNotContain("\"result\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRejectedRecordsApprovalDeniedAuditEvent() throws Exception {
        ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);

        AbstractAiAgentChatAction auditedAction = createAuditedAction(toolExecutionRecorder);

        auditedAction.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            data(false, "not now"), Map.of(), EXTENSIONS, context);

        ArgumentCaptor<ToolExecutionEvent> eventArgumentCaptor = ArgumentCaptor.forClass(ToolExecutionEvent.class);

        verify(toolExecutionRecorder).record(eventArgumentCaptor.capture());

        ToolExecutionEvent toolExecutionEvent = eventArgumentCaptor.getValue();

        assertThat(toolExecutionEvent.surface()).isEqualTo(ToolExecutionSurface.AI_AGENT);
        assertThat(toolExecutionEvent.toolName()).isEqualTo(GATED_TOOL_NAME);
        assertThat(toolExecutionEvent.outcome()).isEqualTo(ToolExecutionOutcome.APPROVAL_DENIED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testApprovedExecutionRunsThroughTheAuditRecorder() throws Exception {
        when(toolCallback.call(eq(GATED_TOOL_INPUT), any(ToolContext.class))).thenReturn("message sent: ts=1721");

        ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);

        // The recorder wraps the execution — make the mock actually run the supplier so the tool executes.
        when(toolExecutionRecorder.record(any(ToolExecutionEvent.Builder.class), any(Supplier.class)))
            .thenAnswer(invocation -> ((Supplier<Object>) invocation.getArgument(1)).get());

        AbstractAiAgentChatAction auditedAction = createAuditedAction(toolExecutionRecorder);

        String resumeData = auditedAction.resolveGatedToolResumeData(
            NO_SIMULATION_INPUT, continueParameters(GATED_TOOL_NAME),
            data(true, null), Map.of(), EXTENSIONS, context);

        verify(toolExecutionRecorder).record(any(ToolExecutionEvent.Builder.class), any(Supplier.class));
        verify(toolCallback).call(eq(GATED_TOOL_INPUT), any(ToolContext.class));

        assertThat(resumeData).contains("message sent: ts=1721");
    }

    @SuppressWarnings("unchecked")
    private AbstractAiAgentChatAction createAuditedAction(ToolExecutionRecorder toolExecutionRecorder) {
        ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider = mock(ObjectProvider.class);

        when(toolExecutionRecorderObjectProvider.getIfAvailable()).thenReturn(toolExecutionRecorder);

        return new AbstractAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService,
            new AgentToolCallingManagers(mock(ToolCallingManager.class)), toolExecutionRecorderObjectProvider) {};
    }

    private static Parameters continueParameters(String gatedToolName) {
        return ParametersFactory.create(
            Map.of(
                ToolSuspendConstants.GATED_TOOL_NAME, gatedToolName,
                ToolSuspendConstants.GATED_TOOL_INPUT, GATED_TOOL_INPUT));
    }

    private static Parameters data(boolean approved, String comment) {
        return ParametersFactory.create(
            comment == null ? Map.of("approved", approved) : Map.of("approved", approved, "comment", comment));
    }

    private static Parameters dataWithApprovedBy(boolean approved, String approvedBy) {
        return ParametersFactory.create(Map.of("approved", approved, "approvedBy", approvedBy));
    }
}
