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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClusterElementMapTest {

    private static final ClusterElementType DATA_SOURCE = new ClusterElementType(
        "DATA_SOURCE", "dataSource", "Data Source");
    private static final ClusterElementType SOURCE = new ClusterElementType("SOURCE", "source", "Source");
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

    @Test
    void testFetchClusterElementRecursivelyFindsSibling() {
        Map<String, Object> sourceElement = Map.of(
            WorkflowConstants.NAME, "airtable_1",
            WorkflowConstants.TYPE, "airtable/v1/airtableSource",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS,
            Map.of(
                "source", sourceElement,
                "destination", Map.of(
                    WorkflowConstants.NAME, "csvFile_1",
                    WorkflowConstants.TYPE, "csvFile/v1/csvFileDestination",
                    WorkflowConstants.PARAMETERS, Map.of()),
                "processor", Map.of(
                    WorkflowConstants.NAME, "dataStreamProcessor_1",
                    WorkflowConstants.TYPE, "dataStreamProcessor/v1/fieldMapper",
                    WorkflowConstants.PARAMETERS, Map.of())));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        Optional<ClusterElement> clusterElementOptional = clusterElementMap.fetchClusterElementRecursively(SOURCE);

        assertThat(clusterElementOptional).isPresent();

        ClusterElement clusterElement = clusterElementOptional.get();

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("airtable_1");

        assertThat(clusterElement.getComponentName()).isEqualTo("airtable");
    }

    @Test
    void testFetchClusterElementRecursivelyFindsNestedChild() {
        Map<String, Object> dataSourceElement = Map.of(
            WorkflowConstants.NAME, "postgres_1",
            WorkflowConstants.TYPE, "postgres/v1/dataSource",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> chatMemoryElement = Map.of(
            WorkflowConstants.NAME, "jdbcChatMemory_1",
            WorkflowConstants.TYPE, "jdbcChatMemory/v1/chatMemory",
            WorkflowConstants.PARAMETERS, Map.of(),
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("dataSource", dataSourceElement));

        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("chatMemory", chatMemoryElement));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        Optional<ClusterElement> clusterElementOptional = clusterElementMap.fetchClusterElementRecursively(DATA_SOURCE);

        assertThat(clusterElementOptional).isPresent();

        ClusterElement clusterElement = clusterElementOptional.get();

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("postgres_1");

        assertThat(clusterElement.getComponentName()).isEqualTo("postgres");
    }

    @Test
    void testFetchClusterElementRecursivelyReturnsEmptyWhenAbsent() {
        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS,
            Map.of("chatMemory", Map.of(
                WorkflowConstants.NAME, "jdbcChatMemory_1",
                WorkflowConstants.TYPE, "jdbcChatMemory/v1/chatMemory",
                WorkflowConstants.PARAMETERS, Map.of())));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        assertThat(clusterElementMap.fetchClusterElementRecursively(DATA_SOURCE)).isEmpty();
        assertThat(clusterElementMap.fetchClusterElementRecursively(SOURCE)).isEmpty();
    }

    @Test
    void testFetchClusterElementRecursivelyPrefersTopLevelOverNested() {
        Map<String, Object> nestedDataSource = Map.of(
            WorkflowConstants.NAME, "nested_dataSource_1",
            WorkflowConstants.TYPE, "postgres/v1/dataSource",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> chatMemoryElement = Map.of(
            WorkflowConstants.NAME, "jdbcChatMemory_1",
            WorkflowConstants.TYPE, "jdbcChatMemory/v1/chatMemory",
            WorkflowConstants.PARAMETERS, Map.of(),
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("dataSource", nestedDataSource));

        Map<String, Object> topLevelDataSource = Map.of(
            WorkflowConstants.NAME, "top_dataSource_1",
            WorkflowConstants.TYPE, "mysql/v1/dataSource",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS,
            Map.of("dataSource", topLevelDataSource, "chatMemory", chatMemoryElement));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        Optional<ClusterElement> clusterElementOptional = clusterElementMap.fetchClusterElementRecursively(DATA_SOURCE);

        assertThat(clusterElementOptional).isPresent();

        ClusterElement clusterElement = clusterElementOptional.get();

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("top_dataSource_1");
    }

    @Test
    void testFetchClusterElementRecursivelyWalksIntoListEntries() {
        Map<String, Object> nestedDataSource = Map.of(
            WorkflowConstants.NAME, "postgres_1",
            WorkflowConstants.TYPE, "postgres/v1/dataSource",
            WorkflowConstants.PARAMETERS, Map.of());

        Map<String, Object> toolWithDataSource = Map.of(
            WorkflowConstants.NAME, "aiAgent_tool_1",
            WorkflowConstants.TYPE, "aiAgent/v1/tool",
            WorkflowConstants.PARAMETERS, Map.of(),
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("dataSource", nestedDataSource));

        Map<String, Object> extensions = Map.of(
            WorkflowExtConstants.CLUSTER_ELEMENTS, Map.of("tools", List.of(toolWithDataSource)));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        Optional<ClusterElement> clusterElementOptional = clusterElementMap.fetchClusterElementRecursively(DATA_SOURCE);

        assertThat(clusterElementOptional).isPresent();

        ClusterElement clusterElement = clusterElementOptional.get();

        assertThat(clusterElement.getWorkflowNodeName()).isEqualTo("postgres_1");
    }
}
