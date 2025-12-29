/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.component.context.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class JsonSchemaUtils {

    public static ModifiableValueProperty<?, ?> getProperty(String jsonSchema) {
        return getProperty(null, jsonSchema);
    }

    public static ModifiableValueProperty<?, ?> getProperty(String propertyName, String jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }

        JsonNode schemaJsonNode = JsonUtils.readTree(jsonSchema);

        return getProperty(propertyName, schemaJsonNode);
    }

    private static void addProperty(
        ModifiableValueProperty<?, ?> property, List<ModifiableValueProperty<?, ?>> properties) {

        if (property instanceof ModifiableArrayProperty) {
            ((ModifiableArrayProperty) property).items(properties);
        } else {
            ((ModifiableObjectProperty) property).properties(properties);
        }
    }

    private static ModifiableArrayProperty getArrayProperty(String propertyName, JsonNode jsonNode) {
        ModifiableArrayProperty arrayProperty = array(propertyName);

        walkThroughProperty(jsonNode.get("items"), arrayProperty);

        return arrayProperty;
    }

    private static ModifiableObjectProperty getObjectProperty(String propertyName, JsonNode jsonNode) {
        ModifiableObjectProperty objectProperty = object(propertyName);

        walkThroughProperty(jsonNode.get("properties"), objectProperty);

        return objectProperty;
    }

    @SuppressWarnings("unchecked")
    private static List<ModifiableValueProperty<?, ?>> getProperties(ModifiableValueProperty<?, ?> property) {
        List<ModifiableValueProperty<?, ?>> properties;

        if (property instanceof ModifiableArrayProperty) {
            properties = (List<ModifiableValueProperty<?, ?>>) OptionalUtils.mapOrElse(
                ((ModifiableArrayProperty) property).getItems(), ArrayList::new, new ArrayList<>());
        } else {
            properties = (List<ModifiableValueProperty<?, ?>>) OptionalUtils.mapOrElse(
                ((ModifiableObjectProperty) property).getProperties(), ArrayList::new, new ArrayList<>());
        }

        return properties;
    }

    private static ModifiableValueProperty<?, ?> getProperty(String propertyName, JsonNode jsonNode) {
        JsonNode typeJsonNode = jsonNode.get("type");

        String type;

        if (typeJsonNode == null) {
            type = "string";
        } else {
            type = typeJsonNode.asText();
        }

        // TODO Add validation for required properties

        return switch (type) {
            case "array" -> {
                ModifiableArrayProperty arrayProperty = array(propertyName);

                walkThroughProperty(jsonNode.get("items"), arrayProperty);

                yield arrayProperty;
            }
            case "boolean" -> bool(propertyName);
            case "integer" -> integer(propertyName);
            case "number" -> number(propertyName);
            case "object" -> {
                ModifiableObjectProperty objectProperty = object(propertyName);

                walkThroughProperty(jsonNode.get("properties"), objectProperty);

                yield objectProperty;
            }
            case "string" -> string(propertyName);
            default -> throw new IllegalArgumentException("Unsupported type: " + typeJsonNode.asText());
        };
    }

    private static void walkThroughProperty(JsonNode jsonNode, ModifiableArrayProperty property) {
        List<ModifiableValueProperty<?, ?>> properties = getProperties(property);

        if (jsonNode.has("items")) {
            properties.add(getArrayProperty(null, jsonNode));
        } else if (jsonNode.has("properties")) {
            properties.add(getObjectProperty(null, jsonNode));
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

            if (propertyJsonNode.has("items")) {
                properties.add(getArrayProperty(propertyName, propertyJsonNode));
            } else if (propertyJsonNode.has("properties")) {
                properties.add(getObjectProperty(propertyName, propertyJsonNode));
            } else {
                properties.add(getProperty(propertyName, propertyJsonNode));
            }

            addProperty(property, properties);
        }
    }
}
