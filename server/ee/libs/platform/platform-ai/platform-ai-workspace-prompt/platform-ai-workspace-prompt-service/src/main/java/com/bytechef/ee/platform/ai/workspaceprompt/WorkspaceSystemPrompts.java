/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt;

import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hot-path lookup for the workspace system prompt, memoized for {@link #CACHE_TTL} so agent tool loops (one advisor
 * pass per model iteration) do not hit the property store on every call. Registered unconditionally, like the
 * {@code AiGuardrails} engine — inert (all lookups miss) when no workspace has a prompt set.
 *
 * <p>
 * Fail-open by contract: a lookup error logs and returns {@code null} (no prompt), never failing the turn. A saved
 * change propagates to running agents within the TTL; there is deliberately no cross-node invalidation.
 * </p>
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceSystemPrompts {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSystemPrompts.class);

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final WorkspaceSystemPromptService workspaceSystemPromptService;
    private final Cache<Long, Optional<String>> promptCache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .build();

    public WorkspaceSystemPrompts(WorkspaceSystemPromptService workspaceSystemPromptService) {
        this.workspaceSystemPromptService = workspaceSystemPromptService;
    }

    /**
     * Returns the workspace's prompt, or {@code null} when the workspace is unknown ({@code null}), has no prompt set,
     * or the lookup failed. This feature is workspace-only — a {@code null} workspace id never falls back to any
     * tenant-wide default.
     */
    public @Nullable String fetchPrompt(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return null;
        }

        try {
            Optional<String> prompt = promptCache.get(
                workspaceId, id -> workspaceSystemPromptService.fetchWorkspaceSystemPrompt(id));

            return prompt.orElse(null);
        } catch (RuntimeException exception) {
            log.warn("Workspace system prompt lookup failed for workspace {}; skipping", workspaceId, exception);

            return null;
        }
    }
}
