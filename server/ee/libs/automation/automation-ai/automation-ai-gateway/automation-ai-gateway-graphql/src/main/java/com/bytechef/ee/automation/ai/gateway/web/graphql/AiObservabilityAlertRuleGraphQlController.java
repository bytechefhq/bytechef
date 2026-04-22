/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertEvaluator;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityNotificationChannelService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
    private final AiObservabilityAlertRuleService aiObservabilityAlertRuleService;
    private final AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityAlertRuleGraphQlController(
        AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator,
        AiObservabilityAlertRuleService aiObservabilityAlertRuleService,
        AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityAlertEvaluator = aiObservabilityAlertEvaluator;
        this.aiObservabilityAlertRuleService = aiObservabilityAlertRuleService;
        this.aiObservabilityNotificationChannelService = aiObservabilityNotificationChannelService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule aiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "VIEWER");

        return alertRule;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiObservabilityAlertRule> aiObservabilityAlertRules(@Argument Long workspaceId) {
        return aiObservabilityAlertRuleService.getAlertRulesByWorkspace(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule createAiObservabilityAlertRule(@Argument Map<String, Object> input) {
        long workspaceId = Long.parseLong((String) input.get("workspaceId"));

        workspaceAuthorization.requireWorkspaceRole(workspaceId, "EDITOR");

        AiObservabilityAlertRule alertRule = new AiObservabilityAlertRule(
            workspaceId,
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

        if (input.get("channelIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> channelIds = (List<String>) input.get("channelIds");

            Set<AiObservabilityAlertRuleChannel> channels = toWorkspaceOwnedChannels(channelIds, workspaceId);

            alertRule.setChannels(channels);
        }

        return aiObservabilityAlertRuleService.create(alertRule);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

        aiObservabilityAlertRuleService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule snoozeAiObservabilityAlertRule(@Argument long id, @Argument long until) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

        return aiObservabilityAlertRuleService.snooze(id, Instant.ofEpochMilli(until));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule unsnoozeAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

        return aiObservabilityAlertRuleService.unsnooze(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Double testAiObservabilityAlertRule(@Argument long id) {
        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

        BigDecimal metricValue = aiObservabilityAlertEvaluator.evaluateMetric(alertRule);

        return metricValue != null ? metricValue.doubleValue() : null;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityAlertRule updateAiObservabilityAlertRule(
        @Argument long id, @Argument Map<String, Object> input) {

        AiObservabilityAlertRule alertRule = aiObservabilityAlertRuleService.getAlertRule(id);

        workspaceAuthorization.requireWorkspaceRole(alertRule.getWorkspaceId(), "EDITOR");

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

        if (input.get("channelIds") != null) {
            @SuppressWarnings("unchecked")
            List<String> channelIds = (List<String>) input.get("channelIds");

            Set<AiObservabilityAlertRuleChannel> channels =
                toWorkspaceOwnedChannels(channelIds, alertRule.getWorkspaceId());

            alertRule.setChannels(channels);
        }

        return aiObservabilityAlertRuleService.update(alertRule);
    }

    /**
     * Rejects any channelId that belongs to a different workspace. Without this guard a workspace A admin could attach
     * a workspace B notification channel to a rule — when the rule fires, workspace B's slack/webhook target gets paged
     * by workspace A's activity.
     */
    private Set<AiObservabilityAlertRuleChannel> toWorkspaceOwnedChannels(List<String> channelIds, long workspaceId) {
        Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();

        for (String rawChannelId : channelIds) {
            long channelId = Long.parseLong(rawChannelId);

            AiObservabilityNotificationChannel channel =
                aiObservabilityNotificationChannelService.getNotificationChannel(channelId);

            Long channelWorkspaceId = channel.getWorkspaceId();

            if (channelWorkspaceId == null || channelWorkspaceId != workspaceId) {
                throw new AccessDeniedException(
                    "Notification channel " + channelId + " does not belong to workspace " + workspaceId);
            }

            channels.add(new AiObservabilityAlertRuleChannel(channelId));
        }

        return channels;
    }

    @SchemaMapping(typeName = "AiObservabilityAlertRule", field = "channelIds")
    public List<String> channelIds(AiObservabilityAlertRule alertRule) {
        return alertRule.getChannels()
            .stream()
            .map(channel -> String.valueOf(channel.notificationChannelId()))
            .toList();
    }
}
