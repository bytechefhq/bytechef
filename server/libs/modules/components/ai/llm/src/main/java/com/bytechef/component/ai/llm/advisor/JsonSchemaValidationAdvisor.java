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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.util.JacksonUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validates a structured output reply against a JSON schema and, when it does not match, asks the model again with the
 * validation error appended to the user message.
 *
 * <p>
 * Replaces {@link org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor}, which keeps calling
 * the model while a reply carries tool calls: its retry loop only ends on a successful validation, and it never
 * validates a tool-call reply. Behind a tool-calling advisor that turned every tool round into
 * {@code 1 + maxRepeatAttempts} model calls. This advisor returns a tool-call reply as is, so the tool-calling advisor
 * can run the tools, and validates only the final answer.
 *
 * @author Ivica Cardic
 */
public final class JsonSchemaValidationAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidationAdvisor.class);

    private static final int DEFAULT_MAX_REPEAT_ATTEMPTS = 3;
    private static final JsonMapper JSON_MAPPER = JacksonUtils.getDefaultJsonMapper();

    private final Schema jsonSchema;
    private final int maxRepeatAttempts;

    public JsonSchemaValidationAdvisor(String outputJsonSchema) {
        this(outputJsonSchema, DEFAULT_MAX_REPEAT_ATTEMPTS);
    }

    public JsonSchemaValidationAdvisor(String outputJsonSchema, int maxRepeatAttempts) {
        if (maxRepeatAttempts < 0) {
            throw new IllegalArgumentException("maxRepeatAttempts must be greater than or equal to 0");
        }

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

        this.jsonSchema = schemaRegistry.getSchema(JSON_MAPPER.readTree(outputJsonSchema));
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

            // A tool-call reply is not the final answer yet, so it goes back to the tool-calling advisor unvalidated.
            if (chatResponse != null && chatResponse.hasToolCalls()) {
                return usageAccumulator.applyAccumulatedUsage(chatClientResponse);
            }

            String errorMessage = validate(chatResponse);

            if (errorMessage == null || attempt == maxRepeatAttempts) {
                if (errorMessage != null) {
                    log.warn("Structured output is still invalid after {} attempts: {}", attempt + 1, errorMessage);
                }

                return usageAccumulator.applyAccumulatedUsage(chatClientResponse);
            }

            if (log.isDebugEnabled()) {
                log.debug("Structured output validation failed, asking the model again: {}", errorMessage);
            }

            String validationErrorMessage = "Output JSON validation failed because of: " + errorMessage;

            Prompt prompt = chatClientRequest.prompt();

            Prompt augmentedPrompt = prompt.augmentUserMessage(
                userMessage -> userMessage.mutate()
                    .text(userMessage.getText() + System.lineSeparator() + validationErrorMessage)
                    .build());

            attemptChatClientRequest = chatClientRequest.mutate()
                .prompt(augmentedPrompt)
                .build();
        }
    }

    @Override
    public String getName() {
        return JsonSchemaValidationAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 2000;
    }

    private @Nullable String validate(@Nullable ChatResponse chatResponse) {
        Generation generation = chatResponse == null ? null : chatResponse.getResult();

        if (generation == null) {
            return "Empty JSON output.";
        }

        AssistantMessage assistantMessage = generation.getOutput();

        String text = assistantMessage.getText();

        if (text == null || text.isBlank()) {
            return "Empty JSON output.";
        }

        try {
            JsonNode jsonNode = JSON_MAPPER.readTree(text);

            List<Error> errors = jsonSchema.validate(jsonNode);

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
