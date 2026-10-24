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

package com.bytechef.ai.copilot.tool.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
class TokenUsageLoggerAdvisorTest {

    private static final ChatClientRequest CHAT_CLIENT_REQUEST = new ChatClientRequest(new Prompt("hi"), Map.of());

    private final TokenUsageLoggerAdvisor tokenUsageLoggerAdvisor = new TokenUsageLoggerAdvisor("ai_hub");

    private ListAppender<ILoggingEvent> logAppender;
    private Level previousLevel;

    @BeforeEach
    void beforeEach() {
        Logger logger = (Logger) LoggerFactory.getLogger(TokenUsageLoggerAdvisor.class);

        previousLevel = logger.getLevel();

        logger.setLevel(Level.DEBUG);

        logAppender = new ListAppender<>();

        logAppender.start();

        logger.addAppender(logAppender);
    }

    @AfterEach
    void afterEach() {
        Logger logger = (Logger) LoggerFactory.getLogger(TokenUsageLoggerAdvisor.class);

        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
    }

    @Test
    void testAdviseCallLogsOneLineWithCacheCounts() {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        ChatClientResponse chatClientResponse = chatClientResponse(
            "claude-sonnet-4-6", "tool_use", new DefaultUsage(544, 168, 712, null, 4200L, 310L),
            List.of(toolCall("toolu_1", "getComponentDefinition"), toolCall("toolu_2", "getActionDefinition")));

        when(callAdvisorChain.nextCall(any())).thenReturn(chatClientResponse);

        assertThat(tokenUsageLoggerAdvisor.adviseCall(CHAT_CLIENT_REQUEST, callAdvisorChain))
            .isSameAs(chatClientResponse);
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                "Token usage agent=ai_hub model=claude-sonnet-4-6 finishReason=tool_use inputTokens=544 " +
                    "cacheReadInputTokens=4200 cacheWriteInputTokens=310 outputTokens=168 " +
                    "toolCalls=getComponentDefinition,getActionDefinition");
    }

    @Test
    void testAdviseStreamKeepsTheLargestCountOfEachChunkAndLogsOnceAtTheEnd() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.nextStream(any())).thenReturn(
            Flux.just(
                chatClientResponse(
                    "claude-sonnet-4-6", null, new DefaultUsage(544, 1, 545, null, 4200L, 310L),
                    List.of(toolCall("toolu_1", "validateWorkflow"))),
                chatClientResponse("", null, null, List.of(toolCall("toolu_1", "validateWorkflow"))),
                chatClientResponse(
                    null, "tool_use", new DefaultUsage(544, 168, 712, null, null, null), List.of())));

        List<ChatClientResponse> chatClientResponses = tokenUsageLoggerAdvisor
            .adviseStream(CHAT_CLIENT_REQUEST, streamAdvisorChain)
            .collectList()
            .block();

        assertThat(chatClientResponses).hasSize(3);
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                "Token usage agent=ai_hub model=claude-sonnet-4-6 finishReason=tool_use inputTokens=544 " +
                    "cacheReadInputTokens=4200 cacheWriteInputTokens=310 outputTokens=168 toolCalls=validateWorkflow");
    }

    @Test
    void testAdviseCallLogsNothingWhenTheModelReportsNoUsage() {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any())).thenReturn(
            chatClientResponse("claude-sonnet-4-6", "end_turn", null, List.of()));

        tokenUsageLoggerAdvisor.adviseCall(CHAT_CLIENT_REQUEST, callAdvisorChain);

        assertThat(logAppender.list).isEmpty();
    }

    private static ChatClientResponse chatClientResponse(
        @Nullable String model, @Nullable String finishReason, @Nullable DefaultUsage usage,
        List<AssistantMessage.ToolCall> toolCalls) {

        ChatResponseMetadata.Builder chatResponseMetadataBuilder = ChatResponseMetadata.builder();

        if (model != null) {
            chatResponseMetadataBuilder.model(model);
        }

        if (usage != null) {
            chatResponseMetadataBuilder.usage(usage);
        }

        ChatGenerationMetadata chatGenerationMetadata = finishReason == null
            ? ChatGenerationMetadata.NULL
            : ChatGenerationMetadata.builder()
                .finishReason(finishReason)
                .build();

        ChatResponse chatResponse = new ChatResponse(
            List.of(
                new Generation(
                    AssistantMessage.builder()
                        .content("text")
                        .toolCalls(toolCalls)
                        .build(),
                    chatGenerationMetadata)),
            chatResponseMetadataBuilder.build());

        return new ChatClientResponse(chatResponse, Map.of());
    }

    private static AssistantMessage.ToolCall toolCall(String id, String name) {
        return new AssistantMessage.ToolCall(id, "function", name, "{}");
    }
}
