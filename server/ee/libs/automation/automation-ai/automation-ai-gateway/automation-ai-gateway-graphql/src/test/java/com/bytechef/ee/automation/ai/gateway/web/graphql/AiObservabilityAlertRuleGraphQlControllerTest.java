/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertEvaluator;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityNotificationChannelService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Guards the cross-workspace notification-channel smuggling path: a workspace A admin must not be able to attach a
 * workspace B channel to a rule. Without the controller-level workspace check, a subsequent rule trigger would page
 * workspace B's slack/webhook target for workspace A's activity — a cross-tenant notification leak.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiObservabilityAlertRuleGraphQlControllerTest {

    private static final long WORKSPACE_A = 100L;
    private static final long WORKSPACE_B = 200L;
    private static final long CHANNEL_IN_WORKSPACE_B = 42L;

    @Mock
    private AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator;

    @Mock
    private AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    @Mock
    private AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;

    @Mock
    private PermissionService permissionService;

    private AiObservabilityAlertRuleGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new AiObservabilityAlertRuleGraphQlController(
            aiObservabilityAlertEvaluator, aiObservabilityAlertRuleService,
            aiObservabilityNotificationChannelService,
            new com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization(
                permissionService));
    }

    @Test
    void testCreateRejectsChannelFromDifferentWorkspace() {
        AiObservabilityNotificationChannel foreignChannel = new AiObservabilityNotificationChannel(
            WORKSPACE_B, "foreign-channel", AiObservabilityNotificationChannelType.SLACK,
            "{\"webhookUrl\":\"https://hooks.slack.com/services/T/C/X\"}");

        when(permissionService.hasWorkspaceRole(WORKSPACE_A, "EDITOR")).thenReturn(true);
        when(aiObservabilityNotificationChannelService.getNotificationChannel(CHANNEL_IN_WORKSPACE_B))
            .thenReturn(foreignChannel);

        Map<String, Object> input = Map.of(
            "workspaceId", String.valueOf(WORKSPACE_A),
            "name", "cost-alert",
            "metric", "COST",
            "condition", "GREATER_THAN",
            "threshold", 100,
            "windowMinutes", 5,
            "cooldownMinutes", 0,
            "enabled", true,
            "channelIds", List.of(String.valueOf(CHANNEL_IN_WORKSPACE_B)));

        assertThatThrownBy(() -> controller.createAiObservabilityAlertRule(input))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("does not belong to workspace " + WORKSPACE_A);
    }

    @Test
    void testUpdateRejectsChannelFromDifferentWorkspace() {
        AiObservabilityAlertRule existingRule = new AiObservabilityAlertRule(
            WORKSPACE_A, "cost-alert",
            com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric.COST,
            com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition.GREATER_THAN,
            java.math.BigDecimal.valueOf(100), 5, 0);

        ReflectionTestUtils.setField(existingRule, "id", 1L);

        AiObservabilityNotificationChannel foreignChannel = new AiObservabilityNotificationChannel(
            WORKSPACE_B, "foreign-channel", AiObservabilityNotificationChannelType.WEBHOOK,
            "{\"url\":\"https://example.com/webhook\"}");

        when(aiObservabilityAlertRuleService.getAlertRule(1L)).thenReturn(existingRule);
        when(permissionService.hasWorkspaceRole(WORKSPACE_A, "EDITOR")).thenReturn(true);
        when(aiObservabilityNotificationChannelService.getNotificationChannel(CHANNEL_IN_WORKSPACE_B))
            .thenReturn(foreignChannel);

        Map<String, Object> input = Map.of(
            "name", "cost-alert",
            "metric", "COST",
            "condition", "GREATER_THAN",
            "threshold", 100,
            "windowMinutes", 5,
            "cooldownMinutes", 0,
            "enabled", true,
            "channelIds", List.of(String.valueOf(CHANNEL_IN_WORKSPACE_B)));

        assertThatThrownBy(() -> controller.updateAiObservabilityAlertRule(1L, input))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("does not belong to workspace " + WORKSPACE_A);
    }

    @Test
    void testCreateRejectsWhenCallerMissingEditorRole() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_A, "EDITOR")).thenReturn(false);

        Map<String, Object> input = Map.of(
            "workspaceId", String.valueOf(WORKSPACE_A),
            "name", "cost-alert",
            "metric", "COST",
            "condition", "GREATER_THAN",
            "threshold", 100,
            "windowMinutes", 5,
            "cooldownMinutes", 0,
            "enabled", true);

        assertThatThrownBy(() -> controller.createAiObservabilityAlertRule(input))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Not authorized for workspace " + WORKSPACE_A);
    }
}
