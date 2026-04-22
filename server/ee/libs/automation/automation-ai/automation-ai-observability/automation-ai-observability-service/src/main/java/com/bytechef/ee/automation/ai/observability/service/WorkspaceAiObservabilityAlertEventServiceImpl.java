/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityAlertRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
class WorkspaceAiObservabilityAlertEventServiceImpl implements WorkspaceAiObservabilityAlertEventService {

    private final WorkspaceAiObservabilityAlertRuleRepository workspaceAiObservabilityAlertRuleRepository;

    WorkspaceAiObservabilityAlertEventServiceImpl(
        WorkspaceAiObservabilityAlertRuleRepository workspaceAiObservabilityAlertRuleRepository) {

        this.workspaceAiObservabilityAlertRuleRepository = workspaceAiObservabilityAlertRuleRepository;
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        workspaceAiObservabilityAlertRuleRepository.deleteAllAlertEventsByWorkspaceIdAndCreatedDateBefore(
            workspaceId, date);
    }
}
