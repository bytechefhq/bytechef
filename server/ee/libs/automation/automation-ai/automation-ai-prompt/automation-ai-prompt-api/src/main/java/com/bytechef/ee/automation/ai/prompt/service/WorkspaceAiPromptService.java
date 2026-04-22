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
 * Workspace-scoped operations on {@link AiPrompt}. Mirrors {@code WorkspaceAiEvalRuleService} in automation-ai-eval —
 * the platform service ({@code AiPromptService}) handles workspace-agnostic CRUD; this service owns the workspace
 * membership row and workspace-scoped queries that join through {@code workspace_ai_prompt}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiPromptService {

    AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId);

    void deleteInWorkspace(long promptId);

    Long getWorkspaceId(long promptId);

    List<AiPrompt> getPromptsByWorkspace(Long workspaceId);

    Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name);
}
