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

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.converter.StructuredOutputConverter;

/**
 * An implementation of {@link StructuredOutputConverter} that transforms the LLM output to a specific object type using
 * JSON schema. This converter works by generating a JSON schema based on a given Java class or parameterized type
 * reference, which is then used to validate and transform the LLM output into the desired type.
 *
 * @author Ivica Cardic
 */
public class JsonSchemaStructuredOutputConverter implements StructuredOutputConverter<Object> {

    private final Context context;
    private final String jsonSchema;
    private final TypeReference<?> typeReference;

    public JsonSchemaStructuredOutputConverter(String jsonSchema, Context context) {
        this.context = context;

        Map<String, Object> jsonSchemaMap = context.json(json -> json.readMap(jsonSchema, Object.class));

        jsonSchemaMap.put("additionalProperties", false);

        if (jsonSchemaMap.get("properties") instanceof Map<?, ?> properties && !jsonSchemaMap.containsKey("required")) {
            List<?> keys = properties.keySet()
                .stream()
                .toList();

            jsonSchemaMap.put("required", keys);
        }

        this.jsonSchema = context.json(json -> json.write(jsonSchemaMap));

        String jsonSchemaType = (String) jsonSchemaMap.get("type");

        typeReference = switch (jsonSchemaType) {
            case "array" -> new TypeReference<List<Object>>() {};
            // For any other type, including primitives LLM returns JSON object, not sure how to force it to
            // return a primitive value
            case null, default -> new TypeReference<Map<String, Object>>() {};
        };
    }

    /**
     * Parses the given text to transform it to the desired target type.
     *
     * @param text The LLM output in string format.
     * @return The parsed output in the desired target type.
     */
    @Override
    public Object convert(@NonNull String text) {
        return context.json(json -> json.read(processText(text), this.typeReference));
    }

    /**
     * Provides the expected format of the response, instructing that it should adhere to the generated JSON schema.
     *
     * @return The instruction format string.
     */
    @Override
    public String getFormat() {
        String template =
            """
                Your response should be in JSON format.%n\
                Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.%n\
                Do not include markdown code blocks in your response.%n\
                Remove the ```json markdown from the output.%n\
                Here is the JSON Schema instance your output must adhere to:%n\
                ```%s```%n\
                """;
        return String.format(template, this.jsonSchema);
    }

    private static String processText(String text) {
        // Remove leading and trailing whitespace
        text = text.trim();

        // Check for and remove triple backticks and "json" identifier
        if (text.startsWith("```") && text.endsWith("```")) {
            // Remove the first line if it contains "```json"
            String[] lines = text.split("\n", 2);

            String line = lines[0].trim();

            if (line.equalsIgnoreCase("```json")) {
                text = lines.length > 1 ? lines[1] : "";
            } else {
                text = text.substring(3); // Remove leading ```
            }

            // Remove trailing ```
            text = text.substring(0, text.length() - 3);

            // Trim again to remove any potential whitespace
            text = text.trim();
        }

        return text;
    }
}
