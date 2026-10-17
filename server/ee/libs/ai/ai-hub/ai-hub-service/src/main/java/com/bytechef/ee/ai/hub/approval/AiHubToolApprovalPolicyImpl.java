/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubToolApprovalPolicyImpl implements AiHubToolApprovalPolicy {

    private static final Logger log = LoggerFactory.getLogger(AiHubToolApprovalPolicyImpl.class);

    private final AiHubToolApprovalRuleRepository ruleRepository;

    @SuppressFBWarnings("EI")
    public AiHubToolApprovalPolicyImpl(AiHubToolApprovalRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Decision decide(long workspaceId) {
        Set<String> required = new HashSet<>();
        Set<String> exempt = new HashSet<>();

        try {
            applyCatalogRules(ruleRepository.findAllByWorkspaceId(workspaceId), required, exempt);
        } catch (RuntimeException exception) {
            log.warn(
                "Tool approval policy lookup failed for workspace={}; gating built-in defaults only", workspaceId,
                exception);

            return new Decision(required, Set.of());
        }

        return new Decision(required, exempt);
    }

    private static void applyCatalogRules(List<AiHubToolApprovalRule> rules, Set<String> required, Set<String> exempt) {
        for (AiHubToolApprovalRule rule : rules) {
            if (rule.getToolKind() != AiHubToolApproval.ToolKind.CATALOG) {
                continue;
            }

            if (rule.getMode() == AiHubToolApprovalRule.Mode.REQUIRE) {
                required.add(rule.getToolName());
            } else {
                exempt.add(rule.getToolName());
            }
        }
    }
}
