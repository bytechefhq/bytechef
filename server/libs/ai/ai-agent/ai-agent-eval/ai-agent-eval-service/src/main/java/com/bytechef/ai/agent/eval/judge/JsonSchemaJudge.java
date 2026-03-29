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

package com.bytechef.ai.agent.eval.judge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * A deterministic judge that validates the agent output is valid JSON and contains all required fields from a schema.
 */
public class JsonSchemaJudge extends DeterministicJudge {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, Object> schema;

    public JsonSchemaJudge(Map<String, Object> schema) {
        super("JsonSchema", "Validates that agent output is valid JSON and contains all required fields from a schema");

        this.schema = Collections.unmodifiableMap(schema);
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");

        Map<String, Object> parsedOutput;

        try {
            parsedOutput = OBJECT_MAPPER.readValue(output, new TypeReference<>() {});
        } catch (JacksonException jsonProcessingException) {
            return Judgment.fail("Output is not valid JSON: " + jsonProcessingException.getMessage());
        }

        Object requiredFields = schema.get("required");

        if (requiredFields instanceof List<?> requiredFieldList) {
            List<String> missingFields = new ArrayList<>();

            for (Object field : requiredFieldList) {
                String fieldName = String.valueOf(field);

                if (!parsedOutput.containsKey(fieldName)) {
                    missingFields.add(fieldName);
                }
            }

            if (!missingFields.isEmpty()) {
                return Judgment.fail("Output is missing required fields: " + missingFields);
            }
        }

        return Judgment.pass("Output is valid JSON and contains all required fields");
    }

}
