/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.commons.util.NumberUtils;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageContext;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Observes the model's token usage on every AI Hub turn and (a) records it into the unified {@code ai_llm_usage}
 * metering store via {@link LlmUsageRecorder} with {@code source = AI_HUB}, and (b) logs it at DEBUG — including the
 * provider's native prompt-cache counters — so prompt-caching effectiveness can be measured directly.
 *
 * <p>
 * Recording context comes from the advisor request context: the per-turn workspace and user ids are published by
 * {@code AiHubSpringAIAgent.advisorParams} from the controller-verified state keys, and the AG-UI thread id rides along
 * as metadata via {@link ChatMemory#CONVERSATION_ID}. A turn with no workspace context (e.g. a unit-test ChatClient) is
 * skipped silently — metering must never fail a chat turn, and {@link LlmUsageRecorder} implementations already swallow
 * their own persistence failures.
 * </p>
 *
 * <p>
 * Streaming: the streaming usage aggregator ({@code MessageAggregator.DefaultUsage}) collapses usage into a 3-key map
 * that drops the provider's prompt-cache counters, so this advisor does NOT aggregate. It taps the raw chunks and
 * accumulates the maximum prompt/completion counters seen (providers report cumulative usage on streaming chunks —
 * Anthropic sends prompt tokens on message_start and cumulative output tokens on message_delta), recording once on
 * stream completion. The model-level chunk usage also carries the provider's native usage object, which is logged at
 * DEBUG as the ground truth for cache effectiveness; if no model-level usage survives to the advisor, that fact is
 * itself logged so the measurement gap is visible rather than silent.
 * </p>
 *
 * <p>
 * Debug logging is gated on this class's logger at DEBUG; the default INFO root level means zero logging overhead in
 * production. Enable with {@code logging.level.com.bytechef.ee.ai.hub.agent.AiHubModelUsageAdvisor=DEBUG}.
 * </p>
 *
 * <p>
 * Registered for every AI Hub agent (ASK + BUILD) in {@link AiHubSpringAIAgent.Builder#build()}. Because the base
 * {@code SpringAIAgent} attaches advisors to the per-request spec rather than baking them into a single ChatClient,
 * this advisor also observes turns served by a per-request override ChatClient (user-selected or personal-agent model),
 * not only the workspace-default client.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubModelUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiHubModelUsageAdvisor.class);

    private static final String UNKNOWN_MODEL = "unknown";

    /**
     * Innermost so it observes the actual model response usage after every other request rewrite has settled. Ties with
     * {@code SimpleLoggerAdvisor} (also {@code Integer.MAX_VALUE}) are harmless — both are read-only observers.
     */
    private static final int ORDER = Integer.MAX_VALUE;

    private final @Nullable String agentName;
    private final @Nullable LlmUsageRecorder llmUsageRecorder;

    public AiHubModelUsageAdvisor(@Nullable String agentName, @Nullable LlmUsageRecorder llmUsageRecorder) {
        this.agentName = agentName;
        this.llmUsageRecorder = llmUsageRecorder;
    }

    @Override
    public String getName() {
        return "ai-hub-model-usage";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        long startedAt = System.currentTimeMillis();

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        Usage usage = extractUsage(chatClientResponse);

        logUsage("call", usage);
        record(
            chatClientRequest.context(), promptTokens(usage), completionTokens(usage),
            extractModel(chatClientResponse), System.currentTimeMillis() - startedAt);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        long startedAt = System.currentTimeMillis();

        // Do NOT aggregate the stream here. The streaming aggregator (MessageAggregator.DefaultUsage) collapses usage
        // into a 3-key map that drops the provider's prompt-cache counters (cache_read / cache_creation), making
        // caching unobservable. Instead, tap the raw chunks: chunk usage counters are cumulative, so the maximum seen
        // across the stream is the turn's total; the model-level chunk usage still carries the provider's native usage
        // object (a non-Map), which is the ground truth for cache effectiveness. Log it when it appears.
        AtomicBoolean loggedModelUsage = new AtomicBoolean(false);
        AtomicLong maxPromptTokens = new AtomicLong();
        AtomicLong maxCompletionTokens = new AtomicLong();
        AtomicReference<String> lastModel = new AtomicReference<>();

        return streamAdvisorChain.nextStream(chatClientRequest)
            .doOnNext(chatClientResponse -> {
                Usage usage = extractUsage(chatClientResponse);

                if (usage == null) {
                    return;
                }

                maxPromptTokens.accumulateAndGet(promptTokens(usage), Math::max);
                maxCompletionTokens.accumulateAndGet(completionTokens(usage), Math::max);

                String model = extractModel(chatClientResponse);

                if (model != null && !model.isBlank()) {
                    lastModel.set(model);
                }

                if (log.isDebugEnabled() && !(usage.getNativeUsage() instanceof Map)) {
                    logUsage("stream", usage);

                    loggedModelUsage.set(true);
                }
            })
            .doOnComplete(() -> {
                if (log.isDebugEnabled() && !loggedModelUsage.get()) {
                    log.debug(
                        "AI Hub model usage (stream) — no model-level usage reached the advisor; the stream "
                            + "aggregator collapsed it to a cache-less map, so prompt-cache effectiveness is not "
                            + "observable at this layer.");
                }

                record(
                    chatClientRequest.context(), (int) maxPromptTokens.get(), (int) maxCompletionTokens.get(),
                    lastModel.get(), System.currentTimeMillis() - startedAt);
            });
    }

    private void record(
        Map<String, Object> context, int promptTokens, int completionTokens, @Nullable String model, long durationMs) {

        if (llmUsageRecorder == null || (promptTokens <= 0 && completionTokens <= 0)) {
            return;
        }

        Long workspaceId = NumberUtils.asLong(context.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID));

        if (workspaceId == null || workspaceId <= 0) {
            return;
        }

        Long userId = NumberUtils.asLong(context.get(AiHubStateKeys.AUTHENTICATED_USER_ID));
        Object threadId = context.get(ChatMemory.CONVERSATION_ID);

        LlmUsageContext llmUsageContext = new LlmUsageContext(
            workspaceId, userId, LlmUsageSource.AI_HUB, null, agentName, null,
            threadId == null ? null : Map.of("threadId", String.valueOf(threadId)));

        llmUsageRecorder.recordLlm(
            llmUsageContext, model == null || model.isBlank() ? UNKNOWN_MODEL : model, promptTokens, completionTokens,
            durationMs);
    }

    private static int promptTokens(@Nullable Usage usage) {
        if (usage == null || usage.getPromptTokens() == null) {
            return 0;
        }

        return usage.getPromptTokens();
    }

    private static int completionTokens(@Nullable Usage usage) {
        if (usage == null || usage.getCompletionTokens() == null) {
            return 0;
        }

        return usage.getCompletionTokens();
    }

    private static @Nullable String extractModel(@Nullable ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null) {
            return null;
        }

        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return null;
        }

        return chatResponse.getMetadata()
            .getModel();
    }

    private static @Nullable Usage extractUsage(@Nullable ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null) {
            return null;
        }

        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return null;
        }

        return chatResponse.getMetadata()
            .getUsage();
    }

    private void logUsage(String phase, @Nullable Usage usage) {
        if (!log.isDebugEnabled() || usage == null) {
            return;
        }

        Object nativeUsage = usage.getNativeUsage();

        log.debug(
            "AI Hub model usage ({}) — promptTokens={}, completionTokens={}, totalTokens={}, usageClass={}, "
                + "nativeUsageClass={}, nativeUsage={}",
            phase, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens(),
            usage.getClass()
                .getName(),
            nativeUsage == null ? "null" : nativeUsage.getClass()
                .getName(),
            nativeUsage);
    }
}
