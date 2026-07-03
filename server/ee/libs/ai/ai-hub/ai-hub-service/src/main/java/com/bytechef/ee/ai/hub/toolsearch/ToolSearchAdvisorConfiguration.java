/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

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
 * cluster element. Re-runs on every {@link ApplicationReadyEvent} for a deterministic fresh-slate-then-load semantic
 * (see feeder javadoc for re-index trade-offs).</li>
 * <li>For each tool-typed cluster element the configuration also constructs a {@link ClusterElementToolCallback} and
 * registers it with a {@link StaticToolCallbackResolver}-backed {@link DefaultToolCallingManager}. This is the registry
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
     * delegates plus the research / data-analyst / image-generator / slide-builder ChatClient sub-agents) and the core
     * interaction tools {@code askUserQuestion} and {@code openWorkflowTab}. Names that are absent for a given mode
     * (e.g. a specialist whose ChatClient bean is disabled) are simply never captured — pinning a missing name is a
     * no-op. Keep this list small; every entry is sent to the model on every turn, which is the cost the tool-search
     * advisor otherwise avoids.
     */
    private static final Set<String> ALWAYS_ON_TOOL_NAMES = Set.of(
        "askUserQuestion", "cluster_element_agent", "code_editor_agent", "converter_agent", "data_analyst",
        "image_generator", "openWorkflowTab", "research", "skills_agent", "slide_builder", "workflow_editor_agent",
        "workflow_execution_agent");

    private static final Logger log = LoggerFactory.getLogger(ToolSearchAdvisorConfiguration.class);

    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    VectorToolIndex toolSearchVectorToolIndex(@Qualifier("toolSearchPgVectorStore") VectorStore vectorStore) {
        return new VectorToolIndex(vectorStore);
    }

    @Bean
    ToolSearchCatalogFeeder toolSearchCatalogFeeder(
        ClusterElementDefinitionService clusterElementDefinitionService, VectorToolIndex toolSearchVectorToolIndex,
        // The pgvector datasource — same JdbcTemplate the vector store uses, so the meta table lives in the same
        // schema and benefits from the same connection pool. Co-locating "all tool-search state" in one schema keeps
        // backups + cleanup straightforward.
        @Qualifier("pgVectorJdbcTemplate") org.springframework.jdbc.core.JdbcTemplate pgVectorJdbcTemplate) {

        return new ToolSearchCatalogFeeder(
            clusterElementDefinitionService, toolSearchVectorToolIndex, pgVectorJdbcTemplate);
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
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    AiHubClusterElementToolCallbacks aiHubClusterElementToolCallbacks(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService) {

        Map<String, ClusterElementToolCallback> callbacks = buildClusterElementToolCallbacks(
            clusterElementDefinitionService, connectionService);

        return new AiHubClusterElementToolCallbacks(new ArrayList<>(callbacks.values()));
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
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks, ObservationRegistry observationRegistry,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        SecurityContextRehydrator securityContextRehydrator) {

        return buildModeAdvisor(
            toolSearchVectorToolIndex, clusterElementToolCallbacks.callbacks(), observationRegistry,
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID),
            securityContextRehydrator);
    }

    @Bean
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    ToolSearchToolCallingAdvisor aiHubBuildToolSearchToolCallAdvisor(
        VectorToolIndex toolSearchVectorToolIndex,
        AiHubClusterElementToolCallbacks clusterElementToolCallbacks, ObservationRegistry observationRegistry,
        ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider,
        SecurityContextRehydrator securityContextRehydrator) {

        return buildModeAdvisor(
            toolSearchVectorToolIndex, clusterElementToolCallbacks.callbacks(), observationRegistry,
            findCatalog(globalToolCatalogProvider, ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID),
            securityContextRehydrator);
    }

    private static ToolSearchToolCallingAdvisor buildModeAdvisor(
        VectorToolIndex vectorToolIndex, List<ToolCallback> clusterElementCallbacks,
        ObservationRegistry observationRegistry, @Nullable AiHubGlobalToolCatalog globalToolCatalog,
        SecurityContextRehydrator securityContextRehydrator) {

        Set<String> additionalSessionIds = globalToolCatalog == null
            ? Set.of(ToolSearchCatalogFeeder.CATALOG_SESSION_ID)
            : Set.of(ToolSearchCatalogFeeder.CATALOG_SESSION_ID, globalToolCatalog.sessionId());

        // RC1's VectorToolIndex scopes each search to one session; MultiSessionToolIndex restores the vendored
        // multi-session union (catalog + per-mode global tools) on top of the shared index.
        ToolIndex searcher = new MultiSessionToolIndex(vectorToolIndex, additionalSessionIds);

        List<ToolCallback> callbackList = new ArrayList<>(clusterElementCallbacks);

        if (globalToolCatalog != null) {
            for (ToolCallback toolCallback : globalToolCatalog.toolCallbacks()) {
                // Discovered global tools resolve through this StaticToolCallbackResolver and execute directly on a
                // Reactor scheduler thread. Mirror AiHubSpringAIAgent.wrapToolCallback so tenant-scoped and
                // @PreAuthorize-protected service calls run under the invoking tenant + principal (and empty results
                // are guarded).
                callbackList.add(AiHubToolCallbackWrappers.wrap(toolCallback, securityContextRehydrator));
            }
        } else {
            log.warn(
                "No AiHubGlobalToolCatalog contributed for this mode — tool search runs catalog-only (no global "
                    + "static tools). automation-ai-hub should contribute one.");
        }

        ToolCallbackResolver resolver = new StaticToolCallbackResolver(callbackList);
        ToolExecutionExceptionProcessor exceptionProcessor = new DefaultToolExecutionExceptionProcessor(false);
        ToolCallingManager toolCallingManager = new DefaultToolCallingManager(
            observationRegistry, resolver, exceptionProcessor);

        // PinnedToolSearchToolCallingAdvisor pins ALWAYS_ON_TOOL_NAMES so they stay callable without a preceding
        // searchTool hit — the system prompt instructs the model to call those specialists/core tools directly by name,
        // but the stock advisor hides every static tool behind tool search and a follow-up turn calling one directly
        // would fail with "No ToolCallback found". It also keeps the advisor's OWN in-loop conversation history enabled
        // (do NOT disable it): Spring AI 2.0.0-RC1 orders the ChatMemory advisor UPSTREAM of this tool-search advisor
        // (Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER MIN+200 vs the tool loop's MIN+300), so ChatMemory wraps the
        // whole tool-execution loop and no longer re-participates per iteration. The tool loop re-traverses only the
        // downstream chain, so the advisor must carry the [assistant(tool_calls), tool-result] pairs itself. Disabling
        // its internal history (correct under the old M-series ordering, where ChatMemory sat downstream and supplied
        // history per-iteration) now strips the conversation on re-invocation: iteration N+1 is sent only [system,
        // tool_result] — the user message and the assistant(tool_calls) are gone — producing an orphaned tool_result
        // that Anthropic rejects with HTTP 400 ("tool_result without a corresponding tool_use"). Mirrors the RC1 fix in
        // AbstractAiAgentChatAction, which removed the same disableInternalConversationHistory() workaround.
        //
        // Session id read from the conversation id (mirrors the vendored advisor, which derived its search session from
        // ChatMemory.CONVERSATION_ID); MultiSessionToolIndex unions it with the catalog and per-mode global sessions.
        //
        // callbackList (cluster elements + per-mode global static tools, the same set registered with the resolver
        // above) is also handed to the advisor so a tool the model DISCOVERS via searchTool can be surfaced as callable
        // on the next iteration. The base advisor surfaces a discovered tool only when its callback is present in the
        // cachedToolCallbacks map it builds from the agent's options tool list — and the searchable catalog is
        // deliberately kept OFF that list (see the "Important asymmetry" note above). Without this, discovered catalog
        // tools resolve to nothing, the model can never call them, and it loops re-issuing searchTool until it bails.
        return new PinnedToolSearchToolCallingAdvisor(
            toolCallingManager, searcher, MAX_SEARCH_RESULTS, ChatMemory.CONVERSATION_ID, ALWAYS_ON_TOOL_NAMES,
            callbackList);
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
            clusterElementDefinitionService.getClusterElementDefinitions(BaseToolFunction.TOOLS);

        Map<String, ClusterElementToolCallback> callbacks = new HashMap<>(toolDefinitions.size());

        for (ClusterElementDefinition toolDefinition : toolDefinitions) {
            String toolName = ToolNameNormalizer.toToolName(
                toolDefinition.getComponentName(), toolDefinition.getName());

            // Description for the LLM tool definition — this is what the model sees AFTER discovery, so it should
            // echo the search-summary content. Title prefix makes it more readable in tool-call traces.
            String description = formatToolDescription(toolDefinition);

            String inputSchema;

            try {
                inputSchema = JsonSchemaGeneratorUtils.generateInputSchema(
                    toolDefinition.getProperties());
            } catch (RuntimeException exception) {
                // A single tool with a malformed property tree should not poison the entire catalog. Log and skip.
                log.warn(
                    "Skipping cluster element '{}' (component {}@{}) — failed to generate input schema: {}",
                    toolDefinition.getName(), toolDefinition.getComponentName(),
                    toolDefinition.getComponentVersion(), exception.toString());

                continue;
            }

            ClusterElementToolCallback callback = new ClusterElementToolCallback(
                toolName, description, inputSchema, toolDefinition.getComponentName(),
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

    /**
     * Re-populates the catalog after Spring has fully wired everything. {@code @PostConstruct} would be too early — the
     * feeder needs to exist, and the surrounding ApplicationContext needs to have completed init for the cluster
     * element registry to be queryable.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void populateCatalogOnAppReady(ApplicationReadyEvent event) {
        ToolSearchCatalogFeeder feeder = event.getApplicationContext()
            .getBean(ToolSearchCatalogFeeder.class);

        feeder.populate();

        // Embed the per-mode global static tool catalogs once. The feeder owns indexing of all persistent sessions
        // through its single injected searcher instance; the per-mode searcher beans only query (their
        // additionalSessionIds filter), so clear-tracking stays consistent and no rows are orphaned.
        for (AiHubGlobalToolCatalog globalToolCatalog : event.getApplicationContext()
            .getBeanProvider(AiHubGlobalToolCatalog.class)) {

            feeder.populateGlobalTools(globalToolCatalog.sessionId(), globalToolCatalog.toolCallbacks());
        }
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
     * Build-once carrier for the cluster-element executable callbacks, shared by both per-mode advisors so the full
     * cluster-element catalog is materialised a single time at startup. Wrapped in a record so Spring does not
     * auto-collect every {@link ToolCallback} bean when the advisors inject it.
     */
    record AiHubClusterElementToolCallbacks(List<ToolCallback> callbacks) {
    }
}
