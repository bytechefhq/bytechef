/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Property.Type;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DescribeSourceComponentEntitiesToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        DescribeSourceComponentEntitiesToolCallback callback = new DescribeSourceComponentEntitiesToolCallback(
            mock(ClusterElementDefinitionService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(DescribeSourceComponentEntitiesToolCallback.TOOL_NAME);
        assertThat(definition.inputSchema()).contains("componentName");
    }

    @Test
    void testCallReturnsClusterElementDescription() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);

        ClusterElementDefinition definition = mock(ClusterElementDefinition.class);

        when(definition.getComponentName()).thenReturn("airtable");
        when(definition.getComponentVersion()).thenReturn(1);
        when(definition.getName()).thenReturn("read");
        when(definition.getTitle()).thenReturn("Read Records");
        when(definition.getDescription()).thenReturn("Reads rows.");

        Property property = mock(Property.class);

        when(property.getName()).thenReturn("baseId");
        when(property.getType()).thenReturn(Type.STRING);
        when(property.getRequired()).thenReturn(true);
        when(property.getDescription()).thenReturn("The Airtable base id.");

        when(definition.getProperties()).thenAnswer(invocation -> List.of(property));

        when(service.getClusterElementDefinition(eq("airtable"), eq(1), eq("read"))).thenReturn(definition);

        DescribeSourceComponentEntitiesToolCallback callback =
            new DescribeSourceComponentEntitiesToolCallback(service);

        String result = callback.call(
            "{\"componentName\":\"airtable\",\"componentVersion\":1,\"clusterElementName\":\"read\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("componentName")
            .asText()).isEqualTo("airtable");
        assertThat(node.get("clusterElementName")
            .asText()).isEqualTo("read");
        assertThat(node.get("properties")
            .isArray()).isTrue();
        assertThat(node.get("properties")).hasSize(1);
        assertThat(node.get("properties")
            .get(0)
            .get("name")
            .asText()).isEqualTo("baseId");
        assertThat(node.get("properties")
            .get(0)
            .get("required")
            .asBoolean()).isTrue();
        assertThat(node.has("fieldsHint")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenClusterElementMissing() throws Exception {
        ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);

        when(service.getClusterElementDefinition(eq("bogus"), eq(1), eq("read")))
            .thenThrow(new IllegalArgumentException("not found"));

        DescribeSourceComponentEntitiesToolCallback callback =
            new DescribeSourceComponentEntitiesToolCallback(service);

        String result = callback.call(
            "{\"componentName\":\"bogus\",\"componentVersion\":1,\"clusterElementName\":\"read\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not found");
    }

    @Test
    void testCallRejectsMissingFields() throws Exception {
        DescribeSourceComponentEntitiesToolCallback callback = new DescribeSourceComponentEntitiesToolCallback(
            mock(ClusterElementDefinitionService.class));

        String result = callback.call("{\"componentName\":\"airtable\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
