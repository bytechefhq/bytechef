/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.workflow.alert.config.WorkflowAlertIntTestConfiguration;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.repository.WorkflowAlertRuleRepository;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the orphan against a real Postgres. {@code workflow_alert_rule.workflow_id} is a nullable column with no
 * foreign key, so a rule can outlive the workflow it scopes to — and a rule in that state is worse than untidy: the
 * evaluator skips any rule whose {@code workflowId} does not equal the finished job's, so it matches nothing ever again
 * while still appearing active in the workspace's rule list.
 *
 * <p>
 * The unit test mocks {@code getWorkflowWorkflowAlertRules}; this one exercises the derived query itself. Every test
 * asserts the rule exists BEFORE the delete — a silent orphan raises no exception, so asserting only the end state
 * would pass equally well against a listener that never ran.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = WorkflowAlertIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class WorkflowAlertRuleWorkflowPreDeleteListenerIntTest {

    private static final String WORKFLOW_ID = "wf-1";
    private static final String OTHER_WORKFLOW_ID = "wf-2";
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private WorkflowAlertRuleRepository workflowAlertRuleRepository;

    @Autowired
    private WorkflowAlertRuleService workflowAlertRuleService;

    private WorkflowAlertRuleWorkflowPreDeleteListener listener;

    @BeforeEach
    void beforeEach() {
        workflowAlertRuleRepository.deleteAll();

        listener = new WorkflowAlertRuleWorkflowPreDeleteListener(workflowAlertRuleService);
    }

    @AfterEach
    void afterEach() {
        workflowAlertRuleRepository.deleteAll();
    }

    @Test
    void testEveryRuleScopedToTheDeletedWorkflowIsRemoved() {
        WorkflowAlertRule first = givenRule("Failure rate", WORKFLOW_ID);
        WorkflowAlertRule second = givenRule("No activity", WORKFLOW_ID);

        // Before: both rules exist and name the workflow.
        assertThat(workflowAlertRuleService.getWorkflowWorkflowAlertRules(WORKFLOW_ID))
            .extracting(WorkflowAlertRule::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId());

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(workflowAlertRuleService.getWorkflowWorkflowAlertRules(WORKFLOW_ID)).isEmpty();
        assertThat(workflowAlertRuleRepository.findById(first.getId())).isEmpty();
        assertThat(workflowAlertRuleRepository.findById(second.getId())).isEmpty();
    }

    /**
     * The rule is DELETED rather than widened. Nulling {@code workflow_id} would be the other repair and is the wrong
     * one: a null scope means "every run in the workspace" to the evaluator, so a rule the user deliberately narrowed
     * to one workflow would start alerting on unrelated runs.
     */
    @Test
    void testTheRuleIsNotLeftBehindWithANullWorkflowId() {
        givenRule("Failure rate", WORKFLOW_ID);

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(workflowAlertRuleRepository.findAllByWorkspaceId(WORKSPACE_ID)).isEmpty();
    }

    @Test
    void testRulesScopedElsewhereAndWorkspaceWideRulesSurvive() {
        WorkflowAlertRule other = givenRule("Other workflow", OTHER_WORKFLOW_ID);
        WorkflowAlertRule workspaceWide = givenRule("Whole workspace", null);

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(workflowAlertRuleRepository.findAllByWorkspaceId(WORKSPACE_ID))
            .extracting(WorkflowAlertRule::getId)
            .containsExactlyInAnyOrder(other.getId(), workspaceWide.getId());
    }

    private WorkflowAlertRule givenRule(String name, String workflowId) {
        WorkflowAlertRule workflowAlertRule = new WorkflowAlertRule();

        workflowAlertRule.setName(name);
        workflowAlertRule.setRuleType(WorkflowAlertRuleType.FAILURE_RATE);
        workflowAlertRule.setThreshold(BigDecimal.valueOf(50));
        workflowAlertRule.setWorkflowId(workflowId);

        return workflowAlertRuleService.createInWorkspace(workflowAlertRule, WORKSPACE_ID);
    }
}
