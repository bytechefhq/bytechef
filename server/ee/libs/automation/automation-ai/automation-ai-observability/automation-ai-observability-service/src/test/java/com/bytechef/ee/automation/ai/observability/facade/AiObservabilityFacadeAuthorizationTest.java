/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on the EE AI Observability facade implementations. The guards were moved
 * off the GraphQL controllers (session, trace, alert-rule, webhook-subscription, notification-channel, export-job) onto
 * the facades so they protect every caller of the facade, not just the GraphQL entry point; this reflection test
 * catches a refactor that silently drops one.
 *
 * <p>
 * The {@code isAuthenticated()} guards (and their programmatic workspace-role checks) remain on the controllers and are
 * intentionally not covered here. Runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiObservabilityFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testSessionGetSessionsByWorkspaceRequiresViewer() {
        assertExpression(AiObservabilitySessionFacadeImpl.class, "getSessionsByWorkspace", VIEWER_EXPRESSION);
    }

    @Test
    void testTraceGetTracesByWorkspaceFilteredRequiresViewer() {
        assertExpression(AiObservabilityTraceFacadeImpl.class, "getTracesByWorkspaceFiltered", VIEWER_EXPRESSION);
    }

    @Test
    void testAlertRuleGetAlertRulesByWorkspaceRequiresViewer() {
        assertExpression(AiObservabilityAlertRuleFacadeImpl.class, "getAlertRulesByWorkspace", VIEWER_EXPRESSION);
    }

    @Test
    void testWebhookSubscriptionGetWebhookSubscriptionsByWorkspaceRequiresViewer() {
        assertExpression(
            AiObservabilityWebhookSubscriptionFacadeImpl.class, "getWebhookSubscriptionsByWorkspace",
            VIEWER_EXPRESSION);
    }

    @Test
    void testNotificationChannelGetNotificationChannelsByWorkspaceRequiresViewer() {
        assertExpression(
            AiObservabilityNotificationChannelFacadeImpl.class, "getNotificationChannelsByWorkspace",
            VIEWER_EXPRESSION);
    }

    @Test
    void testExportJobGetExportJobsByWorkspaceRequiresViewer() {
        assertExpression(AiObservabilityExportJobFacadeImpl.class, "getExportJobsByWorkspace", VIEWER_EXPRESSION);
    }

    @Test
    void testExportJobGetExportJobRequiresAdmin() {
        assertExpression(AiObservabilityExportJobFacadeImpl.class, "getExportJob", ADMIN_EXPRESSION);
    }

    @Test
    void testExportJobCreateExportJobRequiresAdmin() {
        assertExpression(AiObservabilityExportJobFacadeImpl.class, "createExportJob", ADMIN_EXPRESSION);
    }

    @Test
    void testExportJobCancelExportJobRequiresAdmin() {
        assertExpression(AiObservabilityExportJobFacadeImpl.class, "cancelExportJob", ADMIN_EXPRESSION);
    }

    private static void assertExpression(Class<?> facadeImplClass, String methodName, String expectedExpression) {
        PreAuthorize preAuthorize = findMethod(facadeImplClass, methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method '%s' on %s must have @PreAuthorize; dropping it would silently let every authenticated user "
                    + "perform a guarded operation.",
                methodName, facadeImplClass.getSimpleName())
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' on %s @PreAuthorize expression mismatch", methodName, facadeImplClass.getSimpleName())
            .isEqualTo(expectedExpression);
    }

    private static Method findMethod(Class<?> facadeImplClass, String methodName) {
        List<Method> matches = Arrays.stream(facadeImplClass.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on %s", methodName, facadeImplClass.getSimpleName())
            .hasSize(1);

        return matches.get(0);
    }
}
