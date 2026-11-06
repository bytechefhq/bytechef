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
