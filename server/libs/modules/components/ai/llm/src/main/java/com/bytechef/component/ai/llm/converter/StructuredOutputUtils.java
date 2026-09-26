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

package com.bytechef.component.ai.llm.converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.util.JacksonUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Helpers shared by the structured output paths: normalizing an LLM reply before it is parsed, and adapting a response
 * schema to the subset of JSON Schema that provider-native structured output modes accept.
 *
 * @author Ivica Cardic
 */
public final class StructuredOutputUtils {

    private static final JsonMapper JSON_MAPPER = JacksonUtils.getDefaultJsonMapper();

    /**
     * Keywords whose values are maps of name to subschema, so their keys are names and never keywords.
     */
    private static final Set<String> SCHEMA_MAP_KEYWORDS = Set.of(
        "$defs", "definitions", "dependentSchemas", "patternProperties", "properties");

    /**
     * Keywords whose values are data, not subschemas, so they are copied as-is.
     */
    private static final Set<String> VALUE_KEYWORDS = Set.of("const", "default", "enum", "examples", "required");

    private static final Set<String> UNSUPPORTED_STRICT_KEYWORDS = Set.of(
        "contains", "exclusiveMaximum", "exclusiveMinimum", "maxContains", "maxItems", "maxLength", "maximum",
        "minContains", "minItems", "minLength", "minimum", "multipleOf", "uniqueItems");

    private StructuredOutputUtils() {
    }

    /**
     * Removes a surrounding markdown code fence (```json ... ``` or ``` ... ```) from an LLM reply.
     *
     * @param text the LLM reply
     * @return the reply without the fence, trimmed
     */
    public static String stripCodeFence(String text) {
        text = text.trim();

        if (text.startsWith("```") && text.endsWith("```") && text.length() >= 6) {
            String[] lines = text.split("\n", 2);

            String line = lines[0].trim();

            if (line.equalsIgnoreCase("```json")) {
                text = lines.length > 1 ? lines[1] : "";
            } else {
                text = text.substring(3);
            }

            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }

            text = text.trim();
        }

        return text;
    }

    /**
     * Converts a response schema into the strict form required by provider-native structured output: every object
     * schema gets {@code additionalProperties: false}, and numeric, string-length and array-size constraints are
     * dropped. A root object without {@code required} gets all its properties marked required, matching
     * {@link JsonSchemaStructuredOutputConverter}. The dropped constraints are still enforced client-side by the
     * structured output validation against the original schema.
     *
     * @param jsonSchema the response schema as JSON
     * @return the strict schema as JSON
     */
    public static String toStrictJsonSchema(String jsonSchema) {
        Map<String, Object> schema = JSON_MAPPER.readValue(jsonSchema, new TypeReference<>() {});

        Map<String, Object> strictSchema = toStrictSchema(schema);

        if (strictSchema.get("properties") instanceof Map<?, ?> properties && !strictSchema.containsKey("required")) {
            strictSchema.put("required", new ArrayList<>(properties.keySet()));
        }

        return JSON_MAPPER.writeValueAsString(strictSchema);
    }

    private static boolean isObjectSchema(Map<String, Object> schema) {
        Object type = schema.get("type");

        return "object".equals(type) || (type instanceof List<?> types && types.contains("object")) ||
            schema.containsKey("properties");
    }

    private static Map<String, Object> toStrictSchema(Map<?, ?> schema) {
        Map<String, Object> strictSchema = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : schema.entrySet()) {
            String keyword = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (UNSUPPORTED_STRICT_KEYWORDS.contains(keyword)) {
                continue;
            }

            if (VALUE_KEYWORDS.contains(keyword)) {
                strictSchema.put(keyword, value);
            } else if (SCHEMA_MAP_KEYWORDS.contains(keyword) && value instanceof Map<?, ?> namedSchemas) {
                Map<String, Object> strictNamedSchemas = new LinkedHashMap<>();

                for (Map.Entry<?, ?> namedSchema : namedSchemas.entrySet()) {
                    strictNamedSchemas.put(String.valueOf(namedSchema.getKey()), toStrictNode(namedSchema.getValue()));
                }

                strictSchema.put(keyword, strictNamedSchemas);
            } else {
                strictSchema.put(keyword, toStrictNode(value));
            }
        }

        if (isObjectSchema(strictSchema)) {
            strictSchema.put("additionalProperties", false);
        }

        return strictSchema;
    }

    private static Object toStrictNode(Object node) {
        if (node instanceof Map<?, ?> schema) {
            return toStrictSchema(schema);
        }

        if (node instanceof List<?> nodes) {
            List<Object> strictNodes = new ArrayList<>(nodes.size());

            for (Object item : nodes) {
                strictNodes.add(toStrictNode(item));
            }

            return strictNodes;
        }

        return node;
    }
}
