/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt.repository;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic CRUD on {@code ai_prompt}. Workspace-aware queries (joining through {@code workspace_ai_prompt})
 * live on {@code WorkspaceAiPromptRepository} in automation-ai-prompt.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiPromptRepository extends ListCrudRepository<AiPrompt, Long> {
}
