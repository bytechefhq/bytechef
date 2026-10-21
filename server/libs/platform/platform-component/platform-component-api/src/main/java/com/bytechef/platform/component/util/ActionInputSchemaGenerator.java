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

package com.bytechef.platform.component.util;

import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.BooleanProperty;
import com.bytechef.platform.component.domain.DateProperty;
import com.bytechef.platform.component.domain.DateTimeProperty;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.IntegerProperty;
import com.bytechef.platform.component.domain.NumberProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.domain.TimeProperty;
import com.bytechef.platform.component.domain.ValueProperty;
import java.util.List;
import java.util.Map;
import org.springframework.ai.util.JacksonUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds a JSON Schema for an action's input from the full property tree: nested objects, array items, static options
 * as enums, defaults, and display-conditioned properties left out of {@code required}.
 *
 * @author Ivica Cardic
 */
public final class ActionInputSchemaGenerator {

    private static final JsonMapper JSON_MAPPER = JacksonUtils.getDefaultJsonMapper();

    private ActionInputSchemaGenerator() {
    }

    public static Map<String, Object> generateInputSchema(List<? extends Property> properties) {
        ObjectNode schema = objectSchema(properties, true);

        return JSON_MAPPER.convertValue(schema, new TypeReference<Map<String, Object>>() {});
    }

    private static ObjectNode objectSchema(List<? extends Property> properties, boolean closed) {
        ObjectNode schema = JSON_MAPPER.createObjectNode();

        schema.put("type", "object");

        ObjectNode propertiesNode = schema.putObject("properties");
        ArrayNode requiredNode = JSON_MAPPER.createArrayNode();

        for (Property property : properties) {
            if (property.getName() == null || property.getHidden()) {
                continue;
            }

            propertiesNode.set(property.getName(), propertySchema(property));

            if (property.getRequired() && property.getDisplayCondition() == null) {
                requiredNode.add(property.getName());
            }
        }

        schema.set("required", requiredNode);

        if (closed) {
            schema.put("additionalProperties", false);
        }

        return schema;
    }

    private static ObjectNode propertySchema(Property property) {
        ObjectNode schema = switch (property.getType()) {
            case ARRAY -> arraySchema((ArrayProperty) property);
            case BOOLEAN -> typed("boolean");
            case DATE -> formatted("date");
            case DATE_TIME -> formatted("date-time");
            case DYNAMIC_PROPERTIES -> openObject();
            case FILE_ENTRY -> objectSchema(((FileEntryProperty) property).getProperties(), false);
            case INTEGER -> typed("integer");
            case NULL -> typed("null");
            case NUMBER -> typed("number");
            case OBJECT -> nestedObjectSchema((ObjectProperty) property);
            case STRING -> typed("string");
            case TIME -> formatted("time");
        };

        if (property.getDescription() != null) {
            schema.put("description", property.getDescription());
        }

        addEnum(property, schema);
        addDefault(property, schema);

        return schema;
    }

    private static ObjectNode nestedObjectSchema(ObjectProperty property) {
        List<? extends Property> properties = property.getProperties();

        if (properties.isEmpty()) {
            return openObject();
        }

        if (property.getAdditionalProperties()
            .isEmpty()) {

            return objectSchema(properties, true);
        }

        ObjectNode schema = objectSchema(properties, false);

        schema.put("additionalProperties", true);

        return schema;
    }

    private static ObjectNode arraySchema(ArrayProperty property) {
        ObjectNode schema = typed("array");
        List<? extends Property> items = property.getItems()
            .stream()
            .filter(item -> !item.getHidden())
            .toList();

        if (items.isEmpty()) {
            schema.putObject("items");
        } else if (items.size() == 1) {
            schema.set("items", propertySchema(items.getFirst()));
        } else {
            ArrayNode anyOf = schema.putObject("items")
                .putArray("anyOf");

            for (Property item : items) {
                anyOf.add(propertySchema(item));
            }
        }

        addItemsEnum(property, schema, items.size());

        return schema;
    }

    private static void addItemsEnum(ArrayProperty property, ObjectNode schema, int itemTypeCount) {
        List<Option> options = property.getOptions();

        if (options.isEmpty()) {
            return;
        }

        ObjectNode itemsNode;

        if (itemTypeCount == 1) {
            itemsNode = (ObjectNode) schema.get("items");
        } else {
            itemsNode = schema.putObject("items");
        }

        ArrayNode enumNode = itemsNode.putArray("enum");

        for (Option option : options) {
            enumNode.add(JSON_MAPPER.valueToTree(option.getValue()));
        }
    }

    private static void addEnum(Property property, ObjectNode schema) {
        List<Option> options = switch (property) {
            case StringProperty stringProperty -> stringProperty.getOptions();
            case IntegerProperty integerProperty -> integerProperty.getOptions();
            case NumberProperty numberProperty -> numberProperty.getOptions();
            case DateProperty dateProperty -> dateProperty.getOptions();
            case DateTimeProperty dateTimeProperty -> dateTimeProperty.getOptions();
            case TimeProperty timeProperty -> timeProperty.getOptions();
            default -> List.of();
        };

        if (options.isEmpty()) {
            return;
        }

        ArrayNode enumNode = schema.putArray("enum");

        for (Option option : options) {
            enumNode.add(JSON_MAPPER.valueToTree(option.getValue()));
        }
    }

    private static void addDefault(Property property, ObjectNode schema) {
        boolean scalar = property instanceof StringProperty || property instanceof IntegerProperty ||
            property instanceof NumberProperty || property instanceof BooleanProperty;

        if (!scalar) {
            return;
        }

        Object defaultValue = ((ValueProperty<?>) property).getDefaultValue();

        if (defaultValue != null) {
            schema.set("default", JSON_MAPPER.valueToTree(defaultValue));
        }
    }

    private static ObjectNode typed(String type) {
        ObjectNode schema = JSON_MAPPER.createObjectNode();

        schema.put("type", type);

        return schema;
    }

    private static ObjectNode formatted(String format) {
        ObjectNode schema = typed("string");

        schema.put("format", format);

        return schema;
    }

    private static ObjectNode openObject() {
        ObjectNode schema = typed("object");

        schema.put("additionalProperties", true);

        return schema;
    }
}
