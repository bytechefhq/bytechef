/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalRuleRepository;
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

    private final AiEvalRuleRepository aiEvalRuleRepository;
    private final AiEvalRuleService aiEvalRuleService;

    WorkspaceAiEvalRuleServiceImpl(AiEvalRuleRepository aiEvalRuleRepository, AiEvalRuleService aiEvalRuleService) {
        this.aiEvalRuleRepository = aiEvalRuleRepository;
        this.aiEvalRuleService = aiEvalRuleService;
    }

    @Override
    public AiEvalRule createInWorkspace(AiEvalRule evalRule, long workspaceId) {
        Validate.notNull(evalRule, "evalRule must not be null");

        evalRule.setWorkspaceId(workspaceId);

        return aiEvalRuleService.create(evalRule);
    }

    @Override
    public void deleteInWorkspace(long evalRuleId) {
        aiEvalRuleService.delete(evalRuleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long evalRuleId) {
        // findById rather than the platform service's getEvalRule: an unknown id must still yield null (the
        // pre-collapse "no membership row" answer) because callers use this as an authorization probe, not a fetch.
        return aiEvalRuleRepository.findById(evalRuleId)
            .map(AiEvalRule::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId) {
        return aiEvalRuleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEnabledEvalRulesByWorkspace(Long workspaceId) {
        return aiEvalRuleRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEnabledEvalRulesByWorkspaceAndTarget(Long workspaceId, AiEvalRuleTarget target) {
        Validate.notNull(target, "target must not be null");

        return aiEvalRuleRepository.findAllByWorkspaceIdAndEnabledTrueAndTarget(workspaceId, target.ordinal());
    }
}
