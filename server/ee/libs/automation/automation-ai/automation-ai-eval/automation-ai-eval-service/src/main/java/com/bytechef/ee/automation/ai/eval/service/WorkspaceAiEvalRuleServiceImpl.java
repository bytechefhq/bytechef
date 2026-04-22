/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalRule;
import com.bytechef.ee.automation.ai.eval.repository.WorkspaceAiEvalRuleRepository;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiEvalRuleServiceImpl implements WorkspaceAiEvalRuleService {

    private final AiEvalRuleService aiEvalRuleService;
    private final WorkspaceAiEvalRuleRepository workspaceAiEvalRuleRepository;

    WorkspaceAiEvalRuleServiceImpl(
        AiEvalRuleService aiEvalRuleService,
        WorkspaceAiEvalRuleRepository workspaceAiEvalRuleRepository) {

        this.aiEvalRuleService = aiEvalRuleService;
        this.workspaceAiEvalRuleRepository = workspaceAiEvalRuleRepository;
    }

    @Override
    public AiEvalRule createInWorkspace(AiEvalRule evalRule, long workspaceId) {
        Validate.notNull(evalRule, "evalRule must not be null");

        AiEvalRule saved = aiEvalRuleService.create(evalRule);

        workspaceAiEvalRuleRepository.save(new WorkspaceAiEvalRule(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void deleteInWorkspace(long evalRuleId) {
        workspaceAiEvalRuleRepository.findByAiEvalRuleId(evalRuleId)
            .ifPresent(membership -> workspaceAiEvalRuleRepository.deleteById(membership.getId()));

        aiEvalRuleService.delete(evalRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long evalRuleId) {
        return workspaceAiEvalRuleRepository.findByAiEvalRuleId(evalRuleId)
            .map(WorkspaceAiEvalRule::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId) {
        return workspaceAiEvalRuleRepository.findAllRulesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId) {
        return workspaceAiEvalRuleRepository.findAllRulesByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEnabledEvalRulesByWorkspaceAndTarget(Long workspaceId, AiEvalRuleTarget target) {
        Validate.notNull(target, "target must not be null");

        return workspaceAiEvalRuleRepository.findAllRulesByWorkspaceIdAndEnabledTrueAndTarget(
            workspaceId, target.ordinal());
    }
}
