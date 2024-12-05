/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.llm.converter;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.lang.NonNull;

/**
 * An implementation of {@link StructuredOutputConverter} that transforms the LLM output to a specific object type using
 * JSON schema. This converter works by generating a JSON schema based on a given Java class or parameterized type
 * reference, which is then used to validate and transform the LLM output into the desired type.
 */
public class JsonSchemaStructuredOutputConverter implements StructuredOutputConverter<Object> {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaStructuredOutputConverter.class);

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .addModules(JacksonUtils.instantiateAvailableModules())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();

    private final String jsonSchema;
    private final Type type;

    public JsonSchemaStructuredOutputConverter(String jsonSchema) {
        this.jsonSchema = jsonSchema;

        Map<String, ?> jsonSchemaMap = JsonUtils.readMap(jsonSchema);

        String jsonSchemaType = MapUtils.getString(jsonSchemaMap, "type");

        type = switch (jsonSchemaType) {
            case "array" -> new TypeReference<List<Object>>() {}.getType();
            // For any other type, including primitives LLM returns JSON object, not sure how to force it to
            // return a primitive value
            case null, default -> new TypeReference<Map<String, Object>>() {}.getType();
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
        try {
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

            return OBJECT_MAPPER.readValue(text, OBJECT_MAPPER.constructType(this.type));
        } catch (JsonProcessingException e) {
            logger.error("Could not parse the given text to the desired target type:" + text + " into " + this.type);

            throw new RuntimeException(e);
        }
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
}
