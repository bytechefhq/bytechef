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
