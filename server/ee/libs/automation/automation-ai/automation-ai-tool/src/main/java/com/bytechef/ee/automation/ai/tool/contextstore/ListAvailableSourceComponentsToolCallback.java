/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that enumerates the components usable as Context Store SOURCEs (i.e., components that
 * expose DataStream {@code ItemReader} cluster elements). The catalog is hardcoded for MVP since the platform does not
 * yet expose a ClusterElementType-typed enumeration of {@code SOURCE} elements with full metadata. Future work:
 * synthesize this list from
 * {@code ClusterElementDefinitionService.getClusterElementDefinitions(ClusterElementType.SOURCE)}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ListAvailableSourceComponentsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listAvailableSourceComponents";

    private static final String DESCRIPTION = """
        List components usable as Context Store sources — i.e., components that expose DataStream ItemReader cluster
        elements. Returns componentName, componentVersion, title, description, and the available reader cluster
        elements for each. Use this when the user asks what they can connect, or as a precursor to
        createContextStoreSource. For full property/field schema of a specific reader, follow up with
        describeSourceComponentEntities.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private static final List<Map<String, Object>> AVAILABLE_COMPONENTS = List.of(
        Map.of(
            "componentName", "airtable",
            "componentVersion", 1,
            "title", "Airtable",
            "description",
            "Airtable is a cloud-based collaboration platform that combines the features of a database with the simplicity of a spreadsheet.",
            "availableReaders", List.of(Map.of(
                "clusterElementName", "read",
                "label", "Read Records",
                "description", "Reads records from a specified table. The agent must specify base id and table id."))),
        Map.of(
            "componentName", "csvFile",
            "componentVersion", 1,
            "title", "CSV File",
            "description", "Read records from a CSV file as structured rows.",
            "availableReaders", List.of(Map.of(
                "clusterElementName", "read",
                "label", "Read CSV",
                "description", "Streams rows out of a CSV file as records."))),
        Map.of(
            "componentName", "jsonFile",
            "componentVersion", 1,
            "title", "JSON File",
            "description", "Read records from a JSON file (array of objects).",
            "availableReaders", List.of(Map.of(
                "clusterElementName", "read",
                "label", "Read JSON",
                "description", "Streams items out of a JSON array as records."))));

    private final JsonMapper jsonMapper = new JsonMapper();

    public ListAvailableSourceComponentsToolCallback() {
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            return jsonMapper.writeValueAsString(AVAILABLE_COMPONENTS);
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListAvailableSourceComponentsToolCallback.class, TOOL_NAME, exception);
        }
    }
}
