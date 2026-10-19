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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentUtilsApprovalGateTest {

    private final ClusterElementToolCallbacks clusterElementToolCallbacks = mock(ClusterElementToolCallbacks.class);
    private final AiAgentUtilsApprovalGate approvalGate = new AiAgentUtilsApprovalGate(
        clusterElementToolCallbacks, mock(ClusterElementDefinitionService.class), null);

    @Test
    void testGateIsATypeOfTool() {
        ClusterElementDefinition<?> clusterElementDefinition = approvalGate.clusterElementDefinition;

        assertThat(clusterElementDefinition.getName()).isEqualTo("approvalGate");

        ClusterElementDefinition.ClusterElementType clusterElementType = clusterElementDefinition.getType();

        assertThat(clusterElementType.name()).isEqualTo("TOOLS");
    }

    @Test
    void testGateDeclaresNameAndExpiryProperties() {
        ClusterElementDefinition<?> clusterElementDefinition = approvalGate.clusterElementDefinition;

        List<String> propertyNames = clusterElementDefinition.getProperties()
            .orElseThrow()
            .stream()
            .map(Property::getName)
            .toList();

        assertThat(propertyNames).containsExactly("name", "approvalExpiresIn", "approvalExpiresInUnit");
    }

    @Test
    void testRejectsTheSuspendingApprovalToolAsAChild() {
        assertThatThrownBy(() -> AiAgentUtilsApprovalGate.checkGatableChild("approval", "requestApproval"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("requestApproval");
    }

    @Test
    void testRejectsANestedGate() {
        assertThatThrownBy(() -> AiAgentUtilsApprovalGate.checkGatableChild("aiAgentUtils", "approvalGate"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("approval gate");
    }

    @Test
    void testAcceptsAnOrdinaryTool() {
        AiAgentUtilsApprovalGate.checkGatableChild("aiAgentUtils", "grepTool");
    }

    @Test
    void testWrapsEveryCallbackOfEveryGatedElementExactlyOnce() {
        when(clusterElementToolCallbacks.build(any(), any(), any()))
            .thenReturn(List.of(mock(ToolCallback.class), mock(ToolCallback.class)));

        List<ToolCallback> toolCallbacks = approvalGate.buildGatedToolCallbacks(
            "gate", gateClusterElementMap(), Map.of(), actionContext(), null);

        assertThat(toolCallbacks).hasSize(2);
        assertThat(toolCallbacks).allMatch(ApprovalGateToolCallback.class::isInstance);
    }

    @Test
    void testAGateWithNoToolsContributesNothing() {
        List<ToolCallback> toolCallbacks = approvalGate.buildGatedToolCallbacks(
            "gate", ClusterElementMap.of(Map.of()), Map.of(), actionContext(), null);

        assertThat(toolCallbacks).isEmpty();
    }

    private static ActionContextAware actionContext() {
        return mock(ActionContextAware.class);
    }

    private static ClusterElementMap gateClusterElementMap() {
        return ClusterElementMap.of(
            Map.of(
                "clusterElements",
                Map.of(
                    "tools",
                    List.of(Map.of("name", "grepTool_1", "type", "aiAgentUtils/v1/grepTool")))));
    }
}
