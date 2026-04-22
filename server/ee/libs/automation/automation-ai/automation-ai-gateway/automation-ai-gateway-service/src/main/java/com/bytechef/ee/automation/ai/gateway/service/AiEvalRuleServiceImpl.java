/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalRule;
import com.bytechef.ee.automation.ai.gateway.repository.AiEvalRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
class AiEvalRuleServiceImpl implements AiEvalRuleService {

    private final AiEvalRuleRepository aiEvalRuleRepository;

    AiEvalRuleServiceImpl(AiEvalRuleRepository aiEvalRuleRepository) {
        this.aiEvalRuleRepository = aiEvalRuleRepository;
    }

    @Override
    public AiEvalRule create(AiEvalRule evalRule) {
        Validate.notNull(evalRule, "evalRule must not be null");
        Validate.isTrue(evalRule.getId() == null, "evalRule id must be null for creation");

        return aiEvalRuleRepository.save(evalRule);
    }

    @Override
    public void delete(long id) {
        aiEvalRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalRule getEvalRule(long id) {
        return aiEvalRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalRule not found with id: " + id));
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
    public AiEvalRule update(AiEvalRule evalRule) {
        Validate.notNull(evalRule, "evalRule must not be null");
        Validate.notNull(evalRule.getId(), "evalRule id must not be null for update");

        return aiEvalRuleRepository.save(evalRule);
    }
}
