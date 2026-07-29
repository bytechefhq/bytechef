/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.service;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped operations on {@link AiPrompt}. The platform service ({@code AiPromptService}) handles
 * workspace-agnostic CRUD; this service owns the workspace binding (the {@code ai_prompt.workspace_id} column) and the
 * workspace-scoped queries that filter it.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiPromptService {

    AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId);

    void deleteInWorkspace(long promptId);

    /**
     * The owning workspace of {@code promptId}, or {@code null} when the prompt has none or does not exist. Callers use
     * it as an authorization probe, so an unknown id answers null rather than throwing.
     */
    Long getWorkspaceId(long promptId);

    List<AiPrompt> getPromptsByWorkspace(Long workspaceId);

    Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name);
}
