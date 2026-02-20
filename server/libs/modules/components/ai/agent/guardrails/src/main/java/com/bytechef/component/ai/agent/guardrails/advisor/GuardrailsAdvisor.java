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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_BLOCKED_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.MODE_CLASSIFY;

import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcher;
import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcher.KeywordMatchResult;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector.PiiMatch;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector.PiiPattern;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

/**
 * Guardrails advisor that intercepts chat requests and responses to validate content. In CLASSIFY mode, requests with
 * violations are blocked. In SANITIZE mode, the request continues but violations are logged.
 *
 * @author Ivica Cardic
 */
public class GuardrailsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "GuardrailsAdvisor";

    private final List<String> sensitiveWords;
    private final List<PiiPattern> piiPatterns;
    private final List<Pattern> customPatterns;
    private final String mode;
    private final boolean validateInput;
    private final boolean validateOutput;
    private final String blockedMessage;

    private GuardrailsAdvisor(Builder builder) {
        this.sensitiveWords = builder.sensitiveWords;
        this.piiPatterns = builder.piiPatterns;
        this.customPatterns = builder.customPatterns;
        this.mode = builder.mode;
        this.validateInput = builder.validateInput;
        this.validateOutput = builder.validateOutput;
        this.blockedMessage = builder.blockedMessage;
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
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // Validate input
        if (validateInput) {
            String userMessage = extractUserMessage(chatClientRequest);
            GuardrailsResult inputResult = validateContent(userMessage);

            if (inputResult.tripwireTriggered() && MODE_CLASSIFY.equals(mode)) {
                return createBlockedResponse();
            }
        }

        // Call downstream advisors and model
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // Validate output
        if (validateOutput) {
            String assistantMessage = extractAssistantMessage(response);
            GuardrailsResult outputResult = validateContent(assistantMessage);

            if (outputResult.tripwireTriggered()) {
                if (MODE_CLASSIFY.equals(mode)) {
                    return createBlockedResponse();
                }

                // Sanitize mode - mask content in response
                response = sanitizeResponse(response);
            }
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        // Validate input
        if (validateInput) {
            String userMessage = extractUserMessage(chatClientRequest);
            GuardrailsResult inputResult = validateContent(userMessage);

            if (inputResult.tripwireTriggered() && MODE_CLASSIFY.equals(mode)) {
                return Flux.just(createBlockedResponse());
            }
        }

        // For streaming, we validate output as the stream completes
        // Note: Full output validation in streaming mode requires buffering the entire response
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    private GuardrailsResult validateContent(String content) {
        if (content == null || content.isEmpty()) {
            return GuardrailsResult.passed("empty");
        }

        // Check keywords
        if (sensitiveWords != null && !sensitiveWords.isEmpty()) {
            KeywordMatchResult keywordResult = KeywordMatcher.match(content, sensitiveWords);

            if (keywordResult.matched()) {
                return GuardrailsResult.blocked(
                    "keywords",
                    1.0,
                    Map.of("matchedKeywords", keywordResult.matchedKeywords()));
            }
        }

        // Check PII
        if (piiPatterns != null && !piiPatterns.isEmpty()) {
            List<PiiMatch> piiMatches = PiiDetector.detect(content, piiPatterns);

            if (!piiMatches.isEmpty()) {
                List<String> matchedTypes = piiMatches.stream()
                    .map(PiiMatch::type)
                    .distinct()
                    .toList();

                return GuardrailsResult.blocked(
                    "pii",
                    1.0,
                    Map.of("matchedTypes", matchedTypes, "matchCount", piiMatches.size()));
            }
        }

        // Check custom patterns
        if (customPatterns != null && !customPatterns.isEmpty()) {
            for (Pattern pattern : customPatterns) {
                if (pattern.matcher(content)
                    .find()) {
                    return GuardrailsResult.blocked(
                        "customPattern",
                        1.0,
                        Map.of("pattern", pattern.pattern()));
                }
            }
        }

        return GuardrailsResult.passed("all");
    }

    private String extractUserMessage(ChatClientRequest request) {
        StringBuilder userContent = new StringBuilder();

        for (Message message : request.prompt()
            .getInstructions()) {
            if (message.getMessageType() == MessageType.USER) {
                userContent.append(message.getText());
            }
        }

        return userContent.toString();
    }

    private String extractAssistantMessage(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults()
            .isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();

        for (Generation generation : chatResponse.getResults()) {
            if (generation.getOutput() != null && generation.getOutput()
                .getText() != null) {
                content.append(generation.getOutput()
                    .getText());
            }
        }

        return content.toString();
    }

    private ChatClientResponse sanitizeResponse(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults()
            .isEmpty()) {
            return response;
        }

        List<Generation> sanitizedGenerations = chatResponse.getResults()
            .stream()
            .map(generation -> {
                if (generation.getOutput() != null && generation.getOutput()
                    .getText() != null) {
                    return new Generation(
                        new AssistantMessage(sanitizeContent(generation.getOutput()
                            .getText())),
                        generation.getMetadata());
                }

                return generation;
            })
            .toList();

        ChatResponse sanitizedChatResponse = ChatResponse.builder()
            .generations(sanitizedGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return ChatClientResponse.builder()
            .chatResponse(sanitizedChatResponse)
            .build();
    }

    private String sanitizeContent(String content) {
        String sanitized = content;

        // Mask PII
        if (piiPatterns != null && !piiPatterns.isEmpty()) {
            List<PiiMatch> piiMatches = PiiDetector.detect(sanitized, piiPatterns);
            sanitized = PiiDetector.mask(sanitized, piiMatches);
        }

        // Mask keywords
        if (sensitiveWords != null && !sensitiveWords.isEmpty()) {
            sanitized = KeywordMatcher.mask(sanitized, sensitiveWords);
        }

        return sanitized;
    }

    private ChatClientResponse createBlockedResponse() {
        String message = blockedMessage != null ? blockedMessage : DEFAULT_BLOCKED_MESSAGE;

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(message))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }

    /**
     * Builder for GuardrailsAdvisor.
     */
    public static class Builder {

        private List<String> sensitiveWords;
        private List<PiiPattern> piiPatterns;
        private List<Pattern> customPatterns;
        private String mode = MODE_CLASSIFY;
        private boolean validateInput = true;
        private boolean validateOutput = true;
        private String blockedMessage = DEFAULT_BLOCKED_MESSAGE;

        public Builder sensitiveWords(List<String> sensitiveWords) {
            this.sensitiveWords = sensitiveWords == null ? null : List.copyOf(sensitiveWords);

            return this;
        }

        public Builder piiPatterns(List<PiiPattern> piiPatterns) {
            this.piiPatterns = piiPatterns == null ? null : List.copyOf(piiPatterns);

            return this;
        }

        public Builder customPatterns(List<Pattern> customPatterns) {
            this.customPatterns = customPatterns == null ? null : List.copyOf(customPatterns);

            return this;
        }

        public Builder mode(String mode) {
            this.mode = mode;

            return this;
        }

        public Builder validateInput(boolean validateInput) {
            this.validateInput = validateInput;

            return this;
        }

        public Builder validateOutput(boolean validateOutput) {
            this.validateOutput = validateOutput;

            return this;
        }

        public Builder blockedMessage(String blockedMessage) {
            this.blockedMessage = blockedMessage;

            return this;
        }

        public GuardrailsAdvisor build() {
            return new GuardrailsAdvisor(this);
        }
    }
}
