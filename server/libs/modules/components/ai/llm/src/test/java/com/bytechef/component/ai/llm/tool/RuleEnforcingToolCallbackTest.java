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

package com.bytechef.component.ai.llm.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.Decision;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.ToolCall;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class RuleEnforcingToolCallbackTest {

    private static final ApprovalLinks APPROVAL_LINKS = new ApprovalLinks(
        "https://example.com", "abc123", "https://example.com/job/resume/abc123", "https://example.com/resume/abc123",
        null);

    private static final ClusterElement CLUSTER_ELEMENT = new ClusterElement(
        null, null, Map.of(), null, "slack/v1/sendMessage", Map.of(), "slack_1");

    private final ToolCallback delegate = mock(ToolCallback.class);
    private final ComponentRuleEnforcer componentRuleEnforcer = mock(ComponentRuleEnforcer.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService = mock(
        ClusterElementDefinitionService.class);

    private ActionContextAware actionContext;

    @BeforeEach
    void setUp() {
        actionContext = mock(ActionContextAware.class, withSettings().extraInterfaces(ActionContext.class));

        AtomicReference<ActionContext.@Nullable Suspend> suspendReference = new AtomicReference<>();

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any());

        when(actionContext.getSuspend()).thenAnswer(invocation -> suspendReference.get());

        when(delegate.getToolDefinition()).thenReturn(
            DefaultToolDefinition.builder()
                .name("SLACK_SEND_MESSAGE")
                .description("Send a Slack message")
                .inputSchema("{}")
                .build());
    }

    @Test
    void testAllowExecutesTheDelegateAndRecordsTheOutput() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());
        when(delegate.call(anyString(), any())).thenReturn("sent");

        assertThat(newToolCallback().call("{\"channel\": \"#general\"}", null)).isEqualTo("sent");

        verify(componentRuleEnforcer).recordAfterCall(any(), eq("sent"));
    }

    @Test
    void testNoEnforcersExecutesTheDelegateWithoutEvaluation() {
        when(delegate.call(anyString(), any())).thenReturn("sent");

        RuleEnforcingToolCallback toolCallback = new RuleEnforcingToolCallback(
            delegate, CLUSTER_ELEMENT, List.of(), 3L, (ActionContext) actionContext, List.of(), Map.of(),
            clusterElementDefinitionService);

        assertThat(toolCallback.call("{}", null)).isEqualTo("sent");
    }

    @Test
    void testBlockReturnsADenialAndNeverExecutesTheDelegate() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Block("nope"));

        String result = newToolCallback().call("{}", null);

        assertThat(result)
            .contains("\"blocked\":true")
            .contains("nope");

        verify(delegate, never()).call(anyString(), any());
        verify(componentRuleEnforcer, never()).recordAfterCall(any(), any());
    }

    @Test
    void testTheToolCallCarriesTheElementNameAndTheModelChosenArguments() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());

        newToolCallback().call("{\"channel\": \"#general\"}", null);

        ToolCall toolCall = captureToolCall();

        assertThat(toolCall.componentName()).isEqualTo("slack");
        assertThat(toolCall.toolName()).isEqualTo("sendMessage");
        assertThat(toolCall.toolCallName()).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(toolCall.connectionId()).isEqualTo(3L);
        assertThat(toolCall.inputParameters()
            .get("channel")).isEqualTo("#general");
    }

    @Test
    void testMalformedToolInputYieldsEmptyInputParameters() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());

        newToolCallback().call("not json", null);

        assertThat(captureToolCall().inputParameters()).isEmpty();
    }

    @Test
    void testTheToolCallCarriesTheJobPrincipalAndPlatformType() {
        when(actionContext.getJobId()).thenReturn(11L);
        when(actionContext.getJobPrincipalId()).thenReturn(5L);
        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());

        newToolCallback().call("{}", null);

        ToolCall toolCall = captureToolCall();

        assertThat(toolCall.jobId()).isEqualTo(11L);
        assertThat(toolCall.jobPrincipalId()).isEqualTo(5L);
        assertThat(toolCall.platformType()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testAnEnforcerFailureNeverFailsTheToolCall() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenThrow(new IllegalStateException("database is down"));
        when(delegate.call(anyString(), any())).thenReturn("sent");

        assertThat(newToolCallback().call("{}", null)).isEqualTo("sent");
    }

    @Test
    void testAnAfterCallRecordingFailureNeverFailsTheToolCall() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());
        when(delegate.call(anyString(), any())).thenReturn("sent");

        doAnswer(invocation -> {
            throw new IllegalStateException("database is down");
        }).when(componentRuleEnforcer)
            .recordAfterCall(any(), any());

        assertThat(newToolCallback().call("{}", null)).isEqualTo("sent");
    }

    @Test
    void testUnwrapDoesNotStripTheRuleLayer() {
        RuleEnforcingToolCallback toolCallback = newToolCallback();

        assertThat(DelegatingToolCallback.unwrap(toolCallback)).isSameAs(toolCallback);
    }

    @Test
    void testRequireApprovalSuspendsWithTheGateContinueParametersPlusTheRuleIds() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(requireApproval(List.of(7L, 9L)));

        String result = newToolCallback().call("{\"channel\": \"#general\"}", null);

        assertThat(ToolSuspension.isSuspendedToolResult(result, actionContext.getSuspend())).isTrue();

        ArgumentCaptor<ActionContext.Suspend> suspendCaptor = ArgumentCaptor.forClass(ActionContext.Suspend.class);

        verify(actionContext, atLeastOnce()).suspend(suspendCaptor.capture());

        List<ActionContext.Suspend> suspends = suspendCaptor.getAllValues();

        ActionContext.Suspend suspend = suspends.getFirst();

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        assertThat(continueParameters)
            .containsEntry(ToolSuspendConstants.GATED_TOOL_NAME, "SLACK_SEND_MESSAGE")
            .containsEntry(ToolSuspendConstants.GATED_TOOL_INPUT, "{\"channel\": \"#general\"}")
            .containsEntry(ToolSuspendConstants.RULE_IDS, List.of(7L, 9L))
            .containsEntry(ToolSuspendConstants.RULE_COMPONENT_NAME, "slack")
            .containsEntry(ToolSuspendConstants.RULE_TOOL_NAME, "sendMessage");

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("chat"), eq(1), eq("chat"), anyMap(), eq("https://example.com/resume/abc123"), isNull(),
            eq(actionContext));
        verify(delegate, never()).call(anyString(), any());
    }

    @Test
    void testRequireApprovalInTheEditorReturnsTheEditorResultWithoutSuspending() {
        when(actionContext.isEditorEnvironment()).thenReturn(true);
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(requireApproval(List.of(7L)));

        String result = newToolCallback().call("{}", null);

        assertThat(result).isEqualTo(ToolApprovalRequests.EDITOR_ENVIRONMENT_RESULT.formatted("SLACK_SEND_MESSAGE"));

        verify(actionContext, never()).suspend(any());
        verify(delegate, never()).call(anyString(), any());
    }

    @Test
    void testRequireApprovalWithoutApprovalLinksFailsTheCall() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(requireApproval(List.of(7L)));

        assertThatThrownBy(() -> newToolCallback().call("{}", null)).isInstanceOf(ToolSuspensionException.class);

        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testRequireApprovalFailsWhenTheApprovalCannotBeDelivered() {
        when(actionContext.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(requireApproval(List.of(7L)));
        when(
            clusterElementDefinitionService.executeApprovalChannel(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), any(), any()))
                    .thenThrow(new RuntimeException("no chat listener"));

        assertThatThrownBy(() -> newToolCallback().call("{}", null)).isInstanceOf(ToolSuspensionException.class);

        verify(actionContext, never()).suspend(any());
    }

    @Test
    void testRequireApprovalDefersWhenAnotherToolAlreadySuspendedThisRound() {
        when(actionContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(requireApproval(List.of(7L)));

        assertThat(newToolCallback().call("{}", null)).isEqualTo(RuleEnforcingToolCallback.DEFERRED_RESULT);

        verify(actionContext, never()).suspend(any());
        verify(delegate, never()).call(anyString(), any());
    }

    @Test
    void testAnApprovedReExecutionPassesTheReviewerToTheEnforcer() {
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());
        when(delegate.call(anyString(), any())).thenReturn("sent");

        ToolContext toolContext = new ToolContext(Map.of(AiAgentToolContext.APPROVED_BY_KEY, "@jane"));

        newToolCallback().call("{}", toolContext);

        assertThat(captureToolCall().approvedBy()).isEqualTo("@jane");
    }

    @Test
    void testRecordApprovalResolutionReachesEveryEnforcerEvenWhenOneFails() {
        ComponentRuleEnforcer failingComponentRuleEnforcer = mock(ComponentRuleEnforcer.class);

        doAnswer(invocation -> {
            throw new IllegalStateException("database is down");
        }).when(failingComponentRuleEnforcer)
            .recordApprovalResolution(any(), any(), eq(true), any());

        ToolCall toolCall = new ToolCall(
            "slack", "sendMessage", "SLACK_SEND_MESSAGE", Map.of(), null, 11L, null, "@jane", null, null);

        RuleEnforcingToolCallback.recordApprovalResolution(
            List.of(failingComponentRuleEnforcer, componentRuleEnforcer), toolCall, List.of(7L), true, "@jane");

        verify(componentRuleEnforcer).recordApprovalResolution(List.of(7L), toolCall, true, "@jane");
    }

    private ToolCall captureToolCall() {
        ArgumentCaptor<ToolCall> toolCallCaptor = ArgumentCaptor.forClass(ToolCall.class);

        verify(componentRuleEnforcer).checkBeforeCall(toolCallCaptor.capture());

        return toolCallCaptor.getValue();
    }

    private static Decision requireApproval(List<Long> ruleIds) {
        Instant now = Instant.now();

        return new Decision.RequireApproval(
            ruleIds, "Approve tool call", "because the rule says so", now.plusSeconds(3600));
    }

    private RuleEnforcingToolCallback newToolCallback() {
        return new RuleEnforcingToolCallback(
            delegate, CLUSTER_ELEMENT, List.of(componentRuleEnforcer), 3L, (ActionContext) actionContext, List.of(),
            Map.of(), clusterElementDefinitionService);
    }
}
