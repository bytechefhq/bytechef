/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Facade for the AI Hub task-artifact audit listing. Hosts the {@code ADMIN} authorization guard (and the workspace
 * access check) so it is enforced for every caller of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskArtifactFacade {

    AiHubTaskArtifactPage getAiHubTaskArtifacts(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId, @Nullable AiHubTaskArtifactKind kind,
        @Nullable Long from, @Nullable Long to, @Nullable Integer page, @Nullable Integer size);

    /**
     * Page envelope for the audit listing query. {@code pageClamped}/{@code sizeClamped} surface the silent truncation
     * applied when a caller supplies a value above the page/size upper bounds so the UI can show a "you asked for
     * size=N, served size=M" hint rather than silently rendering a smaller page.
     */
    record AiHubTaskArtifactPage(
        List<AiHubTaskArtifact> items, long totalCount, boolean hasMore,
        boolean pageClamped, boolean sizeClamped) {

        public AiHubTaskArtifactPage {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
