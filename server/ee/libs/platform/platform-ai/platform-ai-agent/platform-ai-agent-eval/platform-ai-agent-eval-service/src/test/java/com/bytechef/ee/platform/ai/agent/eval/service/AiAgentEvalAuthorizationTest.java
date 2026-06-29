/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close AI-agent-eval IDOR (T24). Eval entities are tenant-global
 * developer tooling (no workspace scope), so the authoring writes require tenant admin. The reads, the result/verdict
 * writes, {@code updateAgentEvalRun}, and the {@code *ByWorkflowId} cascade deletes are all reached without a user
 * {@code SecurityContext} (the async run executor and the workflow pre-delete listener) and stay ungated -- negative
 * assertions lock that in.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentEvalAuthorizationTest {

    @Test
    void testScenarioWritesRequireTenantAdmin() {
        assertAdmin(AgentEvalScenarioServiceImpl.class, "createAiAgentEvalScenario");
        assertAdmin(AgentEvalScenarioServiceImpl.class, "updateAiAgentEvalScenario");
        assertAdmin(AgentEvalScenarioServiceImpl.class, "deleteAiAgentEvalScenario");
    }

    @Test
    void testTestWritesRequireTenantAdmin() {
        assertAdmin(AgentEvalTestServiceImpl.class, "createAiAgentEvalTest");
        assertAdmin(AgentEvalTestServiceImpl.class, "updateAiAgentEvalTest");
        assertAdmin(AgentEvalTestServiceImpl.class, "deleteAiAgentEvalTest");
    }

    @Test
    void testJudgeWritesRequireTenantAdmin() {
        assertAdmin(AgentJudgeServiceImpl.class, "createAiAgentJudge");
        assertAdmin(AgentJudgeServiceImpl.class, "updateAiAgentJudge");
        assertAdmin(AgentJudgeServiceImpl.class, "deleteAiAgentJudge");
    }

    @Test
    void testScenarioJudgeWritesRequireTenantAdmin() {
        assertAdmin(AgentScenarioJudgeServiceImpl.class, "createAiAgentScenarioJudge");
        assertAdmin(AgentScenarioJudgeServiceImpl.class, "updateAiAgentScenarioJudge");
        assertAdmin(AgentScenarioJudgeServiceImpl.class, "deleteAiAgentScenarioJudge");
    }

    @Test
    void testToolSimulationWritesRequireTenantAdmin() {
        assertAdmin(AgentScenarioToolSimulationServiceImpl.class, "createAiAgentScenarioToolSimulation");
        assertAdmin(AgentScenarioToolSimulationServiceImpl.class, "updateAiAgentScenarioToolSimulation");
        assertAdmin(AgentScenarioToolSimulationServiceImpl.class, "deleteAiAgentScenarioToolSimulation");
    }

    @Test
    void testRunCreateAndDeleteRequireTenantAdmin() {
        assertAdmin(AgentEvalRunServiceImpl.class, "createAgentEvalRun");
        assertAdmin(AgentEvalRunServiceImpl.class, "deleteAgentEvalRun");
    }

    @Test
    void testExecutorAndCascadeMethodsAreNotGated() {
        assertNotGated(AgentEvalRunServiceImpl.class, "updateAgentEvalRun");
        assertNotGated(AgentEvalTestServiceImpl.class, "deleteAgentEvalTestsByWorkflowId");
        assertNotGated(AgentJudgeServiceImpl.class, "deleteAgentJudgesByWorkflowId");
    }

    private static void assertAdmin(Class<?> clazz, String methodName) {
        Method match = findGated(clazz, methodName);

        assertThat(match)
            .as("@PreAuthorize-annotated method %s on %s", methodName, clazz.getSimpleName())
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("isTenantAdmin()");
    }

    private static void assertNotGated(Class<?> clazz, String methodName) {
        boolean anyGated = false;

        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                anyGated = true;
            }
        }

        assertThat(anyGated)
            .as("executor/cascade method %s on %s must NOT carry @PreAuthorize", methodName, clazz.getSimpleName())
            .isFalse();
    }

    private static Method findGated(Class<?> clazz, String methodName) {
        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                return candidate;
            }
        }

        return null;
    }
}
