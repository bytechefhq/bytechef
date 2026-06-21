/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the tenant-admin {@code @PreAuthorize} on every AI-agent-eval read consumed by the GraphQL controller (the T24
 * residual). The shared services stay ungated so the asynchronous {@code AiAgentEvalRunExecutor} can keep reading
 * scenarios/judges with no user security context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentEvalApiFacadeAuthorizationTest {

    @Test
    void testAllReadsRequireTenantAdmin() {
        for (String methodName : new String[] {
            "getAgentJudges", "getAgentEvalTests", "fetchAgentEvalTest", "getAgentEvalRuns", "fetchAgentEvalRun",
            "fetchAgentEvalResult", "getAgentEvalResult"
        }) {

            assertTenantAdmin(methodName);
        }
    }

    private static void assertTenantAdmin(String methodName) {
        Method method = null;

        for (Method candidate : AiAgentEvalApiFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {

                method = candidate;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value())
            .as("@PreAuthorize expression on %s", methodName)
            .isEqualTo("hasPermission('Tenant', 'ADMIN')");
    }
}
