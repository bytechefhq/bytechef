/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Shared wrapping for AI Hub tool callbacks: (1) {@link NonEmptyToolCallback} so an empty tool result does not trigger
 * an Anthropic "non-empty content" HTTP 400, then (2) the shared {@link RehydrateContextToolCallback} so tenant-scoped
 * and {@code @PreAuthorize}-protected service calls run under the invoking tenant + principal on Reactor scheduler
 * threads. When the rehydrator is absent the context layer is skipped.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolCallbackWrappers {

    private AiHubToolCallbackWrappers() {
    }

    public static ToolCallback wrap(
        ToolCallback callback, @Nullable SecurityContextRehydrator securityContextRehydrator) {

        ToolCallback nonEmpty = NonEmptyToolCallback.wrap(callback);

        if (securityContextRehydrator == null) {
            return nonEmpty;
        }

        return RehydrateContextToolCallback.wrap(nonEmpty, securityContextRehydrator);
    }
}
