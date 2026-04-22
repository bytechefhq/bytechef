/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRuleTarget;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
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
    public List<AiEvalRule> getEvalRules(List<Long> ids) {
        Validate.notNull(ids, "ids must not be null");

        if (ids.isEmpty()) {
            return List.of();
        }

        List<AiEvalRule> rules = new ArrayList<>();

        aiEvalRuleRepository.findAllById(ids)
            .forEach(rules::add);

        return rules;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalRule> getEnabledEvalRulesByIdsAndTarget(List<Long> ids, AiEvalRuleTarget target) {
        Validate.notNull(ids, "ids must not be null");
        Validate.notNull(target, "target must not be null");

        if (ids.isEmpty()) {
            return List.of();
        }

        return getEvalRules(ids).stream()
            .filter(AiEvalRule::isEnabled)
            .filter(rule -> rule.getTarget() == target)
            .toList();
    }

    @Override
    public AiEvalRule update(AiEvalRule evalRule) {
        Validate.notNull(evalRule, "evalRule must not be null");
        Validate.notNull(evalRule.getId(), "evalRule id must not be null for update");

        return aiEvalRuleRepository.save(evalRule);
    }
}
