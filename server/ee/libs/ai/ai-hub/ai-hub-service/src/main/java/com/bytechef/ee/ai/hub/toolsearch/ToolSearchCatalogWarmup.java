/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-shot, lazy warm-up of the tool-search pgvector index, shared by both per-mode advisors.
 *
 * <p>
 * Populating the index ({@link ToolSearchCatalogFeeder#populate()} plus the per-mode
 * {@link ToolSearchCatalogFeeder#populateGlobalTools(String, List)} calls) enumerates every tool-typed cluster element,
 * which forces the whole component definition catalog to load. Running it at {@code ApplicationReadyEvent} would pay
 * that on the boot thread, undoing the build-time component index that keeps startup lazy. Instead {@link #warmUp()} is
 * invoked from the advisor's first loop initialization — i.e. the first AI Hub chat turn — and runs the populate
 * <b>synchronously</b> so the index is guaranteed populated before that turn's first {@code searchTool} query returns.
 * </p>
 *
 * <p>
 * The catalog is a single cross-tenant corpus: the feeder writes both its vector rows and its hash-bookkeeping table
 * into one fixed, schema-qualified schema (default {@code public}) regardless of the calling thread's tenant, so this
 * warm-up can run under the first chat turn's tenant without leaking the shared catalog into that user's per-tenant
 * vector schema — the tenant-independence lives in {@link ToolSearchCatalogFeeder}, not here.
 * </p>
 *
 * <p>
 * Idempotent and thread-safe: the first caller runs the populate under a monitor while concurrent first-turn callers
 * block until it completes, then every later call returns on the volatile fast path. A failed populate leaves the flag
 * unset (logged, not propagated) so the next turn retries rather than breaking the chat or permanently degrading
 * search. When no {@link ToolSearchCatalogFeeder} is present (no fixed-key copilot embedding model configured) the
 * warm-up is a no-op.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ToolSearchCatalogWarmup {

    private static final Logger log = LoggerFactory.getLogger(ToolSearchCatalogWarmup.class);

    private final @Nullable ToolSearchCatalogFeeder feeder;
    private final List<AiHubGlobalToolCatalog> globalToolCatalogs;

    private volatile boolean populated;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    ToolSearchCatalogWarmup(
        @Nullable ToolSearchCatalogFeeder feeder, List<AiHubGlobalToolCatalog> globalToolCatalogs) {

        this.feeder = feeder;
        this.globalToolCatalogs = List.copyOf(globalToolCatalogs);
    }

    void warmUp() {
        if (populated || feeder == null) {
            return;
        }

        synchronized (this) {
            if (populated) {
                return;
            }

            try {
                feeder.populate();

                // Embed the per-mode global static tool catalogs once. The feeder owns indexing of all persistent
                // sessions through its single injected searcher instance; the per-mode searcher beans only query
                // (their additionalSessionIds filter), so clear-tracking stays consistent and no rows are orphaned.
                for (AiHubGlobalToolCatalog globalToolCatalog : globalToolCatalogs) {
                    feeder.populateGlobalTools(globalToolCatalog.sessionId(), globalToolCatalog.toolCallbacks());
                }

                populated = true;
            } catch (RuntimeException exception) {
                log.warn(
                    "Tool search catalog warm-up failed; search may be degraded until the next turn retries it",
                    exception);
            }
        }
    }
}
