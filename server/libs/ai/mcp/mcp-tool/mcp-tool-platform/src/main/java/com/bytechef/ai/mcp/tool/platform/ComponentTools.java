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

package com.bytechef.ai.mcp.tool.platform;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class ComponentTools {

    private static final Logger logger = LoggerFactory.getLogger(ComponentTools.class);

    private static final String TRIGGER_NOT_FOUND = "Trigger '%s' not found in component '%s'";
    private static final String ACTION_NOT_FOUND = "Action '%s' not found in component '%s'";
    private static final String OPERATION_NOT_FOUND = "Operation '%s' not found in component '%s'";
    private static final String FAILED_TO_LIST_COMPONENTS = "Failed to list components";
    private static final String FAILED_TO_GET_COMPONENT = "Failed to get component";
    private static final String FAILED_TO_SEARCH_COMPONENTS = "Failed to search components";
    private static final String FAILED_TO_LIST_TRIGGERS = "Failed to list triggers";
    private static final String FAILED_TO_GET_TRIGGER = "Failed to get trigger";
    private static final String FAILED_TO_SEARCH_TRIGGERS = "Failed to search triggers";
    private static final String FAILED_TO_GENERATE_TRIGGER_DEFINITION = "Failed to generate trigger definition";
    private static final String FAILED_TO_LIST_ACTIONS = "Failed to list actions";
    private static final String FAILED_TO_GET_ACTION = "Failed to get action";
    private static final String FAILED_TO_SEARCH_ACTIONS = "Failed to search actions";
    private static final String FAILED_TO_GENERATE_ACTION_DEFINITION = "Failed to generate action definition";
    private static final String FAILED_TO_GET_OUTPUT_PROPERTIES = "Failed to get output properties";
    private static final String FAILED_TO_GET_PROPERTIES = "Failed to get properties";

    private final ComponentDefinitionService componentDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final ConnectionService connectionService;

    private static final String DEFAULT_TRIGGER_DEFINITION = """
        {
            "label": "Function of the Trigger",
            "name": "{triggerName}_1",
            "type": "{componentName}/v{componentVersion}/{triggerName}",
            "parameters": {}
        }
        """;

    private static final String DEFAULT_ACTION_DEFINITION = """
        {
            "label": "Function of the Action",
            "name": "{actionName}_1",
            "type": "{componentName}/v{componentVersion}/{actionName}",
            "parameters": {}
        }
        """;

    @SuppressFBWarnings("EI")
    public ComponentTools(ComponentDefinitionService componentDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionFacade triggerDefinitionFacade,
        ConnectionService connectionService) {
        this.componentDefinitionService = componentDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.connectionService = connectionService;
    }

    // Helper methods
    @Tool(
        description = "Get comprehensive information about a specific component. Returns detailed project information including: name, description, triggers and actions")
    public ComponentInfo getComponent(
        @ToolParam(description = "The name of the component to retrieve in camel case") String componentName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved component {}", componentName);
            }

            return new ComponentInfo(
                componentDefinition.getName(),
                componentDefinition.getDescription(),
                componentDefinition.getComponentCategories()
                    .stream()
                    .map(ComponentCategory::getName)
                    .toList(),
                componentDefinition.getTriggers()
                    .stream()
                    .map(TriggerDefinition::getName)
                    .toList(),
                componentDefinition.getActions()
                    .stream()
                    .map(ActionDefinition::getName)
                    .toList());
        } catch (Exception e) {
            logger.error("Failed to get component {}", componentName, e);

            throw new RuntimeException(FAILED_TO_GET_COMPONENT, e);
        }
    }

    @Tool(
        description = "Get detailed information about a specific trigger. Returns comprehensive trigger information including properties and configuration")
    public TriggerDetailedInfo getTrigger(
        @ToolParam(
            description = "The name of the component that contains the trigger in camel case") String componentName,
        @ToolParam(description = "The name of the trigger to retrieve in camel case") String triggerName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            return componentDefinition.getTriggers()
                .stream()
                .filter(trigger -> trigger.getName()
                    .equals(triggerName))
                .findFirst()
                .map(trigger -> {
                    String outputPropertiesJson = null;
                    OutputResponse outputResponse = trigger.getOutputResponse();

                    if (outputResponse != null && outputResponse.outputSchema() != null) {
                        outputPropertiesJson = ToolUtils.generateOutputPropertiesJson(outputResponse.outputSchema());
                    }

                    com.bytechef.component.definition.TriggerDefinition.TriggerType type = trigger.getType();

                    return new TriggerDetailedInfo(
                        trigger.getName(), trigger.getTitle(), trigger.getDescription(), componentDefinition.getName(),
                        type.name(), ToolUtils.generateParametersJson(trigger.getProperties()), outputPropertiesJson);
                })
                .orElseThrow(() -> new RuntimeException(String.format(TRIGGER_NOT_FOUND, triggerName, componentName)));

        } catch (Exception e) {
            logger.error("Failed to get trigger '{}' from component '{}'", triggerName, componentName, e);

            throw new RuntimeException(FAILED_TO_GET_TRIGGER, e);
        }
    }

    @Tool(
        description = "Get the trigger definition template for the specific trigger. Returns a structured JSON with template and conditional parameters")
    public String getTriggerDefinition(
        @ToolParam(
            description = "The name of the component that contains the trigger in camel case") String componentName,
        @ToolParam(description = "The name of the trigger to generate definition for in camel case") String triggerName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            // Get the actual trigger to extract its properties
            var trigger = componentDefinition.getTriggers()
                .stream()
                .filter(triggerDefinition -> {
                    String name = triggerDefinition.getName();

                    return name.equals(triggerName);
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(TRIGGER_NOT_FOUND, triggerName, componentName)));
            String parametersJson = ToolUtils.generateParametersJson(trigger.getProperties());

            // Generate the trigger definition using the template with actual parameters
            String triggerDefinition = DEFAULT_TRIGGER_DEFINITION
                .replace("{triggerName}", triggerName)
                .replace("{componentName}", componentName)
                .replace("{componentVersion}", String.valueOf(componentDefinition.getVersion()))
                .replace("\"parameters\": {}", "\"parameters\": " + parametersJson);

            if (logger.isDebugEnabled()) {
                logger.debug("Generated trigger definition for {}:{}", componentName, triggerName);
            }

            return triggerDefinition;
        } catch (Exception e) {
            logger.error("Failed to generate trigger definition for '{}:{}'", componentName, triggerName, e);

            throw new RuntimeException(FAILED_TO_GENERATE_TRIGGER_DEFINITION, e);
        }
    }

    @Tool(
        description = "List all actions from all components. Returns a list of actions with their basic information including name, description and component")
    public List<ActionMinimalInfo> listActions() {
        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            List<ActionMinimalInfo> actions = componentDefinitions.stream()
                .flatMap(component -> component.getActions()
                    .stream()
                    .map(action -> new ActionMinimalInfo(
                        action.getName(), action.getDescription(), component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} actions across all components", actions.size());
            }

            return actions;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_ACTIONS, e);

            throw new RuntimeException(FAILED_TO_LIST_ACTIONS, e);
        }
    }

    @Tool(
        description = "List all components in a project. Returns a list of components with their basic information including name and description")
    public List<ComponentMinimalInfo> listComponents() {
        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} components", componentDefinitions.size());
            }

            return componentDefinitions.stream()
                .map(component -> new ComponentMinimalInfo(
                    component.getName(), component.getDescription(), component.getVersion()))
                .toList();
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_COMPONENTS, e);

            throw new RuntimeException(FAILED_TO_LIST_COMPONENTS, e);
        }
    }

    @Tool(
        description = "List all triggers from all components. Returns a list of triggers with their basic information including name, description, component, and type")
    public List<TriggerMinimalInfo> listTriggers() {
        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            List<TriggerMinimalInfo> triggers = componentDefinitions.stream()
                .flatMap(component -> component.getTriggers()
                    .stream()
                    .map(trigger -> new TriggerMinimalInfo(
                        trigger.getName(), trigger.getDescription(), component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} triggers across all components", triggers.size());
            }

            return triggers;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_TRIGGERS, e);

            throw new RuntimeException(FAILED_TO_LIST_TRIGGERS, e);
        }
    }

    @Tool(
        description = "Full-text search across components. Returns a list of components matching the search query in name or description.")
    public List<ComponentMinimalInfo> searchComponents(
        @ToolParam(description = "The search query to match against component names and descriptions") String query) {

        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();
            String lowerQuery = query.toLowerCase()
                .trim();

            List<ComponentMinimalInfo> matchingComponents = componentDefinitions.stream()
                .filter(component -> ToolUtils.matchesQuery(
                    component.getName(), component.getDescription(), null, null, lowerQuery))
                .map(component -> new ComponentMinimalInfo(
                    component.getName(), component.getDescription(), component.getVersion()))
                .sorted((component1, component2) -> ToolUtils.compareTasks(
                    component1.name(), component1.description(), null, component2.name(), component2.description(),
                    null, lowerQuery))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} components matching query '{}'", matchingComponents.size(), query);
            }

            return matchingComponents;
        } catch (Exception e) {
            logger.error("Failed to search components with query '{}'", query, e);

            throw new RuntimeException(FAILED_TO_SEARCH_COMPONENTS, e);
        }
    }

    @Tool(
        description = "Search triggers across all components. Returns a list of triggers matching the search query in name or description")
    public List<TriggerMinimalInfo> searchTriggers(
        @ToolParam(description = "The search query to match against trigger names and descriptions") String query) {

        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            String lowerQuery = query.toLowerCase()
                .trim();

            List<TriggerMinimalInfo> matchingTriggers = componentDefinitions.stream()
                .flatMap(component -> component.getTriggers()
                    .stream()
                    .filter(trigger -> ToolUtils.matchesQuery(
                        trigger.getName(), trigger.getDescription(), component.getName(), component.getDescription(),
                        lowerQuery))
                    .map(trigger -> new TriggerMinimalInfo(
                        trigger.getName(), trigger.getDescription(), component.getName())))
                .sorted((trigger1, trigger2) -> ToolUtils.compareTasks(
                    trigger1.name(), trigger1.description(), trigger1.componentName(), trigger2.name(),
                    trigger2.description(), trigger2.componentName(), lowerQuery))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} triggers matching query '{}'", matchingTriggers.size(), query);
            }

            return matchingTriggers;
        } catch (Exception e) {
            logger.error("Failed to search triggers with query '{}'", query, e);

            throw new RuntimeException(FAILED_TO_SEARCH_TRIGGERS, e);
        }
    }

    @Tool(
        description = "Get detailed information about a specific action. Returns comprehensive action information including properties and configuration")
    public ActionDetailedInfo getAction(
        @ToolParam(
            description = "The name of the component that contains the action in camel case") String componentName,
        @ToolParam(description = "The name of the action to retrieve in camel case") String actionName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            return componentDefinition.getActions()
                .stream()
                .filter(actionDefinition -> {
                    String name = actionDefinition.getName();

                    return name.equals(actionName);
                })
                .findFirst()
                .map(action -> {
                    String outputPropertiesJson = null;
                    OutputResponse outputResponse = action.getOutputResponse();

                    if (outputResponse != null && outputResponse.outputSchema() != null) {
                        outputPropertiesJson = ToolUtils.generateOutputPropertiesJson(outputResponse.outputSchema());
                    }

                    return new ActionDetailedInfo(
                        action.getName(), action.getTitle(), action.getDescription(), componentDefinition.getName(),
                        ToolUtils.generateParametersJson(action.getProperties()), outputPropertiesJson);
                })
                .orElseThrow(() -> new RuntimeException(String.format(ACTION_NOT_FOUND, actionName, componentName)));

        } catch (Exception e) {
            logger.error("Failed to get action '{}' from component '{}'", actionName, componentName, e);

            throw new RuntimeException(FAILED_TO_GET_ACTION, e);
        }
    }

    @Tool(
        description = "Search actions across all components. Returns a list of actions matching the search query in name or description")
    public List<ActionMinimalInfo> searchActions(
        @ToolParam(description = "The search query to match against action names and descriptions") String query) {

        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            String lowerQuery = StringUtils.trim(query.toLowerCase());

            List<ActionMinimalInfo> matchingActions = componentDefinitions.stream()
                .flatMap(component -> component.getActions()
                    .stream()
                    .filter(actionDefinition -> ToolUtils.matchesQuery(
                        actionDefinition.getName(), actionDefinition.getDescription(), component.getName(),
                        component.getDescription(), lowerQuery))
                    .map(actionDefinition -> new ActionMinimalInfo(
                        actionDefinition.getName(), actionDefinition.getDescription(), component.getName())))
                .sorted((action1, action2) -> ToolUtils.compareTasks(
                    action1.name(), action1.description(), action1.componentName(), action2.name(),
                    action2.description(), action2.componentName(), lowerQuery))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} actions matching query '{}'", matchingActions.size(), query);
            }

            return matchingActions;
        } catch (Exception e) {
            logger.error("Failed to search actions with query '{}'", query, e);

            throw new RuntimeException(FAILED_TO_SEARCH_ACTIONS, e);
        }
    }

    @Tool(
        description = "Get the action definition template for the specific action. Returns a structured JSON with template and conditional parameters")
    public String getActionDefinition(
        @ToolParam(
            description = "The name of the component that contains the action in camel case") String componentName,
        @ToolParam(description = "The name of the action to generate definition for in camel case") String actionName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            // Get the actual action to extract its properties
            var action = componentDefinition.getActions()
                .stream()
                .filter(actionDefinition -> Objects.equals(actionDefinition.getName(), actionName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(ACTION_NOT_FOUND, actionName, componentName)));
            String parametersJson = ToolUtils.generateParametersJson(action.getProperties());

            // Generate the action definition using the template with actual parameters
            String actionDefinition = DEFAULT_ACTION_DEFINITION
                .replace("{actionName}", actionName)
                .replace("{componentName}", componentName)
                .replace("{componentVersion}", String.valueOf(componentDefinition.getVersion()))
                .replace("\"parameters\": {}", "\"parameters\": " + parametersJson);

            if (logger.isDebugEnabled()) {
                logger.debug("Generated action definition for {}:{}", componentName, actionName);
            }

            return actionDefinition;
        } catch (Exception e) {
            logger.error("Failed to generate action definition for '{}:{}'", componentName, actionName, e);

            throw new RuntimeException(FAILED_TO_GENERATE_ACTION_DEFINITION, e);
        }
    }

    @Tool(
        description = "Get the output property of a specific trigger or action. Returns the structure of the output property")
    public PropertyInfo getOutputProperty(
        @ToolParam(
            description = "The name of the component that contains the trigger or action in camel case") String componentName,
        @ToolParam(
            description = "The name of the trigger or action to retrieve output properties for in camel case") String operationName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            OutputResponse outputResponse = null;

            // First, try to find in triggers
            var triggerOptional = componentDefinition.getTriggers()
                .stream()
                .filter(trigger -> {
                    String name = trigger.getName();

                    return name.equals(operationName);
                })
                .findFirst();

            if (triggerOptional.isPresent()) {
                var trigger = triggerOptional.get();

                if (trigger.isOutputDefined()) {
                    if (trigger.isOutputSchemaDefined()) {
                        outputResponse = trigger.getOutputResponse();
                    } else if (trigger.isOutputFunctionDefined()) {
                        outputResponse = triggerDefinitionFacade.executeOutput(
                            componentDefinition.getName(), componentDefinition.getVersion(), trigger.getName(),
                            Map.of(), null);
                    }

                    if (outputResponse == null) {
                        try {
                            var output = triggerDefinitionFacade.executeTrigger(
                                componentDefinition.getName(), componentDefinition.getVersion(), trigger.getName(),
                                null, null, null, null, null, null, true, null);
                            if (output != null) {
                                outputResponse = SchemaUtils.toOutput(
                                    output, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);
                            }
                        } catch (Exception e) {
                            throw new Exception("Please make a " + componentDefinition.getName() + " connector");
                        }
                    }
                }
            } else {
                // If not found in triggers, try actions
                var actionOptional = componentDefinition.getActions()
                    .stream()
                    .filter(action -> action.getName()
                        .equals(operationName))
                    .findFirst();

                if (actionOptional.isPresent()) {
                    var actionDefinition = actionOptional.get();

                    if (actionDefinition.isOutputDefined()) {
                        if (actionDefinition.isOutputSchemaDefined()) {
                            outputResponse = actionDefinition.getOutputResponse();
                        } else if (actionDefinition.isOutputFunctionDefined()) {
                            try {

                                outputResponse = actionDefinitionFacade.executeOutput(
                                    componentDefinition.getName(), componentDefinition.getVersion(),
                                    actionDefinition.getName(), Map.of(), Map.of());
                            } catch (Exception e) {
                                try {
                                    List<Connection> connections =
                                        connectionService.getConnections(componentName, version, ModeType.AUTOMATION);
                                    Map<String, Long> connectionIds = Map.of(operationName, connections.get(0)
                                        .getId());

                                    var output = actionDefinitionFacade.executePerform(componentDefinition.getName(),
                                        componentDefinition.getVersion(), actionDefinition.getName(), null, null, null,
                                        null, null, connectionIds, null, true, null);
                                    if (output != null) {
                                        outputResponse = SchemaUtils.toOutput(
                                            output, PropertyFactory.OUTPUT_FACTORY_FUNCTION,
                                            PropertyFactory.PROPERTY_FACTORY);
                                    }
                                } catch (Exception e2) {
                                    throw new Exception(
                                        "Please make a " + componentDefinition.getName() + " connection");
                                }
                            }
                        }
                    }
                } else {
                    throw new RuntimeException(String.format(OPERATION_NOT_FOUND, operationName, componentName));
                }
            }

            if (outputResponse == null || outputResponse.outputSchema() == null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved output properties for {}:{}", componentName, operationName);
            }

            return ToolUtils.convertToPropertyInfo(outputResponse.outputSchema());
        } catch (Exception e) {
            logger.error("Failed to get output properties for '{}:{}'", componentName, operationName, e);

            throw new RuntimeException(FAILED_TO_GET_OUTPUT_PROPERTIES + ": " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Get all properties of a specific trigger or action. Returns a hierarchical list of properties including nested properties")
    public List<PropertyInfo> getProperties(
        @ToolParam(
            description = "The name of the component that contains the trigger or action in camel case") String componentName,
        @ToolParam(
            description = "The name of the trigger or action to retrieve properties for in camel case") String operationName,
        @ToolParam(required = false, description = "The version of the component") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            List<? extends BaseProperty> properties;

            // First try to find in triggers
            var triggerOptional = componentDefinition.getTriggers()
                .stream()
                .filter(triggerDefinition -> {
                    String name = triggerDefinition.getName();

                    return name.equals(operationName);
                })
                .findFirst();

            if (triggerOptional.isPresent()) {
                TriggerDefinition triggerDefinition = triggerOptional.get();

                properties = triggerDefinition.getProperties();
            } else {
                // If not found in triggers, try actions
                var actionOptional = componentDefinition.getActions()
                    .stream()
                    .filter(actionDefinition -> {
                        String name = actionDefinition.getName();

                        return name.equals(operationName);
                    })
                    .findFirst();

                if (actionOptional.isPresent()) {
                    ActionDefinition actionDefinition = actionOptional.get();

                    properties = actionDefinition.getProperties();
                } else {
                    throw new RuntimeException(String.format(OPERATION_NOT_FOUND, operationName, componentName));
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved {} properties for {}:{}", properties.size(), componentName, operationName);
            }

            return ToolUtils.convertToPropertyInfoList(properties);
        } catch (Exception e) {
            logger.error("Failed to get properties for '{}:{}'", componentName, operationName, e);

            throw new RuntimeException(FAILED_TO_GET_PROPERTIES, e);
        }
    }

    /**
     * Minimal component information record for the response.
     */
    private ComponentDefinition getComponentDefinition(String componentName, Integer version) {
        return version != null
            ? componentDefinitionService.getComponentDefinition(componentName, version)
            : componentDefinitionService.getComponentDefinition(componentName, null);
    }

    /**
     * Detailed action information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ActionDetailedInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the action") String name,
        @JsonProperty("title") @JsonPropertyDescription("The title of the action") String title,
        @JsonProperty("description") @JsonPropertyDescription("The description of the action") String description,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component that contains this action") String componentName,
        @JsonProperty("properties") @JsonPropertyDescription("The properties defined in the action") String properties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the action (if output is defined)") String outputProperties) {
    }

    /**
     * Action information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ActionMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the action") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the action") String description,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component that contains this action") String componentName) {
    }

    @SuppressFBWarnings("EI")
    public record ComponentInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the component") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the component") String description,
        @JsonProperty("categories") @JsonPropertyDescription("The categories that the component belongs to") List<String> category,
        @JsonProperty("triggers") @JsonPropertyDescription("Triggers that are defined in the component") List<String> triggers,
        @JsonProperty("actions") @JsonPropertyDescription("Actions that are defined in the component") List<String> actions) {

    }

    /**
     * Minimal component information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ComponentMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the component") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the component") String description,
        @JsonProperty("version") @JsonPropertyDescription("The version of the component") int version) {

    }

    /**
     * Detailed trigger information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TriggerDetailedInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the trigger") String name,
        @JsonProperty("title") @JsonPropertyDescription("The title of the trigger") String title,
        @JsonProperty("description") @JsonPropertyDescription("The description of the trigger") String description,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component that contains this trigger") String componentName,
        @JsonProperty("type") @JsonPropertyDescription("The type of the trigger") String type,
        @JsonProperty("properties") @JsonPropertyDescription("The properties defined in the trigger") String properties,
        @JsonProperty("outputProperties") @JsonPropertyDescription("The output properties of the trigger (if output is defined)") String outputProperties) {
    }

    /**
     * Trigger information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record TriggerMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the trigger") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the trigger") String description,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component that contains this trigger") String componentName) {
    }
}
