/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowAlertRuleWorkflowPreDeleteListenerTest {

    private static final String WORKFLOW_ID = "wf-1";

    private final WorkflowAlertRuleService workflowAlertRuleService = mock(WorkflowAlertRuleService.class);
    private final WorkflowAlertRuleWorkflowPreDeleteListener listener =
        new WorkflowAlertRuleWorkflowPreDeleteListener(workflowAlertRuleService);

    @Test
    void testEveryRuleScopedToTheWorkflowIsDeleted() {
        when(workflowAlertRuleService.getWorkflowWorkflowAlertRules(WORKFLOW_ID))
            .thenReturn(List.of(rule(11L), rule(12L)));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(workflowAlertRuleService).delete(11L);
        verify(workflowAlertRuleService).delete(12L);
    }

    /**
     * The alternative repair — nulling {@code workflow_id} — would widen the rule to every run in the workspace,
     * because that is what a null scope means to the evaluator. Nothing may write to the rule.
     */
    @Test
    void testTheRuleIsDeletedRatherThanWidenedToWorkspaceScope() {
        WorkflowAlertRule workflowAlertRule = rule(11L);

        when(workflowAlertRuleService.getWorkflowWorkflowAlertRules(WORKFLOW_ID))
            .thenReturn(List.of(workflowAlertRule));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(workflowAlertRuleService).delete(11L);
        verify(workflowAlertRuleService, never()).update(workflowAlertRule);
    }

    @Test
    void testAWorkflowWithNoRulesWritesNothing() {
        when(workflowAlertRuleService.getWorkflowWorkflowAlertRules(WORKFLOW_ID)).thenReturn(List.of());

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        verify(workflowAlertRuleService).getWorkflowWorkflowAlertRules(WORKFLOW_ID);
        verifyNoMoreInteractions(workflowAlertRuleService);
    }

    private static WorkflowAlertRule rule(long id) {
        WorkflowAlertRule workflowAlertRule = new WorkflowAlertRule();

        workflowAlertRule.setId(id);
        workflowAlertRule.setWorkflowId(WORKFLOW_ID);

        return workflowAlertRule;
    }
}
