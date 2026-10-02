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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.tool.ToolSuspension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
public final class PiiTokenBoundaryToolCallingManager implements ToolCallingManager {

    private static final Logger log = LoggerFactory.getLogger(PiiTokenBoundaryToolCallingManager.class);

    private final ToolCallingManager delegate;
    private final SensitiveDataRedactor redactor;
    private final Supplier<@Nullable SensitiveDataMetrics> metricsSupplier;

    private PiiTokenBoundaryToolCallingManager(
        ToolCallingManager delegate, SensitiveDataRedactor redactor,
        Supplier<@Nullable SensitiveDataMetrics> metricsSupplier) {

        this.delegate = delegate;
        this.redactor = redactor;
        this.metricsSupplier = metricsSupplier;
    }

    public static ToolCallingManager wrap(
        ToolCallingManager delegate, SensitiveDataRedactor redactor,
        Supplier<@Nullable SensitiveDataMetrics> metricsSupplier) {

        return new PiiTokenBoundaryToolCallingManager(delegate, redactor, metricsSupplier);
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        ToolContext toolContext = toolContext(prompt);
        PiiTokenSession session = PiiTokenSessionToolContext.from(toolContext);

        if (session == null) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        SensitiveDataPolicy policy = resolvePolicy(toolContext);

        ToolExecutionResult result = delegate.executeToolCalls(
            withoutSession(prompt), restoreToolCallArguments(chatResponse, session, policy));

        return tokenizeConversationHistory(result, session, policy);
    }

    private static Prompt withoutSession(Prompt prompt) {
        if (!(prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return prompt;
        }

        Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();

        if (CollectionUtils.isEmpty(toolContext)) {
            return prompt;
        }

        Map<String, Object> strippedToolContext = PiiTokenSessionToolContext.without(toolContext);

        if (strippedToolContext == toolContext) {
            return prompt;
        }

        ChatOptions strippedChatOptions = toolCallingChatOptions.mutate()
            .toolContext(null)
            .toolContext(strippedToolContext)
            .build();

        return new Prompt(prompt.getInstructions(), strippedChatOptions);
    }

    private static @Nullable ToolContext toolContext(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
            && !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {

            return new ToolContext(toolCallingChatOptions.getToolContext());
        }

        return null;
    }

    private static SensitiveDataPolicy resolvePolicy(@Nullable ToolContext toolContext) {
        SensitiveDataPolicy policy = SensitiveDataPolicyToolContext.from(toolContext);

        return policy != null ? policy : SensitiveDataPolicy.DEFAULT;
    }

    private ChatResponse restoreToolCallArguments(
        ChatResponse chatResponse, PiiTokenSession session, SensitiveDataPolicy policy) {

        List<Generation> generations = chatResponse.getResults();

        if (generations.isEmpty()) {
            throw new IllegalStateException("Expected a generation in the ChatResponse passed to executeToolCalls");
        }

        boolean anyGenerationHasToolCalls = generations.stream()
            .anyMatch(generation -> !CollectionUtils.isEmpty(generation.getOutput()
                .getToolCalls()));

        if (!anyGenerationHasToolCalls) {
            return chatResponse;
        }

        if (!policy.restoreOutboundArguments()) {
            recordRestoreSuppressedIfAnyArgumentWasResolvable(generations, session);

            return chatResponse;
        }

        List<Generation> rebuiltGenerations = new ArrayList<>(generations.size());
        boolean anyArgumentsRestored = false;
        boolean anyArgumentUnresolved = false;

        for (Generation generation : generations) {
            AssistantMessage output = generation.getOutput();
            List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

            if (CollectionUtils.isEmpty(toolCalls)) {
                rebuiltGenerations.add(generation);

                continue;
            }

            List<AssistantMessage.ToolCall> restoredToolCalls = new ArrayList<>(toolCalls.size());

            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                PiiTokenSession.RestoreResult restoreResult = session.restoreWithUnresolvedCount(toolCall.arguments());
                String restoredArguments = restoreResult.text();

                if (!Objects.equals(restoredArguments, toolCall.arguments())) {
                    anyArgumentsRestored = true;
                }

                if (restoreResult.unresolvedCount() > 0) {
                    anyArgumentUnresolved = true;
                }

                restoredToolCalls.add(
                    new AssistantMessage.ToolCall(toolCall.id(), toolCall.type(), toolCall.name(), restoredArguments));
            }

            AssistantMessage rebuiltAssistantMessage = AssistantMessage.builder()
                .content(output.getText())
                .properties(output.getMetadata())
                .toolCalls(restoredToolCalls)
                .media(output.getMedia())
                .build();

            rebuiltGenerations.add(new Generation(rebuiltAssistantMessage, generation.getMetadata()));
        }

        SensitiveDataMetrics metrics = metricsSupplier.get();

        if (metrics != null) {
            if (anyArgumentsRestored) {
                metrics.recordToolArgsRestored();
            }

            if (anyArgumentUnresolved) {
                metrics.recordTokenUnresolved();
            }
        }

        return new ChatResponse(rebuiltGenerations, chatResponse.getMetadata());
    }

    private void recordRestoreSuppressedIfAnyArgumentWasResolvable(
        List<Generation> generations, PiiTokenSession session) {

        boolean anyArgumentWasResolvable = generations.stream()
            .flatMap(generation -> generation.getOutput()
                .getToolCalls()
                .stream())
            .anyMatch(toolCall -> !Objects.equals(
                session.restoreWithUnresolvedCount(toolCall.arguments())
                    .text(),
                toolCall.arguments()));

        if (!anyArgumentWasResolvable) {
            return;
        }

        SensitiveDataMetrics metrics = metricsSupplier.get();

        if (metrics != null) {
            metrics.recordRestoreSuppressed();
        }
    }

    private ToolExecutionResult tokenizeConversationHistory(
        ToolExecutionResult result, PiiTokenSession session, SensitiveDataPolicy policy) {
        List<Message> conversationHistory = result.conversationHistory();
        List<Message> tokenizedConversationHistory = new ArrayList<>(conversationHistory.size());
        boolean rewroteAMessage = false;
        boolean anyToolResultValueTokenized = false;
        boolean anyAssistantArgumentRetokenized = false;

        for (Message message : conversationHistory) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                List<ToolResponseMessage.ToolResponse> tokenizedResponses =
                    new ArrayList<>(toolResponseMessage.getResponses()
                        .size());

                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    if (ToolSuspension.hasSuspendedToolResultForm(response.responseData())) {
                        tokenizedResponses.add(response);

                        continue;
                    }

                    String tokenized = tokenizeOrRedact(response.responseData(), session, policy);

                    if (!Objects.equals(tokenized, response.responseData())) {
                        anyToolResultValueTokenized = true;
                    }

                    tokenizedResponses.add(new ToolResponseMessage.ToolResponse(response.id(), response.name(),
                        tokenized));
                }

                tokenizedConversationHistory.add(ToolResponseMessage.builder()
                    .responses(tokenizedResponses)
                    .metadata(toolResponseMessage.getMetadata())
                    .build());
                rewroteAMessage = true;
            } else if (message instanceof AssistantMessage assistantMessage
                && !CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {

                List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
                List<AssistantMessage.ToolCall> retokenizedToolCalls = new ArrayList<>(toolCalls.size());
                boolean thisMessageChanged = false;

                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    String retokenizedArguments = tokenizeOrRedact(toolCall.arguments(), session, policy);

                    if (!Objects.equals(retokenizedArguments, toolCall.arguments())) {
                        anyAssistantArgumentRetokenized = true;
                        thisMessageChanged = true;
                    }

                    retokenizedToolCalls.add(new AssistantMessage.ToolCall(
                        toolCall.id(), toolCall.type(), toolCall.name(), retokenizedArguments));
                }

                if (thisMessageChanged) {
                    tokenizedConversationHistory.add(AssistantMessage.builder()
                        .content(assistantMessage.getText())
                        .properties(assistantMessage.getMetadata())
                        .toolCalls(retokenizedToolCalls)
                        .build());
                    rewroteAMessage = true;
                } else {
                    tokenizedConversationHistory.add(message);
                }
            } else {
                tokenizedConversationHistory.add(message);
            }
        }

        SensitiveDataMetrics metrics = metricsSupplier.get();

        if (metrics != null) {
            if (anyToolResultValueTokenized) {
                metrics.recordToolResultTokenized();
            }

            if (anyAssistantArgumentRetokenized) {
                metrics.recordAssistantHistoryRetokenized();
            }
        }

        if (!rewroteAMessage) {
            return result;
        }

        return ToolExecutionResult.builder()
            .conversationHistory(tokenizedConversationHistory)
            .returnDirect(result.returnDirect())
            .build();
    }

    private String tokenizeOrRedact(String text, PiiTokenSession session, SensitiveDataPolicy policy) {
        try {
            return redactor
                .tokenizeWithSpans(text, policy.kinds(), session, policy.minConfidence(), metricsSupplier.get())
                .text();
        } catch (RuntimeException tokenizeException) {
            log.warn("Failed to tokenize outgoing conversation history text; falling back to redaction",
                tokenizeException);

            try {
                return redactor.redact(text, policy.kinds(), policy.minConfidence(), metricsSupplier.get());
            } catch (RuntimeException redactException) {
                log.warn(
                    "Failed to redact outgoing conversation history text after tokenization also failed; "
                        + "substituting empty text",
                    redactException);

                return "";
            }
        }
    }
}
