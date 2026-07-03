/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AutoMemoryToolSupport {

    private AutoMemoryToolSupport() {
    }

    static AiHubToolInvocationContext resolveContext(@Nullable ToolContext toolContext) {
        AiHubToolInvocationContext context = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (context != null) {
            return context;
        }

        // Return an empty sentinel so callbacks can use the same null-field guards regardless of whether the
        // tool context was missing entirely or merely missing the workspace/user keys. Without this, every
        // callback would have to add an extra null check before reading workspaceId/userId, which is brittle —
        // a forgotten check turns a missing context into an NPE that bubbles up as an opaque tool failure
        // instead of the actionable "Workspace context unavailable" message the rest of the surface relies on.
        return new AiHubToolInvocationContext(null, null, null, null, null);
    }

    /**
     * Returns the actionable tool-error message when the resolved context is missing the workspace or user id required
     * to scope a memory operation, or {@code null} when both are present. Every memory storage operation runs the same
     * two-step guard ("memories are scoped per (workspace, user)") immediately after
     * {@link #resolveContext(ToolContext)}; pulling it here keeps the wording — and the order in which the two are
     * checked — identical across every caller instead of hand-maintained copies that could silently drift.
     */
    @Nullable
    static String contextError(AiHubToolInvocationContext context) {
        if (context.workspaceId() == null) {
            return "Workspace context unavailable - open this chat from the AI Hub of a workspace.";
        }

        if (context.userId() == null) {
            return "User context unavailable - memories are scoped per user and require an authenticated turn.";
        }

        return null;
    }

    /**
     * Derives the memory {@code name} slug from a tool-supplied path: strips an optional {@code .md} suffix and lower-
     * cases. The slug — not the path — is the authoritative key for
     * {@link com.bytechef.platform.ai.auto.memory.AiAutoMemory}.
     */
    static String toMemoryName(String path) {
        String trimmed = path == null ? "" : path.trim();

        if (trimmed.endsWith(".md")) {
            trimmed = trimmed.substring(0, trimmed.length() - ".md".length());
        }

        return trimmed.toLowerCase();
    }
}
