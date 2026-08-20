/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the workspace-membership gate on every {@code workspaceId}-keyed method of {@link AiAgentFacadeImpl}.
 *
 * <p>
 * The expression is compared as a string rather than exercised through a live evaluator on purpose. What went wrong
 * here was not a scope that evaluated incorrectly — it was {@code isAuthenticated()} standing where a membership check
 * belonged, which any behavioural test run as a workspace member would have passed. The identity of the expression is
 * the thing under test.
 *
 * <p>
 * The primitive {@code long.class} in every lookup below is load-bearing, not incidental: {@code #workspaceId} is only
 * a usable gate key while the parameter cannot be null, since a boxed {@code null} would reach
 * {@code AutomationPermissionEvaluator} as a null target id. Widening any of these back to {@code Long} fails its test
 * at the method lookup, before the expression is even read.
 *
 * <p>
 * {@link #testEveryWorkspaceKeyedMethodIsGated()} is the backstop: the per-method pins above it only catch a gate that
 * is <em>changed</em>, never a seventh {@code workspaceId} method added later with {@code isAuthenticated()} and no
 * test of its own.
 *
 * @author Ivica Cardic
 */
class AiAgentFacadeAuthorizationTest {

    private static final String WORKFLOW_CREATE =
        "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_CREATE')";

    private static final String WORKFLOW_VIEW =
        "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')";

    /**
     * {@code WORKFLOW_CREATE} rather than the {@code WORKFLOW_VIEW} the reads take, because provisioning an agent
     * writes: it mints a hidden {@code __AI_AGENT__} project and its generated draft workflow in the target workspace.
     * {@code WORKFLOW_CREATE} is EDITOR-rank in {@code WorkflowPermissionScopeProvider}, so a workspace VIEWER can list
     * agents and cannot create one — which is the distinction the single {@code isAuthenticated()} this replaced could
     * not express.
     */
    @Test
    void testCreateAgentRequiresWorkspaceWorkflowCreator() {
        assertExpression(WORKFLOW_CREATE, "createAgent", String.class, String.class, long.class);
    }

    /**
     * Same scope as {@link #testCreateAgentRequiresWorkspaceWorkflowCreator()} because it is the same operation with a
     * different source of the field values: import provisions a fresh agent, project and draft workflow in
     * {@code workspaceId}. If the two ever disagree, one route into the workspace is cheaper than the other.
     */
    @Test
    void testImportAgentRequiresWorkspaceWorkflowCreator() {
        assertExpression(WORKFLOW_CREATE, "importAgent", long.class, String.class);
    }

    /**
     * The listing that matters most: it discloses the same agent names and titles
     * {@link #testGetWorkspaceChatAgentsRequiresWorkspaceWorkflowViewer()} covers, so gating only the chat picker would
     * have closed one of two doors into the same room.
     */
    @Test
    void testGetAgentsRequiresWorkspaceWorkflowViewer() {
        assertExpression(WORKFLOW_VIEW, "getAgents", long.class);
    }

    @Test
    void testGetAgentTagsRequiresWorkspaceWorkflowViewer() {
        assertExpression(WORKFLOW_VIEW, "getAgentTags", long.class);
    }

    @Test
    void testGetAgentDeploymentTagsRequiresWorkspaceWorkflowViewer() {
        assertExpression(WORKFLOW_VIEW, "getAgentDeploymentTags", long.class);
    }

    @Test
    void testGetAgentDeploymentsRequiresWorkspaceWorkflowViewer() {
        assertExpression(WORKFLOW_VIEW, "getAgentDeployments", long.class);
    }

    /**
     * The scope and target are copied from {@code ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows}, the other
     * cascade of the same launcher popup. If the two ever disagree, one of the two halves of one picker is enforcing
     * something the other is not.
     */
    @Test
    void testGetWorkspaceChatAgentsRequiresWorkspaceWorkflowViewer() {
        assertExpression(WORKFLOW_VIEW, "getWorkspaceChatAgents", long.class, long.class);
    }

    /**
     * Catches the case the pins above structurally cannot: a method added later that takes a {@code workspaceId} and
     * carries {@code isAuthenticated()}, or none at all.
     *
     * <p>
     * Discovery is keyed on the parameter <em>name</em>, which only survives compilation with {@code -parameters}.
     * Without it every parameter reads {@code arg0}, the discovered set would be empty, and an "all gated" assertion
     * would pass while checking nothing — so the discovered names are asserted first, against the seven known methods.
     * That assertion fails loudly if the flag ever goes away, and it is also what makes a newly added
     * {@code workspaceId} method fail here rather than slip through ungated.
     */
    @Test
    void testEveryWorkspaceKeyedMethodIsGated() {
        List<Method> workspaceKeyedMethods = Arrays.stream(AiAgentFacadeImpl.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> !method.isSynthetic())
            .filter(
                method -> Arrays.stream(method.getParameters())
                    .anyMatch(parameter -> "workspaceId".equals(parameter.getName())))
            .toList();

        assertThat(
            workspaceKeyedMethods.stream()
                .map(Method::getName)
                .sorted()
                .toList())
                    .as(
                        "public workspaceId-keyed methods of AiAgentFacadeImpl (empty here means -parameters is off " +
                            "and this test is checking nothing)")
                    .containsExactly(
                        "createAgent", "getAgentDeploymentTags", "getAgentDeployments", "getAgentTags", "getAgents",
                        "getWorkspaceChatAgents", "importAgent");

        assertThat(
            workspaceKeyedMethods.stream()
                .filter(method -> {
                    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                    return preAuthorize == null || !preAuthorize.value()
                        .startsWith("hasPermission(#workspaceId, 'Workspace', ");
                })
                .map(Method::getName)
                .sorted()
                .toList())
                    .as("workspaceId-keyed methods without a workspace-keyed hasPermission gate")
                    .isEmpty();
    }

    private static void assertExpression(String expression, String methodName, Class<?>... parameterTypes) {
        Method method = findMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return AiAgentFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }
    }
}
