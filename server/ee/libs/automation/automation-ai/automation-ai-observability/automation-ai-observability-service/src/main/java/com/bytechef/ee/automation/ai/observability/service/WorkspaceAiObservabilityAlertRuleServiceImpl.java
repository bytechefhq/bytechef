/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityAlertRuleRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware delegate that pairs the platform-side {@link AiObservabilityAlertRuleService} (entity CRUD +
 * scheduler) with writes to the {@code workspace_ai_observability_alert_rule} membership table. Owning workspace ↔ rule
 * pairing here keeps the platform service workspace-agnostic so non-tenant agent surfaces can reuse it.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiObservabilityAlertRuleServiceImpl implements WorkspaceAiObservabilityAlertRuleService {

    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final WorkspaceAiObservabilityAlertRuleRepository workspaceAiObservabilityAlertRuleRepository;

    WorkspaceAiObservabilityAlertRuleServiceImpl(
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService,
        WorkspaceAiObservabilityAlertRuleRepository workspaceAiObservabilityAlertRuleRepository) {

        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.workspaceAiObservabilityAlertRuleRepository = workspaceAiObservabilityAlertRuleRepository;
    }

    @Override
    public AiObservabilityAlertRule createInWorkspace(AiObservabilityAlertRule alertRule, long workspaceId) {
        AiObservabilityAlertRule savedAlertRule = aiObservabilityAlertRuleService.create(alertRule);

        workspaceAiObservabilityAlertRuleRepository.save(
            new WorkspaceAiObservabilityAlertRule(savedAlertRule.getId(), workspaceId));

        return savedAlertRule;
    }

    @Override
    public void delete(long id) {
        workspaceAiObservabilityAlertRuleRepository.findByAiObservabilityAlertRuleId(id)
            .ifPresent(membership -> workspaceAiObservabilityAlertRuleRepository.deleteById(membership.getId()));

        aiObservabilityAlertRuleService.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityAlertRuleRepository.findAllAlertRulesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long alertRuleId) {
        return workspaceAiObservabilityAlertRuleRepository.findByAiObservabilityAlertRuleId(alertRuleId)
            .map(WorkspaceAiObservabilityAlertRule::getWorkspaceId)
            .orElse(null);
    }
}
