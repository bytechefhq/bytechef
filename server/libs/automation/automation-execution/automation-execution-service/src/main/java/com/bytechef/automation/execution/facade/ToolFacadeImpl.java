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

package com.bytechef.automation.execution.facade;

import com.bytechef.automation.execution.dto.ToolDTO;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

/**
 * @author Matija Petanjek
 */
@Service("com.bytechef.automation.execution.facade.ToolFacade")
public class ToolFacadeImpl implements ToolFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final McpComponentService mcpComponentService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public ToolFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade, McpComponentService mcpComponentService,
        McpToolService mcpToolService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.mcpComponentService = mcpComponentService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(ToolDTO toolDTO) {
        FunctionToolCallback.Builder<Map<String, Object>, Object> functionToolCallbackBuilder =
            FunctionToolCallback
                .builder(toolDTO.name(), getToolCallbackFunction(toolDTO.name(), toolDTO.connectionId()))
                .inputType(Map.class)
                .inputSchema(toolDTO.parameters())
                .description(toolDTO.description());

        return functionToolCallbackBuilder.build();
    }

    @Override
    public List<ToolCallback> getToolCallbacks() {
        Map<String, ToolCallback> toolCallbackMap = new HashMap<>();

        List<ToolDTO> toolDTOs = getTools();

        for (ToolDTO toolDTO : toolDTOs) {
            toolCallbackMap.put(toolDTO.name(), getFunctionToolCallback(toolDTO));
        }

        return new ArrayList<>(toolCallbackMap.values());
    }

    @Override
    public List<ToolDTO> getTools() {
        return mcpToolService.getMcpTools()
            .stream()
            .map(this::toTool)
            .toList();
    }

    @Override
    public ToolDTO toTool(McpTool mcpTool) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                mcpComponent.getComponentName(), mcpTool.getName());

        return new ToolDTO(
            getToolName(mcpComponent.getComponentName(), clusterElementDefinition.getName()),
            clusterElementDefinition.getDescription(),
            JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties()),
            mcpComponent.getConnectionId());
    }

    private String getToolName(String componentName, String clusterElementName) {
        StringBuilder sb = new StringBuilder();

        sb.append(componentName.toUpperCase());
        sb.append("_");

        sb.append(Character.toUpperCase(clusterElementName.charAt(0)));

        for (int i = 1; i < clusterElementName.length(); i++) {
            char c = clusterElementName.charAt(i);

            if (Character.isUpperCase(c)) {
                sb.append('_')
                    .append(c);
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }

        return sb.toString();
    }

    private static ComponentClusterElementNameResult getComponentClusterElementNames(String toolName) {
        String lowerCase = toolName.toLowerCase();

        String[] parts = lowerCase.split("_");

        String componentName = parts[0];
        StringBuilder clusterElementName = new StringBuilder();

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];

            if (!part.isEmpty()) {
                if (clusterElementName.isEmpty()) {
                    clusterElementName.append(part);
                } else {
                    clusterElementName.append(Character.toUpperCase(part.charAt(0)));
                    clusterElementName.append(part.substring(1));
                }
            }
        }

        return new ComponentClusterElementNameResult(componentName, clusterElementName.toString());
    }

    private Function<Map<String, Object>, Object> getToolCallbackFunction(String toolName, Long connectionId) {
        return inputParameters -> {
            ComponentClusterElementNameResult result = getComponentClusterElementNames(toolName);

            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    result.componentName(), result.clusterElementName());

            String componentName = clusterElementDefinition.getComponentName();

            return clusterElementDefinitionFacade.executeTool(
                componentName, clusterElementDefinition.getComponentVersion(), clusterElementDefinition.getName(),
                inputParameters, connectionId);
        };
    }

    private record ComponentClusterElementNameResult(String componentName, String clusterElementName) {
    }
}
