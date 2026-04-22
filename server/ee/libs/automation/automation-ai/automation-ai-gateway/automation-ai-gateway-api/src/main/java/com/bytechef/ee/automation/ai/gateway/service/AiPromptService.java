/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiPromptService {

    AiPrompt create(AiPrompt prompt);

    void delete(long id);

    AiPrompt getPrompt(long id);

    List<AiPrompt> getPromptsByWorkspace(Long workspaceId);

    Optional<AiPrompt> getPromptByName(Long workspaceId, Long projectId, String name);

    AiPrompt update(AiPrompt prompt);
}
