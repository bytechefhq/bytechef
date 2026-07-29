/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that returns the static metadata of a source component's reader cluster element —
 * properties (name, type, required) and labels — so the agent can construct a valid {@code createContextStoreSource}
 * call. Returns the read-only static schema only; full {@code getFields()} discovery (which fields the reader can emit
 * at runtime) requires a connection and is deferred to a future revision.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class DescribeSourceComponentEntitiesToolCallback implements ToolCallback {

    static final String TOOL_NAME = "describeSourceComponentEntities";

    private static final String DESCRIPTION = """
        Describe the static metadata of a source component's reader cluster element — properties (name, type,
        required) plus title/description. Helps the agent construct a valid createContextStoreSource call. Note: full
        runtime field discovery (the actual fields the reader will emit) requires a configured connection and is not
        returned by this tool; the agent should ask the user about expected field shapes when indexedFields cannot be
        inferred from the static property schema alone.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {"type": "string"},
                    "componentVersion": {"type": "integer"},
                    "clusterElementName": {"type": "string"}
                },
                "required": ["componentName", "componentVersion", "clusterElementName"]
            }""";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DescribeSourceComponentEntitiesToolCallback(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
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
            DescribeSourceComponentEntitiesToolInput input =
                jsonMapper.readValue(toolInput, DescribeSourceComponentEntitiesToolInput.class);

            if (input.componentName() == null || input.componentName()
                .isBlank()) {
                return toolError("componentName is required");
            }

            if (input.componentVersion() == null) {
                return toolError("componentVersion is required");
            }

            if (input.clusterElementName() == null || input.clusterElementName()
                .isBlank()) {
                return toolError("clusterElementName is required");
            }

            ClusterElementDefinition definition;

            try {
                definition = clusterElementDefinitionService.getClusterElementDefinition(
                    input.componentName(), input.componentVersion(), input.clusterElementName());
            } catch (RuntimeException exception) {
                return toolError(
                    "Cluster element '" + input.clusterElementName() + "' not found on component '"
                        + input.componentName() + "' v" + input.componentVersion() + ": " + exception.getMessage());
            }

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("componentName", definition.getComponentName());
            response.put("componentVersion", definition.getComponentVersion());
            response.put("clusterElementName", definition.getName());
            response.put("title", definition.getTitle());
            response.put("description", definition.getDescription());

            List<Map<String, Object>> propertyDescriptors = new ArrayList<>();
            List<? extends Property> properties = definition.getProperties();

            for (Property property : properties) {
                Map<String, Object> propertyDescriptor = new LinkedHashMap<>();

                propertyDescriptor.put("name", property.getName());
                propertyDescriptor.put("type", property.getType()
                    .name());
                propertyDescriptor.put("required", property.getRequired());

                if (property instanceof ValueProperty<?> valueProperty && valueProperty.getLabel() != null) {
                    propertyDescriptor.put("label", valueProperty.getLabel());
                }

                if (property.getDescription() != null) {
                    propertyDescriptor.put("description", property.getDescription());
                }

                propertyDescriptors.add(propertyDescriptor);
            }

            response.put("properties", propertyDescriptors);
            response.put(
                "fieldsHint",
                "Static metadata only. Runtime field discovery (the fields this reader will emit) requires a "
                    + "configured connection and is not exposed by this tool. Ask the user for the expected record "
                    + "shape if it cannot be inferred from the property names above.");

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DescribeSourceComponentEntitiesToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DescribeSourceComponentEntitiesToolInput(
        String componentName, Integer componentVersion, String clusterElementName) {
    }
}
