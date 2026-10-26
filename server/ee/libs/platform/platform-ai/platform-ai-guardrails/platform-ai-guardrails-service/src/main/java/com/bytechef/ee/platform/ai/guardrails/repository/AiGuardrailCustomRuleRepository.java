/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.repository;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGuardrailCustomRuleRepository extends ListCrudRepository<AiGuardrailCustomRule, Long> {

    List<AiGuardrailCustomRule> findAllByWorkspaceIdOrderByTypeAsc(long workspaceId);

    List<AiGuardrailCustomRule> findAllByWorkspaceIdAndEnabledTrueOrderByTypeAsc(long workspaceId);

    Optional<AiGuardrailCustomRule> findByIdAndWorkspaceId(long id, long workspaceId);

    long countByWorkspaceId(long workspaceId);
}
