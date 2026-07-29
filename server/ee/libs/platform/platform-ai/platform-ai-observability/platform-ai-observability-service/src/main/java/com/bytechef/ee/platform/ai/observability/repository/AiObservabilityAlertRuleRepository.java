/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityAlertRuleRepository extends ListCrudRepository<AiObservabilityAlertRule, Long> {

    List<AiObservabilityAlertRule> findAllByEnabled(boolean enabled);

    /**
     * Rules owned by one workspace. A rule whose {@code workspace_id} is null belongs to no workspace and is invisible
     * here, which is the intended behavior for a workspace-scoped listing.
     */
    List<AiObservabilityAlertRule> findAllByWorkspaceId(Long workspaceId);
}
