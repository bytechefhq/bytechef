/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.service;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import com.bytechef.ee.automation.workflow.alert.repository.WorkflowAlertEventRepository;
import com.bytechef.ee.automation.workflow.alert.repository.WorkspaceWorkflowAlertRuleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
@Transactional
public class WorkflowAlertEventServiceImpl implements WorkflowAlertEventService {

    private final WorkflowAlertEventRepository workflowAlertEventRepository;
    private final WorkspaceWorkflowAlertRuleRepository workspaceWorkflowAlertRuleRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowAlertEventServiceImpl(
        WorkflowAlertEventRepository workflowAlertEventRepository,
        WorkspaceWorkflowAlertRuleRepository workspaceWorkflowAlertRuleRepository) {

        this.workflowAlertEventRepository = workflowAlertEventRepository;
        this.workspaceWorkflowAlertRuleRepository = workspaceWorkflowAlertRuleRepository;
    }

    @Override
    public WorkflowAlertEvent create(WorkflowAlertEvent workflowAlertEvent) {
        return workflowAlertEventRepository.save(workflowAlertEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowAlertEvent> getWorkflowAlertEvents(long workspaceId) {
        List<Long> ruleIds = workspaceWorkflowAlertRuleRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(workspaceWorkflowAlertRule -> workspaceWorkflowAlertRule.getWorkflowAlertRuleId())
            .toList();

        if (ruleIds.isEmpty()) {
            return List.of();
        }

        return workflowAlertEventRepository.findTop100ByWorkflowAlertRuleIdInOrderByCreatedDateDesc(ruleIds);
    }
}
