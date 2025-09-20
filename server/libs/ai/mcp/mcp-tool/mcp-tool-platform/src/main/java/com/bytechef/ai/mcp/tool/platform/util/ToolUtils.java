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

package com.bytechef.ai.mcp.tool.platform.util;

import com.bytechef.ai.mcp.tool.model.PropertyInfo;
import com.bytechef.platform.domain.BaseProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class containing common helper methods for MCP tools.
 *
 * @author Marko Kriskovic
 */
public final class ToolUtils {

    private ToolUtils() {
    }

    /**
     * Compares two tasks for sorting based on search query relevance. First compares by total word match count, then by
     * priority of where matches are found: 1. Name matches (highest priority) 2. Component name matches 3. Description
     * matches (the lowest priority)
     *
     * @param name1          first task name
     * @param description1   first task description
     * @param componentName1 first component name
     * @param name2          second task name
     * @param description2   second task description
     * @param componentName2 second component name
     * @param lowerQuery     the search query (should be lowercase)
     * @return comparison result for sorting
     */
    public static int compareTasks(
        String name1, String description1, String componentName1, String name2, String description2,
        String componentName2, String lowerQuery) {

        String lowerName1 = safeToLowerCase(name1);
        String lowerDescription1 = safeToLowerCase(description1);
        String lowerComponentName1 = safeToLowerCase(componentName1);
        String lowerName2 = safeToLowerCase(name2);
        String lowerDescription2 = safeToLowerCase(description2);
        String lowerComponentName2 = safeToLowerCase(componentName2);

        String trim = lowerQuery.trim();

        String[] queryWords = trim.split("\\s+");

        // Count matches for task 1
        TaskMatchInfo match1 = countMatches(lowerName1, lowerDescription1, lowerComponentName1, queryWords);

        // Count matches for task 2
        TaskMatchInfo match2 = countMatches(lowerName2, lowerDescription2, lowerComponentName2, queryWords);

        // First compare by total match count (more matches = better)
        int totalMatchComparison = Integer.compare(match2.totalMatches(), match1.totalMatches());

        if (totalMatchComparison != 0) {
            return totalMatchComparison;
        }

        // If total matches are equal, compare by match location priority
        // Name matches have highest priority (3), component matches (2), description matches (1)
        int priority1 = calculatePriority(match1);
        int priority2 = calculatePriority(match2);

        int priorityComparison = Integer.compare(priority2, priority1);

        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // If still equal, sort alphabetically by name
        return lowerName1.compareToIgnoreCase(lowerName2);
    }

    /**
     * Converts a single BaseProperty to PropertyInfo with nested properties support.
     *
     * @param property the property to convert
     * @return PropertyInfo object
     */
    public static PropertyInfo convertToPropertyInfo(BaseProperty property) {
        PropertyDecorator decorator = new PropertyDecorator(property);

        List<PropertyInfo> nestedPropertyInfos = null;

        PropertyDecorator.Type type = decorator.getType();

        if (type == PropertyDecorator.Type.OBJECT) {
            nestedPropertyInfos = convertToPropertyInfoList(
                decorator.getObjectProperties()
                    .stream()
                    .map(pd -> pd.property)
                    .toList());
        } else if (type == PropertyDecorator.Type.ARRAY) {
            nestedPropertyInfos = convertToPropertyInfoList(
                decorator.getItems()
                    .stream()
                    .map(pd -> pd.property)
                    .toList());
        } else if (type == PropertyDecorator.Type.FILE_ENTRY) {
            nestedPropertyInfos = convertToPropertyInfoList(
                decorator.getFileEntryProperties()
                    .stream()
                    .map(pd -> pd.property)
                    .toList());
        }

        return new PropertyInfo(
            property.getName(), type.name(), property.getDescription(), property.getRequired(),
            property.getExpressionEnabled(), property.getDisplayCondition(), nestedPropertyInfos);
    }

    /**
     * Converts a list of BaseProperty objects to PropertyInfo objects.
     *
     * @param properties the list of properties to convert
     * @return list of PropertyInfo objects
     */
    public static List<PropertyInfo> convertToPropertyInfoList(List<? extends BaseProperty> properties) {
        return properties.stream()
            .map(ToolUtils::convertToPropertyInfo)
            .toList();
    }

    /**
     * Generates a JSON representation of output properties from a BaseProperty schema.
     *
     * @param outputSchema the output schema property
     * @return JSON string representation of the output properties
     */
    public static String generateOutputPropertiesJson(BaseProperty outputSchema) {
        if (outputSchema == null) {
            return "{}";
        }

        PropertyDecorator propertyDecorator = new PropertyDecorator(outputSchema);

        return generateSampleValue(propertyDecorator);
    }

    /**
     * Generates a JSON representation of properties using PropertyDecorator for detailed type information.
     *
     * @param properties the list of properties to convert to JSON
     * @return JSON string representation of the properties
     */
    public static String generateParametersJson(List<? extends BaseProperty> properties) {
        if (properties.isEmpty()) {
            return "{}";
        }

        List<PropertyDecorator> propertyDecorators = PropertyDecorator.toPropertyDecorators(properties);

        return generateObjectValue(propertyDecorators, "", "\"");
    }

    /**
     * Checks if the given name or description matches the search query. Splits the query into individual words and
     * matches if any word is found.
     *
     * @param name                 the name to search in
     * @param description          the description to search in
     * @param componentName        the component name to search in
     * @param componentDescription the component description to search in
     * @param query                the search query (should be lowercase)
     * @return true if any word from the query is found in any of the fields
     */
    public static boolean matchesQuery(
        String name, String description, String componentName, String componentDescription, String query) {

        String lowerName = safeToLowerCase(name);
        String lowerDescription = safeToLowerCase(description);
        String lowerComponentName = safeToLowerCase(componentName);
        String lowerComponentDescription = safeToLowerCase(componentDescription);

        String lowerCase = query.toLowerCase();

        String trim = lowerCase.trim();

        String[] queryWords = trim.split("\\s+");

        for (String word : queryWords) {
            if (word.isEmpty()) {
                continue;
            }

            if (lowerName.contains(word) || lowerDescription.contains(word) || lowerComponentName.contains(word) ||
                lowerComponentDescription.contains(word)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Creates a map of display conditions to property names that have those conditions. Recursively searches through
     * nested properties (objects, arrays, file entries).
     *
     * @param properties the list of properties to analyze
     * @return map where key is display condition and value is list of property names with that condition
     */
    static Map<String, List<String>> getDisplayConditions(List<PropertyDecorator> properties) {
        Map<String, List<String>> displayConditionsMap = new HashMap<>();

        collectDisplayConditions(properties, displayConditionsMap, "");

        return displayConditionsMap;
    }

    /**
     * Generates a JSON array representation from a list of property decorators.
     *
     * @param properties the list of property decorators
     * @return JSON array string representation
     */
    static String generateArrayValue(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("[ ");

        for (var property : properties) {
            parameters.append(generateSampleValue(property))
                .append(", ");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        parameters.append(" ]");

        return parameters.toString();
    }

    /**
     * Generates a JSON object representation from a list of property decorators.
     *
     * @param properties the list of property decorators
     * @return JSON object string representation
     */
    static String generateObjectValue(
        List<PropertyDecorator> properties, String displayCondition, String required) {

        StringBuilder parameters = new StringBuilder();

        parameters.append("{ ")
            .append("\"metadata\": \"")
            .append(displayCondition.trim())
            .append(required)
            .append(", ");

        for (var property : properties) {
            parameters.append("\"")
                .append(property.getName())
                .append("\": ")
                .append(generateSampleValue(property))
                .append(", ");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        parameters.append(" }");

        return parameters.toString();
    }

    /**
     * Generates a sample value representation for a property decorator.
     *
     * @param property the property decorator
     * @return string representation of the sample value
     */
    static String generateSampleValue(PropertyDecorator property) {
        String required = property.getRequired() ? " (required)\"" : "\"";
        String displayCondition = property.getDisplayCondition() == null ? "" : " @" + property.displayCondition + "@";

        return switch (property.getType()) {
            case PropertyDecorator.Type.ARRAY -> generateArrayValue(property.getItems());
            case PropertyDecorator.Type.BOOLEAN -> "\"boolean" + displayCondition + required;
            case PropertyDecorator.Type.DATE -> "\"date" + displayCondition + required;
            case PropertyDecorator.Type.DATE_TIME -> "\"datetime" + displayCondition + required;
            case PropertyDecorator.Type.DYNAMIC_PROPERTIES -> "{}" + displayCondition + required;
            case PropertyDecorator.Type.INTEGER -> "\"integer" + displayCondition + required;
            case PropertyDecorator.Type.NUMBER -> "\"float" + displayCondition + required;
            case PropertyDecorator.Type.OBJECT ->
                generateObjectValue(property.getObjectProperties(), displayCondition, required);
            case PropertyDecorator.Type.FILE_ENTRY ->
                generateObjectValue(property.getFileEntryProperties(), displayCondition, required);
            case PropertyDecorator.Type.TIME -> "\"time" + displayCondition + required;
            case PropertyDecorator.Type.TASK -> "\"task" + displayCondition + required;
            default -> "\"string" + displayCondition + required;
        };
    }

    /**
     * Calculates priority score based on where matches are found. Name matches get highest weight, followed by
     * component, then description.
     */
    private static int calculatePriority(TaskMatchInfo matchInfo) {
        return matchInfo.nameMatches * 3 + matchInfo.componentMatches * 2 + matchInfo.descriptionMatches;
    }

    private static void collectDisplayConditions(
        List<PropertyDecorator> properties, Map<String, List<String>> displayConditionsMap, String parentPath) {

        for (PropertyDecorator property : properties) {
            String propertyPath = parentPath.isEmpty() ? property.getName() : parentPath + "." + property.getName();
            String displayCondition = property.property.getDisplayCondition();

            if (displayCondition != null && !StringUtils.isBlank(displayCondition)) {
                List<String> propertyPaths =
                    displayConditionsMap.computeIfAbsent(displayCondition, k -> new ArrayList<>());

                propertyPaths.add(propertyPath);
            }

            switch (property.getType()) {
                case OBJECT ->
                    collectDisplayConditions(property.getObjectProperties(), displayConditionsMap, propertyPath);
                case ARRAY -> collectDisplayConditions(property.getItems(), displayConditionsMap, propertyPath);
                case FILE_ENTRY ->
                    collectDisplayConditions(property.getFileEntryProperties(), displayConditionsMap, propertyPath);
                default -> {
                }
            }
        }
    }

    /**
     * Counts matches for each field and returns match information.
     */
    private static TaskMatchInfo countMatches(
        String name, String description, String componentName, String[] queryWords) {

        int nameMatches = 0;
        int componentMatches = 0;
        int descriptionMatches = 0;

        for (String word : queryWords) {
            if (word.isEmpty()) {
                continue;
            }

            if (name.contains(word)) {
                nameMatches++;
            }

            if (componentName.contains(word)) {
                componentMatches++;
            }

            if (description.contains(word)) {
                descriptionMatches++;
            }
        }

        return new TaskMatchInfo(nameMatches, componentMatches, descriptionMatches);
    }

    private static String safeToLowerCase(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    /**
     * Record to hold match information for a task.
     */
    static class PropertyDecorator {

        public enum Location {
            COMPONENT,
            TASK_DISPATCHER
        }

        public enum Type {
            ARRAY,
            BOOLEAN,
            DATE,
            DATE_TIME,
            DYNAMIC_PROPERTIES,
            FILE_ENTRY,
            INTEGER,
            NULL,
            NUMBER,
            OBJECT,
            STRING,
            TIME,
            TASK
        }

        private final BaseProperty property;
        private final Type type;
        private final Location location;
        private final Boolean required;
        private final String displayCondition;

        public PropertyDecorator(BaseProperty property) {
            this.property = property;
            this.required = property.getRequired();
            this.displayCondition = property.getDisplayCondition();

            switch (property) {
                case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.component.domain.DynamicPropertiesProperty ignored -> {
                    this.type = Type.DYNAMIC_PROPERTIES;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NullProperty ignored -> {
                    this.type = Type.NULL;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.TaskProperty ignored -> {
                    this.type = Type.TASK;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.COMPONENT;
                }
                default -> {
                    this.type = Type.NULL;
                    this.location = Location.COMPONENT;
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

        public Boolean getRequired() {
            return required;
        }

        public String getDisplayCondition() {
            return displayCondition;
        }

        public static List<PropertyDecorator> toPropertyDecorators(List<? extends BaseProperty> properties) {
            return properties.stream()
                .map(PropertyDecorator::new)
                .toList();
        }
    }

    private record TaskMatchInfo(int nameMatches, int componentMatches, int descriptionMatches) {

        int totalMatches() {
            return nameMatches + componentMatches + descriptionMatches;
        }
    }
}
