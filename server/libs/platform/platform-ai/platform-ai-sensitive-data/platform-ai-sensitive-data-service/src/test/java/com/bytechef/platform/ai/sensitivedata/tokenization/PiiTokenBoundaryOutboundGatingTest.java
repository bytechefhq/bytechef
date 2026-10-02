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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class PiiTokenBoundaryOutboundGatingTest {

    @Test
    void testArgumentsAreRestoredUnderTheDefaultPolicy() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "ada@example.com");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(delegate.lastArguments()).contains("ada@example.com");
    }

    @Test
    void testArgumentsKeepTheirTokensWhenOutboundRestorationIsOff() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "ada@example.com");
        SensitiveDataPolicy outboundRestorationOffPolicy = new SensitiveDataPolicy(
            Set.of(SensitiveKind.PII, SensitiveKind.SECRET), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, false);

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(
            promptWithSessionAndPolicy(session, outboundRestorationOffPolicy),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(delegate.lastArguments()).doesNotContain("ada@example.com");
        assertThat(delegate.lastArguments()).contains("[PII_EMAIL_ADDRESS_1_");
    }

    @Test
    void testInboundToolResultTokenizationIsUnaffected() {
        PiiTokenSession session = PiiTokenSession.create();
        SensitiveDataPolicy outboundRestorationOffPolicy = new SensitiveDataPolicy(
            Set.of(SensitiveKind.PII, SensitiveKind.SECRET), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, false);

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"grace@example.com\"}"), redactor(), () -> null);

        ToolExecutionResult result =
            manager.executeToolCalls(promptWithSessionAndPolicy(session, outboundRestorationOffPolicy), anyToolCall());

        assertThat(lastResponseData(result)).doesNotContain("grace@example.com")
            .contains("[PII_EMAIL_ADDRESS_");
    }

    @Test
    void testASecretIsUnaffectedEitherWay() {
        PiiTokenSession session = PiiTokenSession.create();
        String secretArguments = "{\"key\":\"AKIAIOSFODNN7EXAMPLE\"}";
        SensitiveDataPolicy outboundRestorationOffPolicy = new SensitiveDataPolicy(
            Set.of(SensitiveKind.PII, SensitiveKind.SECRET), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, false);

        RecordingToolCallingManager delegateWithRestorationOn = new RecordingToolCallingManager();
        ToolCallingManager managerWithRestorationOn =
            PiiTokenBoundaryToolCallingManager.wrap(delegateWithRestorationOn, redactor(), () -> null);

        managerWithRestorationOn.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("configure", secretArguments));

        RecordingToolCallingManager delegateWithRestorationOff = new RecordingToolCallingManager();
        ToolCallingManager managerWithRestorationOff =
            PiiTokenBoundaryToolCallingManager.wrap(delegateWithRestorationOff, redactor(), () -> null);

        managerWithRestorationOff.executeToolCalls(
            promptWithSessionAndPolicy(session, outboundRestorationOffPolicy),
            chatResponseWithToolCall("configure", secretArguments));

        assertThat(delegateWithRestorationOn.lastArguments()).isEqualTo(secretArguments);
        assertThat(delegateWithRestorationOff.lastArguments()).isEqualTo(secretArguments);
    }

    private static ToolCallingManager returningResult(String responseData) {
        return new ReturningResultToolCallingManager(responseData);
    }

    private static ChatResponse anyToolCall() {
        return chatResponseWithToolCall("lookupCustomer", "{}");
    }

    private static String lastResponseData(ToolExecutionResult result) {
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) result.conversationHistory()
            .getLast();

        return toolResponseMessage.getResponses()
            .getFirst()
            .responseData();
    }

    private static SensitiveDataRedactor redactor() {
        return new SensitiveDataRedactor(
            List.of(literalDetector("email", SensitiveKind.PII, "EMAIL_ADDRESS", "ada@example.com"),
                literalDetector("email2", SensitiveKind.PII, "EMAIL_ADDRESS", "grace@example.com"),
                literalDetector("aws-key", SensitiveKind.SECRET, "AWS_ACCESS_KEY", "AKIAIOSFODNN7EXAMPLE")));
    }

    private static SensitiveDataDetector literalDetector(
        String name, SensitiveKind kind, String category, String literal) {

        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                int start = text.indexOf(literal);

                if (start < 0) {
                    return List.of();
                }

                return List.of(new SensitiveSpan(kind, category, start, start + literal.length(), 0.9));
            }
        };
    }

    private static Prompt promptWithSession(PiiTokenSession session) {
        return new Prompt(
            List.of(),
            ToolCallingChatOptions.builder()
                .toolContext(PiiTokenSessionToolContext.into(Map.of(), session))
                .build());
    }

    private static Prompt promptWithSessionAndPolicy(PiiTokenSession session, SensitiveDataPolicy policy) {
        Map<String, Object> toolContext = SensitiveDataPolicyToolContext.into(
            PiiTokenSessionToolContext.into(Map.of(), session), policy);

        return new Prompt(
            List.of(),
            ToolCallingChatOptions.builder()
                .toolContext(toolContext)
                .build());
    }

    private static ChatResponse chatResponseWithToolCall(String toolName, String arguments) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCall("call-1", toolName, arguments)))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private static AssistantMessage.ToolCall toolCall(String id, String toolName, String arguments) {
        return new AssistantMessage.ToolCall(id, "function", toolName, arguments);
    }

    private static AssistantMessage assistantMessageOf(ChatResponse chatResponse) {
        Generation generation = chatResponse.getResult();

        if (generation == null) {
            throw new IllegalStateException("Expected a generation in the ChatResponse passed to executeToolCalls");
        }

        return generation.getOutput();
    }

    private static final class ReturningResultToolCallingManager implements ToolCallingManager {

        private final String responseData;

        private ReturningResultToolCallingManager(String responseData) {
            this.responseData = responseData;
        }

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
                "call-1", "lookupCustomer", responseData);
            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                .responses(List.of(response))
                .build();

            return ToolExecutionResult.builder()
                .conversationHistory(List.of(assistantMessageOf(chatResponse), toolResponseMessage))
                .build();
        }
    }

    private static final class RecordingToolCallingManager implements ToolCallingManager {

        private @Nullable ChatResponse lastChatResponse;

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            lastChatResponse = chatResponse;

            AssistantMessage assistantMessage = assistantMessageOf(chatResponse);
            List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());

            conversationHistory.add(assistantMessage);
            conversationHistory.add(inertToolResponseFor(assistantMessage));

            return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .build();
        }

        private String lastArguments() {
            ChatResponse chatResponse = lastChatResponse;

            if (chatResponse == null) {
                throw new IllegalStateException("executeToolCalls was never called");
            }

            Generation generation = chatResponse.getResult();

            if (generation == null) {
                throw new IllegalStateException("Expected a generation in the recorded ChatResponse");
            }

            return generation.getOutput()
                .getToolCalls()
                .getFirst()
                .arguments();
        }

        private static ToolResponseMessage inertToolResponseFor(AssistantMessage assistantMessage) {
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>(assistantMessage.getToolCalls()
                .size());

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                responses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), "ok"));
            }

            return ToolResponseMessage.builder()
                .responses(responses)
                .build();
        }
    }
}
