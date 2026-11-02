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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

/**
 * @author Ivica Cardic
 */
class ApprovalGateToolCallbackTest {

    private static final ApprovalLinks APPROVAL_LINKS = new ApprovalLinks(
        "https://example.com", "abc123", "https://example.com/job/resume/abc123", "https://example.com/resume/abc123");

    private final ToolCallback delegate = mock(ToolCallback.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService = mock(
        ClusterElementDefinitionService.class);
    private final AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();
    private ActionContextAware actionContext;

    @BeforeEach
    void setUp() {
        actionContext = mock(ActionContextAware.class, withSettings().extraInterfaces(ActionContext.class));

        when(actionContext.getSuspend()).thenAnswer(invocation -> suspendReference.get());

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any(ActionContext.Suspend.class));

        when(delegate.getToolDefinition()).thenReturn(
            DefaultToolDefinition.builder()
                .name("SLACK_SEND_MESSAGE")
                .description("Send a Slack message")
                .inputSchema("{}")
                .build());
    }

    @Test
    void testFirstCallDeliversDefaultChatChannelAndSuspends() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, null, null);

        String result = gate.call("{\"channel\": \"#general\"}", null);

        assertThat(ToolSuspension.isSuspendedToolResult(result, suspendReference.get())).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> channelInputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("chat"), eq(1), eq("chat"), channelInputParametersArgumentCaptor.capture(),
            eq("https://example.com/resume/abc123"), isNull(), eq(actionContext));

        assertThat(channelInputParametersArgumentCaptor.getValue())
            .containsEntry("resumeId", "abc123")
            .containsEntry("resumeUrl", "https://example.com/job/resume/abc123");

        Map<String, ?> continueParameters = suspendReference.get()
            .continueParameters();

        assertThat(continueParameters.get(ToolSuspendConstants.GATED_TOOL_NAME)).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(continueParameters.get(ToolSuspendConstants.GATED_TOOL_INPUT))
            .isEqualTo("{\"channel\": \"#general\"}");
        assertThat(continueParameters.get(MetadataConstants.APPROVAL_RESUME)).isEqualTo(true);
        assertThat(continueParameters.get(MetadataConstants.APPROVAL_FORM_PARAMETERS)).isInstanceOf(Map.class);
        assertThat(continueParameters.get(MetadataConstants.APPROVAL_RAISED_AT)).isNotNull();

        verify(delegate, never()).call(any());
        verify(delegate, never()).call(any(), any());
    }

    @Test
    void testFirstCallRecordsApprovalRequiredAuditEvent() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);
        when(actionContext.getJobId()).thenReturn(42L);

        ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, toolExecutionRecorder,
            null);

        gate.call("{\"channel\": \"#general\"}", null);

        ArgumentCaptor<ToolExecutionEvent> eventCaptor = ArgumentCaptor.forClass(ToolExecutionEvent.class);

        verify(toolExecutionRecorder).record(eventCaptor.capture());

        ToolExecutionEvent toolExecutionEvent = eventCaptor.getValue();

        assertThat(toolExecutionEvent.surface()).isEqualTo(ToolExecutionSurface.AI_AGENT);
        assertThat(toolExecutionEvent.toolName()).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(toolExecutionEvent.outcome()).isEqualTo(ToolExecutionOutcome.APPROVAL_REQUIRED);
        assertThat(toolExecutionEvent.jobId()).isEqualTo(42L);
    }

    @Test
    void testEditorEnvironmentNeitherSendsTheRequestNorSuspends() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(true);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, null, null);

        String result = gate.call("{}", null);

        assertThat(result)
            .isEqualTo(ApprovalGateToolCallback.EDITOR_ENVIRONMENT_RESULT.formatted("SLACK_SEND_MESSAGE"));

        verifyNoInteractions(clusterElementDefinitionService);
        verify(actionContext, never()).suspend(any());
        verify(delegate, never()).call(any(), any());
    }

    @Test
    void testMissingResumeUrlFailsTheAgent() {
        when(actionContext.getApprovalLinks(any())).thenReturn(null);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, null, null);

        assertThatThrownBy(() -> gate.call("{}", null)).isInstanceOf(ToolSuspensionException.class);

        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testFanOutDeliversRemainingChannelsWhenOneFails() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        when(clusterElementDefinitionService.executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), anyMap(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("channel_not_found"));

        List<ClusterElement> approvalChannels = List.of(
            new ClusterElement(null, null, Map.of(), null, "slack/v1/approvalChannel", Map.of(), "slack_1"),
            new ClusterElement(
                null, null, Map.of(), null, "approvalTask/v1/approvalTask", Map.of(), "approvalTask_1"));

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, approvalChannels, Map.of(), clusterElementDefinitionService, actionContext, null, null);

        String result = gate.call("{}", null);

        assertThat(ToolSuspension.isSuspendedToolResult(result, suspendReference.get())).isTrue();

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("approvalTask"), anyInt(), anyString(), anyMap(), anyString(), any(), any());
    }

    @Test
    void testFanOutFailsTheAgentWhenEveryChannelFails() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        when(clusterElementDefinitionService.executeApprovalChannel(
            anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("channel_not_found"));

        List<ClusterElement> approvalChannels = List.of(
            new ClusterElement(null, null, Map.of(), null, "slack/v1/approvalChannel", Map.of(), "slack_1"),
            new ClusterElement(null, null, Map.of(), null, "discord/v1/approvalChannel", Map.of(), "discord_1"));

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, approvalChannels, Map.of(), clusterElementDefinitionService, actionContext, null, null);

        assertThatThrownBy(() -> gate.call("{}", null))
            .isInstanceOf(ToolSuspensionException.class)
            .hasCauseInstanceOf(IllegalStateException.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), anyMap(), anyString(), any(), any());
        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("discord"), anyInt(), anyString(), anyMap(), anyString(), any(), any());
        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testConfiguredApprovalExpiryDrivesSuspendExpiry() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, null,
            Duration.ofHours(4));

        gate.call("{}", null);

        Instant expiresAt = suspendReference.get()
            .expiresAt();

        assertThat(expiresAt).isBetween(
            Instant.now()
                .plus(Duration.ofHours(3)),
            Instant.now()
                .plus(Duration.ofHours(5)));
    }

    @Test
    void testApprovalLinksAreMintedForTheDefaultApprovalExpiry() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, List.of(), Map.of(), clusterElementDefinitionService, actionContext, null, null);

        gate.call("{}", null);

        Instant expiresAt = suspendReference.get()
            .expiresAt();

        verify(actionContext).getApprovalLinks(expiresAt);

        assertThat(expiresAt).isBetween(
            Instant.now()
                .plus(ActionContextAware.DEFAULT_APPROVAL_EXPIRY)
                .minus(Duration.ofMinutes(1)),
            Instant.now()
                .plus(ActionContextAware.DEFAULT_APPROVAL_EXPIRY));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGateNameTitlesTheApprovalRequest() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, "Outbound messages", List.of(), Map.of(), clusterElementDefinitionService, actionContext, null,
            null);

        gate.call("{}", null);

        ArgumentCaptor<Map<String, Object>> channelInputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("chat"), eq(1), eq("chat"), channelInputParametersArgumentCaptor.capture(), anyString(), isNull(),
            eq(actionContext));

        assertThat(channelInputParametersArgumentCaptor.getValue())
            .containsEntry("formTitle", "Outbound messages: approve tool call SLACK_SEND_MESSAGE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testChannelParametersCannotOverrideServerOwnedKeys() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        Map<String, Object> channelParameters = Map.of(
            "expiresAt", "2000-01-01T00:00:00Z", "formTitle", "Spoofed title", "formDescription", "Spoofed",
            "resumeId", "spoofed", "channel", "#approvals");

        List<ClusterElement> approvalChannels = List.of(
            new ClusterElement(null, null, Map.of(), null, "slack/v1/approvalChannel", channelParameters, "slack_1"));

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, approvalChannels, Map.of(), clusterElementDefinitionService, actionContext, null, null);

        gate.call("{}", null);

        ArgumentCaptor<Map<String, Object>> channelInputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), channelInputParametersArgumentCaptor.capture(), anyString(), any(),
            any());

        Instant expiresAt = suspendReference.get()
            .expiresAt();

        assertThat(channelInputParametersArgumentCaptor.getValue())
            .containsEntry("expiresAt", expiresAt.toString())
            .containsEntry("formTitle", "Approve tool call: SLACK_SEND_MESSAGE")
            .containsEntry("resumeId", "abc123")
            .containsEntry("channel", "#approvals");
        assertThat((String) channelInputParametersArgumentCaptor.getValue()
            .get("formDescription")).startsWith("The AI agent wants to call the tool 'SLACK_SEND_MESSAGE'");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMintedShortResumeIdIsPassedToChannels() {
        when(actionContext.getApprovalLinks(any())).thenReturn(
            new ApprovalLinks(
                "https://example.com", "abc123", "https://example.com/job/resume/abc123",
                "https://example.com/resume/abc123", "short-1"));
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        assertThat(deliverToTelegram(Map.of("chat_id", "7", "shortResumeId", "spoofed")))
            .containsEntry("shortResumeId", "short-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testChannelParametersCannotSupplyAShortResumeIdTheServerDidNotMint() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        assertThat(deliverToTelegram(Map.of("chat_id", "7", "shortResumeId", "spoofed")))
            .doesNotContainKey("shortResumeId");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deliverToTelegram(Map<String, Object> channelParameters) {
        List<ClusterElement> approvalChannels = List.of(
            new ClusterElement(
                null, null, Map.of(), null, "telegram/v1/approvalChannel", channelParameters, "telegram_1"));

        ApprovalGateToolCallback gate = new ApprovalGateToolCallback(
            delegate, null, approvalChannels, Map.of(), clusterElementDefinitionService, actionContext, null, null);

        gate.call("{}", null);

        ArgumentCaptor<Map<String, Object>> channelInputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("telegram"), anyInt(), anyString(), channelInputParametersArgumentCaptor.capture(), anyString(), any(),
            any());

        return channelInputParametersArgumentCaptor.getValue();
    }
}
