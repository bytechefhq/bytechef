/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.config.AiHubPgVectorConfiguration;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires up the Tool Search Tool advisor for the AI Hub. The advisor exposes one meta-tool ({@code searchTool}) to the
 * LLM; when the LLM calls it with a natural-language query, the advisor performs vector search against the catalog,
 * expands the matching tool definitions into the next chat turn, and dispatches the follow-up tool call through the
 * underlying {@link ToolCallingManager}.
 *
 * <p>
 * Wiring shape:
 * </p>
 * <ol>
 * <li>{@link VectorToolIndex} wraps {@code toolSearchPgVectorStore} (the sibling pgvector store dedicated to tool
 * embeddings).</li>
 * <li>{@link ToolSearchCatalogFeeder} populates the searcher's index with one {@code ToolReference} per tool-typed
 * cluster element. Driven lazily by {@link ToolSearchCatalogWarmup} on the first chat turn (not at startup) for a
 * deterministic fresh-slate-then-load semantic (see feeder javadoc for re-index trade-offs).</li>
 * <li>For each tool-typed cluster element the configuration also constructs a {@link ClusterElementToolCallback} and
 * registers it with a {@link MapToolCallbackResolver}-backed {@link DefaultToolCallingManager}. This is the registry
 * the advisor uses to dispatch when the LLM picks a discovered tool by name — the tool name string MUST match what
 * {@link ToolNameNormalizer#toToolName(String, String)} produces in the feeder.</li>
 * <li>{@link ToolSearchToolCallingAdvisor} ties searcher + manager together; this is the bean that gets attached to the
 * agent's {@code .advisor(...)} chain.</li>
 * </ol>
 *
 * <p>
 * <b>Important asymmetry:</b> the callbacks are registered with the {@link ToolCallingManager}, NOT added to the
 * agent's {@code toolCallbacks} list. If they appeared in the agent's regular tool list the LLM would see all 1000+
 * tool definitions in every turn — defeating the entire token-savings purpose of the search advisor. The advisor's job
 * is to keep them invisible until the LLM searches for them.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ToolSearchAdvisorConfiguration {

    /**
     * Top-K tools returned by a single {@code searchTool} call. Higher values give the LLM more options to pick from
     * but cost more tokens per turn. 5 is the empirical sweet spot per the upstream library blog post; revisit if smoke
     * testing shows the LLM frequently misses on top-5 and would have hit on top-10.
     */
    private static final int MAX_SEARCH_RESULTS = 5;

    /**
     * Tools the system prompt tells the model to call directly by name, so they must stay callable on every iteration
     * rather than being hidden behind a {@code searchTool} hit: the specialist sub-agents (the {@code *_agent}
     * delegates plus the research / data-analyst / image-generator / slide-builder ChatClient sub-agents), the core
     * interaction tools {@code askUserQuestion} and {@code openWorkflowTab}, and the interactive picker tools that
     * render UI in the chat panel ({@code selectConnection}, {@code createConnection}, {@code selectPropertyOption},
     * {@code selectTriggerPropertyOption}). The pickers must be pinned like {@code askUserQuestion}: they render only
     * off a tool-result event, and chat memory does not persist the intermediate {@code searchTool} exchange that once
     * surfaced them, so on a follow-up turn an unpinned picker is not in the narrowed tool list — the model then
     * narrates "I've rendered the picker above" without a real call and nothing renders. The auto-memory tools
     * ({@code MemoryView}, {@code MemoryCreate}, {@code MemoryStrReplace}, {@code MemoryInsert}, {@code MemoryDelete},
     * {@code MemoryRename}) are pinned for the same reason: {@code AutoMemoryToolsAdvisor} injects them (and the memory
     * system prompt that instructs the model to use them) before the tool-search loop runs, so without pinning the
     * narrowing strips them every iteration and the model can never recall or record memories. The read-only
     * state-visibility tools of the tool-attach flow ({@code listTaskTools}, {@code listConnectionsForComponent},
     * {@code lookupActionPropertyOptions}, {@code lookupTriggerPropertyOptions}) are pinned alongside their
     * {@code select*} render siblings: the build system prompt tells the model to call each of them directly by name,
     * so an unpinned one fails with "No ToolCallback found" the moment the model starts the attach flow. Names that are
     * absent for a given mode (e.g. a specialist whose ChatClient bean is disabled, or memory tools in a mode that
     * doesn't mount the advisor) are simply never captured — pinning a missing name is a no-op. Keep this list small;
     * every entry is sent to the model on every turn, which is the cost the tool-search advisor otherwise avoids.
     */
    static final Set<String> ALWAYS_ON_TOOL_NAMES = Set.of(
        "askUserQuestion", "cluster_element_agent", "code_editor_agent", "converter_agent", "createConnection",
        "data_analyst", "image_generator", "listConnectionsForComponent", "listTaskTools",
        "lookupActionPropertyOptions", "lookupTriggerPropertyOptions", "MemoryCreate", "MemoryDelete", "MemoryInsert",
        "MemoryRename", "MemoryStrReplace", "MemoryView", "openWorkflowTab", "research", "selectConnection",
        "selectPropertyOption", "selectTriggerPropertyOption", "skills_agent", "slide_builder",
        "workflow_editor_agent", "workflow_execution_agent");

    private static final Logger log = LoggerFactory.getLogger(ToolSearchAdvisorConfiguration.class);

    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    VectorToolIndex toolSearchVectorToolIndex(@Qualifier("toolSearchPgVectorStore") VectorStore vectorStore) {
        return new VectorToolIndex(vectorStore);
    }

    @Bean
    @Nullable
    ToolSearchCatalogFeeder toolSearchCatalogFeeder(
        ClusterElementDefinitionService clusterElementDefinitionService,
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        @Qualifier("copilotEmbeddingModel") ObjectProvider<EmbeddingModel> copilotEmbeddingModelProvider,
        PgVectorStoreProperties properties, ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        EmbeddingModel copilotEmbeddingModel = copilotEmbeddingModelProvider.getIfAvailable();

        if (copilotEmbeddingModel == null) {
            log.info(
                "Tool search catalog indexing disabled: no fixed-key copilot embedding model. Set "
                    + "bytechef.ai.copilot.embedding.provider + .api-key to enable it.");

            return null;
        }

        VectorStore loaderVectorStore = AiHubPgVectorConfiguration.buildToolSearchVectorStore(
            pgVectorJdbcTemplate, copilotEmbeddingModel, properties, observationRegistry, customObservationConvention,
            batchingStrategy);

        return new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, new VectorToolIndex(loaderVectorStore), pgVectorJdbcTemplate,
            properties.getSchemaName());
    }

    /**
     * One-shot warm-up shared by both per-mode advisors, invoked on the first chat turn (see
     * {@link ToolSearchCatalogWarmup}). Resolving the feeder and global catalogs through {@link ObjectProvider} keeps
     * this bean cheap to construct — it captures references only; the catalog is enumerated on first warm-up, not here.
     * The feeder is absent when no fixed-key copilot embedding model is configured, in which case the warm-up no-ops.
     */
    @Bean
    ToolSearchCatalogWarmup toolSearchCatalogWarmup(
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider) {

        return new ToolSearchCatalogWarmup(
            toolSearchCatalogFeederProvider.getIfAvailable(),
            globalToolCatalogProvider.orderedStream()
                .toList());
    }

    /**
     * v2 dynamic per-task tool callback resolver. Injected into the agent so each chat turn synthesizes the task's
     * attached tools as Spring AI {@link ToolCallback}s on top of the static set.
     */
    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    AiHubTaskBindingToolCallbackResolver taskBindingToolCallbackResolver(
        com.bytechef.ee.ai.hub.task.AiHubTaskService taskService,
        com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade taskToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService,
        com.bytechef.ee.ai.hub.mcpserver.AiHubMcpToolCallbackProvider mcpToolCallbackProvider,
        com.bytechef.ee.ai.hub.skill.AiHubSkillsToolProvider skillsToolCallbackProvider) {

        return new AiHubTaskBindingToolCallbackResolver(
            taskService, taskToolFacade, clusterElementDefinitionService, connectionService, mcpToolCallbackProvider,
            skillsToolCallbackProvider);
    }

    @Bean
    AiHubClusterElementToolCallbacks aiHubClusterElementToolCallbacks(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        // Wrap the catalog materialisation in a memoised supplier rather than building it here: building the callbacks
        // enumerates every tool-typed cluster element, which forces the full component definition catalog to load.
        // Doing
        // that in this bean's constructor would run at Spring startup (the advisor beans below inject this one),
        // undoing
        // the build-time component index that keeps boot lazy. The supplier is resolved on the first chat turn instead.
        return new AiHubClusterElementToolCallbacks(
            MemoizationUtils.memoize(
                () -> new LinkedHashMap<String, ToolCallback>(
                    buildClusterElementToolCallbacks(clusterElementDefinitionService, connectionService))));
    }

    /**
     * Construction note: the search-specific {@link ToolCallingManager} is built inline here and never published as a
     * top-level bean. If it were a bean it would be the only {@code ToolCallingManager} in the context (Spring AI's
     * autoconfig backs off on {@code @ConditionalOnMissingBean}), and any unqualified {@code ToolCallingManager}
     * consumer — notably {@code AiAgentComponentHandler} — would silently pick it up, forming a cycle through
     * {@link ClusterElementDefinitionService} → component handler discovery → AiAgent → ToolCallingManager. Inlining
     * the manager keeps it scoped to this advisor and lets Spring AI's default serve unqualified consumers.
     */
    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    ToolSearchToolCallingAdvisor aiHubAskToolSearchToolCallAdvisor(
        VectorToolIndex toolSearchVectorToolIndex,
        @Qualifier("toolSearchPgVectorStore") VectorStore toolSearchPgVectorStore,
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks, ObservationRegistry observationRegistry,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        SecurityContextRehydrator securityContextRehydrator, ToolSearchCatalogWarmup toolSearchCatalogWarmup) {

        return buildModeAdvisor(
            toolSearchVectorToolIndex, toolSearchPgVectorStore, clusterElementToolCallbacks.callbacks(),
            observationRegistry, findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID),
            securityContextRehydrator, toolSearchCatalogWarmup);
    }

    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    ToolSearchToolCallingAdvisor aiHubBuildToolSearchToolCallAdvisor(
        VectorToolIndex toolSearchVectorToolIndex,
        @Qualifier("toolSearchPgVectorStore") VectorStore toolSearchPgVectorStore,
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks, ObservationRegistry observationRegistry,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        SecurityContextRehydrator securityContextRehydrator, ToolSearchCatalogWarmup toolSearchCatalogWarmup) {

        return buildModeAdvisor(
            toolSearchVectorToolIndex, toolSearchPgVectorStore, clusterElementToolCallbacks.callbacks(),
            observationRegistry,
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            securityContextRehydrator, toolSearchCatalogWarmup);
    }

    private static ToolSearchToolCallingAdvisor buildModeAdvisor(
        VectorToolIndex vectorToolIndex, VectorStore toolSearchPgVectorStore,
        Supplier<Map<String, ToolCallback>> clusterElementCallbacksMapSupplier,
        ObservationRegistry observationRegistry, @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        SecurityContextRehydrator securityContextRehydrator, ToolSearchCatalogWarmup toolSearchCatalogWarmup) {

        Set<String> additionalSessionIds = globalToolCatalog == null
            ? Set.of(ToolSearchCatalogFeeder.CATALOG_SESSION_ID)
            : Set.of(ToolSearchCatalogFeeder.CATALOG_SESSION_ID, globalToolCatalog.sessionId());

        // RC1's VectorToolIndex scopes each search to one session; MultiSessionToolIndex restores the vendored
        // multi-session union (catalog + per-mode global tools) on top of the shared index. It queries the underlying
        // store directly with a sessionId IN (...) filter so the query is embedded once, not once per session.
        ToolIndex searcher = new MultiSessionToolIndex(
            vectorToolIndex, toolSearchPgVectorStore, additionalSessionIds);

        if (globalToolCatalog == null) {
            log.warn(
                "No AiHubGlobalToolCatalog contributed for this mode — tool search runs catalog-only (no global "
                    + "static tools). automation-ai-hub should contribute one.");
        }

        // Keyed by the tool name — cluster-element names come free from the index stub when the callback is built
        // (buildClusterElementToolCallbacks returns a name->callback map), and the per-mode global tools have eager
        // (cheap) definitions. Keeping the map, rather than flattening to a list, lets both the resolver and the
        // advisor look tools up by name WITHOUT calling getToolDefinition() — which for a lazy
        // ClusterElementToolCallback
        // would force its input schema (and component) to load. So a schema materialises only when the model invokes a
        // surfaced tool.
        Supplier<Map<String, ToolCallback>> callbackMapSupplier = MemoizationUtils.memoize(() -> {
            Map<String, ToolCallback> callbackMap =
                new LinkedHashMap<>(clusterElementCallbacksMapSupplier.get());

            if (globalToolCatalog != null) {
                for (ToolCallback toolCallback : globalToolCatalog.toolCallbacks()) {
                    // Discovered global tools resolve through this resolver and execute directly on a Reactor scheduler
                    // thread. Mirror AiHubSpringAIAgent.wrapToolCallback so tenant-scoped and @PreAuthorize-protected
                    // service calls run under the invoking tenant + principal (and empty results are guarded).
                    ToolCallback wrapped = AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator);

                    callbackMap.put(
                        wrapped.getToolDefinition()
                            .name(),
                        wrapped);
                }
            }

            return callbackMap;
        });

        ToolExecutionExceptionProcessor exceptionProcessor = new DefaultToolExecutionExceptionProcessor(false);

        // Lazy so constructing this advisor at startup does not build the resolver. MapToolCallbackResolver keys off
        // the
        // pre-known names, so building it never calls getToolDefinition() (unlike StaticToolCallbackResolver).
        ToolCallingManager toolCallingManager = new LazyToolCallingManager(
            () -> new DefaultToolCallingManager(
                observationRegistry, new MapToolCallbackResolver(callbackMapSupplier.get()), exceptionProcessor));

        // PinnedToolSearchToolCallingAdvisor pins ALWAYS_ON_TOOL_NAMES so they stay callable without a preceding
        // searchTool hit — the system prompt instructs the model to call those specialists/core tools directly by name,
        // but the stock advisor hides every static tool behind tool search and a follow-up turn calling one directly
        // would fail with "No ToolCallback found". Its OWN in-loop conversation history is DISABLED: the AI Hub mounts
        // a session-backed memory advisor (SessionMemoryAdvisor, TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER = MIN+400)
        // INSIDE the tool loop — downstream of this advisor (MIN+300) — so it re-participates on every iteration,
        // persisting and rehydrating the full [user, assistant(tool_calls), tool_result] transcript from the session
        // store. That in-loop memory is what keeps each iteration's prompt valid (no orphaned tool_result); keeping
        // the advisor's internal history enabled on top of it would inject the same intra-turn messages twice.
        // Mirrors AbstractAiAgentChatAction's conditional disableInternalConversationHistory() for
        // tool-message-persisting memory types. (Historical note: while memory sat UPSTREAM of the loop — the
        // MessageChatMemoryAdvisor arrangement — the internal history was load-bearing and disabling it produced
        // Anthropic HTTP 400 "tool_result without a corresponding tool_use".)
        //
        // Session id read from the conversation id (mirrors the vendored advisor, which derived its search session from
        // ChatMemory.CONVERSATION_ID); MultiSessionToolIndex unions it with the catalog and per-mode global sessions.
        //
        // callbackMap (cluster elements + per-mode global static tools, the same set registered with the resolver
        // above) is also handed to the advisor so a tool the model DISCOVERS via searchTool can be surfaced as callable
        // on the next iteration. The base advisor surfaces a discovered tool only when its callback is present in the
        // cachedToolCallbacks map it builds from the agent's options tool list — and the searchable catalog is
        // deliberately kept OFF that list (see the "Important asymmetry" note above). Without this, discovered catalog
        // tools resolve to nothing, the model can never call them, and it loops re-issuing searchTool until it bails.
        return new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, searcher, MAX_SEARCH_RESULTS, ChatMemory.CONVERSATION_ID, ALWAYS_ON_TOOL_NAMES,
            callbackMapSupplier, toolSearchCatalogWarmup::warmUp);
    }

    private static @Nullable AiHubGlobalToolCatalog findCatalog(
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider, String sessionId) {

        return globalToolCatalogProvider.orderedStream()
            .filter(catalog -> sessionId.equals(catalog.sessionId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Builds one {@link ClusterElementToolCallback} per tool-typed cluster element. The map's key is the LLM-visible
     * tool name (must match the feeder's index entry).
     */
    private static Map<String, ClusterElementToolCallback> buildClusterElementToolCallbacks(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        List<ClusterElementDefinition> toolDefinitions =
            clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);

        Map<String, ClusterElementToolCallback> callbacks = new HashMap<>(toolDefinitions.size());

        for (ClusterElementDefinition toolDefinition : toolDefinitions) {
            String toolName = ToolNameNormalizer.toToolName(
                toolDefinition.getComponentName(), toolDefinition.getName());

            // Description for the LLM tool definition — this is what the model sees AFTER discovery, so it should
            // echo the search-summary content. Title prefix makes it more readable in tool-call traces.
            String description = formatToolDescription(toolDefinition);

            ClusterElementToolCallback callback = new ClusterElementToolCallback(
                toolName, description, toolDefinition.getComponentName(),
                toolDefinition.getComponentVersion(), toolDefinition.getName(),
                clusterElementDefinitionService, connectionService);

            // If two cluster elements normalize to the same toolName (rare but possible across components if naming
            // collides after sanitization), the second wins — which is wrong silently. Log so the collision is
            // discoverable in production logs; v2 should make ToolNameNormalizer disambiguate by appending a hash
            // when a collision is detected.
            if (callbacks.put(toolName, callback) != null) {
                log.warn(
                    "Tool name collision on '{}' — overwriting earlier callback. Investigate ToolNameNormalizer.",
                    toolName);
            }
        }

        return Map.copyOf(callbacks);
    }

    private static String formatToolDescription(ClusterElementDefinition toolDefinition) {
        String description = toolDefinition.getDescription();
        String title = toolDefinition.getTitle();

        if (description != null && !description.isBlank()) {
            return title != null && !title.isBlank() ? title + ": " + description : description;
        }

        return title != null && !title.isBlank() ? title : "(no description)";
    }

    /**
     * Lazy carrier for the cluster-element executable callbacks, shared by both per-mode advisors so the full
     * cluster-element catalog is materialised a single time — on first use, not at startup. Holds a memoised
     * {@link Supplier} rather than the list itself so injecting this bean at startup does not force the catalog to load
     * (see {@link #aiHubClusterElementToolCallbacks}). Wrapped in a record so Spring does not auto-collect every
     * {@link ToolCallback} bean when the advisors inject it.
     */
    record AiHubClusterElementToolCallbacks(Supplier<Map<String, ToolCallback>> callbacks) {
    }
}
