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

import com.bytechef.component.definition.Property.Type;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class ComponentTools {

    private static final Logger logger = LoggerFactory.getLogger(ComponentTools.class);

    // Error message constants
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

    private static final String DEFAULT_TRIGGER_DEFINITION = """
        {
            "label": "Function of the Trigger",
            "name": "{triggerName}_1",
            "type": "{componentName}/v{componentVersion}/{triggerName}"
            "parameters": {}
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
    public ComponentTools(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    // Helper methods
    private ComponentDefinition getComponentDefinition(String componentName, Integer version) {
        return version != null
            ? componentDefinitionService.getComponentDefinition(componentName, version)
            : componentDefinitionService.getComponentDefinition(componentName, null);
    }

    private RuntimeException createNotFoundException(String message, String... args) {
        return new RuntimeException(String.format(message, (Object[]) args));
    }

    private RuntimeException createOperationException(String message, Exception cause) {
        return new RuntimeException(message + ": " + cause.getMessage(), cause);
    }

    private String safeToLowerCase(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    private boolean matchesQuery(String name, String description, String query) {
        String lowerName = safeToLowerCase(name);
        String lowerDescription = safeToLowerCase(description);
        return lowerName.contains(query) || lowerDescription.contains(query);
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
                    component.getName(),
                    component.getDescription(),
                    component.getVersion()))
                .toList();
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_COMPONENTS, e);
            throw createOperationException(FAILED_TO_LIST_COMPONENTS, e);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific component. Returns detailed project information including: name, description, triggers and actions")
    public ComponentInfo getComponent(
        @ToolParam(description = "The name of the component to retrieve in camel case") String componentName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

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
                    .map(category -> category.getName())
                    .toList(),
                componentDefinition.getTriggers()
                    .stream()
                    .map(trigger -> trigger.getName())
                    .toList(),
                componentDefinition.getActions()
                    .stream()
                    .map(action -> action.getName())
                    .toList());
        } catch (Exception e) {
            logger.error("Failed to get component {}", componentName, e);
            throw createOperationException(FAILED_TO_GET_COMPONENT, e);
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
                .filter(component -> matchesQuery(
                    component.getName(),
                    component.getDescription(),
                    lowerQuery))
                .map(component -> new ComponentMinimalInfo(
                    component.getName(),
                    component.getDescription(),
                    component.getVersion()))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} components matching query '{}'", matchingComponents.size(), query);
            }

            return matchingComponents;
        } catch (Exception e) {
            logger.error("Failed to search components with query '{}'", query, e);
            throw createOperationException(FAILED_TO_SEARCH_COMPONENTS, e);
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
                        trigger.getName(),
                        trigger.getDescription(),
                        component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} triggers across all components", triggers.size());
            }

            return triggers;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_TRIGGERS, e);
            throw createOperationException(FAILED_TO_LIST_TRIGGERS, e);
        }
    }

    @Tool(
        description = "Get detailed information about a specific trigger. Returns comprehensive trigger information including properties and configuration")
    public TriggerDetailedInfo getTrigger(
        @ToolParam(
            description = "The name of the component that contains the trigger in camel case") String componentName,
        @ToolParam(description = "The name of the trigger to retrieve in camel case") String triggerName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

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

                    if (trigger.isOutputDefined() && outputResponse != null && outputResponse.outputSchema() != null) {
                        outputPropertiesJson = getOutputPropertiesJson(outputResponse.outputSchema());
                    }

                    return new TriggerDetailedInfo(
                        trigger.getName(),
                        trigger.getTitle(),
                        trigger.getDescription(),
                        componentDefinition.getName(),
                        trigger.getType()
                            .name(),
                        getParametersJson(trigger.getProperties()),
                        outputPropertiesJson);
                })
                .orElseThrow(() -> createNotFoundException(TRIGGER_NOT_FOUND, triggerName, componentName));

        } catch (Exception e) {
            logger.error("Failed to get trigger '{}' from component '{}'", triggerName, componentName, e);
            throw createOperationException(FAILED_TO_GET_TRIGGER, e);
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
                    .filter(trigger -> matchesQuery(
                        trigger.getName(),
                        trigger.getDescription(),
                        lowerQuery))
                    .map(trigger -> new TriggerMinimalInfo(
                        trigger.getName(),
                        trigger.getDescription(),
                        component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} triggers matching query '{}'", matchingTriggers.size(), query);
            }

            return matchingTriggers;
        } catch (Exception e) {
            logger.error("Failed to search triggers with query '{}'", query, e);
            throw createOperationException(FAILED_TO_SEARCH_TRIGGERS, e);
        }
    }

    @Tool(
        description = "Get the trigger definition template for the specific trigger. Returns a JSON template that can be used to configure the trigger in workflows")
    public String getTriggerDefinition(
        @ToolParam(
            description = "The name of the component that contains the trigger in camel case") String componentName,
        @ToolParam(description = "The name of the trigger to generate definition for in camel case") String triggerName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            // Get the actual trigger to extract its properties
            var trigger = componentDefinition.getTriggers()
                .stream()
                .filter(t -> t.getName()
                    .equals(triggerName))
                .findFirst()
                .orElseThrow(() -> createNotFoundException(TRIGGER_NOT_FOUND, triggerName, componentName));
            String parametersJson = getParametersJson(trigger.getProperties());

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
            throw createOperationException(FAILED_TO_GENERATE_TRIGGER_DEFINITION, e);
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
                        action.getName(),
                        action.getDescription(),
                        component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} actions across all components", actions.size());
            }

            return actions;
        } catch (Exception e) {
            logger.error(FAILED_TO_LIST_ACTIONS, e);
            throw createOperationException(FAILED_TO_LIST_ACTIONS, e);
        }
    }

    @Tool(
        description = "Get detailed information about a specific action. Returns comprehensive action information including properties and configuration")
    public ActionDetailedInfo getAction(
        @ToolParam(
            description = "The name of the component that contains the action in camel case") String componentName,
        @ToolParam(description = "The name of the action to retrieve in camel case") String actionName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            return componentDefinition.getActions()
                .stream()
                .filter(action -> action.getName()
                    .equals(actionName))
                .findFirst()
                .map(action -> {
                    String outputPropertiesJson = null;
                    OutputResponse outputResponse = action.getOutputResponse();

                    if (action.isOutputDefined() && outputResponse != null && outputResponse.outputSchema() != null) {
                        outputPropertiesJson = getOutputPropertiesJson(outputResponse.outputSchema());
                    }

                    return new ActionDetailedInfo(
                        action.getName(),
                        action.getTitle(),
                        action.getDescription(),
                        componentDefinition.getName(),
                        getParametersJson(action.getProperties()),
                        outputPropertiesJson);
                })
                .orElseThrow(() -> createNotFoundException(ACTION_NOT_FOUND, actionName, componentName));

        } catch (Exception e) {
            logger.error("Failed to get action '{}' from component '{}'", actionName, componentName, e);
            throw createOperationException(FAILED_TO_GET_ACTION, e);
        }
    }

    @Tool(
        description = "Search actions across all components. Returns a list of actions matching the search query in name or description")
    public List<ActionMinimalInfo> searchActions(
        @ToolParam(description = "The search query to match against action names and descriptions") String query) {

        try {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            String lowerQuery = query.toLowerCase()
                .trim();

            List<ActionMinimalInfo> matchingActions = componentDefinitions.stream()
                .flatMap(component -> component.getActions()
                    .stream()
                    .filter(action -> matchesQuery(
                        action.getName(),
                        action.getDescription(),
                        lowerQuery))
                    .map(action -> new ActionMinimalInfo(
                        action.getName(),
                        action.getDescription(),
                        component.getName())))
                .toList();

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} actions matching query '{}'", matchingActions.size(), query);
            }

            return matchingActions;
        } catch (Exception e) {
            logger.error("Failed to search actions with query '{}'", query, e);
            throw createOperationException(FAILED_TO_SEARCH_ACTIONS, e);
        }
    }

    @Tool(
        description = "Get the action definition template for the specific action. Returns a JSON template that can be used to configure the action in workflows")
    public String getActionDefinition(
        @ToolParam(
            description = "The name of the component that contains the action in camel case") String componentName,
        @ToolParam(description = "The name of the action to generate definition for in camel case") String actionName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            // Get the actual action to extract its properties
            var action = componentDefinition.getActions()
                .stream()
                .filter(a -> a.getName()
                    .equals(actionName))
                .findFirst()
                .orElseThrow(() -> createNotFoundException(ACTION_NOT_FOUND, actionName, componentName));
            String parametersJson = getParametersJson(action.getProperties());

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
            throw createOperationException(FAILED_TO_GENERATE_ACTION_DEFINITION, e);
        }
    }

    private String getParametersJson(List<? extends BaseProperty> properties) {
        if (properties.isEmpty()) {
            return "{}";
        }

        List<PropertyDecorator> propertyDecorators = PropertyDecorator.toPropertyDecorators(properties);
        return getObjectValue(propertyDecorators);
    }

    private String getOutputPropertiesJson(BaseProperty outputSchema) {
        if (outputSchema == null) {
            return "{}";
        }

        PropertyDecorator propertyDecorator = new PropertyDecorator(outputSchema);
        return getSampleValue(propertyDecorator);
    }

    @Tool(
        description = "Get the output property of a specific trigger or action. Returns the structure of the output property")
    public PropertyInfo getOutputProperty(
        @ToolParam(
            description = "The name of the component that contains the trigger or action in camel case") String componentName,
        @ToolParam(
            description = "The name of the trigger or action to retrieve output properties for in camel case") String operationName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            OutputResponse outputResponse = null;

            // First try to find in triggers
            var triggerOptional = componentDefinition.getTriggers()
                .stream()
                .filter(trigger -> trigger.getName()
                    .equals(operationName))
                .findFirst();

            if (triggerOptional.isPresent()) {
                var trigger = triggerOptional.get();
                outputResponse = trigger.isOutputDefined() ? trigger.getOutputResponse() : null;
            } else {
                // If not found in triggers, try actions
                var actionOptional = componentDefinition.getActions()
                    .stream()
                    .filter(action -> action.getName()
                        .equals(operationName))
                    .findFirst();

                if (actionOptional.isPresent()) {
                    var action = actionOptional.get();
                    outputResponse = action.isOutputDefined() ? action.getOutputResponse() : null;
                } else {
                    throw createNotFoundException(OPERATION_NOT_FOUND, operationName, componentName);
                }
            }

            if (outputResponse == null || outputResponse.outputSchema() == null) {
                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved output properties for {}:{}", componentName, operationName);
            }

            return convertToPropertyInfo(outputResponse.outputSchema());
        } catch (Exception e) {
            logger.error("Failed to get output properties for '{}:{}'", componentName, operationName, e);
            throw createOperationException(FAILED_TO_GET_OUTPUT_PROPERTIES, e);
        }
    }

    private String getArrayValue(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("[ ");

        for (var property : properties) {
            parameters.append(getSampleValue(property))
                .append(", ");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        return parameters.append("]")
            .toString();
    }

    private String getSampleValue(PropertyDecorator property) {
        String required = property.required ? " (required)" : "";

        return switch (property.getType()) {
            case ARRAY -> getArrayValue(property.getItems());
            case BOOLEAN -> "boolean" + required;
            case DATE -> "date" + required;
            case DATE_TIME -> "datetime" + required;
            case DYNAMIC_PROPERTIES -> "{}" + required;
            case INTEGER -> "integer" + required;
            case NUMBER -> "float" + required;
            case OBJECT -> getObjectValue(property.getObjectProperties());
            case FILE_ENTRY -> getObjectValue(property.getFileEntryProperties());
            case TIME -> "time" + required;
            default -> "string" + required;
        };
    }

    private String getObjectValue(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("{ ");

        for (var property : properties) {
            parameters.append("\"")
                .append(property.getName())
                .append("\": ")
                .append(getSampleValue(property))
                .append(", ");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        return parameters.append("}")
            .toString();
    }

    @Tool(
        description = "Get all properties of a specific trigger or action. Returns a hierarchical list of properties including nested properties")
    public List<PropertyInfo> getProperties(
        @ToolParam(
            description = "The name of the component that contains the trigger or action in camel case") String componentName,
        @ToolParam(
            description = "The name of the trigger or action to retrieve properties for in camel case") String operationName,
        @ToolParam(description = "The version of the component (optional)") Integer version) {

        try {
            ComponentDefinition componentDefinition = getComponentDefinition(componentName, version);

            List<? extends BaseProperty> properties = null;

            // First try to find in triggers
            var triggerOptional = componentDefinition.getTriggers()
                .stream()
                .filter(trigger -> trigger.getName()
                    .equals(operationName))
                .findFirst();

            if (triggerOptional.isPresent()) {
                properties = triggerOptional.get()
                    .getProperties();
            } else {
                // If not found in triggers, try actions
                var actionOptional = componentDefinition.getActions()
                    .stream()
                    .filter(action -> action.getName()
                        .equals(operationName))
                    .findFirst();

                if (actionOptional.isPresent()) {
                    properties = actionOptional.get()
                        .getProperties();
                } else {
                    throw createNotFoundException(OPERATION_NOT_FOUND, operationName, componentName);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved {} properties for {}:{}", properties.size(), componentName, operationName);
            }

            return convertToPropertyInfoList(properties);
        } catch (Exception e) {
            logger.error("Failed to get properties for '{}:{}'", componentName, operationName, e);
            throw createOperationException(FAILED_TO_GET_PROPERTIES, e);
        }
    }

    private List<PropertyInfo> convertToPropertyInfoList(List<? extends BaseProperty> properties) {
        return properties.stream()
            .map(this::convertToPropertyInfo)
            .toList();
    }

    private PropertyInfo convertToPropertyInfo(BaseProperty property) {
        PropertyDecorator decorator = new PropertyDecorator(property);

        List<PropertyInfo> nestedProperties = null;

        if (decorator.getType() == Type.OBJECT) {
            nestedProperties = convertToPropertyInfoList(decorator.getObjectProperties()
                .stream()
                .map(pd -> pd.property)
                .toList());
        } else if (decorator.getType() == Type.ARRAY) {
            nestedProperties = convertToPropertyInfoList(decorator.getItems()
                .stream()
                .map(pd -> pd.property)
                .toList());
        } else if (decorator.getType() == Type.FILE_ENTRY) {
            nestedProperties = convertToPropertyInfoList(decorator.getFileEntryProperties()
                .stream()
                .map(pd -> pd.property)
                .toList());
        }

        return new PropertyInfo(
            property.getName(),
            decorator.getType()
                .name(),
            property.getDescription(),
            property.getRequired(),
            property.getExpressionEnabled(),
            property.getDisplayCondition(),
            nestedProperties);
    }

    /**
     * Property information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record PropertyInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the property") String name,
        @JsonProperty("type") @JsonPropertyDescription("The type of the property") String type,
        @JsonProperty("description") @JsonPropertyDescription("The description of the property") String description,
        @JsonProperty("required") @JsonPropertyDescription("Whether the property is required") boolean required,
        @JsonProperty("expressionEnabled") @JsonPropertyDescription("Whether expressions are enabled for this property") boolean expressionEnabled,
        @JsonProperty("displayCondition") @JsonPropertyDescription("The display condition for the property") String displayCondition,
        @JsonProperty("nestedProperties") @JsonPropertyDescription("Nested properties for object/array/file_entry types") List<PropertyInfo> nestedProperties) {
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
     * Minimal component information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ComponentInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the component") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the component") String description,
        @JsonProperty("categories") @JsonPropertyDescription("The categories that the component belongs to") List<String> category,
        @JsonProperty("triggers") @JsonPropertyDescription("Triggers that are defined in the component") List<String> triggers,
        @JsonProperty("actions") @JsonPropertyDescription("Actions that are defined in the component") List<String> actions) {
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
     * Action information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ActionMinimalInfo(
        @JsonProperty("name") @JsonPropertyDescription("The name of the action") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the action") String description,
        @JsonProperty("componentName") @JsonPropertyDescription("The name of the component that contains this action") String componentName) {
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

    private static class PropertyDecorator {

        enum Location {
            COMPONENT,
            TASK_DISPATCHER
        }

        private final BaseProperty property;
        private final Type type;
        private final Location location;
        private final Boolean required;

        public PropertyDecorator(BaseProperty property) {
            this.property = property;

            switch (property) {
                case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.DynamicPropertiesProperty ignored -> {
                    this.type = Type.DYNAMIC_PROPERTIES;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NullProperty ignored -> {
                    this.type = Type.NULL;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.TASK_DISPATCHER;
                    this.required = ignored.getRequired();
                }
                case com.bytechef.platform.component.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.COMPONENT;
                    this.required = ignored.getRequired();
                }
                default -> {
                    this.type = Type.NULL;
                    this.location = Location.COMPONENT;
                    this.required = false;
                }
            }
        }

        public List<PropertyDecorator> getItems() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty) property).getItems());
                case COMPONENT ->
                    toPropertyDecorators(((com.bytechef.platform.component.domain.ArrayProperty) property).getItems());
            };
        }

        public List<PropertyDecorator> getFileEntryProperties() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty) property)
                        .getProperties());
                case COMPONENT -> toPropertyDecorators(
                    ((com.bytechef.platform.component.domain.FileEntryProperty) property).getProperties());
            };
        }

        public String getName() {
            return property.getName();
        }

        public List<PropertyDecorator> getObjectProperties() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty) property).getProperties());
                case COMPONENT -> toPropertyDecorators(
                    ((com.bytechef.platform.component.domain.ObjectProperty) property).getProperties());
            };
        }

        public Type getType() {
            return type;
        }

        public static List<PropertyDecorator> toPropertyDecorators(List<? extends BaseProperty> properties) {
            return properties.stream()
                .map(PropertyDecorator::new)
                .toList();
        }
    }
}
