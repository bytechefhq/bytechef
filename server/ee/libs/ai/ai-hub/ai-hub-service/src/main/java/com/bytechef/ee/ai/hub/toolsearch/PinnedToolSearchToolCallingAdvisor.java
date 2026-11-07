/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.commons.util.NumberUtils;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.context.EnvironmentContextThreadLocalAccessor;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.eviction.LruEvictionStrategy;
import org.springframework.core.io.DefaultResourceLoader;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PinnedToolSearchToolCallingAdvisor extends ToolSearchToolCallingAdvisor {

    private static final Logger log = LoggerFactory.getLogger(PinnedToolSearchToolCallingAdvisor.class);

    private static final String PINNED_TOOL_CALLBACKS_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".pinnedToolCallbacks";

    private static final String TOOL_SEARCH_UNAVAILABLE_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".toolSearchUnavailable";

    private static final String BASE_CACHED_TOOL_CALLBACKS_KEY =
        ToolSearchToolCallingAdvisor.class.getName() + ".cachedToolCallbacks";

    private final Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier;
    private final Runnable catalogWarmUp;
    private final AtomicBoolean toolSearchUnavailableLogged = new AtomicBoolean();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PinnedToolSearchToolCallingAdvisor(
        ToolCallingManager toolCallingManager, ToolIndex toolIndex, int maxResults, String sessionIdKeyName,
        Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier, Runnable catalogWarmUp) {

        super(
            toolCallingManager, DEFAULT_ORDER, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, toolIndex,
            loadDefaultSystemMessageSuffix(), true, maxResults, false, sessionIdKeyName,
            new LruEvictionStrategy(1000));

        this.catalogToolCallbacksSupplier = catalogToolCallbacksSupplier;
        this.catalogWarmUp = catalogWarmUp;
    }

    @Override
    protected ChatClientRequest doInitializeLoop(
        ChatClientRequest chatClientRequest,
        CallAdvisorChain callAdvisorChain) {

        catalogWarmUp.run();

        ChatClientRequest initialized;

        try {
            initialized = super.doInitializeLoop(chatClientRequest, callAdvisorChain);
        } catch (RuntimeException exception) {
            return markToolSearchUnavailable(chatClientRequest, exception);
        }

        seedCatalogToolCallbacks(initialized);

        return capturePinnedToolCallbacks(initialized);
    }

    @Override
    protected ChatClientRequest doInitializeLoopStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        catalogWarmUp.run();

        ChatClientRequest initialized;

        try {
            initialized = runWithEnvironment(
                chatClientRequest, () -> super.doInitializeLoopStream(chatClientRequest, streamAdvisorChain));
        } catch (RuntimeException exception) {
            return markToolSearchUnavailable(chatClientRequest, exception);
        }

        seedCatalogToolCallbacks(initialized);

        return capturePinnedToolCallbacks(initialized);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Environment environment = resolveEnvironment(chatClientRequest);

        Flux<ChatClientResponse> responseFlux = super.adviseStream(chatClientRequest, streamAdvisorChain);

        if (environment == null) {
            return responseFlux;
        }

        return responseFlux.contextWrite(
            context -> context.put(EnvironmentContextThreadLocalAccessor.KEY, environment));
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return runWithEnvironment(chatClientRequest, () -> super.adviseCall(chatClientRequest, callAdvisorChain));
    }

    private <T> T runWithEnvironment(ChatClientRequest chatClientRequest, Supplier<T> action) {
        Environment environment = resolveEnvironment(chatClientRequest);

        if (environment == null) {
            return action.get();
        }

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(environment);

        try {
            return action.get();
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    private @Nullable Environment resolveEnvironment(ChatClientRequest chatClientRequest) {
        Long environmentId = NumberUtils.asLong(
            chatClientRequest.context()
                .get(AiHubStateKeys.ENVIRONMENT_ID));

        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            return null;
        }

        return Environment.values()[environmentId.intValue()];
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        if (isToolSearchUnavailable(chatClientRequest)) {
            return chatClientRequest;
        }

        return injectPinnedToolCallbacks(super.doBeforeCall(chatClientRequest, callAdvisorChain));
    }

    @Override
    protected ChatClientRequest doBeforeStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        if (isToolSearchUnavailable(chatClientRequest)) {
            return chatClientRequest;
        }

        return injectPinnedToolCallbacks(super.doBeforeStream(chatClientRequest, streamAdvisorChain));
    }

    private ChatClientRequest markToolSearchUnavailable(
        ChatClientRequest chatClientRequest, RuntimeException exception) {

        if (toolSearchUnavailableLogged.compareAndSet(false, true)) {
            log.warn(
                "AI Hub tool search is unavailable, continuing the turn with the pinned tools only: {}",
                exception.getMessage(), exception);
        } else if (log.isDebugEnabled()) {
            log.debug("AI Hub tool search is unavailable: {}", exception.getMessage());
        }

        chatClientRequest.context()
            .put(TOOL_SEARCH_UNAVAILABLE_KEY, Boolean.TRUE);

        return chatClientRequest;
    }

    private static boolean isToolSearchUnavailable(ChatClientRequest chatClientRequest) {
        return Boolean.TRUE.equals(
            chatClientRequest.context()
                .get(TOOL_SEARCH_UNAVAILABLE_KEY));
    }

    private void seedCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
        Map<String, ToolCallback> catalogToolCallbacks = catalogToolCallbacksSupplier.get();

        if (catalogToolCallbacks.isEmpty()) {
            return;
        }

        Object cached = chatClientRequest.context()
            .get(BASE_CACHED_TOOL_CALLBACKS_KEY);

        if (!(cached instanceof Map<?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, ToolCallback> cachedToolCallbacks = (Map<String, ToolCallback>) cached;

        for (Map.Entry<String, ToolCallback> entry : catalogToolCallbacks.entrySet()) {
            cachedToolCallbacks.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private ChatClientRequest capturePinnedToolCallbacks(ChatClientRequest chatClientRequest) {
        if (chatClientRequest.prompt()
            .getOptions() instanceof ToolCallingChatOptions toolOptions) {

            List<ToolCallback> configuredToolCallbacks = toolOptions.getToolCallbacks();

            if (configuredToolCallbacks != null) {
                chatClientRequest.context()
                    .put(PINNED_TOOL_CALLBACKS_KEY, new ArrayList<>(configuredToolCallbacks));
            }
        }

        return chatClientRequest;
    }

    private ChatClientRequest injectPinnedToolCallbacks(ChatClientRequest chatClientRequest) {
        if (!(chatClientRequest.prompt()
            .getOptions() instanceof ToolCallingChatOptions toolOptions)) {

            return chatClientRequest;
        }

        Object pinnedObject = chatClientRequest.context()
            .get(PINNED_TOOL_CALLBACKS_KEY);

        if (!(pinnedObject instanceof List<?> pinnedToolCallbacks) || pinnedToolCallbacks.isEmpty()) {
            return chatClientRequest;
        }

        List<ToolCallback> currentToolCallbacks = toolOptions.getToolCallbacks();

        List<ToolCallback> toolCallbacks =
            currentToolCallbacks == null ? new ArrayList<>() : new ArrayList<>(currentToolCallbacks);

        Set<String> presentToolNames = new HashSet<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            presentToolNames.add(toolCallback.getToolDefinition()
                .name());
        }

        boolean changed = false;

        for (Object pinned : pinnedToolCallbacks) {
            ToolCallback toolCallback = (ToolCallback) pinned;

            if (presentToolNames.add(toolCallback.getToolDefinition()
                .name())) {

                toolCallbacks.add(toolCallback);

                changed = true;
            }
        }

        if (!changed) {
            return chatClientRequest;
        }

        ToolCallingChatOptions toolOptionsCopy = ((ToolCallingChatOptions.Builder<?>) toolOptions.mutate())
            .toolCallbacks(toolCallbacks)
            .build();

        return chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt()
                .mutate()
                .chatOptions(toolOptionsCopy)
                .build())
            .build();
    }

    private static String loadDefaultSystemMessageSuffix() {
        try {
            return new DefaultResourceLoader()
                .getResource("classpath:/DEFAULT_SYSTEM_PROMPT_SUFFIX.md")
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load tool-search default system prompt suffix", exception);
        }
    }
}
