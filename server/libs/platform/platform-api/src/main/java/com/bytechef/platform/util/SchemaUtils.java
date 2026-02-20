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

package com.bytechef.platform.util;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.definition.BaseControlType;
import com.bytechef.definition.BaseFileEntry;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseArrayProperty;
import com.bytechef.definition.BaseProperty.BaseBooleanProperty;
import com.bytechef.definition.BaseProperty.BaseDateProperty;
import com.bytechef.definition.BaseProperty.BaseDateTimeProperty;
import com.bytechef.definition.BaseProperty.BaseFileEntryProperty;
import com.bytechef.definition.BaseProperty.BaseIntegerProperty;
import com.bytechef.definition.BaseProperty.BaseNullProperty;
import com.bytechef.definition.BaseProperty.BaseNumberProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.definition.BaseProperty.BaseStringProperty;
import com.bytechef.definition.BaseProperty.BaseTimeProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.constant.PlatformConstants;
import com.bytechef.platform.domain.OutputResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
public class SchemaUtils {

    private static final Logger logger = LoggerFactory.getLogger(SchemaUtils.class);

    public static BaseProperty getOutputSchema(Object value, SchemaPropertyFactory schemaPropertyFactory) {
        return getOutputSchema(null, value, schemaPropertyFactory);
    }

    public static BaseProperty getOutputSchema(String name, Object value, SchemaPropertyFactory schemaPropertyFactory) {
        BaseProperty outputProperty;

        if (value == null) {
            outputProperty = schemaPropertyFactory.create(name, null, BaseNullProperty.class);
        } else {
            Class<?> valueClass = value.getClass();

            if (value instanceof List<?> || valueClass.isArray()) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseArrayProperty.class);
            } else if (value instanceof Boolean) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseBooleanProperty.class);
            } else if (value instanceof Date || value instanceof LocalDate) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseDateProperty.class);
            } else if (value instanceof LocalDateTime) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseDateTimeProperty.class);
            } else if (value instanceof BaseFileEntry) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseFileEntryProperty.class);
            } else if (value instanceof Integer) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseIntegerProperty.class);
            } else if (value instanceof Number) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseNumberProperty.class);
            } else if (value instanceof Map<?, ?>) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseObjectProperty.class);
            } else if (value instanceof String) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseStringProperty.class);
            } else if (value instanceof LocalTime) {
                outputProperty = schemaPropertyFactory.create(name, value, BaseTimeProperty.class);
            } else {
                if (ConvertUtils.canConvert(value, Map.class)) {
                    outputProperty = schemaPropertyFactory.create(name, value, BaseObjectProperty.class);
                } else {
                    outputProperty = schemaPropertyFactory.create(name, value, BaseProperty.class);
                }
            }
        }

        return outputProperty;
    }

    public static OutputResponse toOutput(
        BaseOutputDefinition.OutputResponse outputResponse, OutputFactoryFunction outputFactoryFunction,
        SchemaPropertyFactory propertyFactoryFunction) {

        Object sampleOutput = outputResponse.getSampleOutput();

        if (sampleOutput == null && outputResponse.getOutputSchema() != null) {
            sampleOutput = getSampleOutput(outputResponse.getOutputSchema());
        } else if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.readMap(string);
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace(e.getMessage(), e);
                }
            }
        }

        BaseValueProperty<?> outputSchema = outputResponse.getOutputSchema();

        if (outputSchema == null && sampleOutput != null) {
            outputSchema = (BaseValueProperty<?>) getOutputSchema(sampleOutput, propertyFactoryFunction);
        }

        return outputFactoryFunction.apply(outputSchema, sampleOutput, outputResponse.getPlaceholder());
    }

    public static OutputResponse toOutput(
        Object value, OutputFactoryFunction outputFactoryFunction, SchemaPropertyFactory propertyFactoryFunction) {

        return toOutput(
            BaseOutputDefinition.OutputResponse.of(
                (BaseValueProperty<?>) getOutputSchema(value, propertyFactoryFunction), value),
            outputFactoryFunction, propertyFactoryFunction);
    }

    private static Object getSampleOutput(BaseProperty definitionProperty) {
        if (definitionProperty instanceof BaseValueProperty<?> valueProperty) {
            if (valueProperty.getExampleValue()
                .isPresent()) {
                return valueProperty.getExampleValue()
                    .get();
            }

            if (valueProperty.getDefaultValue()
                .isPresent()) {
                return valueProperty.getDefaultValue()
                    .get();
            }
        }

        return switch (definitionProperty) {
            case BaseArrayProperty<? extends BaseProperty> baseArrayProperty -> {
                List<? extends BaseProperty> properties = baseArrayProperty.getItems()
                    .orElse(List.of());
                List<Object> items = new ArrayList<>();

                if (!properties.isEmpty()) {
                    items.add(getSampleOutput(properties.getFirst()));
                }

                yield items;
            }
            case BaseBooleanProperty ignored -> true;
            case BaseDateProperty ignored -> LocalDate.now();
            case BaseDateTimeProperty ignored -> LocalDateTime.now();
            case BaseFileEntryProperty<?> ignored ->
                Map.of(
                    "extension", "txt", "mimeType", "text/plain", "name", "sampleName", "url",
                    PlatformConstants.FILE_ENTRY_SAMPLE_URL);
            case BaseIntegerProperty ignored -> 57;
            case BaseNullProperty ignored -> null;
            case BaseNumberProperty ignored -> 23.34;
            case BaseObjectProperty<? extends BaseProperty> baseObjectProperty -> {
                List<? extends BaseProperty> baseProperties = baseObjectProperty.getProperties()
                    .orElse(List.of());
                Map<String, Object> map = new HashMap<>();

                for (BaseProperty property : baseProperties) {
                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case BaseStringProperty p -> {
                BaseControlType baseControlType = p.getControlType();

                if (Objects.equals(baseControlType.name(), "EMAIL")) {
                    yield "sample_email@" + (p.getName() == null ? "gmail.com" : p.getName());
                } else {
                    yield "sample " + (p.getName() == null ? "string" : p.getName());
                }
            }
            case BaseTimeProperty ignored -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }

    public static @Nullable BaseValueProperty<?> getJsonSchemaProperty(
        String jsonSchema, JsonSchemaPropertyFactory factory) {

        return getJsonSchemaProperty(null, jsonSchema, factory);
    }

    public static @Nullable BaseValueProperty<?> getJsonSchemaProperty(
        String propertyName, String jsonSchema, JsonSchemaPropertyFactory factory) {

        if (jsonSchema == null) {
            return null;
        }

        JsonNode schemaJsonNode = JsonUtils.readTree(jsonSchema);

        return getJsonSchemaProperty(propertyName, schemaJsonNode, factory);
    }

    private static BaseValueProperty<?> getJsonSchemaProperty(
        String propertyName, JsonNode jsonNode, JsonSchemaPropertyFactory factory) {

        JsonNode typeJsonNode = jsonNode.get("type");

        String type;

        if (typeJsonNode == null) {
            type = "string";
        } else {
            type = typeJsonNode.stringValue();
        }

        return switch (type) {
            case "array" -> {
                BaseValueProperty<?> arrayProperty = factory.create(propertyName, type);

                JsonNode itemsJsonNode = jsonNode.get("items");

                if (itemsJsonNode != null) {
                    walkThroughArrayProperty(itemsJsonNode, arrayProperty, factory);
                }

                yield arrayProperty;
            }
            case "boolean" -> factory.create(propertyName, type);
            case "integer" -> factory.create(propertyName, type);
            case "number" -> factory.create(propertyName, type);
            case "object" -> {
                BaseValueProperty<?> objectProperty = factory.create(propertyName, type);

                JsonNode propertiesJsonNode = jsonNode.get("properties");

                if (propertiesJsonNode != null) {
                    walkThroughObjectProperty(propertiesJsonNode, objectProperty, factory);
                }

                yield objectProperty;
            }
            case "string" -> factory.create(propertyName, type);
            default -> throw new IllegalArgumentException("Unsupported JSON schema type: " + type);
        };
    }

    private static void walkThroughArrayProperty(
        JsonNode jsonNode, BaseValueProperty<?> property, JsonSchemaPropertyFactory factory) {

        List<BaseValueProperty<?>> children = factory.getChildren(property);

        children.add(getJsonSchemaProperty(null, jsonNode, factory));

        factory.addChildren(property, children);
    }

    private static void walkThroughObjectProperty(
        JsonNode jsonNode, BaseValueProperty<?> property, JsonSchemaPropertyFactory factory) {

        if (jsonNode == null) {
            return;
        }

        for (Map.Entry<String, JsonNode> field : jsonNode.properties()) {
            String fieldName = field.getKey();
            JsonNode fieldJsonNode = field.getValue();

            List<BaseValueProperty<?>> children = factory.getChildren(property);

            children.add(getJsonSchemaProperty(fieldName, fieldJsonNode, factory));

            factory.addChildren(property, children);
        }
    }

    public interface JsonSchemaPropertyFactory {

        void addChildren(
            BaseValueProperty<?> property, List<BaseValueProperty<?>> children);

        BaseValueProperty<?> create(String name, String type);

        List<BaseValueProperty<?>> getChildren(BaseValueProperty<?> property);
    }

    @FunctionalInterface
    public interface OutputFactoryFunction {

        OutputResponse apply(BaseProperty outputSchema, Object sampleOutput, Object placeholder);
    }

    @FunctionalInterface
    public interface SchemaPropertyFactory {

        BaseProperty create(String name, Object value, Class<? extends BaseProperty> basePropertyClass);
    }
}
