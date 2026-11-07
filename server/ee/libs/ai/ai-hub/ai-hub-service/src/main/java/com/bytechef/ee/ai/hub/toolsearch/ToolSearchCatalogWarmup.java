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
