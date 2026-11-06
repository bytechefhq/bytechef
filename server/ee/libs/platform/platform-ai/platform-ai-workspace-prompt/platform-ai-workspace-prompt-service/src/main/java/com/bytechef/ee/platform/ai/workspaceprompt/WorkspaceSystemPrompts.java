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
