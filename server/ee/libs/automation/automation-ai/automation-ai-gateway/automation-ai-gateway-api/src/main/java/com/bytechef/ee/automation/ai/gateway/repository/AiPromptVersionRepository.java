/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiPromptVersionRepository
    extends ListCrudRepository<AiPromptVersion, Long> {

    List<AiPromptVersion> findAllByPromptId(Long promptId);

    List<AiPromptVersion> findAllByPromptIdOrderByVersionNumberDesc(Long promptId);

    Optional<AiPromptVersion> findByPromptIdAndActiveAndEnvironment(
        Long promptId, boolean active, String environment);

    Optional<AiPromptVersion> findTopByPromptIdOrderByVersionNumberDesc(Long promptId);

    List<AiPromptVersion> findAllByPromptIdAndEnvironmentAndActive(
        Long promptId, String environment, boolean active);
}
