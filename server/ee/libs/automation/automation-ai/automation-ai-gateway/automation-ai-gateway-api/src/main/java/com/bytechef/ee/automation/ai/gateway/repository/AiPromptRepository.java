/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiPromptRepository extends ListCrudRepository<AiPrompt, Long> {

    List<AiPrompt> findAllByWorkspaceId(Long workspaceId);

    Optional<AiPrompt> findByWorkspaceIdAndProjectIdAndName(
        Long workspaceId, Long projectId, String name);
}
