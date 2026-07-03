/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Opaque carrier for a per-mode set of AI Hub global static tool callbacks plus the persistent search session they are
 * embedded under. Contributed by {@code automation-ai-hub} (which can see the automation/platform tool beans) and
 * consumed by {@code ToolSearchAdvisorConfiguration} in {@code platform-ai-hub} (which cannot). Wrapping the
 * {@link List} in a record prevents Spring from auto-collecting every {@link ToolCallback} bean in the context when the
 * config injects it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiHubGlobalToolCatalog(String sessionId, List<ToolCallback> toolCallbacks) {

    public AiHubGlobalToolCatalog {
        java.util.Objects.requireNonNull(sessionId, "sessionId");
        toolCallbacks = List.copyOf(toolCallbacks);
    }
}
