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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
class ClusterElementToolCallbacksTest {

    private final AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ClusterElementToolCallbacks clusterElementToolCallbacks =
        new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);

    /**
     * The branch AiAgentUtilsTaskTool omitted. Ten of the thirteen aiAgentUtils tool elements are
     * ToolCallbackProviderFunction, so without this branch a subagent received a facade-built FunctionToolCallback --
     * whose input schema comes from the element's parameters rather than the provider -- instead of the real tools.
     */
    @Test
    void testProviderFunctionElementReturnsTheProvidersCallbacks() {
        ToolCallback providerToolCallback = mock(ToolCallback.class);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParameters, connectionParameters, context) -> ToolCallbackProvider.from(providerToolCallback);

        when(clusterElementDefinitionService.getClusterElement(anyString(), anyInt(), anyString()))
            .thenReturn(toolCallbackProviderFunction);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/grepTool", "grepTool_1"), Map.of(), false, mock(ActionContext.class));

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
            clusterElement("aiAgentUtils/v1/taskTool", "taskTool_1"), Map.of(), false, mock(ActionContext.class));

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
                any(ClusterElement.class), any(ComponentConnection.class), anyBoolean()))
                    .thenReturn(facadeToolCallback);

        List<ToolCallback> toolCallbacks = clusterElementToolCallbacks.build(
            clusterElement("aiAgentUtils/v1/createAiSkill", "createAiSkill_1"),
            Map.of("createAiSkill_1", mock(ComponentConnection.class)), false, mock(ActionContext.class));

        assertThat(toolCallbacks).containsExactly(facadeToolCallback);
    }

    /**
     * A provider that throws must fail with a message naming the element, not with the bare IllegalStateException the
     * task tool's copy produced.
     */
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

        try {
            clusterElementToolCallbacks.build(
                clusterElement("aiAgentUtils/v1/grepTool", "grepTool_1"), Map.of(), false, actionContext);

            assertThat(false)
                .as("expected an IllegalStateException")
                .isTrue();
        } catch (IllegalStateException illegalStateException) {
            assertThat(illegalStateException.getMessage()).contains("grepTool", "aiAgentUtils", "boom");
        }
    }

    private static ClusterElement clusterElement(String type, String workflowNodeName) {
        return new ClusterElement(null, null, Map.of(), null, type, Map.of(), workflowNodeName);
    }
}
