/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.agui.spring.ai.SpringAIAgent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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

    public static final String USER_TOOL_CALLBACKS_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".userToolCallbacks";

    private static final String PINNED_TOOL_CALLBACKS_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".pinnedToolCallbacks";

    private static final String RUN_CATALOG_TOOL_CALLBACKS_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".runCatalogToolCallbacks";

    private static final String TOOL_SEARCH_UNAVAILABLE_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".toolSearchUnavailable";

    private static final String BASE_CACHED_TOOL_CALLBACKS_KEY =
        ToolSearchToolCallingAdvisor.class.getName() + ".cachedToolCallbacks";

    private final Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier;
    private final Runnable catalogWarmUp;
    private final UserToolIndexer userToolIndexer;
    private final AtomicBoolean toolSearchUnavailableLogged = new AtomicBoolean();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PinnedToolSearchToolCallingAdvisor(
        ToolCallingManager toolCallingManager, ToolIndex toolIndex, int maxResults, String sessionIdKeyName,
        Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier, Runnable catalogWarmUp) {

        this(
            toolCallingManager, toolIndex, maxResults, sessionIdKeyName, catalogToolCallbacksSupplier, catalogWarmUp,
            (threadId, toolCallbacks) -> {});
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PinnedToolSearchToolCallingAdvisor(
        ToolCallingManager toolCallingManager, ToolIndex toolIndex, int maxResults, String sessionIdKeyName,
        Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier, Runnable catalogWarmUp,
        UserToolIndexer userToolIndexer) {

        super(
            toolCallingManager, DEFAULT_ORDER, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, toolIndex,
            loadDefaultSystemMessageSuffix(), true, maxResults, false, sessionIdKeyName,
            new LruEvictionStrategy(1000));

        this.catalogToolCallbacksSupplier = catalogToolCallbacksSupplier;
        this.catalogWarmUp = catalogWarmUp;
        this.userToolIndexer = userToolIndexer;
    }

    @FunctionalInterface
    public interface UserToolIndexer {

        void index(String threadId, List<ToolCallback> toolCallbacks);
    }

    @SuppressFBWarnings("EI")
    public record UserToolCallbacks(
        String threadId, Map<String, ToolCallback> componentToolCallbacks,
        Map<String, ToolCallback> additionalToolCallbacks) {
    }

    @Override
    protected ChatClientRequest doInitializeLoop(
        ChatClientRequest chatClientRequest,
        CallAdvisorChain callAdvisorChain) {

        catalogWarmUp.run();

        captureRunCatalogToolCallbacks(chatClientRequest);

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

        captureRunCatalogToolCallbacks(chatClientRequest);

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
            return withRunCatalogToolCallbacks(chatClientRequest);
        }

        return withRunCatalogToolCallbacks(
            injectPinnedToolCallbacks(super.doBeforeCall(chatClientRequest, callAdvisorChain)));
    }

    @Override
    protected ChatClientRequest doBeforeStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        if (isToolSearchUnavailable(chatClientRequest)) {
            return withRunCatalogToolCallbacks(chatClientRequest);
        }

        return withRunCatalogToolCallbacks(
            injectPinnedToolCallbacks(super.doBeforeStream(chatClientRequest, streamAdvisorChain)));
    }

    private ChatClientRequest markToolSearchUnavailable(
        ChatClientRequest chatClientRequest, RuntimeException exception) {

        if (toolSearchUnavailableLogged.compareAndSet(false, true)) {
            log.warn(
                "AI Hub tool search is unavailable, so the catalog tools cannot be found and the turn continues " +
                    "with the pinned tools only: {}",
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

    private void captureRunCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
        Map<String, ToolCallback> catalogToolCallbacks = catalogToolCallbacksSupplier.get();

        UserToolCallbacks userToolCallbacks = userToolCallbacks(chatClientRequest);

        if (catalogToolCallbacks.isEmpty() && userToolCallbacks == null) {
            return;
        }

        UnaryOperator<ToolCallback> toolCallbackDecorator = toolCallbackDecorator(chatClientRequest);

        Map<String, ToolCallback> runCatalogToolCallbacks = new LinkedHashMap<>();

        for (Map.Entry<String, ToolCallback> entry : catalogToolCallbacks.entrySet()) {
            runCatalogToolCallbacks.put(entry.getKey(), toolCallbackDecorator.apply(entry.getValue()));
        }

        if (userToolCallbacks != null) {
            List<ToolCallback> unindexedToolCallbacks = new ArrayList<>();

            for (Map.Entry<String, ToolCallback> entry : userToolCallbacks.componentToolCallbacks()
                .entrySet()) {

                if (!catalogToolCallbacks.containsKey(entry.getKey())) {
                    unindexedToolCallbacks.add(entry.getValue());
                }

                runCatalogToolCallbacks.put(entry.getKey(), toolCallbackDecorator.apply(entry.getValue()));
            }

            for (Map.Entry<String, ToolCallback> entry : userToolCallbacks.additionalToolCallbacks()
                .entrySet()) {

                if (!runCatalogToolCallbacks.containsKey(entry.getKey())) {
                    unindexedToolCallbacks.add(entry.getValue());

                    runCatalogToolCallbacks.put(entry.getKey(), toolCallbackDecorator.apply(entry.getValue()));
                }
            }

            indexUserToolCallbacks(userToolCallbacks.threadId(), unindexedToolCallbacks);
        }

        chatClientRequest.context()
            .put(RUN_CATALOG_TOOL_CALLBACKS_KEY, runCatalogToolCallbacks);
    }

    private void indexUserToolCallbacks(String threadId, List<ToolCallback> toolCallbacks) {
        try {
            userToolIndexer.index(threadId, toolCallbacks);
        } catch (RuntimeException exception) {
            log.warn(
                "Indexing the {} chat tools of thread {} failed, so tool search cannot find them this turn",
                toolCallbacks.size(), threadId, exception);
        }
    }

    private static @Nullable UserToolCallbacks userToolCallbacks(ChatClientRequest chatClientRequest) {
        Object userToolCallbacks = chatClientRequest.context()
            .get(USER_TOOL_CALLBACKS_KEY);

        return userToolCallbacks instanceof UserToolCallbacks callbacks ? callbacks : null;
    }

    @SuppressWarnings("unchecked")
    private static UnaryOperator<ToolCallback> toolCallbackDecorator(ChatClientRequest chatClientRequest) {
        Object toolCallbackDecorator = chatClientRequest.context()
            .get(SpringAIAgent.TOOL_CALLBACK_DECORATOR_KEY);

        if (toolCallbackDecorator instanceof UnaryOperator<?> unaryOperator) {
            return (UnaryOperator<ToolCallback>) unaryOperator;
        }

        return UnaryOperator.identity();
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ToolCallback> runCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
        Object runCatalogToolCallbacks = chatClientRequest.context()
            .get(RUN_CATALOG_TOOL_CALLBACKS_KEY);

        return runCatalogToolCallbacks instanceof Map<?, ?> map ? (Map<String, ToolCallback>) map : null;
    }

    private void seedCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
        Map<String, ToolCallback> runCatalogToolCallbacks = runCatalogToolCallbacks(chatClientRequest);

        if (runCatalogToolCallbacks == null) {
            return;
        }

        Object cached = chatClientRequest.context()
            .get(BASE_CACHED_TOOL_CALLBACKS_KEY);

        if (!(cached instanceof Map<?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, ToolCallback> cachedToolCallbacks = (Map<String, ToolCallback>) cached;

        for (Map.Entry<String, ToolCallback> entry : runCatalogToolCallbacks.entrySet()) {
            cachedToolCallbacks.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static ChatClientRequest withRunCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
        Map<String, ToolCallback> runCatalogToolCallbacks = runCatalogToolCallbacks(chatClientRequest);

        if (runCatalogToolCallbacks == null ||
            !(chatClientRequest.prompt()
                .getOptions() instanceof ToolCallingChatOptions toolOptions)) {

            return chatClientRequest;
        }

        ToolCallingChatOptions toolOptionsCopy = ((ToolCallingChatOptions.Builder<?>) toolOptions.mutate())
            .toolContext(RunCatalogToolCallingManager.RUN_CATALOG_TOOL_CALLBACKS_KEY, runCatalogToolCallbacks)
            .build();

        return chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt()
                .mutate()
                .chatOptions(toolOptionsCopy)
                .build())
            .build();
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
