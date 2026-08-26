/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.service;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.repository.WorkflowAlertRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class WorkflowAlertRuleServiceImpl implements WorkflowAlertRuleService {

    private final WorkflowAlertRuleRepository workflowAlertRuleRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowAlertRuleServiceImpl(WorkflowAlertRuleRepository workflowAlertRuleRepository) {
        this.workflowAlertRuleRepository = workflowAlertRuleRepository;
    }

    @Override
    public WorkflowAlertRule createInWorkspace(WorkflowAlertRule workflowAlertRule, long workspaceId) {
        workflowAlertRule.setWorkspaceId(workspaceId);

        return workflowAlertRuleRepository.save(workflowAlertRule);
    }

    @Override
    public void delete(long id) {
        workflowAlertRuleRepository.deleteById(id);
    }

    /**
     * Reads the owning workspace straight off the rule row. An unknown id and a workspace-less rule both answer
     * {@link Optional#empty()} — the same result the missing membership row used to give — because callers treat this
     * as a probe rather than a lookup that must succeed.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceId(long workflowAlertRuleId) {
        return workflowAlertRuleRepository.findById(workflowAlertRuleId)
            .map(WorkflowAlertRule::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowAlertRule getWorkflowAlertRule(long id) {
        return workflowAlertRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("WorkflowAlertRule not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertRule> getWorkflowAlertRules(long workspaceId) {
        return workflowAlertRuleRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .sorted(Comparator.comparing(WorkflowAlertRule::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertRule> getWorkflowWorkflowAlertRules(String workflowId) {
        return workflowAlertRuleRepository.findAllByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getWorkflowAlertRuleIds(long workspaceId) {
        return workflowAlertRuleRepository.findAllIdsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertRule> getEnabledWorkflowAlertRules(long workspaceId) {
        return workflowAlertRuleRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .filter(WorkflowAlertRule::isEnabled)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertRule> getEnabledWorkflowAlertRules(WorkflowAlertRuleType ruleType) {
        return workflowAlertRuleRepository.findAllByRuleTypeAndEnabledTrue(ruleType.ordinal());
    }

    @Override
    public WorkflowAlertRule update(WorkflowAlertRule workflowAlertRule) {
        return workflowAlertRuleRepository.save(workflowAlertRule);
    }
}
