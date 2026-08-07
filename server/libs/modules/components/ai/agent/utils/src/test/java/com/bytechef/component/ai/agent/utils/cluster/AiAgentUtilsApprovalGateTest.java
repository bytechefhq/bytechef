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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
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

    /**
     * Every callback a gated element contributes is wrapped exactly once. A tool element that yields two callbacks must
     * produce two gated callbacks, not one gate around a collection.
     */
    @Test
    void testWrapsEveryCallbackOfEveryGatedElementExactlyOnce() {
        when(clusterElementToolCallbacks.build(any(), any(), anyBoolean(), any()))
            .thenReturn(List.of(mock(ToolCallback.class), mock(ToolCallback.class)));

        List<ToolCallback> toolCallbacks = approvalGate.buildGatedToolCallbacks(
            gateClusterElementMap(), Map.of(), false, actionContext(), null);

        assertThat(toolCallbacks).hasSize(2);
        assertThat(toolCallbacks).allMatch(ApprovalGateToolCallback.class::isInstance);
    }

    @Test
    void testAGateWithNoToolsContributesNothing() {
        List<ToolCallback> toolCallbacks = approvalGate.buildGatedToolCallbacks(
            ClusterElementMap.of(Map.of()), Map.of(), false, actionContext(), null);

        assertThat(toolCallbacks).isEmpty();
    }

    /**
     * ApprovalGateToolCallback casts the context to ActionContextAware in its constructor, so a plain ActionContext
     * mock is not enough.
     */
    private static ActionContext actionContext() {
        return mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));
    }

    /**
     * ClusterElementMap parses raw workflow-definition maps nested under "clusterElements", not ClusterElement
     * instances, so the fixture mirrors what a saved gate node actually looks like.
     */
    private static ClusterElementMap gateClusterElementMap() {
        return ClusterElementMap.of(
            Map.of(
                "clusterElements",
                Map.of(
                    "tools",
                    List.of(Map.of("name", "grepTool_1", "type", "aiAgentUtils/v1/grepTool")))));
    }
}
