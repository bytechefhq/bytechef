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

package com.bytechef.ai.mcp.tool;

import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class FlowTools {

    private static final Logger logger = LoggerFactory.getLogger(FlowTools.class);

    // Error message constants
    private static final String FAILED_TO_LIST_FLOWS = "Failed to list flows";
    private static final String FAILED_TO_GET_FLOW = "Failed to get flow";
    private static final String FAILED_TO_SEARCH_FLOWS = "Failed to search flows";

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    private static final String DEFAULT_FLOW_DEFINITION = """
        {
            "label": "Flow Task",
            "name": "{flowName}_1",
            "type": "{flowName}/v{flowVersion}",
            "parameters": {}
        }
        """;

    @SuppressFBWarnings("EI")
    public FlowTools(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    // Helper methods
    private TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);
    }

    @Tool(
        description = "List all flows in the project. Returns a list of flows with their basic information including name and description")
    public List<FlowMinimalInfo> listFlows() {
        try {
            List<TaskDispatcherDefinition> taskDispatcherDefinitions =
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} flows", taskDispatcherDefinitions.size());
            }

            return taskDispatcherDefinitions.stream()
                .map(flow -> new FlowMinimalInfo(
                    flow.getName(),
                    flow.getDescription(),
                    flow.getVersion()))
                .toList();
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_FLOWS, e);
            throw ToolUtils.createOperationException(FAILED_TO_LIST_FLOWS, e);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific flow. Returns detailed flow information including: name, description, and properties")
    public FlowInfo getFlow(
        @ToolParam(description = "The name of the flow to retrieve") String name,
        @ToolParam(description = "The version of the flow (optional)") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition = getTaskDispatcherDefinition(name, version);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved flow {}", name);
            }

            String type = taskDispatcherDefinition.getName() + "/v" + taskDispatcherDefinition.getVersion();
            String properties = ToolUtils.generateParametersJson(taskDispatcherDefinition.getProperties());
            String taskProperties = ToolUtils.generateParametersJson(taskDispatcherDefinition.getTaskProperties());

            String outputPropertiesJson = null;
            OutputResponse outputResponse = taskDispatcherDefinition.getOutputResponse();

            if (taskDispatcherDefinition.isOutputDefined() && outputResponse != null
                && outputResponse.outputSchema() != null) {
                outputPropertiesJson = ToolUtils.generateOutputPropertiesJson(outputResponse.outputSchema());
            }

            return new FlowInfo(
                taskDispatcherDefinition.getName(),
                taskDispatcherDefinition.getDescription(),
                taskDispatcherDefinition.getTitle(),
                type,
                properties,
                taskProperties,
                outputPropertiesJson);
        } catch (Exception e) {
            logger.error("Failed to get flow {}", name, e);
            throw ToolUtils.createOperationException(FAILED_TO_GET_FLOW, e);
        }
    }

    @Tool(
        description = "Full-text search across flows. Returns a list of flows matching the search query in name or description.")
    public List<FlowMinimalInfo> searchFlows(
        @ToolParam(description = "The search query to match against flow names and descriptions") String query) {

        try {
            List<TaskDispatcherDefinition> taskDispatcherDefinitions =
                taskDispatcherDefinitionService.getTaskDispatcherDefinitions();
            String lowerQuery = query.toLowerCase()
                .trim();

            List<FlowMinimalInfo> matchingFlows = taskDispatcherDefinitions.stream()
                .filter(flow -> ToolUtils.matchesQuery(
                    flow.getName(),
                    flow.getDescription(),
                    lowerQuery))
                .map(flow -> new FlowMinimalInfo(
                    flow.getName(),
                    flow.getDescription(),
                    flow.getVersion()))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} flows matching query '{}'", matchingFlows.size(), query);
            }

            return matchingFlows;
        } catch (Exception e) {
            logger.error("Failed to search flows with query '{}'", query, e);
            throw ToolUtils.createOperationException(FAILED_TO_SEARCH_FLOWS, e);
        }
    }

    @Tool(
        description = "Get all properties of a specific flow. Returns a hierarchical list of properties including nested properties")
    public List<ToolUtils.PropertyInfo> getFlowProperties(
        @ToolParam(description = "The name of the flow to retrieve properties for") String name,
        @ToolParam(description = "The version of the flow (optional)") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition = getTaskDispatcherDefinition(name, version);

            List<? extends BaseProperty> properties = taskDispatcherDefinition.getProperties();
            List<? extends BaseProperty> taskProperties = taskDispatcherDefinition.getTaskProperties();

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved {} properties for flow '{}'", properties.size(), name);
                logger.debug("Retrieved {} task properties for flow '{}'", taskProperties.size(), name);
            }

            List<ToolUtils.PropertyInfo> propertyInfos =
                new ArrayList<>(ToolUtils.convertToPropertyInfoList(properties));
            propertyInfos.addAll(ToolUtils.convertToPropertyInfoList(taskProperties));

            return propertyInfos;
        } catch (Exception e) {
            logger.error("Failed to get properties for flow '{}'", name, e);
            throw ToolUtils.createOperationException("Failed to get properties", e);
        }
    }

    @Tool(
        description = "Get the flow definition template for a specific flow. Returns a JSON template that can be used to configure the flow in workflows")
    public String getFlowDefinition(
        @ToolParam(description = "The name of the flow to generate definition for") String name,
        @ToolParam(description = "The version of the flow (optional)") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition = getTaskDispatcherDefinition(name, version);

            // Extract properties for the flow definition
            String parametersJson = ToolUtils.generateParametersJson(taskDispatcherDefinition.getProperties());

            // Generate the flow definition using the template with actual parameters
            String flowDefinition = DEFAULT_FLOW_DEFINITION
                .replace("{flowName}", name)
                .replace("{flowVersion}", String.valueOf(taskDispatcherDefinition.getVersion()))
                .replace("\"parameters\": {}", "\"parameters\": " + parametersJson);

            if (logger.isDebugEnabled()) {
                logger.debug("Generated flow definition for {}", name);
            }

            return flowDefinition;
        } catch (Exception e) {
            logger.error("Failed to generate flow definition for '{}'", name, e);
            throw ToolUtils.createOperationException("Failed to generate flow definition", e);
        }
    }

    @Tool(
        description = "Get the output property of a specific flow. Returns the structure of the output property")
    public ToolUtils.PropertyInfo getFlowOutput(
        @ToolParam(description = "The name of the flow to retrieve output properties for") String name,
        @ToolParam(description = "The version of the flow (optional)") Integer version) {

        try {
            TaskDispatcherDefinition taskDispatcherDefinition = getTaskDispatcherDefinition(name, version);

            OutputResponse outputResponse = taskDispatcherDefinition.isOutputDefined()
                ? taskDispatcherDefinition.getOutputResponse()
                : null;

            if (outputResponse == null || outputResponse.outputSchema() == null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved output properties for flow '{}'", name);
            }

            return ToolUtils.convertToPropertyInfo(outputResponse.outputSchema());
        } catch (Exception e) {
            logger.error("Failed to get output properties for flow '{}'", name, e);
            throw ToolUtils.createOperationException("Failed to get output properties", e);
        }
    }

    /**
     * Minimal flow information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record FlowMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the flow") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the flow") String description,
        @JsonProperty("version") @JsonPropertyDescription("The version of the flow") int version) {
    }

    /**
     * Comprehensive flow information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record FlowInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the flow") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the flow") String description,
        @JsonProperty("title") @JsonPropertyDescription("The title of the flow") String title,
        @JsonProperty("type") @JsonPropertyDescription("The type of the flow in format {name}/v{version}") String type,
        @JsonProperty("properties") @JsonPropertyDescription("The properties of the flow as JSON string") String properties,
        @JsonProperty("taskProperties") @JsonPropertyDescription("The task properties of the flow as JSON string") String taskProperties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the flow as JSON string") String outputProperties) {
    }
}
