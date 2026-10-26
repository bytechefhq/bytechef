/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailCustomRuleService;
import com.bytechef.ee.platform.ai.guardrails.web.graphql.AiGuardrailCustomRuleGraphQlController.AiGuardrailCustomRuleInput;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailCustomRuleGraphQlControllerTest {

    private static final long WORKSPACE_ID = 42L;
    private static final long OTHER_WORKSPACE_ID = 99L;

    private final AiGuardrailCustomRuleService aiGuardrailCustomRuleService =
        mock(AiGuardrailCustomRuleService.class);

    private final AiGuardrailCustomRuleGraphQlController controller =
        new AiGuardrailCustomRuleGraphQlController(aiGuardrailCustomRuleService);

    @Test
    void testTheReadIsGatedOnTheWorkspaceItReads() {
        assertThat(preAuthorizeOf("aiGuardrailCustomRules", long.class))
            .contains("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')");
    }

    @Test
    void testEveryMutationKeepsAdminAndNamesTheEditScopeOnItsOwnWorkspaceArgument() {
        List<Method> mutations = List.of(AiGuardrailCustomRuleGraphQlController.class.getDeclaredMethods())
            .stream()
            .filter(method -> method.isAnnotationPresent(MutationMapping.class))
            .toList();

        assertThat(mutations)
            .as("create, updatePattern, setEnabled and delete")
            .hasSize(4);

        for (Method mutation : mutations) {
            PreAuthorize preAuthorize = mutation.getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as("%s must be gated", mutation.getName())
                .isNotNull();

            String workspaceReference =
                mutation.getName()
                    .equals("createAiGuardrailCustomRule") ? "#input.workspaceId" : "#workspaceId";

            assertThat(preAuthorize.value())
                .as("%s must permit ROLE_ADMIN and the workspace-scoped AI_GATEWAY_EDIT permission", mutation.getName())
                .isEqualTo(
                    "hasAuthority('ROLE_ADMIN') or hasPermission(" + workspaceReference
                        + ", 'Workspace', 'AI_GATEWAY_EDIT')");
        }
    }

    @Test
    void testCreateAllowsWorkspaceAdminButRefusesViewerAndOtherWorkspaceAdmin() {
        AiGuardrailCustomRuleInput input = new AiGuardrailCustomRuleInput(
            WORKSPACE_ID, "REGEX", "\\d+", 1, 0.5, null, null, null);

        assertWorkspaceEditScopeGate(
            "createAiGuardrailCustomRule", new Class<?>[] {
                AiGuardrailCustomRuleInput.class
            },
            new Object[] {
                input
            });
    }

    @Test
    void testUpdatePatternAllowsWorkspaceAdminButRefusesViewerAndOtherWorkspaceAdmin() {
        assertWorkspaceEditScopeGate(
            "updateAiGuardrailCustomRulePattern", new Class<?>[] {
                long.class, long.class, String.class
            },
            new Object[] {
                WORKSPACE_ID, 7L, "\\bACME-\\d{4}\\b"
            });
    }

    @Test
    void testSetEnabledAllowsWorkspaceAdminButRefusesViewerAndOtherWorkspaceAdmin() {
        assertWorkspaceEditScopeGate(
            "setAiGuardrailCustomRuleEnabled", new Class<?>[] {
                long.class, long.class, boolean.class
            },
            new Object[] {
                WORKSPACE_ID, 7L, true
            });
    }

    @Test
    void testDeleteAllowsWorkspaceAdminButRefusesViewerAndOtherWorkspaceAdmin() {
        assertWorkspaceEditScopeGate(
            "deleteAiGuardrailCustomRule", new Class<?>[] {
                long.class, long.class
            }, new Object[] {
                WORKSPACE_ID, 7L
            });
    }

    @Test
    void testTheControllerDelegatesRatherThanValidating() {
        controller.updateAiGuardrailCustomRulePattern(42L, 7L, "\\bACME-\\d{4}\\b");

        verify(aiGuardrailCustomRuleService).updatePattern(7L, 42L, "\\bACME-\\d{4}\\b");
    }

    @Test
    void testDeleteIsScopedToTheWorkspaceItWasGivenNotJustTheId() {
        controller.deleteAiGuardrailCustomRule(42L, 7L);

        verify(aiGuardrailCustomRuleService).delete(7L, 42L);
    }

    private void assertWorkspaceEditScopeGate(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        Authentication tenantAdmin = new UsernamePasswordAuthenticationToken(
            "tenant-admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        PermissionEvaluator grantsNothingAtAll = mock(PermissionEvaluator.class);

        assertThat(
            AiGuardrailGateTestSupport.isAuthorized(
                AiGuardrailCustomRuleGraphQlController.class, controller, methodName, parameterTypes, arguments,
                tenantAdmin, grantsNothingAtAll))
                    .as("a tenant admin must be allowed even with no workspace permission granted")
                    .isTrue();

        Authentication nonTenantAdmin = new UsernamePasswordAuthenticationToken(
            "workspace-admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        PermissionEvaluator grantsTargetWorkspace = mock(PermissionEvaluator.class);

        when(grantsTargetWorkspace.hasPermission(nonTenantAdmin, WORKSPACE_ID, "Workspace", "AI_GATEWAY_EDIT"))
            .thenReturn(true);

        assertThat(
            AiGuardrailGateTestSupport.isAuthorized(
                AiGuardrailCustomRuleGraphQlController.class, controller, methodName, parameterTypes, arguments,
                nonTenantAdmin, grantsTargetWorkspace))
                    .as("a workspace admin of the target workspace must be allowed")
                    .isTrue();

        PermissionEvaluator grantsNoWorkspace = mock(PermissionEvaluator.class);

        assertThat(
            AiGuardrailGateTestSupport.isAuthorized(
                AiGuardrailCustomRuleGraphQlController.class, controller, methodName, parameterTypes, arguments,
                nonTenantAdmin, grantsNoWorkspace))
                    .as("a workspace member (viewer) of the target workspace must be refused")
                    .isFalse();

        PermissionEvaluator grantsOnlyAnotherWorkspace = mock(PermissionEvaluator.class);

        when(
            grantsOnlyAnotherWorkspace.hasPermission(
                nonTenantAdmin, OTHER_WORKSPACE_ID, "Workspace", "AI_GATEWAY_EDIT"))
                    .thenReturn(true);

        assertThat(
            AiGuardrailGateTestSupport.isAuthorized(
                AiGuardrailCustomRuleGraphQlController.class, controller, methodName, parameterTypes, arguments,
                nonTenantAdmin, grantsOnlyAnotherWorkspace))
                    .as("a workspace admin of a different workspace must be refused")
                    .isFalse();
    }

    private static String preAuthorizeOf(String name, Class<?>... parameterTypes) {
        try {
            PreAuthorize preAuthorize = AiGuardrailCustomRuleGraphQlController.class
                .getDeclaredMethod(name, parameterTypes)
                .getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize).isNotNull();

            return preAuthorize.value();
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException(noSuchMethodException);
        }
    }
}
