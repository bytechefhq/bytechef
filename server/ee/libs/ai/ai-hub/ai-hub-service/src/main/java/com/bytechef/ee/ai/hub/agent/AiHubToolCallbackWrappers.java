/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Shared wrapping for AI Hub tool callbacks: (1) {@link NonEmptyToolCallback} so an empty tool result does not trigger
 * an Anthropic "non-empty content" HTTP 400, then (2) the {@link AiHubApprovalGate} so a flagged tool call waits for a
 * person's approval instead of executing, then (3) the shared {@link RehydrateContextToolCallback} so tenant-scoped and
 * {@code @PreAuthorize}-protected service calls run under the invoking tenant + principal on Reactor scheduler threads.
 * When the gate (or the rehydrator) is absent, that layer is skipped.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolCallbackWrappers {

    private AiHubToolCallbackWrappers() {
    }

    /**
     * Kept for callers that don't pass an {@link AiHubApprovalGate}; delegates with a {@code null} gate, so the
     * approval gate layer is skipped.
     */
    public static ToolCallback wrap(
        ToolCallback callback, @Nullable SecurityContextRehydrator securityContextRehydrator) {

        return wrap(callback, securityContextRehydrator, null);
    }

    public static ToolCallback wrap(
        ToolCallback callback, @Nullable SecurityContextRehydrator securityContextRehydrator,
        @Nullable AiHubApprovalGate approvalGate) {

        ToolCallback nonEmpty = NonEmptyToolCallback.wrap(callback);
        ToolCallback gated = approvalGate == null ? nonEmpty : approvalGate.wrap(nonEmpty);

        if (securityContextRehydrator == null) {
            return gated;
        }

        return RehydrateContextToolCallback.wrap(gated, securityContextRehydrator);
    }
}
