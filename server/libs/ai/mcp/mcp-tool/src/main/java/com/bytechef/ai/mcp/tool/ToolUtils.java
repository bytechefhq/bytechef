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
import com.bytechef.platform.domain.BaseProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Utility class containing common helper methods for MCP tools.
 *
 * @author Marko Kriskovic
 */
public final class ToolUtils {

    private ToolUtils() {
        // Utility class
    }

    /**
     * Creates a RuntimeException with formatted message using the provided arguments.
     *
     * @param message the message template
     * @param args    the arguments to format the message
     * @return a new RuntimeException with the formatted message
     */
    public static RuntimeException createNotFoundException(String message, String... args) {
        return new RuntimeException(String.format(message, (Object[]) args));
    }

    /**
     * Creates a RuntimeException that wraps another exception with additional context.
     *
     * @param message the context message
     * @param cause   the underlying cause
     * @return a new RuntimeException with the combined message and cause
     */
    public static RuntimeException createOperationException(String message, Exception cause) {
        return new RuntimeException(message + ": " + cause.getMessage(), cause);
    }

    /**
     * Safely converts a string to lowercase, returning empty string if null.
     *
     * @param value the string to convert
     * @return lowercase string or empty string if null
     */
    public static String safeToLowerCase(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    /**
     * Checks if the given name or description matches the search query.
     *
     * @param name        the name to search in
     * @param description the description to search in
     * @param query       the search query (should be lowercase)
     * @return true if either name or description contains the query
     */
    public static boolean matchesQuery(String name, String description, String query) {
        String lowerName = safeToLowerCase(name);
        String lowerDescription = safeToLowerCase(description);
        return lowerName.contains(query) || lowerDescription.contains(query);
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
     * Converts a single BaseProperty to PropertyInfo with nested properties support.
     *
     * @param property the property to convert
     * @return PropertyInfo object
     */
    public static PropertyInfo convertToPropertyInfo(BaseProperty property) {
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

    public static class PropertyDecorator {

        public enum Location {
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

        public Boolean getRequired() {
            return required;
        }

        public static List<PropertyDecorator> toPropertyDecorators(List<? extends BaseProperty> properties) {
            return properties.stream()
                .map(PropertyDecorator::new)
                .toList();
        }
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
        return generateObjectValue(propertyDecorators);
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
     * Generates a sample value representation for a property decorator.
     *
     * @param property the property decorator
     * @return string representation of the sample value
     */
    public static String generateSampleValue(PropertyDecorator property) {
        String required = property.getRequired() ? " (required)" : "";

        return switch (property.getType()) {
            case ARRAY -> generateArrayValue(property.getItems());
            case BOOLEAN -> "boolean" + required;
            case DATE -> "date" + required;
            case DATE_TIME -> "datetime" + required;
            case DYNAMIC_PROPERTIES -> "{}" + required;
            case INTEGER -> "integer" + required;
            case NUMBER -> "float" + required;
            case OBJECT -> generateObjectValue(property.getObjectProperties());
            case FILE_ENTRY -> generateObjectValue(property.getFileEntryProperties());
            case TIME -> "time" + required;
            default -> "string" + required;
        };
    }

    /**
     * Generates a JSON array representation from a list of property decorators.
     *
     * @param properties the list of property decorators
     * @return JSON array string representation
     */
    public static String generateArrayValue(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("[ ");

        for (var property : properties) {
            parameters.append(generateSampleValue(property))
                .append(", ");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        return parameters.append("]")
            .toString();
    }

    /**
     * Generates a JSON object representation from a list of property decorators.
     *
     * @param properties the list of property decorators
     * @return JSON object string representation
     */
    public static String generateObjectValue(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("{ ");

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

        return parameters.append("}")
            .toString();
    }
}
