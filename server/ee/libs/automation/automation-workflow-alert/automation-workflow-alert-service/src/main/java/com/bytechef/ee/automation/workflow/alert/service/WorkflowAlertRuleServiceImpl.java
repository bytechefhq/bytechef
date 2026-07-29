/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.service;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.domain.WorkspaceWorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.repository.WorkflowAlertRuleRepository;
import com.bytechef.ee.automation.workflow.alert.repository.WorkspaceWorkflowAlertRuleRepository;
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
    private final WorkspaceWorkflowAlertRuleRepository workspaceWorkflowAlertRuleRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowAlertRuleServiceImpl(
        WorkflowAlertRuleRepository workflowAlertRuleRepository,
        WorkspaceWorkflowAlertRuleRepository workspaceWorkflowAlertRuleRepository) {

        this.workflowAlertRuleRepository = workflowAlertRuleRepository;
        this.workspaceWorkflowAlertRuleRepository = workspaceWorkflowAlertRuleRepository;
    }

    @Override
    public WorkflowAlertRule createInWorkspace(WorkflowAlertRule workflowAlertRule, long workspaceId) {
        WorkflowAlertRule savedWorkflowAlertRule = workflowAlertRuleRepository.save(workflowAlertRule);

        workspaceWorkflowAlertRuleRepository.save(
            new WorkspaceWorkflowAlertRule(savedWorkflowAlertRule.getId(), workspaceId));

        return savedWorkflowAlertRule;
    }

    @Override
    public void delete(long id) {
        // The workspace_workflow_alert_rule membership row cascades with the rule (FK ON DELETE CASCADE).
        workflowAlertRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceId(long workflowAlertRuleId) {
        return workspaceWorkflowAlertRuleRepository.findByWorkflowAlertRuleId(workflowAlertRuleId)
            .map(WorkspaceWorkflowAlertRule::getWorkspaceId);
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
        return workflowAlertRuleRepository.findAllById(getWorkflowAlertRuleIds(workspaceId))
            .stream()
            .sorted(Comparator.comparing(WorkflowAlertRule::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getWorkflowAlertRuleIds(long workspaceId) {
        return workspaceWorkflowAlertRuleRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceWorkflowAlertRule::getWorkflowAlertRuleId)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertRule> getEnabledWorkflowAlertRules(long workspaceId) {
        return workflowAlertRuleRepository.findAllById(getWorkflowAlertRuleIds(workspaceId))
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
