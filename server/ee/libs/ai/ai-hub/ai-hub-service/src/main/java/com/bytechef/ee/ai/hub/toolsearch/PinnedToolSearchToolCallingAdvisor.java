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
 * A {@link ToolSearchToolCallingAdvisor} that keeps the agent's entire static tool list always callable, bypassing the
 * search gate.
 *
 * <p>
 * The stock advisor replaces the model's per-iteration tool list with {@code {searchTool} ∪ {tools named in prior
 * searchTool responses present in the current message window}} — every other registered tool is invisible until a
 * {@code searchTool} call surfaces it. That is correct for the large searchable catalog (1000+ cluster-element / global
 * copilot tools), which is registered with the search-loop's {@code MapToolCallbackResolver} and NOT placed on the
 * agent's options list. It is wrong for the AI Hub's own static tools — the specialist sub-agents
 * ({@code buildWorkflow} et al.), core interaction tools ({@code askUserQuestion}, {@code openWorkflowTab}), the
 * interactive pickers, the auto-memory tools, and every workspace-resource read/mutation tool ({@code listDataTables},
 * {@code queryKnowledgeBase}, {@code getAssetFileContent}, deployment / context-store / API-collection tools, …). Those
 * are the tools placed directly on the agent's options list, are resolvable at execution time ONLY while on that list,
 * and the system prompt instructs the model to call each of them directly by name. On a follow-up turn the model does
 * so — it "knows" the name from the prompt and prior conversation — but the prior {@code searchTool} response that once
 * surfaced it is no longer in the window (chat memory does not persist the intermediate search exchange), so the
 * underlying tool-calling manager throws {@code No ToolCallback found for tool name: ...}.
 * </p>
 *
 * <p>
 * This subclass closes that gap. {@code prepareIteration} (the method that performs the replacement) is {@code private}
 * in the base class, so the always-on union is applied around the {@code protected} hooks instead: the full static tool
 * list is captured once at loop initialization — while it is still on the options, already context-rehydration-wrapped
 * — and re-injected before every model call after the base class has narrowed the list. Because the searchable catalog
 * is never on the options list, capturing the whole list pins exactly the direct-call set and nothing searchable; that
 * is the invariant a hand-maintained pinned-name subset kept violating (any omitted static tool was silently
 * uncallable). Capture and re-inject thread the callbacks through the request context, mirroring how the base class
 * threads its own cached-callbacks map.
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

    private final Supplier<Map<String, ToolCallback>> catalogToolCallbacksSupplier;
    private final Runnable catalogWarmUp;

    /**
     * Constructs the advisor with the search-loop collaborators {@code buildModeAdvisor} already owns. The remaining
     * constructor arguments follow the stock {@code ToolSearchToolCallingAdvisor.Builder} defaults (the base builder
     * exposes no getters for them, so they are reproduced here rather than read back): the default tool-execution
     * eligibility checker, reference-tool-name accumulation on, and an LRU eviction strategy. The one deliberate
     * deviation is the in-loop conversation history, which is <b>disabled</b>: the AI Hub mounts a session-backed
     * memory advisor INSIDE the tool loop (order greater than this advisor's), which persists and rehydrates the full
     * tool request/response transcript on every iteration — keeping the advisor's own history enabled on top of that
     * would inject the same intra-turn messages twice (see {@code buildModeAdvisor}).
     *
     * <p>
     * The searchable catalog is supplied as a memoised {@link Supplier} of a name-keyed {@link Map}, not a materialised
     * list, so the advisor can be constructed at Spring startup without forcing the full component definition catalog
     * to load, and so seeding never needs to call {@link ToolCallback#getToolDefinition()} to learn a callback's name.
     * The supplier is resolved lazily on the first {@code seedCatalogToolCallbacks} (i.e. the first chat turn); see
     * {@code
     * buildModeAdvisor} and {@link LazyToolCallingManager} for the matching lazy delegate the base manager wraps.
     * </p>
     *
     * <p>
     * {@code catalogWarmUp} is the one-shot {@link ToolSearchCatalogWarmup} — invoked on the first loop initialization
     * (the first chat turn) to populate the pgvector search index synchronously before the turn's first {@code
     * searchTool} query, replacing the former {@code ApplicationReadyEvent} population so startup stays lazy.
     * </p>
     */
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

        // First chat turn: populate the pgvector search index (once, synchronously) before the base class runs so the
        // catalog is queryable for this turn's searchTool. No-op after the first successful warm-up.
        catalogWarmUp.run();

        ChatClientRequest initialized = super.doInitializeLoop(chatClientRequest, callAdvisorChain);

        seedCatalogToolCallbacks(initialized);

        return capturePinnedToolCallbacks(initialized);
    }

    @Override
    protected ChatClientRequest doInitializeLoopStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain) {

        // First chat turn: populate the pgvector search index (once, synchronously) before the base class runs. Bound
        // to the default tenant internally, independent of this request's environment/tenant. No-op after warm-up.
        catalogWarmUp.run();

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
     *
     * <p>
     * {@code catalogToolCallbacksSupplier} hands back the tool name pre-keyed (see {@code buildModeAdvisor}), so this
     * merge never calls {@link ToolCallback#getToolDefinition()} — which, for a lazy
     * {@link ClusterElementToolCallback}, would force its input schema (and component) to load on every seeding pass. A
     * schema materializes only when the model actually invokes the tool.
     * </p>
     */
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

    /**
     * Snapshots the ENTIRE static tool list into the request context so it can be re-injected on every iteration after
     * the base class narrows the list.
     *
     * <p>
     * At loop initialization the options carry exactly the agent's configured static tool list (already wrapped for
     * tenant + SecurityContext rehydration by the agent builder) — the base advisor's search narrowing happens later,
     * in {@code prepareIteration}. Every tool on that list is a "call directly by name" tool: the large searchable
     * catalog (cluster elements + per-mode global copilot tools) is deliberately kept OFF the options list (registered
     * with the search-loop's {@code MapToolCallbackResolver} instead — see {@code ToolSearchAdvisorConfiguration}), so
     * a static tool is resolvable at execution time ONLY while it is on the options list. Capturing the whole list
     * therefore pins exactly the set that must stay callable, and cannot accidentally un-hide a searchable catalog tool
     * (none are on the list to capture). This is what a hand-maintained pinned-name subset kept getting wrong: any
     * static tool the system prompt names but the subset omitted (a data-table / knowledge-base / asset-file /
     * deployment / context-store tool, …) was silently uncallable and failed with "No ToolCallback found" the moment
     * the model called it.
     * </p>
     */
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
