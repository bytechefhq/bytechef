/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads and applies the JSON Schema a Call-workflow trigger declares for its input. A blank schema declares nothing and
 * accepts any input; a schema that cannot be read is reported with {@link IllegalArgumentException}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppWorkflowSchemas {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final SchemaRegistry SCHEMA_REGISTRY = SchemaRegistry.withDefaultDialect(
        SpecificationVersion.DRAFT_2020_12);

    private AppWorkflowSchemas() {
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Map<String, Object> parse(@Nullable String jsonSchema) {
        if (jsonSchema == null || jsonSchema.isBlank()) {
            return null;
        }

        try {
            return JSON_MAPPER.readValue(jsonSchema, Map.class);
        } catch (JacksonException jacksonException) {
            throw new IllegalArgumentException("The schema is not valid JSON", jacksonException);
        }
    }

    /**
     * Checks that a schema can be both parsed and compiled, references included. A blank schema is readable.
     *
     * @throws IllegalArgumentException when the schema is not valid JSON or not a compilable JSON Schema
     */
    public static void checkReadable(@Nullable String jsonSchema) {
        if (jsonSchema == null || jsonSchema.isBlank()) {
            return;
        }

        compile(jsonSchema);
    }

    public static List<String> validate(@Nullable String inputSchema, Map<String, ?> input) {
        if (inputSchema == null || inputSchema.isBlank()) {
            return List.of();
        }

        Schema schema = compile(inputSchema);

        JsonNode inputNode = JSON_MAPPER.valueToTree(input);

        try {
            return schema.validate(inputNode)
                .stream()
                .map(Error::getMessage)
                .toList();
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("The input schema cannot be applied", runtimeException);
        }
    }

    private static Schema compile(String jsonSchema) {
        try {
            Schema schema = SCHEMA_REGISTRY.getSchema(JSON_MAPPER.readTree(jsonSchema));

            schema.initializeValidators();

            return schema;
        } catch (JacksonException jacksonException) {
            throw new IllegalArgumentException("The schema is not valid JSON", jacksonException);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("The schema is not a compilable JSON Schema", runtimeException);
        }
    }
}
