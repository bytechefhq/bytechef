/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailCustomRuleRepository;
import com.bytechef.platform.ai.sensitivedata.CustomPatternValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class AiGuardrailCustomRuleServiceImpl implements AiGuardrailCustomRuleService {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailCustomRuleServiceImpl.class);

    private final AiGuardrailCustomRuleRepository aiGuardrailCustomRuleRepository;
    private final @Nullable AiGuardrailMetrics metrics;
    private final int maxRulesPerWorkspace;

    @SuppressFBWarnings("EI")
    public AiGuardrailCustomRuleServiceImpl(
        AiGuardrailCustomRuleRepository aiGuardrailCustomRuleRepository,
        ObjectProvider<AiGuardrailMetrics> metricsProvider,
        @Value("${bytechef.ai.guardrails.custom-rules.max-per-workspace:50}") int maxRulesPerWorkspace) {

        this.aiGuardrailCustomRuleRepository = aiGuardrailCustomRuleRepository;
        this.metrics = metricsProvider.getIfAvailable();
        this.maxRulesPerWorkspace = maxRulesPerWorkspace;
    }

    @Override
    public AiGuardrailCustomRule create(AiGuardrailCustomRule customRule) {
        long existing = aiGuardrailCustomRuleRepository.countByWorkspaceId(customRule.getWorkspaceId());

        if (existing >= maxRulesPerWorkspace) {
            throw new IllegalArgumentException(
                "a workspace may define at most " + maxRulesPerWorkspace + " custom rules, and this one already has " +
                    existing);
        }

        validate(customRule);

        customRule.setEnabled(false);

        return aiGuardrailCustomRuleRepository.save(customRule);
    }

    private void validate(AiGuardrailCustomRule customRule) {
        try {
            CustomPatternValidator.validate(customRule.getType(), customRule.getPattern());
        } catch (CustomPatternValidator.CustomPatternRejectedException customPatternRejectedException) {
            if (metrics != null) {
                metrics.record(
                    "custom_rule_rejected_" + customPatternRejectedException.getDefence()
                        .name()
                        .toLowerCase(Locale.ROOT));
            }

            log.info(
                "Rejected custom guardrail rule '{}' for workspace {} ({}): {}", customRule.getType(),
                customRule.getWorkspaceId(), customPatternRejectedException.getDefence(),
                customPatternRejectedException.getMessage());

            throw customPatternRejectedException;
        }
    }

    @Override
    public AiGuardrailCustomRule updatePattern(long id, long workspaceId, String pattern) {
        AiGuardrailCustomRule customRule = aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(id, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("AiGuardrailCustomRule not found: " + id));

        customRule.setPattern(pattern);

        validate(customRule);

        return aiGuardrailCustomRuleRepository.save(customRule);
    }

    @Override
    public AiGuardrailCustomRule setEnabled(long id, long workspaceId, boolean enabled) {
        AiGuardrailCustomRule customRule = aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(id, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("AiGuardrailCustomRule not found: " + id));

        customRule.setEnabled(enabled);

        return aiGuardrailCustomRuleRepository.save(customRule);
    }

    @Override
    public void delete(long id, long workspaceId) {
        AiGuardrailCustomRule customRule = aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(id, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("AiGuardrailCustomRule not found: " + id));

        aiGuardrailCustomRuleRepository.delete(customRule);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiGuardrailCustomRule> fetchRule(long id, long workspaceId) {
        return aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(id, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGuardrailCustomRule> getRules(long workspaceId) {
        return aiGuardrailCustomRuleRepository.findAllByWorkspaceIdOrderByTypeAsc(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGuardrailCustomRule> getEnabledRules(long workspaceId) {
        return aiGuardrailCustomRuleRepository.findAllByWorkspaceIdAndEnabledTrueOrderByTypeAsc(workspaceId);
    }
}
