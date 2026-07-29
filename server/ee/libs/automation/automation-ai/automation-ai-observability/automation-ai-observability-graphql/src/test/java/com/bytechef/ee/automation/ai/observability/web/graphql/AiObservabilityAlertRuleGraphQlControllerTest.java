/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityAlertRuleFacade;
import com.bytechef.ee.automation.ai.observability.service.AiObservabilityAlertEvaluator;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertRuleService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.platform.notification.workspace.service.WorkspaceNotificationService;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.service.NotificationService;
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
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiObservabilityAlertRuleGraphQlControllerTest {

    private static final long WORKSPACE_A = 100L;
    private static final long WORKSPACE_B = 200L;
    private static final long NOTIFICATION_IN_WORKSPACE_B = 42L;

    @Mock
    private AiObservabilityAlertEvaluator aiObservabilityAlertEvaluator;

    @Mock
    private AiObservabilityAlertRuleFacade aiObservabilityAlertRuleFacade;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;

    @Mock
    private AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

    @Mock
    private WorkspaceNotificationService workspaceNotificationService;

    @Mock
    private PermissionService permissionService;

    private AiObservabilityAlertRuleGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new AiObservabilityAlertRuleGraphQlController(
            aiObservabilityAlertEvaluator, aiObservabilityAlertRuleFacade, aiObservabilityAlertRuleService,
            notificationService, workspaceAiObservabilityAlertRuleService, workspaceNotificationService,
            new com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization(
                permissionService));
    }

    @Test
    void testCreateRejectsChannelFromDifferentWorkspace() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_A, "EDITOR")).thenReturn(true);
        when(notificationService.getNotification(NOTIFICATION_IN_WORKSPACE_B)).thenReturn(new Notification());
        when(workspaceNotificationService.fetchWorkspaceIdByNotificationId(NOTIFICATION_IN_WORKSPACE_B))
            .thenReturn(java.util.Optional.of(WORKSPACE_B));

        Map<String, Object> input = Map.of(
            "workspaceId", String.valueOf(WORKSPACE_A),
            "name", "cost-alert",
            "metric", "COST",
            "condition", "GREATER_THAN",
            "threshold", 100,
            "windowMinutes", 5,
            "cooldownMinutes", 0,
            "enabled", true,
            "notificationIds", List.of(String.valueOf(NOTIFICATION_IN_WORKSPACE_B)));

        assertThatThrownBy(() -> controller.createAiObservabilityAlertRule(input))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("does not belong to workspace " + WORKSPACE_A);
    }

    @Test
    void testUpdateRejectsChannelFromDifferentWorkspace() {
        AiObservabilityAlertRule existingRule = new AiObservabilityAlertRule(
            "cost-alert",
            com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertMetric.COST,
            com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition.GREATER_THAN,
            java.math.BigDecimal.valueOf(100), 5, 0);

        ReflectionTestUtils.setField(existingRule, "id", 1L);

        when(aiObservabilityAlertRuleService.getAlertRule(1L)).thenReturn(existingRule);
        when(workspaceAiObservabilityAlertRuleService.getWorkspaceId(1L)).thenReturn(WORKSPACE_A);
        when(permissionService.hasWorkspaceRole(WORKSPACE_A, "EDITOR")).thenReturn(true);
        when(notificationService.getNotification(NOTIFICATION_IN_WORKSPACE_B)).thenReturn(new Notification());
        when(workspaceNotificationService.fetchWorkspaceIdByNotificationId(NOTIFICATION_IN_WORKSPACE_B))
            .thenReturn(java.util.Optional.of(WORKSPACE_B));

        Map<String, Object> input = Map.of(
            "name", "cost-alert",
            "metric", "COST",
            "condition", "GREATER_THAN",
            "threshold", 100,
            "windowMinutes", 5,
            "cooldownMinutes", 0,
            "enabled", true,
            "notificationIds", List.of(String.valueOf(NOTIFICATION_IN_WORKSPACE_B)));

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
            .hasMessage("Not authorized for the requested workspace");
    }
}
