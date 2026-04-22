/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityAlertRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.scheduler.AlertScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiObservabilityAlertRuleServiceImpl implements AiObservabilityAlertRuleService {

    private final AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository;
    private final AlertScheduler alertScheduler;

    AiObservabilityAlertRuleServiceImpl(
        AiObservabilityAlertRuleRepository aiObservabilityAlertRuleRepository,
        AlertScheduler alertScheduler) {

        this.aiObservabilityAlertRuleRepository = aiObservabilityAlertRuleRepository;
        this.alertScheduler = alertScheduler;
    }

    @Override
    public AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule) {
        Validate.notNull(alertRule, "alertRule must not be null");
        Validate.isTrue(alertRule.getId() == null, "alertRule id must be null for creation");

        AiObservabilityAlertRule savedAlertRule = aiObservabilityAlertRuleRepository.save(alertRule);

        if (savedAlertRule.isEnabled()) {
            alertScheduler.scheduleAlertEvaluation(savedAlertRule.getId(), savedAlertRule.getWindowMinutes());
        }

        return savedAlertRule;
    }

    @Override
    public void delete(long id) {
        alertScheduler.cancelAlertEvaluation(id);

        aiObservabilityAlertRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityAlertRule getAlertRule(long id) {
        return aiObservabilityAlertRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityAlertRule not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId) {
        return aiObservabilityAlertRuleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertRule> getEnabledAlertRules() {
        return aiObservabilityAlertRuleRepository.findAllByEnabled(true);
    }

    @Override
    public AiObservabilityAlertRule snooze(long id, Instant until) {
        Validate.notNull(until, "until must not be null");

        AiObservabilityAlertRule alertRule = getAlertRule(id);

        alertRule.setSnoozedUntil(until);

        return aiObservabilityAlertRuleRepository.save(alertRule);
    }

    @Override
    public AiObservabilityAlertRule unsnooze(long id) {
        AiObservabilityAlertRule alertRule = getAlertRule(id);

        alertRule.setSnoozedUntil(null);

        return aiObservabilityAlertRuleRepository.save(alertRule);
    }

    @Override
    public AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule) {
        Validate.notNull(alertRule, "alertRule must not be null");
        Validate.notNull(alertRule.getId(), "alertRule id must not be null for update");

        AiObservabilityAlertRule savedAlertRule = aiObservabilityAlertRuleRepository.save(alertRule);

        if (savedAlertRule.isEnabled()) {
            alertScheduler.scheduleAlertEvaluation(savedAlertRule.getId(), savedAlertRule.getWindowMinutes());
        } else {
            alertScheduler.cancelAlertEvaluation(savedAlertRule.getId());
        }

        return savedAlertRule;
    }
}
