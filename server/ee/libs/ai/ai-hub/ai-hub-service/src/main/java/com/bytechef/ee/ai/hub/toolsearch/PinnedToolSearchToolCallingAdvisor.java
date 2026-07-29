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
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
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
 * A {@link ToolSearchToolCallingAdvisor} that keeps a fixed set of tools always callable, bypassing the search gate.
 *
 * <p>
 * The stock advisor replaces the model's per-iteration tool list with {@code {searchTool} ∪ {tools named in prior
 * searchTool responses present in the current message window}} — every other registered tool is invisible until a
 * {@code searchTool} call surfaces it. That is correct for the large searchable catalog (1000+ cluster-element / global
 * tools) but wrong for the handful of tools the system prompt instructs the model to call <b>directly by name</b>: the
 * specialist sub-agents ({@code workflow_editor_agent} et al.) and core interaction tools ({@code askUserQuestion},
 * {@code openWorkflowTab}). On a follow-up turn the model calls such a tool directly — it "knows" the name from the
 * prompt and prior conversation — but the prior {@code searchTool} response that once surfaced it is no longer in the
 * window (chat memory does not persist the intermediate search exchange), so the underlying tool-calling manager throws
 * {@code No ToolCallback found for tool name: ...}.
 * </p>
 *
 * <p>
 * This subclass closes that gap. {@code prepareIteration} (the method that performs the replacement) is {@code private}
 * in the base class, so the always-on union is applied around the {@code protected} hooks instead: the pinned callbacks
 * are captured once at loop initialization — while the full, already context-rehydration-wrapped static tool list is
 * still on the options — and re-injected before every model call after the base class has narrowed the list. Capture
 * and re-inject thread the pinned callbacks through the request context, mirroring how the base class threads its own
 * cached-callbacks map.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PinnedToolSearchToolCallingAdvisor extends ToolSearchToolCallingAdvisor {

    private static final String PINNED_TOOL_CALLBACKS_KEY =
        PinnedToolSearchToolCallingAdvisor.class.getName() + ".pinnedToolCallbacks";

    /**
     * The base advisor's private context key under which it stashes the per-session lookup of resolvable callbacks
     * (built solely from the agent's options tool list). {@code prepareIteration} consults this map to decide whether a
     * tool the model discovered via searchTool can be surfaced as callable. Reproduced here verbatim — the base class
     * exposes no accessor — so {@link #seedCatalogToolCallbacks} can merge the searchable catalog into it.
     */
    private static final String BASE_CACHED_TOOL_CALLBACKS_KEY =
        ToolSearchToolCallingAdvisor.class.getName() + ".cachedToolCallbacks";

    private final Set<String> pinnedToolNames;
    private final List<ToolCallback> catalogToolCallbacks;

    /**
     * Constructs the advisor with the search-loop collaborators {@code buildModeAdvisor} already owns. The remaining
     * constructor arguments are fixed to the stock {@code ToolSearchToolCallingAdvisor.Builder} defaults (the base
     * builder exposes no getters for them, so they are reproduced here rather than read back): the default
     * tool-execution eligibility checker, reference-tool-name accumulation on, an LRU eviction strategy, and —
     * load-bearing — the in-loop conversation history left enabled (see {@code buildModeAdvisor} for why disabling it
     * strips the conversation under the RC1 ChatMemory ordering).
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PinnedToolSearchToolCallingAdvisor(
        ToolCallingManager toolCallingManager, ToolIndex toolIndex, int maxResults, String sessionIdKeyName,
        Set<String> pinnedToolNames, List<ToolCallback> catalogToolCallbacks) {

        super(
            toolCallingManager, DEFAULT_ORDER, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, toolIndex,
            loadDefaultSystemMessageSuffix(), true, maxResults, true, sessionIdKeyName, new LruEvictionStrategy(1000));

        this.pinnedToolNames = Set.copyOf(pinnedToolNames);
        this.catalogToolCallbacks = List.copyOf(catalogToolCallbacks);
    }

    @Override
    protected ChatClientRequest doInitializeLoop(
        ChatClientRequest chatClientRequest,
        CallAdvisorChain callAdvisorChain) {

        ChatClientRequest initialized = super.doInitializeLoop(chatClientRequest, callAdvisorChain);

        seedCatalogToolCallbacks(initialized);

        return capturePinnedToolCallbacks(initialized);
    }

    @Override
    protected ChatClientRequest doInitializeLoopStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        // Session indexing (toolIndex.indexTools -> EmbeddingModel.embed) runs synchronously here on a
        // Schedulers.boundedElastic() worker where the ThreadLocal-bound EnvironmentContext is unset (the agent binds
        // it on the HTTP handler thread, which this hook does not inherit). Bind it deterministically from the advisor
        // request context so the environment-scoped embedding provider resolves; the searchTool query embedding on the
        // stream path is covered separately by adviseStream's contextWrite.
        ChatClientRequest initialized = runWithEnvironment(
            chatClientRequest, () -> super.doInitializeLoopStream(chatClientRequest, streamAdvisorChain));

        seedCatalogToolCallbacks(initialized);

        return capturePinnedToolCallbacks(initialized);
    }

    /**
     * Propagates the environment across the whole streaming pipeline so the {@code searchTool} query embedding — which
     * executes later in the tool-calling loop on a {@code Schedulers.boundedElastic()} worker, outside the synchronous
     * init hook — resolves the environment-scoped provider. Writing the {@link EnvironmentContextThreadLocalAccessor}
     * key into the Reactor Context (with automatic context propagation already enabled globally) rebinds
     * {@link EnvironmentContext} on every downstream worker.
     */
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

    /**
     * Blocking counterpart of {@link #adviseStream}: session indexing and the {@code searchTool} query embedding both
     * run synchronously within this call, so binding {@link EnvironmentContext} for its duration covers both.
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return runWithEnvironment(chatClientRequest, () -> super.adviseCall(chatClientRequest, callAdvisorChain));
    }

    /**
     * Runs {@code action} with {@link EnvironmentContext} bound to the request's environment (restoring the prior
     * binding afterward), or unchanged when the request carries no resolvable environment.
     */
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
        return injectPinnedToolCallbacks(super.doBeforeCall(chatClientRequest, callAdvisorChain));
    }

    @Override
    protected ChatClientRequest doBeforeStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        return injectPinnedToolCallbacks(super.doBeforeStream(chatClientRequest, streamAdvisorChain));
    }

    /**
     * Merges the searchable catalog callbacks into the base advisor's per-session lookup map so a tool the model
     * discovers via searchTool can be surfaced as callable by {@code prepareIteration} on the next iteration.
     *
     * <p>
     * The base advisor builds that lookup ({@link #BASE_CACHED_TOOL_CALLBACKS_KEY}) only from the agent's options tool
     * list, but the searchable catalog (cluster elements + per-mode global static tools) is deliberately kept off that
     * list so the model isn't shown 1000+ definitions every turn. Without this merge a discovered catalog tool is never
     * found in the lookup, never added to the iteration's tool list, and the model loops re-issuing searchTool. The map
     * the base class stashes is a mutable {@code ConcurrentHashMap}, so merging in place (via {@code putIfAbsent},
     * never overwriting an options-listed callback of the same name) reaches the same map {@code prepareIteration}
     * reads.
     * </p>
     */
    private void seedCatalogToolCallbacks(ChatClientRequest chatClientRequest) {
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

        for (ToolCallback toolCallback : catalogToolCallbacks) {
            cachedToolCallbacks.putIfAbsent(
                toolCallback.getToolDefinition()
                    .name(),
                toolCallback);
        }
    }

    /**
     * Snapshots the pinned callbacks from the full static tool list (present on the options at loop initialization,
     * already wrapped for tenant + SecurityContext rehydration by the agent builder) into the request context, so they
     * can be re-injected on every iteration after the base class narrows the list.
     */
    private ChatClientRequest capturePinnedToolCallbacks(ChatClientRequest chatClientRequest) {
        if (chatClientRequest.prompt()
            .getOptions() instanceof ToolCallingChatOptions toolOptions) {

            List<ToolCallback> pinnedToolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : toolOptions.getToolCallbacks()) {
                if (pinnedToolNames.contains(toolCallback.getToolDefinition()
                    .name())) {

                    pinnedToolCallbacks.add(toolCallback);
                }
            }

            chatClientRequest.context()
                .put(PINNED_TOOL_CALLBACKS_KEY, pinnedToolCallbacks);
        }

        return chatClientRequest;
    }

    /**
     * Unions the captured pinned callbacks into the iteration's tool list, deduping by tool name so a pinned tool the
     * search just surfaced is not added twice. Returns the request unchanged when nothing was captured or everything is
     * already present.
     */
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

        List<ToolCallback> toolCallbacks = new ArrayList<>(toolOptions.getToolCallbacks());

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
