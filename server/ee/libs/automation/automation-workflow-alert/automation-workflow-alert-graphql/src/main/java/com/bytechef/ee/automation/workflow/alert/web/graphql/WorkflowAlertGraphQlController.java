/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.workflow.alert.dispatcher.WorkflowAlertDispatcher;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertEventService;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * CRUD surface for workspace alert rules and their fired-alert history. Delivery targets are platform
 * {@code Notification} ids — channel management stays on the Notifications page; this controller never touches channel
 * settings.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class WorkflowAlertGraphQlController {

    private final WorkflowAlertDispatcher workflowAlertDispatcher;
    private final WorkflowAlertEventService workflowAlertEventService;
    private final WorkflowAlertRuleService workflowAlertRuleService;

    @SuppressFBWarnings("EI")
    public WorkflowAlertGraphQlController(
        WorkflowAlertDispatcher workflowAlertDispatcher, WorkflowAlertEventService workflowAlertEventService,
        WorkflowAlertRuleService workflowAlertRuleService) {

        this.workflowAlertDispatcher = workflowAlertDispatcher;
        this.workflowAlertEventService = workflowAlertEventService;
        this.workflowAlertRuleService = workflowAlertRuleService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkflowAlertRule> workflowAlertRules(@Argument long workspaceId) {
        return workflowAlertRuleService.getWorkflowAlertRules(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkflowAlertEvent> workflowAlertEvents(@Argument long workspaceId) {
        return workflowAlertEventService.getWorkflowAlertEvents(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public WorkflowAlertRule createWorkflowAlertRule(
        @Argument long workspaceId, @Argument WorkflowAlertRuleGraphQlInput input) {

        WorkflowAlertRule workflowAlertRule = new WorkflowAlertRule();

        applyInput(workflowAlertRule, input);

        return workflowAlertRuleService.createInWorkspace(workflowAlertRule, workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public WorkflowAlertRule updateWorkflowAlertRule(
        @Argument long id, @Argument WorkflowAlertRuleGraphQlInput input) {

        WorkflowAlertRule workflowAlertRule = workflowAlertRuleService.getWorkflowAlertRule(id);

        applyInput(workflowAlertRule, input);

        return workflowAlertRuleService.update(workflowAlertRule);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean deleteWorkflowAlertRule(@Argument long id) {
        workflowAlertRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public WorkflowAlertRule enableWorkflowAlertRule(@Argument long id, @Argument boolean enabled) {
        WorkflowAlertRule workflowAlertRule = workflowAlertRuleService.getWorkflowAlertRule(id);

        workflowAlertRule.setEnabled(enabled);

        return workflowAlertRuleService.update(workflowAlertRule);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean sendTestWorkflowAlert(@Argument long id) {
        WorkflowAlertRule workflowAlertRule = workflowAlertRuleService.getWorkflowAlertRule(id);

        // Synthetic event, deliberately NOT persisted to the alert history — this only verifies channel wiring.
        workflowAlertDispatcher.dispatch(
            workflowAlertRule,
            new WorkflowAlertEvent(
                workflowAlertRule.getId(), null, BigDecimal.ZERO,
                "Test alert from ByteChef for rule '" + workflowAlertRule.getName() + "'"));

        return true;
    }

    @SchemaMapping(typeName = "WorkflowAlertRule", field = "lastTriggeredDate")
    public @Nullable String lastTriggeredDate(WorkflowAlertRule workflowAlertRule) {
        return workflowAlertRule.getLastTriggeredDate() == null
            ? null
            : String.valueOf(workflowAlertRule.getLastTriggeredDate());
    }

    @SchemaMapping(typeName = "WorkflowAlertEvent", field = "createdDate")
    public @Nullable String createdDate(WorkflowAlertEvent workflowAlertEvent) {
        return workflowAlertEvent.getCreatedDate() == null
            ? null
            : String.valueOf(workflowAlertEvent.getCreatedDate());
    }

    private static void applyInput(WorkflowAlertRule workflowAlertRule, WorkflowAlertRuleGraphQlInput input) {
        workflowAlertRule.setName(input.name());
        workflowAlertRule.setWorkflowId(input.workflowId());
        workflowAlertRule.setRuleType(input.ruleType());
        workflowAlertRule.setThreshold(input.threshold());
        workflowAlertRule.setWindowMinutes(input.windowMinutes());
        workflowAlertRule.setNotificationIds(input.notificationIds());

        Integer cooldownMinutes = input.cooldownMinutes();

        if (cooldownMinutes != null) {
            workflowAlertRule.setCooldownMinutes(cooldownMinutes);
        }

        Boolean enabled = input.enabled();

        if (enabled != null) {
            workflowAlertRule.setEnabled(enabled);
        }
    }
}
