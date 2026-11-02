/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubToolApprovalRuleServiceImpl implements AiHubToolApprovalRuleService {

    private final AiHubToolApprovalRuleRepository ruleRepository;

    @SuppressFBWarnings("EI")
    public AiHubToolApprovalRuleServiceImpl(AiHubToolApprovalRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubToolApprovalRule> getRules(long workspaceId) {
        return ruleRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public AiHubToolApprovalRule save(AiHubToolApprovalRule rule) {
        return ruleRepository.save(rule);
    }

    @Override
    public void delete(long workspaceId, long ruleId) {
        AiHubToolApprovalRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new NotFoundException("AiHubToolApprovalRule", ruleId));

        if (rule.getWorkspaceId() != workspaceId) {
            throw new NotFoundException("AiHubToolApprovalRule", ruleId);
        }

        ruleRepository.delete(rule);
    }
}
