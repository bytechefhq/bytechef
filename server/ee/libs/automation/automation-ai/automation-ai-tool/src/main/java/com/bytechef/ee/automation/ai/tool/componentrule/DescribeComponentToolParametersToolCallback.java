/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class DescribeComponentToolParametersToolCallback implements ToolCallback {

    static final String TOOL_NAME = "describeComponentToolParameters";

    private static final String DESCRIPTION = """
        Describe the input parameters of one component tool: each parameter's name, type, whether it is required,
        and its description, plus the tool's risk level. Call this BEFORE proposing a condition, so the condition
        references parameter names that actually exist. Read-only.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string", "description": "The component that owns the tool"},
                "toolName": {"type": "string", "description": "The tool whose parameters to describe"}
            },
            "required": ["componentName", "toolName"]
        }""";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DescribeComponentToolParametersToolCallback(
        ComponentDefinitionService componentDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.componentDefinitionService = componentDefinitionService;
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
            DescribeInput input = jsonMapper.readValue(toolInput, DescribeInput.class);
            String componentName = input.componentName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required");
            }

            String toolName = input.toolName();

            if (toolName == null || toolName.isBlank()) {
                return toolError("toolName is required");
            }

            Map<String, ComponentDefinition> componentDefinitionsByName = componentDefinitionService
                .getComponentDefinitions()
                .stream()
                .collect(
                    Collectors.toMap(
                        ComponentDefinition::getName, Function.identity(),
                        DescribeComponentToolParametersToolCallback::preferHighestVersion));

            ComponentDefinition componentDefinition = componentDefinitionsByName.get(componentName);

            if (componentDefinition == null) {
                return toolError("No component named '" + componentName + "' was found");
            }

            ClusterElementDefinition clusterElementDefinition;

            try {
                clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
                    componentName, componentDefinition.getVersion(), toolName, BaseToolFunction.TOOLS.name());
            } catch (RuntimeException exception) {
                return toolError(
                    "No tool named '" + toolName + "' was found on component '" + componentName + "': "
                        + exception.getMessage());
            }

            List<? extends Property> properties = clusterElementDefinition.getProperties();

            return jsonMapper.writeValueAsString(
                Map.of(
                    "riskLevel", clusterElementDefinition.getRiskLevel()
                        .name(),
                    "parameters", properties.stream()
                        .map(DescribeComponentToolParametersToolCallback::toSummary)
                        .toList()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DescribeComponentToolParametersToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static ComponentDefinition preferHighestVersion(
        ComponentDefinition firstComponentDefinition, ComponentDefinition secondComponentDefinition) {

        return firstComponentDefinition.getVersion() >= secondComponentDefinition.getVersion()
            ? firstComponentDefinition
            : secondComponentDefinition;
    }

    private static PropertySummary toSummary(Property property) {
        Type type = property.getType();

        return new PropertySummary(
            property.getName(), type == null ? null : type.name(), property.getRequired(),
            property.getDescription());
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DescribeInput(@Nullable String componentName, @Nullable String toolName) {
    }

    public record PropertySummary(
        String name, @Nullable String type, boolean required, @Nullable String description) {
    }
}
