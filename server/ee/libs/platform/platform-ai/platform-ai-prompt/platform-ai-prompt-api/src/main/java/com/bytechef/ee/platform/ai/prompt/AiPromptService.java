/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt;

/**
 * Workspace-agnostic CRUD on {@link AiPrompt}. Workspace-aware queries (by workspace, by-workspace+project+name) live
 * in {@code com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService} — mirrors the
 * {@code platform-connection.ConnectionService} / {@code automation-configuration.WorkspaceConnectionService} split.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiPromptService {

    AiPrompt create(AiPrompt prompt);

    void delete(long id);

    AiPrompt getPrompt(long id);

    AiPrompt update(AiPrompt prompt);
}
