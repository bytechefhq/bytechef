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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.bool;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.number;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableArrayProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
public class JsonSchemaUtils {

    public static @Nullable ModifiableValueProperty<?, ?> getProperty(String jsonSchema) {
        return getProperty(null, jsonSchema);
    }

    public static @Nullable ModifiableValueProperty<?, ?> getProperty(String propertyName, String jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }

        JsonNode schemaJsonNode = JsonUtils.readTree(jsonSchema);

        return getProperty(propertyName, schemaJsonNode);
    }

    @SuppressWarnings("unchecked")
    private static void addProperty(
        ModifiableValueProperty<?, ?> property, List<ModifiableValueProperty<?, ?>> properties) {

        if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
            modifiableArrayProperty.items(properties);
        } else {
            ((ModifiableObjectProperty) property).properties(properties);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ModifiableValueProperty<?, ?>> getProperties(ModifiableValueProperty<?, ?> property) {
        if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
            return (List<ModifiableValueProperty<?, ?>>) (List<?>) modifiableArrayProperty.getItems()
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        } else {
            return (List<ModifiableValueProperty<?, ?>>) (List<?>) ((ModifiableObjectProperty) property).getProperties()
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        }
    }

    private static ModifiableValueProperty<?, ?> getProperty(String propertyName, JsonNode jsonNode) {
        JsonNode typeJsonNode = jsonNode.get("type");

        String type;

        if (typeJsonNode == null) {
            type = "string";
        } else {
            type = typeJsonNode.asText();
        }

        return switch (type) {
            case "array" -> {
                ModifiableArrayProperty arrayProperty = array(propertyName);

                JsonNode itemsJsonNode = jsonNode.get("items");

                if (itemsJsonNode != null) {
                    walkThroughProperty(itemsJsonNode, arrayProperty);
                }

                yield arrayProperty;
            }
            case "boolean" -> bool(propertyName);
            case "integer" -> integer(propertyName);
            case "number" -> number(propertyName);
            case "object" -> {
                ModifiableObjectProperty objectProperty = object(propertyName);

                JsonNode propertiesJsonNode = jsonNode.get("properties");

                if (propertiesJsonNode != null) {
                    walkThroughProperty(propertiesJsonNode, objectProperty);
                }

                yield objectProperty;
            }
            case "string" -> string(propertyName);
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    private static void walkThroughProperty(JsonNode jsonNode, ModifiableArrayProperty property) {
        List<ModifiableValueProperty<?, ?>> properties = getProperties(property);

        if (jsonNode.has("items")) {
            properties.add(getProperty(null, jsonNode));
        } else if (jsonNode.has("properties")) {
            properties.add(getProperty(null, jsonNode));
        } else {
            properties.add(getProperty(null, jsonNode));
        }

        addProperty(property, properties);
    }

    private static void walkThroughProperty(JsonNode jsonNode, ModifiableObjectProperty property) {
        if (jsonNode == null) {
            return;
        }

        for (Map.Entry<String, JsonNode> field : jsonNode.properties()) {
            String propertyName = field.getKey();
            JsonNode propertyJsonNode = field.getValue();

            List<ModifiableValueProperty<?, ?>> properties = getProperties(property);

            properties.add(getProperty(propertyName, propertyJsonNode));

            addProperty(property, properties);
        }
    }
}
