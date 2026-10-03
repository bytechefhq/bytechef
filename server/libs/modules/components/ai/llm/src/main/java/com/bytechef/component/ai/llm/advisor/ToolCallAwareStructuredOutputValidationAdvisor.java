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

package com.bytechef.component.ai.llm.advisor;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.UsageAccumulator;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public final class ToolCallAwareStructuredOutputValidationAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAwareStructuredOutputValidationAdvisor.class);

    private static final int DEFAULT_MAX_REPEAT_ATTEMPTS = 3;
    private static final int ORDER = LOWEST_PRECEDENCE - 2000;

    private final JsonMapper jsonMapper = new JsonMapper();
    private final Schema jsonSchema;
    private final int maxRepeatAttempts;

    public ToolCallAwareStructuredOutputValidationAdvisor(String outputJsonSchema) {
        this(outputJsonSchema, DEFAULT_MAX_REPEAT_ATTEMPTS);
    }

    public ToolCallAwareStructuredOutputValidationAdvisor(String outputJsonSchema, int maxRepeatAttempts) {
        if (maxRepeatAttempts < 0) {
            throw new IllegalArgumentException("maxRepeatAttempts must be greater than or equal to 0");
        }

        JsonNode schemaNode;

        try {
            schemaNode = jsonMapper.readTree(outputJsonSchema);
        } catch (JacksonException jacksonException) {
            throw new IllegalArgumentException("Failed to parse JSON schema", jacksonException);
        }

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

        this.jsonSchema = schemaRegistry.getSchema(schemaNode);
        this.maxRepeatAttempts = maxRepeatAttempts;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientRequest attemptChatClientRequest = chatClientRequest;
        UsageAccumulator usageAccumulator = new UsageAccumulator();

        for (int attempt = 0;; attempt++) {
            ChatClientResponse chatClientResponse = callAdvisorChain.copy(this)
                .nextCall(attemptChatClientRequest);

            ChatResponse chatResponse = chatClientResponse.chatResponse();

            usageAccumulator.addRoundResponse(chatResponse);

            if (chatResponse != null && chatResponse.hasToolCalls()) {
                return usageAccumulator.applyAccumulatedUsage(chatClientResponse);
            }

            String validationError = validate(chatResponse);

            if (validationError == null || attempt >= maxRepeatAttempts) {
                return usageAccumulator.applyAccumulatedUsage(chatClientResponse);
            }

            log.warn("JSON validation failed: {}", validationError);

            Prompt prompt = chatClientRequest.prompt();

            Prompt augmentedPrompt = prompt.augmentUserMessage(
                userMessage -> userMessage.mutate()
                    .text(
                        userMessage.getText() + System.lineSeparator() +
                            "Output JSON validation failed because of: " + validationError)
                    .build());

            attemptChatClientRequest = chatClientRequest.mutate()
                .prompt(augmentedPrompt)
                .build();
        }
    }

    @Override
    public String getName() {
        return "Tool Call Aware Structured Output Validation Advisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private @Nullable String validate(@Nullable ChatResponse chatResponse) {
        @Nullable
        Generation generation = chatResponse == null ? null : chatResponse.getResult();

        @Nullable
        String json = generation == null ? null : generation.getOutput()
            .getText();

        if (json == null || json.isBlank()) {
            return "Missing JSON output for validation.";
        }

        try {
            List<Error> errors = jsonSchema.validate(jsonMapper.readTree(json));

            if (errors.isEmpty()) {
                return null;
            }

            return errors.stream()
                .map(Error::getMessage)
                .collect(Collectors.joining("; "));
        } catch (JacksonException jacksonException) {
            return "Invalid JSON: " + jacksonException.getOriginalMessage();
        }
    }
}
