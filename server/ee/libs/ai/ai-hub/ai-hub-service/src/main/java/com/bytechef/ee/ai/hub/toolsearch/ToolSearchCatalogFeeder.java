/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Loads tool catalogs into a {@link VectorToolIndex}. Two indexing modes exist:
 *
 * <ul>
 * <li><b>Workspace catalog (v1).</b> {@link #populate()} re-indexes every tool-typed cluster element under the single
 * {@link #CATALOG_SESSION_ID} session. Used as the default search corpus when a chat turn isn't bound to a
 * task-specific tool subset (e.g. brand-new task that hasn't attached anything yet).</li>
 * <li><b>Per-task subset (v2).</b> {@link #populateForTask(long, Collection)} re-indexes the supplied tool list under a
 * task-scoped session id ({@link #taskSessionId(long)}). Used when a task has been seeded from a personal-agent
 * template or has had tools attached/detached, so search queries on that task only return its in-scope tools instead of
 * the workspace-wide catalog. Pairs with {@link #clearTaskSession(long)} on task deletion to keep the vector store from
 * accumulating orphaned per-task rows.</li>
 * </ul>
 *
 * <p>
 * <b>Why per-task sessions matter:</b> the workspace-wide catalog can grow into the thousands of tool entries once a
 * workspace connects 50+ components. Even with top-K=5 search, the LLM's tool picks become noisier — Slack's
 * sendMessage and a custom Webhook's POST both surface for "send a message". A personal agent that pre-attaches the
 * three tools the agent's purpose actually needs (e.g. "summarize PRs": GitHub list-prs, GitHub get-pr-diff, Linear
 * create-issue) gives the LLM exactly those three to choose from, eliminating the noise. The session-id partition is
 * how we tell the {@link VectorToolIndex} "search this subset, not the workspace catalog" without re-architecting the
 * underlying pgvector store.
 * </p>
 *
 * <p>
 * <b>Re-index semantics:</b> each populate call clears its target session's in-memory document-id map first via
 * {@link VectorToolIndex#clearIndex(String)}, then re-issues an {@code indexTool} call per entry. Two implications:
 * </p>
 * <ul>
 * <li>After a JVM restart the in-memory id counter resets to {@code 0} but stale rows from the previous JVM still exist
 * in pgvector. The session-id metadata filter on subsequent searches keeps stale rows from being matched (they belong
 * to a session id we'd never re-issue), so they're benign. A future cleanup job can prune by metadata-filtered delete
 * on the underlying VectorStore — left as a v3 follow-up because the storage cost is negligible at the catalog sizes we
 * ship today.</li>
 * <li>Re-embedding the catalog on each boot costs cents at the {@code text-embedding-3-small} price point. The
 * {@link #populate()} path short-circuits when the current catalog's content hash matches the hash stored in
 * {@link #META_TABLE_NAME} from the previous successful populate — so cold-start cost drops to one tiny SQL query when
 * the catalog hasn't changed, and the 90-second embedding spin only happens on first boot or after a component
 * description / tool list actually changes. Per-task subset re-indexes ({@link #populateForTask}) skip the hash check
 * because those are 5–20 entries, on-demand, and the savings vs the bookkeeping aren't worth it.</li>
 * </ul>
 *
 * <p>
 * Tool name shape exposed to the LLM: <code>componentName_clusterElementName</code>. This is the lookup key the advisor
 * uses to dispatch when the LLM picks a discovered tool by name; it MUST match the {@code toolName} on the registered
 * {@link ClusterElementToolCallback} (see {@link ToolSearchAdvisorConfiguration}). The same normalization applies
 * whether the entry is indexed under the workspace catalog or a task subset.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ToolSearchCatalogFeeder {

    /**
     * Session id for the workspace-wide tool catalog — the default search corpus used for tasks that don't have an
     * explicit tool subset attached. v1 used this as the only session id; v2 keeps it as the fallback while adding
     * per-task sessions on top.
     */
    public static final String CATALOG_SESSION_ID = "ai_hub_tool_catalog";

    /**
     * Per-mode persistent sessions for the AI Hub global static tool beans (project/workflow/component/task/...).
     * Embedded once at startup via {@link #populateGlobalTools(String, List)} and unioned into the per-mode searcher so
     * they are discoverable without being re-embedded on every user turn. Split per mode because the ASK read-only
     * variants and the BUILD full set share tool names (e.g. {@code listProjects}) and would collide in one session.
     */
    public static final String GLOBAL_ASK_SESSION_ID = CATALOG_SESSION_ID + ":global:ask";

    public static final String GLOBAL_BUILD_SESSION_ID = CATALOG_SESSION_ID + ":global:build";

    /**
     * Prefix for per-task session ids, joined with the task primary key via {@code :}. Keeping the prefix distinct from
     * the workspace catalog id prevents collisions and makes the partition obvious in pgvector traces — a search trace
     * showing {@code session_id = ai_hub_tool_catalog:task:42} immediately pinpoints which task drove the corpus.
     */
    private static final String TASK_SESSION_PREFIX = CATALOG_SESSION_ID + ":task:";

    /**
     * Per-session bookkeeping for the catalog-hash skip. Lives in the same pgvector schema/datasource as the
     * vector-store tables (we already inject {@code pgVectorJdbcTemplate}, and co-locating keeps "all tool-search state
     * in one schema" easy to reason about). Created on first {@link #populate()} via {@code CREATE TABLE IF NOT EXISTS}
     * so deployments don't need a separate Liquibase migration plumbed into the pgvector datasource — same pattern
     * Spring AI's {@code PgVectorStore} uses for its own table.
     */
    static final String META_TABLE_NAME = "ai_hub_tool_search_catalog_meta";

    private static final Logger log = LoggerFactory.getLogger(ToolSearchCatalogFeeder.class);

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final VectorToolIndex vectorToolIndex;
    private final JdbcTemplate pgVectorJdbcTemplate;

    private volatile boolean metaTableEnsured;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ToolSearchCatalogFeeder(
        ClusterElementDefinitionService clusterElementDefinitionService, VectorToolIndex vectorToolIndex,
        JdbcTemplate pgVectorJdbcTemplate) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.vectorToolIndex = vectorToolIndex;
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
    }

    /**
     * Returns the session id used to index the given task's tool subset. Stable across calls so
     * {@link #populateForTask(long, Collection)} and {@link #clearTaskSession(long)} agree on which pgvector partition
     * to write/clear.
     */
    public static String taskSessionId(long taskId) {
        return TASK_SESSION_PREFIX + taskId;
    }

    /**
     * Re-populates the workspace-wide tool catalog. Skips the embedding-and-write loop entirely when the catalog's
     * content hash matches the hash recorded by the previous successful populate — saving 90 seconds and several
     * hundred embedding API calls on every cold start where the catalog is unchanged. Idempotent: every call either
     * short-circuits on hash match, or clears the session's in-memory tool-id tracking and re-issues an
     * {@code indexTool} per cluster element marked tool-eligible by its component author.
     */
    @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
    public void populate() {
        List<ClusterElementDefinition> toolDefinitions =
            clusterElementDefinitionService.getClusterElementDefinitions(BaseToolFunction.TOOLS);

        // Compute the canonical hash from (toolName, summary) pairs sorted by toolName. Skipping entries with no
        // summary mirrors the indexCatalog loop — they never make it into pgvector, so they must not influence the
        // hash either or we'd thrash repopulate every boot.
        List<CatalogEntry> entries = new ArrayList<>();

        for (ClusterElementDefinition toolDefinition : toolDefinitions) {
            String summary = buildSummary(toolDefinition);

            if (summary == null || summary.isBlank()) {
                continue;
            }

            String toolName = ToolNameNormalizer.toToolName(
                toolDefinition.getComponentName(), toolDefinition.getName());

            entries.add(new CatalogEntry(toolName, summary));
        }

        String currentHash = computeCatalogHash(entries);

        ensureMetaTable();

        Optional<String> storedHash = readStoredHash(CATALOG_SESSION_ID);

        if (storedHash.isPresent() && storedHash.get()
            .equals(currentHash)) {

            log.info(
                "Tool search catalog unchanged (hash matches {} indexed entries under session {}); skipping re-embedding",
                entries.size(), CATALOG_SESSION_ID);

            return;
        }

        int indexed = indexCatalog(CATALOG_SESSION_ID, toolDefinitions);

        writeStoredHash(CATALOG_SESSION_ID, currentHash, indexed);

        log.info(
            "Tool search catalog populated: indexed {} of {} tool-typed cluster elements under session {}",
            indexed, toolDefinitions.size(), CATALOG_SESSION_ID);
    }

    /**
     * Re-populates a task-scoped tool subset. The supplied {@code taskTools} list is expected to be the tools currently
     * bound to the task (typically pulled from {@code AiHubTaskToolFacade.listTaskTools(taskId)} mapped to the
     * component-typed cluster-element triple). Resolution from binding to cluster-element definition happens here so
     * the caller doesn't need a {@code ClusterElementDefinitionService} reference.
     *
     * <p>
     * <b>Safe to re-call:</b> the index is cleared and re-built on every invocation. Call sites:
     * </p>
     * <ul>
     * <li>AiHubTask creation (after personal-agent template tools have been copied) — populates the initial
     * subset.</li>
     * <li>Tool attached/detached on the task — refreshes the subset so the next search sees the change before the next
     * chat turn.</li>
     * <li>AiHubTask delete — pair with {@link #clearTaskSession(long)} to release the entries.</li>
     * </ul>
     *
     * <p>
     * Bindings whose cluster element no longer exists in the catalog (component upgrade dropped the action) are logged
     * at WARN and skipped. Re-attempting the populate after the user removes the dead binding will produce a clean
     * subset.
     * </p>
     *
     * @param taskId    the task primary key
     * @param taskTools the task's currently bound tools as (componentName, componentVersion, clusterElementName)
     *                  triples; an empty collection clears the session
     */
    public void populateForTask(long taskId, Collection<TaskToolReference> taskTools) {
        String sessionId = taskSessionId(taskId);

        if (taskTools == null || taskTools.isEmpty()) {
            // No subset to index — clear so a previously-populated session doesn't leak into search results after
            // the user has detached every tool. Equivalent to calling clearTaskSession explicitly; provided
            // here so call sites don't have to special-case the empty-collection branch.
            vectorToolIndex.clearIndex(sessionId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "AiHubTask {} has no attached tools; cleared session {} (search will fall back to catalog)",
                    taskId, sessionId);
            }

            return;
        }

        List<ClusterElementDefinition> resolvedDefinitions = new ArrayList<>(taskTools.size());

        for (TaskToolReference reference : taskTools) {
            try {
                ClusterElementDefinition definition = clusterElementDefinitionService.getClusterElementDefinition(
                    reference.componentName(), reference.componentVersion(), reference.clusterElementName());

                resolvedDefinitions.add(definition);
            } catch (RuntimeException exception) {
                // Component upgrade dropped the action, OR the binding was attached against a component version
                // that no longer ships. Log + skip rather than fail the populate — the user's other bound tools
                // still get indexed and the task remains usable.
                log.warn(
                    "Skipping task tool {}/{} v{} for task {} — cluster element no longer in catalog",
                    reference.componentName(), reference.clusterElementName(), reference.componentVersion(),
                    taskId, exception);
            }
        }

        int indexed = indexCatalog(sessionId, resolvedDefinitions);

        log.info(
            "Tool search subset populated for task {}: indexed {} of {} attached tools under session {}",
            taskId, indexed, taskTools.size(), sessionId);
    }

    /**
     * Re-populates a persistent global static-tool session from the supplied tool callbacks. Mirrors
     * {@link #populate()} (hash-skip + clear-then-index) but sources its {@code (name, summary)} entries from
     * {@link ToolCallback} definitions rather than cluster-element definitions. Called once per mode at startup so the
     * AI Hub static tool beans embed a single time instead of being re-embedded by the advisor's per-turn self-index.
     *
     * @param sessionId     the persistent session id ({@link #GLOBAL_ASK_SESSION_ID} or
     *                      {@link #GLOBAL_BUILD_SESSION_ID})
     * @param toolCallbacks the static tool callbacks to index; entries with a blank description are skipped
     */
    @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
    public void populateGlobalTools(String sessionId, List<ToolCallback> toolCallbacks) {
        List<CatalogEntry> entries = new ArrayList<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            String summary = toolDefinition.description();

            if (summary == null || summary.isBlank()) {
                continue;
            }

            entries.add(new CatalogEntry(toolDefinition.name(), summary));
        }

        String currentHash = computeCatalogHash(entries);

        ensureMetaTable();

        Optional<String> storedHash = readStoredHash(sessionId);

        if (storedHash.isPresent() && storedHash.get()
            .equals(currentHash)) {

            log.info(
                "Global tool session unchanged (hash matches {} entries under session {}); skipping re-embedding",
                entries.size(), sessionId);

            return;
        }

        int indexed = indexEntries(sessionId, entries);

        writeStoredHash(sessionId, currentHash, indexed);

        log.info(
            "Global tool session populated: indexed {} of {} static tools under session {}",
            indexed, toolCallbacks.size(), sessionId);
    }

    /**
     * Removes every entry from the given task's session. Used on task deletion to keep the vector store from
     * accumulating orphaned per-task rows. Idempotent — calling with an unknown task id is a benign no-op (no-op clear
     * in the underlying searcher).
     */
    public void clearTaskSession(long taskId) {
        String sessionId = taskSessionId(taskId);

        vectorToolIndex.clearIndex(sessionId);

        if (log.isDebugEnabled()) {
            log.debug("Cleared tool search session {} for task {}", sessionId, taskId);
        }
    }

    /**
     * Shared indexing routine used by both the workspace-catalog and per-task paths. Builds the filtered
     * {@link CatalogEntry} list (skipping cluster elements with a blank summary) then delegates to
     * {@link #indexEntries(String, List)} for the clear-then-index work. Returns the number of entries actually
     * indexed.
     */
    private int indexCatalog(String sessionId, List<ClusterElementDefinition> toolDefinitions) {
        List<CatalogEntry> entries = new ArrayList<>();

        for (ClusterElementDefinition toolDefinition : toolDefinitions) {
            String toolName = ToolNameNormalizer.toToolName(
                toolDefinition.getComponentName(), toolDefinition.getName());
            String summary = buildSummary(toolDefinition);

            if (summary == null || summary.isBlank()) {
                // No description means the embedding would be the bare component+action name — search quality
                // collapses. Skip rather than poison the index with high-noise vectors. Component author should
                // add a description if they want their tool discoverable from chat.
                log.debug("Skipping tool-typed cluster element {} (no description)", toolName);

                continue;
            }

            entries.add(new CatalogEntry(toolName, summary));
        }

        return indexEntries(sessionId, entries);
    }

    /**
     * Shared clear-then-index routine. Clears the target persistent session (restart-safe, by metadata) then issues one
     * {@code indexTool} per entry. Used by the workspace catalog, per-task subset, and global tool paths.
     */
    private int indexEntries(String sessionId, List<CatalogEntry> entries) {
        vectorToolIndex.clearIndex(sessionId);

        int indexed = 0;

        for (CatalogEntry entry : entries) {
            ToolReference reference = ToolReference.builder()
                .toolName(entry.toolName())
                .summary(entry.summary())
                .build();

            vectorToolIndex.indexTool(sessionId, reference);

            indexed++;
        }

        return indexed;
    }

    /**
     * Combines the human-readable description with the title (when present) to give the embedding model maximum surface
     * area for semantic matching. The component author's description is the load-bearing piece — title is a short label
     * and may not contain enough signal alone.
     */
    private static String buildSummary(ClusterElementDefinition toolDefinition) {
        String description = toolDefinition.getDescription();
        String title = toolDefinition.getTitle();

        if (description != null && !description.isBlank()) {
            return title != null && !title.isBlank() ? title + ". " + description : description;
        }

        return title != null && !title.isBlank() ? title : null;
    }

    /**
     * Lightweight DTO for {@link #populateForTask(long, Collection)} input. Decouples the feeder from the task-tool
     * persistence shape — callers can adapt {@code AiHubTaskToolBinding} (full join including connection/parameters)
     * into this triple without dragging the binding's connection-typed dependencies into the feeder's API surface.
     */
    public record TaskToolReference(String componentName, int componentVersion, String clusterElementName) {

        public TaskToolReference {
            Objects.requireNonNull(componentName, "componentName");
            Objects.requireNonNull(clusterElementName, "clusterElementName");
        }
    }

    /**
     * Hash-input pair. Kept as a record so the canonical sort + serialisation is centralised here rather than scattered
     * across populate(). Hash recipe: SHA-256 over the UTF-8 bytes of {@code toolName   summary  } for each entry, in
     * {@code toolName} order. The NUL separator is safe because tool names and summaries are plain text from component
     * metadata and never contain NUL.
     */
    record CatalogEntry(String toolName, String summary) {
    }

    static String computeCatalogHash(List<CatalogEntry> entries) {
        List<CatalogEntry> sorted = new ArrayList<>(entries);

        sorted.sort(Comparator.comparing(CatalogEntry::toolName));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (CatalogEntry entry : sorted) {
                digest.update(entry.toolName()
                    .getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.summary()
                    .getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }

            return HexFormat.of()
                .formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 is mandated by every JDK we run on; surfacing this would be a deployment defect, not a runtime
            // condition the caller can recover from. Wrap so it propagates as a clear runtime exception.
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private void ensureMetaTable() {
        if (metaTableEnsured) {
            return;
        }

        pgVectorJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + META_TABLE_NAME + " ("
                + "session_id TEXT PRIMARY KEY,"
                + "catalog_hash TEXT NOT NULL,"
                + "tool_count INTEGER NOT NULL,"
                + "last_indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");

        metaTableEnsured = true;
    }

    private Optional<String> readStoredHash(String sessionId) {
        try {
            String hash = pgVectorJdbcTemplate.queryForObject(
                "SELECT catalog_hash FROM " + META_TABLE_NAME + " WHERE session_id = ?",
                String.class, sessionId);

            return Optional.ofNullable(hash);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private void writeStoredHash(String sessionId, String hash, int toolCount) {
        pgVectorJdbcTemplate.update(
            "INSERT INTO " + META_TABLE_NAME + " (session_id, catalog_hash, tool_count, last_indexed_at)"
                + " VALUES (?, ?, ?, NOW())"
                + " ON CONFLICT (session_id) DO UPDATE"
                + " SET catalog_hash = EXCLUDED.catalog_hash,"
                + "     tool_count = EXCLUDED.tool_count,"
                + "     last_indexed_at = NOW()",
            sessionId, hash, toolCount);
    }
}
