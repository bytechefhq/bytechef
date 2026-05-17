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

package com.bytechef.component.ai.agent.guardrails.advisor;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_VALIDATE_OUTPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SANITIZE_WITHHELD_KEY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;

import com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import reactor.core.publisher.Flux;

/**
 * Spring-AI advisor that runs sanitizers over the user message (input) and the assistant message (output). PREFLIGHT
 * (rule-based) sanitizers run first and mask their entities; LLM-based sanitizers run afterwards on the already-masked
 * text. Any exception escaping a sanitizer triggers a withheld-placeholder response.
 *
 * <p>
 * Streaming: {@link #adviseStream} sanitizes per chunk — cross-chunk patterns are not masked and chunks already shipped
 * cannot be recalled. Use {@link #adviseCall} when full-text guarantees are required.
 *
 * @author Ivica Cardic
 */
public final class SanitizeTextAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "SanitizeTextAdvisor";
    private static final String SANITIZER_FAILURE_PLACEHOLDER = "[sanitizer failed — response withheld]";

    private final List<SanitizerEntry> sanitizers;
    private final Context context;

    private SanitizeTextAdvisor(Builder builder) {
        this.sanitizers = List.copyOf(builder.sanitizers);
        this.context = Objects.requireNonNull(builder.context, "context");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest sanitizedRequest;

        try {
            sanitizedRequest = sanitizeRequest(request);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            return withheldResponse(request);
        }

        ChatClientResponse response;

        try {
            response = chain.nextCall(sanitizedRequest);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            return withheldResponse(request);
        }

        try {
            return rewriteResponse(response);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            return withheldResponse(request);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest sanitizedRequest;

        try {
            sanitizedRequest = sanitizeRequest(request);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            return Flux.just(withheldResponse(request));
        }

        return chain.nextStream(sanitizedRequest)
            .map(this::rewriteResponse)
            .onErrorResume(error -> {
                LlmClassifierUtils.restoreInterruptIfWrapped(error);

                return Flux.just(withheldResponse(request));
            });
    }

    String sanitizeForTesting(String text) {
        return sanitize(text, false);
    }

    private static ChatClientResponse withheldResponse(ChatClientRequest request) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(SANITIZER_FAILURE_PLACEHOLDER))))
            .metadata(SANITIZE_WITHHELD_KEY, Boolean.TRUE)
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();
    }

    private ChatClientResponse rewriteResponse(ChatClientResponse response) {
        if (sanitizers.isEmpty()) {
            return response;
        }

        boolean anyOutputSanitizer = sanitizers.stream()
            .anyMatch(entry -> entry.context.inputParameters()
                .getBoolean(VALIDATE_OUTPUT, DEFAULT_VALIDATE_OUTPUT));

        if (!anyOutputSanitizer) {
            return response;
        }

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        List<Generation> results = chatResponse.getResults();
        List<Generation> rewrittenGenerations = new ArrayList<>(results.size());

        for (Generation generation : results) {
            AssistantMessage original = generation.getOutput();

            if (original == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String originalText = original.getText();

            if (originalText == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String sanitized = sanitize(originalText, false);

            AssistantMessage rewrittenAssistantMessage = AssistantMessage.builder()
                .content(sanitized)
                .properties(original.getMetadata())
                .toolCalls(original.getToolCalls())
                .media(original.getMedia())
                .build();

            rewrittenGenerations.add(new Generation(rewrittenAssistantMessage, generation.getMetadata()));
        }

        ChatResponse rewrittenChatResponse = ChatResponse.builder()
            .generations(rewrittenGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(rewrittenChatResponse)
            .build();
    }

    private String sanitize(String text, boolean isInput) {
        Map<String, Throwable> closedFailures = new LinkedHashMap<>();
        String intermediate = text;
        MaskEntityMapUtils maskEntities = new MaskEntityMapUtils(context);

        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.PREFLIGHT || !shouldRun(entry, isInput)) {
                continue;
            }

            try {
                if (entry.function instanceof PreflightMasking masking) {
                    MaskResult maskResult = masking.mask(intermediate, entry.context);

                    switch (maskResult) {
                        case MaskResult.Entities entitiesResult -> maskEntities.merge(entitiesResult.entities());
                        case MaskResult.Masked maskedResult -> intermediate = maskedResult.text();
                        case MaskResult.Unchanged ignored -> {
                        }
                    }
                } else {
                    String preflightResult = entry.function.apply(intermediate, entry.context);

                    if (preflightResult == null) {
                        closedFailures.put(entry.sanitizerName,
                            new IllegalStateException("Sanitizer returned null"));
                    } else {
                        intermediate = preflightResult;
                    }
                }
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

                closedFailures.put(entry.sanitizerName, throwable);
            }
        }

        if (!maskEntities.isEmpty()) {
            intermediate = maskEntities.applyTo(intermediate);
        }

        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.LLM || !shouldRun(entry, isInput)) {
                continue;
            }

            intermediate = collectingApply(entry, intermediate, closedFailures);
        }

        if (!closedFailures.isEmpty()) {
            throw new SanitizerExecutionFailureException(closedFailures);
        }

        return intermediate;
    }

    private ChatClientRequest sanitizeRequest(ChatClientRequest request) {
        boolean anyInputSanitizer = sanitizers.stream()
            .anyMatch(entry -> entry.context.inputParameters()
                .getBoolean(VALIDATE_INPUT, DEFAULT_VALIDATE_INPUT));

        if (!anyInputSanitizer) {
            return request;
        }

        Prompt prompt = request.prompt();
        List<Message> messages = prompt.getInstructions();
        List<Message> sanitizedMessages = new ArrayList<>(messages.size());
        boolean modified = false;

        for (Message message : messages) {
            if (message.getMessageType() != MessageType.USER) {
                sanitizedMessages.add(message);

                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                sanitizedMessages.add(message);

                continue;
            }

            String sanitizedText = sanitize(text, true);

            if (!sanitizedText.equals(text)) {
                sanitizedMessages.add(rewriteUserMessage(message, sanitizedText));

                modified = true;
            } else {
                sanitizedMessages.add(message);
            }
        }

        if (!modified) {
            return request;
        }

        Prompt newPrompt = new Prompt(sanitizedMessages, prompt.getOptions());

        return request.mutate()
            .prompt(newPrompt)
            .build();
    }

    private static UserMessage rewriteUserMessage(Message original, String sanitizedText) {
        UserMessage.Builder builder = UserMessage.builder()
            .text(sanitizedText);

        Map<String, Object> metadata = original.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            builder.metadata(metadata);
        }

        if (original instanceof UserMessage userMessage) {
            List<Media> media = userMessage.getMedia();

            if (media != null && !media.isEmpty()) {
                builder.media(media);
            }
        }

        return builder.build();
    }

    private static boolean shouldRun(SanitizerEntry entry, boolean isInput) {
        Parameters parameters = entry.context.inputParameters();

        if (isInput) {
            return parameters.getBoolean(VALIDATE_INPUT, DEFAULT_VALIDATE_INPUT);
        }

        return parameters.getBoolean(VALIDATE_OUTPUT, DEFAULT_VALIDATE_OUTPUT);
    }

    private static String collectingApply(
        SanitizerEntry entry, String text, Map<String, Throwable> closedFailures) {

        try {
            String result = entry.function.apply(text, entry.context);

            if (result == null) {
                closedFailures.put(entry.sanitizerName,
                    new IllegalStateException("Sanitizer returned null"));

                return text;
            }

            return result;
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            closedFailures.put(entry.sanitizerName, throwable);

            return text;
        }
    }

    public static final class Builder {

        private final List<PendingSanitizer> pendingSanitizers = new ArrayList<>();
        private final List<SanitizerEntry> sanitizers = new ArrayList<>();
        private Context context;

        public Builder context(Context value) {
            this.context = value;

            return this;
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters) {

            return add(sanitizerName, function, inputParameters, connectionParameters, null, null, Map.of(), null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, null, extensions,
                componentConnections, null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, null, extensions,
                componentConnections, chatClient);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, parentParameters, extensions,
                componentConnections, null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            pendingSanitizers.add(new PendingSanitizer(
                sanitizerName, function, inputParameters, connectionParameters, parentParameters, extensions,
                componentConnections, chatClient));

            return this;
        }

        public SanitizeTextAdvisor build() {
            Objects.requireNonNull(context, "context");

            for (PendingSanitizer pending : pendingSanitizers) {
                GuardrailContext guardrailContext = GuardrailContext.builder()
                    .inputParameters(pending.inputParameters)
                    .connectionParameters(pending.connectionParameters)
                    .parentParameters(pending.parentParameters)
                    .extensions(pending.extensions)
                    .componentConnections(pending.componentConnections)
                    .chatClient(pending.chatClient)
                    .context(context)
                    .build();

                sanitizers.add(new SanitizerEntry(pending.sanitizerName, pending.function, guardrailContext));
            }

            return new SanitizeTextAdvisor(this);
        }
    }

    private record PendingSanitizer(
        String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
        Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {
    }

    private record SanitizerEntry(
        String sanitizerName, GuardrailSanitizerFunction function, GuardrailContext context) {
    }
}
