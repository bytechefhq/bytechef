/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.repository;

import com.bytechef.ee.platform.ai.llm.usage.WorkspaceAiLlmUsage;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace ↔ AI LLM usage membership repository. The recorder writes to this on the hot path; readers don't normally
 * consult it directly (queries on AiLlmUsageRepository JOIN through it).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiLlmUsageRepository extends ListCrudRepository<WorkspaceAiLlmUsage, Long> {

    Optional<WorkspaceAiLlmUsage> findByAiLlmUsageId(long aiLlmUsageId);
}
