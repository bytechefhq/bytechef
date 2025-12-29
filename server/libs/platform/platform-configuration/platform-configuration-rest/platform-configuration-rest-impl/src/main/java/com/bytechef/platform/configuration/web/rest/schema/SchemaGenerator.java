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

package com.bytechef.platform.configuration.web.rest.schema;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Ivica Cardic
 */
@Component
public class SchemaGenerator {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public SchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateSchemaFromJson(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            ObjectNode schemaNode = objectMapper.createObjectNode();

            schemaNode.put("$schema", "https://json-schema.org/draft/2020-12/schema");

            if (jsonNode.isObject()) {
                schemaNode.put("type", "object");

                ObjectNode propertiesNode = schemaNode.putObject("properties");

                generateSchemaFromObject(jsonNode, propertiesNode);
            } else if (jsonNode.isArray()) {
                schemaNode.put("type", "array");

                if (!jsonNode.isEmpty()) {
                    ObjectNode itemsNode = schemaNode.putObject("items");

                    generateSchemaFromObject(jsonNode.get(0), itemsNode);
                }
            }

            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate JSON schema", e);
        }
    }

    private void generateSchemaFromObject(JsonNode jsonNode, ObjectNode propertiesNode) {
        for (Map.Entry<String, JsonNode> field : jsonNode.properties()) {
            ObjectNode propertyNode = propertiesNode.putObject(field.getKey());

            JsonNode value = field.getValue();

            if (value.isTextual()) {
                propertyNode.put("type", "string");
            } else if (value.isInt()) {
                propertyNode.put("type", "integer");
            } else if (value.isBoolean()) {
                propertyNode.put("type", "boolean");
            } else if (value.isObject()) {
                propertyNode.put("type", "object");

                ObjectNode childPropertiesNode = propertyNode.putObject("properties");

                generateSchemaFromObject(value, childPropertiesNode);
            } else if (value.isArray()) {
                propertyNode.put("type", "array");

                if (!value.isEmpty()) {
                    ObjectNode itemsNode = propertyNode.putObject("items");

                    generateSchemaFromObject(value.get(0), itemsNode);
                }
            } else {
                propertyNode.put("type", "string"); // default to string for unknown types
            }
        }
    }
}
