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

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolationAction;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailViolationService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailViolationGraphQlControllerTest {

    private final AiGuardrailViolationService aiGuardrailViolationService = mock(AiGuardrailViolationService.class);

    private final AiGuardrailViolationGraphQlController controller =
        new AiGuardrailViolationGraphQlController(aiGuardrailViolationService);

    @Test
    void testAnAbsentLimitFallsBackToTheDefaultRatherThanUnbounded() {
        when(aiGuardrailViolationService.getViolations(42L, 100)).thenReturn(List.of());

        controller.aiGuardrailViolations(42L, null);

        verify(aiGuardrailViolationService).getViolations(42L, 100);
    }

    @Test
    void testTheQueryReturnsWhatTheServiceGives() {
        AiGuardrailViolation violation = new AiGuardrailViolation(
            "EMAIL_ADDRESS", 0, 5, 11, new BigDecimal("0.90"), AiGuardrailViolationAction.REDACTED, "ai_agent", 42L, 1,
            null);

        when(aiGuardrailViolationService.getViolations(42L, 25)).thenReturn(List.of(violation));

        assertThat(controller.aiGuardrailViolations(42L, 25)).containsExactly(violation);
    }

    @Test
    void testTheReadIsGatedOnTheWorkspaceItReads() throws NoSuchMethodException {
        Method method = AiGuardrailViolationGraphQlController.class.getDeclaredMethod(
            "aiGuardrailViolations", Long.class, Integer.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value())
            .as("a workspace read must require a scope on THAT workspace")
            .contains("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')");
        assertThat(preAuthorize.value())
            .as("the tenant-default bucket has no workspace to scope against, so it must be admin-only")
            .contains("#workspaceId != null");
    }

    @Test
    void testThereIsNoWriteOrByIdSurface() {
        List<String> methods = List.of(AiGuardrailViolationGraphQlController.class.getDeclaredMethods())
            .stream()
            .filter(method -> !method.isSynthetic())
            .map(Method::getName)
            .filter(name -> !name.contains("$"))
            .toList();

        assertThat(methods).containsExactly("aiGuardrailViolations");
    }
}
