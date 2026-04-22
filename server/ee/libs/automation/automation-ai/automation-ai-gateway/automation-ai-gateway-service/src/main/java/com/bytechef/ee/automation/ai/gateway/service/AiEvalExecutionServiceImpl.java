/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecution;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecutionStatus;
import com.bytechef.ee.automation.ai.gateway.repository.AiEvalExecutionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
class AiEvalExecutionServiceImpl implements AiEvalExecutionService {

    private final AiEvalExecutionRepository aiEvalExecutionRepository;

    AiEvalExecutionServiceImpl(AiEvalExecutionRepository aiEvalExecutionRepository) {
        this.aiEvalExecutionRepository = aiEvalExecutionRepository;
    }

    @Override
    public void deleteOlderThan(Instant date) {
        Validate.notNull(date, "date must not be null");

        aiEvalExecutionRepository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        aiEvalExecutionRepository.deleteAllByWorkspaceIdAndCreatedDateBefore(workspaceId, date);
    }

    @Override
    public AiEvalExecution create(AiEvalExecution evalExecution) {
        Validate.notNull(evalExecution, "evalExecution must not be null");
        Validate.isTrue(evalExecution.getId() == null, "evalExecution id must be null for creation");

        return aiEvalExecutionRepository.save(evalExecution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExecution> getExecutionsByEvalRule(Long evalRuleId) {
        return aiEvalExecutionRepository.findAllByEvalRuleId(evalRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExecution> getExecutionsByTrace(Long traceId) {
        return aiEvalExecutionRepository.findAllByTraceId(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalExecution> getPendingExecutions() {
        return aiEvalExecutionRepository.findAllByStatus(
            AiEvalExecutionStatus.PENDING.ordinal());
    }

    @Override
    public AiEvalExecution update(AiEvalExecution evalExecution) {
        Validate.notNull(evalExecution, "evalExecution must not be null");
        Validate.notNull(evalExecution.getId(), "evalExecution id must not be null for update");

        return aiEvalExecutionRepository.save(evalExecution);
    }
}
