/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityAlertEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class AiObservabilityAlertEventServiceImpl implements AiObservabilityAlertEventService {

    private final AiObservabilityAlertEventRepository aiObservabilityAlertEventRepository;

    AiObservabilityAlertEventServiceImpl(
        AiObservabilityAlertEventRepository aiObservabilityAlertEventRepository) {

        this.aiObservabilityAlertEventRepository = aiObservabilityAlertEventRepository;
    }

    @Override
    public void deleteOlderThan(Instant date) {
        Validate.notNull(date, "date must not be null");

        aiObservabilityAlertEventRepository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        aiObservabilityAlertEventRepository.deleteAllByWorkspaceIdAndCreatedDateBefore(workspaceId, date);
    }

    @Override
    public AiObservabilityAlertEvent create(AiObservabilityAlertEvent alertEvent) {
        Validate.notNull(alertEvent, "alertEvent must not be null");
        Validate.isTrue(alertEvent.getId() == null, "alertEvent id must be null for creation");

        return aiObservabilityAlertEventRepository.save(alertEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityAlertEvent getAlertEvent(Long id) {
        Validate.notNull(id, "id must not be null");

        return aiObservabilityAlertEventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityAlertEvent not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertEvent> getAlertEventsByRule(Long alertRuleId) {
        return aiObservabilityAlertEventRepository.findAllByAlertRuleIdOrderByCreatedDateDesc(alertRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityAlertEvent> getLatestEventByRule(Long alertRuleId) {
        return aiObservabilityAlertEventRepository.findFirstByAlertRuleIdOrderByCreatedDateDesc(alertRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityAlertEvent> getAlertEventsByRuleAfter(Long alertRuleId, Instant after) {
        return aiObservabilityAlertEventRepository.findAllByAlertRuleIdAndCreatedDateAfter(alertRuleId, after);
    }

    @Override
    public AiObservabilityAlertEvent update(AiObservabilityAlertEvent alertEvent) {
        Validate.notNull(alertEvent, "alertEvent must not be null");
        Validate.notNull(alertEvent.getId(), "alertEvent id must not be null for update");

        return aiObservabilityAlertEventRepository.save(alertEvent);
    }
}
