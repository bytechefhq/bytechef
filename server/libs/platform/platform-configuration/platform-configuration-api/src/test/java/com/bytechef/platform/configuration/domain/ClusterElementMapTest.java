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

package com.bytechef.platform.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClusterElementMapTest {

    private static final ClusterElementType TOOLS = new ClusterElementType("TOOLS", "tools", "Tools", true, false);

    @Test
    void testGetClusterElementResolvesDirectChild() {
        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS,
            Map.of("tools", List.of(
                Map.of(
                    WorkflowConstants.NAME, "hubspot_1",
                    WorkflowConstants.TYPE, "hubspot/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of()))));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(TOOLS, "hubspot_1");

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("hubspot_1");
        assertThat(clusterElement.getComponentName()).isEqualTo("hubspot");
    }

    @Test
    void testGetClusterElementResolvesNestedChild() {
        Map<String, Object> hubspotTool = Map.of(
            WorkflowConstants.NAME, "hubspot_1",
            WorkflowConstants.TYPE, "hubspot/v1/createContact",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> aiAgentTool = Map.of(
            WorkflowConstants.NAME, "aiAgent_tool_1",
            WorkflowConstants.TYPE, "aiAgent/v1/aiAgentTool",
            WorkflowConstants.PARAMETERS, Map.of(),
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("tools", List.of(hubspotTool)));

        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("tools", List.of(aiAgentTool)));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(TOOLS, "hubspot_1");

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("hubspot_1");
        assertThat(clusterElement.getComponentName()).isEqualTo("hubspot");
    }

    @Test
    void testGetClusterElementThrowsWhenMissing() {
        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS,
            Map.of("tools", List.of(
                Map.of(
                    WorkflowConstants.NAME, "hubspot_1",
                    WorkflowConstants.TYPE, "hubspot/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of()))));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        assertThatThrownBy(() -> clusterElementMap.getClusterElement(TOOLS, "missing_1"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
