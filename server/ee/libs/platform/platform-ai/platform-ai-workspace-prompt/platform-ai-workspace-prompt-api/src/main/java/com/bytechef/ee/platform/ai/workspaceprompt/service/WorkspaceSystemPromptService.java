/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.service;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * CRUD for the per-workspace system prompt. Values are always stored stripped; a blank or null prompt on save deletes
 * the row.
 *
 * @version ee
 */
public interface WorkspaceSystemPromptService {

    /**
     * Returns the workspace's stored prompt, or empty when none is set. Never returns a blank string.
     */
    Optional<String> fetchWorkspaceSystemPrompt(long workspaceId);

    /**
     * Saves the stripped prompt for the workspace and returns it. A null/blank {@code prompt} deletes the stored row
     * and returns empty. Throws {@link IllegalArgumentException} when the stripped prompt exceeds
     * {@link com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt#MAX_LENGTH}.
     */
    Optional<String> saveWorkspaceSystemPrompt(long workspaceId, @Nullable String prompt);
}
