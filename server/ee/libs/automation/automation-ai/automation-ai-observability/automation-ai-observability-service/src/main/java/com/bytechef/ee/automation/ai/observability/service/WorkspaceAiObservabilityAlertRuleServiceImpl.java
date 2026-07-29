/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityAlertRuleRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware delegate that pairs the platform-side {@link AiObservabilityAlertRuleService} (entity CRUD +
 * scheduler) with the rule's {@code workspace_id} column. Keeping the workspace binding here leaves the platform
 * service workspace-agnostic so non-tenant agent surfaces can reuse it.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiObservabilityAlertRuleServiceImpl implements WorkspaceAiObservabilityAlertRuleService {

    private final AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository;
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    WorkspaceAiObservabilityAlertRuleServiceImpl(
        AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService) {

        this.aiObservabilityAlertRuleRepository = aiObservabilityAlertRuleRepository;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
    }

    @Override
    public AiObservabilityAlertRule createInWorkspace(AiObservabilityAlertRule alertRule, long workspaceId) {
        alertRule.setWorkspaceId(workspaceId);

        return aiObservabilityAlertRuleService.create(alertRule);
    }

    @Override
    public void delete(long id) {
        aiObservabilityAlertRuleService.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId) {
        return aiObservabilityAlertRuleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long alertRuleId) {
        // findById rather than the service's getAlertRule: an unknown id must still yield null (the pre-collapse
        // "no membership row" answer) because callers use this as an authorization probe, not as a fetch.
        return aiObservabilityAlertRuleRepository.findById(alertRuleId)
            .map(AiObservabilityAlertRule::getWorkspaceId)
            .orElse(null);
    }
}
