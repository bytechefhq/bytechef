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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.Decision;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.ToolCall;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class ClusterElementToolCallbacksTest {

    private final AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ClusterElementToolCallbacks clusterElementToolCallbacks =
        new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);

    @Test
    void testProviderFunctionElementReturnsTheProvidersCallbacks() {
        ToolCallback providerToolCallback = mock(ToolCallback.class);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParameters, connectionParameters, context) -> ToolCallbackProvider.from(providerToolCallback);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(toolCallbackProviderFunction);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/grepTool", "grepTool_1"), Map.of(), mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(providerToolCallback);

        verifyNoInteractions(aiAgentToolFacade);
    }

    @Test
    void testMultipleConnectionsProviderFunctionElementReturnsTheProvidersCallbacks() {
        ToolCallback providerToolCallback = mock(ToolCallback.class);

        MultipleConnectionsToolCallbackProviderFunction providerFunction =
            (
                inputParameters, connectionParameters, extensions, componentConnections,
                context) -> ToolCallbackProvider.from(providerToolCallback);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(providerFunction);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/taskTool", "taskTool_1"), Map.of(), mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(providerToolCallback);

        verifyNoInteractions(aiAgentToolFacade);
    }

    @Test
    void testPlainFunctionElementDelegatesToTheFacade() {
        ToolCallback facadeToolCallback = mock(ToolCallback.class);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(new Object());
        when(
            aiAgentToolFacade.getFunctionToolCallback(
                any(ClusterElement.class), any(ComponentConnection.class), any(ActionContext.class)))
                    .thenReturn(facadeToolCallback);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/createAiSkill", "createAiSkill_1"),
            Map.of("createAiSkill_1", mock(ComponentConnection.class)), mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(facadeToolCallback);
    }

    @Test
    void testInitializationFailureNamesTheClusterElement() {
        ToolCallbackProviderFunction toolCallbackProviderFunction = (
            inputParameters, connectionParameters,
            context) -> {
            throw new IllegalArgumentException("boom");
        };

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(toolCallbackProviderFunction);

        ActionContext actionContext = mock(ActionContext.class);

        assertThatThrownBy(() -> clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/grepTool", "grepTool_1"), Map.of(), actionContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll("grepTool", "aiAgentUtils", "boom");
    }

    @Test
    void testEnforcedElementWrapsEachCallbackAndCarriesTheResolvedConnectionId() {
        ComponentRuleEnforcer componentRuleEnforcer = mock(ComponentRuleEnforcer.class);
        ClusterElementToolCallbacks enforcedClusterElementToolCallbacks = new ClusterElementToolCallbacks(
            aiAgentToolFacade, clusterElementDefinitionService, List.of(componentRuleEnforcer));

        ToolCallback facadeToolCallback = mock(ToolCallback.class);

        when(facadeToolCallback.getToolDefinition()).thenReturn(
            DefaultToolDefinition.builder()
                .name("CREATE_AI_SKILL")
                .description("Create an AI skill")
                .inputSchema("{}")
                .build());

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(new Object());

        ComponentConnection componentConnection = new ComponentConnection("aiAgentUtils", 1, 42L, Map.of(), null);

        when(
            aiAgentToolFacade.getFunctionToolCallback(
                any(ClusterElement.class), any(ComponentConnection.class), any(ActionContext.class)))
                    .thenReturn(facadeToolCallback);
        when(componentRuleEnforcer.checkBeforeCall(any())).thenReturn(new Decision.Allow());

        ActionContext actionContext =
            mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        List<ToolCallback> toolCallbacks = enforcedClusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/createAiSkill", "createAiSkill_1"),
            Map.of("createAiSkill_1", componentConnection), actionContext, List.of());

        assertThat(toolCallbacks).hasSize(1);
        assertThat(toolCallbacks.getFirst()).isInstanceOf(RuleEnforcingToolCallback.class);

        toolCallbacks.getFirst()
            .call("{}", null);

        ArgumentCaptor<ToolCall> toolCallCaptor = ArgumentCaptor.forClass(ToolCall.class);

        verify(componentRuleEnforcer).checkBeforeCall(toolCallCaptor.capture());

        ToolCall toolCall = toolCallCaptor.getValue();

        assertThat(toolCall.connectionId()).isEqualTo(42L);
    }

    @Test
    void testAnAlreadyGovernedCallbackIsNotWrappedASecondTime() {
        ComponentRuleEnforcer componentRuleEnforcer = mock(ComponentRuleEnforcer.class);
        ClusterElementToolCallbacks enforcedClusterElementToolCallbacks = new ClusterElementToolCallbacks(
            aiAgentToolFacade, clusterElementDefinitionService, List.of(componentRuleEnforcer));

        RuleEnforcingToolCallback ruleEnforcingToolCallback = new RuleEnforcingToolCallback(
            mock(ToolCallback.class), clusterElement("slack/v1/sendMessage", "slack_1"), List.of(componentRuleEnforcer),
            null, mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class)), List.of(),
            Map.of(), clusterElementDefinitionService);

        DelegatingToolCallback delegatingToolCallback = new DelegatingToolCallback() {

            @Override
            public ToolCallback getDelegate() {
                return ruleEnforcingToolCallback;
            }

            @Override
            public ToolDefinition getToolDefinition() {
                return ruleEnforcingToolCallback.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                return ruleEnforcingToolCallback.call(toolInput);
            }
        };

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParameters, connectionParameters, context) -> (ToolCallbackProvider) () -> new ToolCallback[] {
                delegatingToolCallback
            };

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(toolCallbackProviderFunction);

        List<ToolCallback> toolCallbacks = enforcedClusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/approvalGate", "approvalGate_1"), Map.of(), mock(ActionContext.class),
            List.of());

        assertThat(toolCallbacks).containsExactly(delegatingToolCallback);
    }

    @Test
    void testRecordRuleApprovalResolutionForwardsTheDecisionToTheEnforcers() {
        ComponentRuleEnforcer componentRuleEnforcer = mock(ComponentRuleEnforcer.class);
        ClusterElementToolCallbacks enforcedClusterElementToolCallbacks = new ClusterElementToolCallbacks(
            aiAgentToolFacade, clusterElementDefinitionService, List.of(componentRuleEnforcer));

        enforcedClusterElementToolCallbacks.recordRuleApprovalResolution(
            List.of(7L), "slack", "sendMessage", "SLACK_SEND_MESSAGE", false, "@jane", 11L);

        ArgumentCaptor<ToolCall> toolCallCaptor = ArgumentCaptor.forClass(ToolCall.class);

        verify(componentRuleEnforcer).recordApprovalResolution(
            eq(List.of(7L)), toolCallCaptor.capture(), eq(false), eq("@jane"));

        ToolCall toolCall = toolCallCaptor.getValue();

        assertThat(toolCall.componentName()).isEqualTo("slack");
        assertThat(toolCall.toolName()).isEqualTo("sendMessage");
        assertThat(toolCall.toolCallName()).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(toolCall.jobId()).isEqualTo(11L);
        assertThat(toolCall.approvedBy()).isEqualTo("@jane");
    }

    private static ClusterElement clusterElement(String type, String workflowNodeName) {
        return new ClusterElement(null, null, Map.of(), null, type, Map.of(), workflowNodeName);
    }
}
