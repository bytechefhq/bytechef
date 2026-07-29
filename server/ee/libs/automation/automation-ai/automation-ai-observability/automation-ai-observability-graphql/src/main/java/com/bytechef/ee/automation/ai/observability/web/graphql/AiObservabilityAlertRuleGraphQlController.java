/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityAlertRuleFacade;
import com.bytechef.ee.automation.ai.observability.service.AiObservabilityAlertEvaluator;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.platform.notification.workspace.service.WorkspaceNotificationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityAlertRuleGraphQlController {

    private final AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator;
    private final AiObservabilityAlertRuleFacade aiObservabilityAlertRuleFacade;
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final NotificationService notificationService;
    private final WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;
    private final WorkspaceNotificationService workspaceNotificationService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityAlertRuleGraphQlController(
        AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator,
        AiObservabilityAlertRuleFacade aiObservabilityAlertRuleFacade,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService, NotificationService notificationService,
        WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService,
        WorkspaceNotificationService workspaceNotificationService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityAlertEvaluator = aiObservabilityAlertEvaluator;
        this.aiObservabilityAlertRuleFacade = aiObservabilityAlertRuleFacade;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.workspaceAiObservabilityAlertRuleService = workspaceAiObservabilityAlertRuleService;
        this.notificationService = notificationService;
        this.workspaceNotificationService = workspaceNotificationService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule aiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(workspaceAiObservabilityAlertRuleService.getWorkspaceId(id),
            "VIEWER");

        return alertRule;
    }

    @QueryMapping
    public List<AiObservabilityAlertRule> aiObservabilityAlertRules(@Argument Long workspaceId) {
        // Authorization (workspace VIEWER role) is enforced on AiObservabilityAlertRuleFacade so it protects every
        // caller of the facade, not just this GraphQL entry point.
        return aiObservabilityAlertRuleFacade.getAlertRulesByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule createAiObservabilityAlertRule(@Argument Map<String, Object> input) {
        long workspaceId = Long.parseLong((String) input.get("workspaceId"));

        workspaceAuthorization.requireWorkspaceRole(workspaceId, "EDITOR");

        AiObservabilityAlertRule alertRule = new AiObservabilityAlertRule(
            (String) input.get("name"),
            AiObservabilityAlertMetric.valueOf((String) input.get("metric")),
            AiObservabilityAlertCondition.valueOf((String) input.get("condition")),
            BigDecimal.valueOf(((Number) input.get("threshold")).doubleValue()),
            (int) input.get("windowMinutes"),
            (int) input.get("cooldownMinutes"));

        alertRule.setEnabled((boolean) input.get("enabled"));

        if (input.get("projectId") != null) {
            alertRule.setProjectId(Long.valueOf((String) input.get("projectId")));
        }

        if (input.get("filters") != null) {
            alertRule.setFilters((String) input.get("filters"));
        }

        if (input.get("notificationIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> notificationIds = (List<String>) input.get("notificationIds");

            alertRule.setChannels(toWorkspaceVisibleNotifications(notificationIds, workspaceId));
        }

        return workspaceAiObservabilityAlertRuleService.createInWorkspace(alertRule, workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()),
            "EDITOR");

        aiObservabilityAlertRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule snoozeAiObservabilityAlertRule(@Argument long id, @Argument long until) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()),
            "EDITOR");

        return aiObservabilityAlertRuleService.snooze(id, Instant.ofEpochMilli(until));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule unsnoozeAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()),
            "EDITOR");

        return aiObservabilityAlertRuleService.unsnooze(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Double testAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()),
            "EDITOR");

        BigDecimal metricValue = aiObservabilityAlertEvaluator.evaluateMetric(alertRule);

        return metricValue != null ? metricValue.doubleValue() : null;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule updateAiObservabilityAlertRule(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId()),
            "EDITOR");

        alertRule.setName((String) input.get("name"));
        alertRule.setMetricAndThreshold(
            AiObservabilityAlertMetric.valueOf((String) input.get("metric")),
            BigDecimal.valueOf(((Number) input.get("threshold")).doubleValue()));
        alertRule.setCondition(AiObservabilityAlertCondition.valueOf((String) input.get("condition")));
        alertRule.setWindowMinutes((int) input.get("windowMinutes"));
        alertRule.setCooldownMinutes((int) input.get("cooldownMinutes"));
        alertRule.setEnabled((boolean) input.get("enabled"));

        if (input.get("filters") != null) {
            alertRule.setFilters((String) input.get("filters"));
        }

        if (input.get("notificationIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> notificationIds = (List<String>) input.get("notificationIds");

            alertRule.setChannels(
                toWorkspaceVisibleNotifications(
                    notificationIds, workspaceAiObservabilityAlertRuleService.getWorkspaceId(alertRule.getId())));
        }

        return aiObservabilityAlertRuleService.update(alertRule);
    }

    /**
     * Rejects any notification that is scoped to a DIFFERENT workspace. Without this guard a workspace A admin could
     * attach workspace B's notification to a rule — when the rule fires, workspace B's email/slack/webhook target gets
     * paged by workspace A's activity. Global (unassigned) notifications are visible to every workspace and pass.
     */
    private Set<AiObservabilityAlertRuleChannel> toWorkspaceVisibleNotifications(
        List<String> notificationIds, long workspaceId) {

        Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();

        for (String rawNotificationId : notificationIds) {
            long notificationId = Long.parseLong(rawNotificationId);

            // Existence check — a dangling id should fail the mutation, not the later dispatch.
            notificationService.getNotification(notificationId);

            Long notificationWorkspaceId = workspaceNotificationService
                .fetchWorkspaceIdByNotificationId(notificationId)
                .orElse(null);

            if (notificationWorkspaceId != null && notificationWorkspaceId != workspaceId) {
                throw new AccessDeniedException(
                    "Notification " + notificationId + " does not belong to workspace " + workspaceId);
            }

            channels.add(new AiObservabilityAlertRuleChannel(notificationId));
        }

        return channels;
    }

    @SchemaMapping(typeName = "AiObservabilityAlertRule", field = "notificationIds")
    public List<String> notificationIds(AiObservabilityAlertRule alertRule) {
        return alertRule.getChannels()
            .stream()
            .map(channel -> String.valueOf(channel.notificationId()))
            .toList();
    }
}
