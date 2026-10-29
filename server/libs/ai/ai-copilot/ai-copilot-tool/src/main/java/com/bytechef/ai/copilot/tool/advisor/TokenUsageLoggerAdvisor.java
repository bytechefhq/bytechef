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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

/**
 * Logs the token usage of every model call an agent makes, one line per call, at DEBUG. It sits innermost in the
 * advisor chain, so each round of a tool-calling loop is logged on its own. A streamed call reports usage across its
 * chunks, so the largest value of each count is kept and logged once the stream ends.
 *
 * @author Ivica Cardic
 */
public class TokenUsageLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageLoggerAdvisor.class);

    private final String agentName;

    public TokenUsageLoggerAdvisor(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        if (log.isDebugEnabled()) {
            TokenUsage tokenUsage = new TokenUsage();

            tokenUsage.add(chatClientResponse.chatResponse());

            log(tokenUsage);
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        if (!log.isDebugEnabled()) {
            return chatClientResponses;
        }

        TokenUsage tokenUsage = new TokenUsage();

        return chatClientResponses
            .doOnNext(chatClientResponse -> tokenUsage.add(chatClientResponse.chatResponse()))
            .doFinally(signalType -> log(tokenUsage));
    }

    @Override
    public String getName() {
        return "TokenUsageLoggerAdvisor";
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private void log(TokenUsage tokenUsage) {
        if (!tokenUsage.reported) {
            return;
        }

        log.debug(
            "Token usage agent={} model={} finishReason={} inputTokens={} cacheReadInputTokens={} " +
                "cacheWriteInputTokens={} outputTokens={} toolCalls={}",
            agentName, tokenUsage.model, tokenUsage.finishReason, tokenUsage.inputTokens,
            tokenUsage.cacheReadInputTokens, tokenUsage.cacheWriteInputTokens, tokenUsage.outputTokens,
            String.join(",", tokenUsage.toolCallNames.values()));
    }

    private static final class TokenUsage {

        private long cacheReadInputTokens;
        private long cacheWriteInputTokens;
        private @Nullable String finishReason;
        private long inputTokens;
        private @Nullable String model;
        private long outputTokens;
        private boolean reported;
        private final Map<String, String> toolCallNames = new LinkedHashMap<>();

        private void add(@Nullable ChatResponse chatResponse) {
            if (chatResponse == null) {
                return;
            }

            ChatResponseMetadata chatResponseMetadata = chatResponse.getMetadata();

            String responseModel = chatResponseMetadata.getModel();

            if (responseModel != null && !responseModel.isEmpty()) {
                model = responseModel;
            }

            for (Generation resultGeneration : chatResponse.getResults()) {
                for (AssistantMessage.ToolCall toolCall : resultGeneration.getOutput()
                    .getToolCalls()) {

                    toolCallNames.put(toolCall.id(), toolCall.name());
                }
            }

            Generation generation = chatResponse.getResult();

            if (generation != null) {
                String generationFinishReason = generation.getMetadata()
                    .getFinishReason();

                if (generationFinishReason != null && !generationFinishReason.isEmpty()) {
                    finishReason = generationFinishReason;
                }
            }

            Usage usage = chatResponseMetadata.getUsage();

            if (usage == null || usage instanceof EmptyUsage) {
                return;
            }

            reported = true;

            inputTokens = max(inputTokens, usage.getPromptTokens());
            outputTokens = max(outputTokens, usage.getCompletionTokens());
            cacheReadInputTokens = max(cacheReadInputTokens, usage.getCacheReadInputTokens());
            cacheWriteInputTokens = max(cacheWriteInputTokens, usage.getCacheWriteInputTokens());
        }

        private static long max(long current, @Nullable Number candidate) {
            return candidate == null ? current : Math.max(current, candidate.longValue());
        }
    }
}
