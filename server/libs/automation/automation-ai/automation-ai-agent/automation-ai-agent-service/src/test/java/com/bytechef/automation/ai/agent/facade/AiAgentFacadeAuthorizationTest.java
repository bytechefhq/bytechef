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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expression on every method of {@link AiAgentFacadeImpl} — the seven keyed on a
 * {@code workspaceId}, the twelve keyed on an agent id, the four keyed on a channel or element id, and the one that is
 * deliberately left at {@code isAuthenticated()}.
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
 * {@link #testEveryWorkspaceKeyedMethodIsGated()} and {@link #testEveryFacadeMethodIsGated()} are the backstops: the
 * per-method pins above them only catch a gate that is <em>changed</em>, never a method added later with
 * {@code isAuthenticated()} and no test of its own. The second is the broader of the two — it holds every method of the
 * {@code AiAgentFacade} interface to a {@code hasPermission} expression against a single named exemption, so a new
 * ungated method has to be argued for in this file before it compiles green.
 *
 * @author Ivica Cardic
 */
class AiAgentFacadeAuthorizationTest {

    private static final String AGENT_CREATE_BY_WORKSPACE_ID =
        "hasPermission(#workspaceId, 'Workspace', 'AGENT_CREATE')";

    private static final String AGENT_VIEW_BY_WORKSPACE_ID =
        "hasPermission(#workspaceId, 'Workspace', 'AGENT_VIEW')";

    private static final String AGENT_DELETE_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_DELETE')";

    private static final String AGENT_EDIT_BY_AGENT_ID = "hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT')";

    private static final String AGENT_EDIT_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_EDIT')";

    private static final String AGENT_VIEW_BY_AGENT_ID = "hasPermission(#agentId, 'AiAgent', 'AGENT_VIEW')";

    private static final String AGENT_VIEW_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_VIEW')";

    private static final String CHANNEL_AGENT_EDIT =
        "hasPermission(#channelId, 'AiAgentChannel', 'AGENT_EDIT')";

    private static final String ELEMENT_AGENT_EDIT =
        "hasPermission(#elementId, 'AiAgentElement', 'AGENT_EDIT')";

    /**
     * The one method that carries no resource gate. Named here rather than skipped silently so
     * {@link #testEveryFacadeMethodIsGated()} reads as an exemption with a reason attached rather than a hole.
     * {@code getAgentChannelDefinitions} reads the component registry, takes no id, and returns the same catalog for
     * every caller in the tenant.
     */
    private static final Set<String> UNGATED_METHOD_NAMES = Set.of("getAgentChannelDefinitions");

    /**
     * {@code AGENT_CREATE} rather than the {@code AGENT_VIEW} the reads take, because provisioning an agent writes: it
     * mints a hidden {@code __AI_AGENT__} project and its generated draft workflow in the target workspace.
     * {@code AGENT_CREATE} is EDITOR-rank in {@code AgentPermissionScopeProvider}, so a workspace VIEWER can list
     * agents and cannot create one — which is the distinction the single {@code isAuthenticated()} this replaced could
     * not express.
     */
    @Test
    void testCreateAgentRequiresWorkspaceAgentCreator() {
        assertExpression(AGENT_CREATE_BY_WORKSPACE_ID, "createAgent", String.class, String.class, long.class);
    }

    /**
     * Same scope as {@link #testCreateAgentRequiresWorkspaceAgentCreator()} because it is the same operation with a
     * different source of the field values: import provisions a fresh agent, project and draft workflow in
     * {@code workspaceId}. If the two ever disagree, one route into the workspace is cheaper than the other.
     */
    @Test
    void testImportAgentRequiresWorkspaceAgentCreator() {
        assertExpression(AGENT_CREATE_BY_WORKSPACE_ID, "importAgent", long.class, String.class);
    }

    /**
     * The listing that matters most: it discloses the same agent names and titles
     * {@link #testGetWorkspaceChatAgentsRequiresWorkspaceAgentViewer()} covers, so gating only the chat picker would
     * have closed one of two doors into the same room.
     */
    @Test
    void testGetAgentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgents", long.class);
    }

    @Test
    void testGetAgentTagsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgentTags", long.class);
    }

    @Test
    void testGetAgentDeploymentTagsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgentDeploymentTags", long.class);
    }

    @Test
    void testGetAgentDeploymentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgentDeployments", long.class);
    }

    /**
     * The target is the one {@code ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows} carries — the other cascade
     * of the same launcher popup — but the scope is deliberately no longer identical to it. That sibling lists
     * workflows and keeps {@code WORKFLOW_VIEW}; this one lists agents and takes {@code AGENT_VIEW}. Both are
     * VIEWER-rank, so the two halves of the popup still agree for every built-in role; they part company only in a
     * custom role that grants one vocabulary and not the other, which is exactly what an agent scope family is for.
     */
    @Test
    void testGetWorkspaceChatAgentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getWorkspaceChatAgents", long.class, long.class);
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

    /**
     * {@code AGENT_DELETE} rather than the {@code AGENT_EDIT} the draft mutators take: this is the one operation on
     * this facade that removes the agent, its hidden {@code __AI_AGENT__} project and every published version with it.
     * The scope is EDITOR-rank like the rest of the family, so this is a naming distinction rather than a privilege one
     * today — but the two are separable in a custom role, and a role built to let someone tune agents without being
     * able to destroy them is exactly the distinction {@code isAuthenticated()} could not express.
     */
    @Test
    void testDeleteAgentRequiresAgentDelete() {
        assertExpression(AGENT_DELETE_BY_ID, "deleteAgent", long.class);
    }

    /**
     * The by-id read that {@link #testGetAgentsRequiresWorkspaceAgentViewer()} does not cover: the listing was gated
     * this morning, but until now any authenticated user in the tenant could fetch any single agent — title,
     * description, instructions, channel and element configuration — by passing its id past the workspace filter the
     * listing applies.
     */
    @Test
    void testGetAgentRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "getAgent", long.class);
    }

    @Test
    void testGetAgentVersionsRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "getAgentVersions", long.class);
    }

    /**
     * {@code AGENT_VIEW} and not something stronger, but it is the read with the widest blast radius: the export
     * document carries the agent's instructions, settings, channels and elements in full. If any read on this facade is
     * ever relaxed, this is the one that must not be.
     */
    @Test
    void testExportAgentRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "exportAgent", long.class);
    }

    @Test
    void testGetDraftWorkflowIdRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_AGENT_ID, "getDraftWorkflowId", long.class);
    }

    @Test
    void testUpdateAgentRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgent", long.class, String.class, String.class, String.class);
    }

    @Test
    void testUpdateAgentSettingsRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgentSettings", long.class, Map.class);
    }

    @Test
    void testUpdateAgentTagsRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgentTags", long.class, List.class);
    }

    @Test
    void testUpdateAgentDeploymentTagsRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgentDeploymentTags", long.class, List.class);
    }

    /**
     * Publishing cuts a new version of the agent's generated workflow and is therefore a write on the agent, not a
     * separate act of deployment — {@code AGENT_EDIT}, the same scope as editing the draft it publishes.
     */
    @Test
    void testPublishAgentRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "publishAgent", long.class, String.class);
    }

    @Test
    void testAddAgentChannelRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_AGENT_ID, "addAgentChannel", long.class, String.class, Map.class, Long.class);
    }

    @Test
    void testAddAgentElementRequiresAgentEdit() {
        assertExpression(
            AGENT_EDIT_BY_AGENT_ID, "addAgentElement", long.class, String.class, Long.class, Map.class, Long.class);
    }

    /**
     * The channel and element mutators are the four methods that could not be gated on an agent id at all: they are
     * keyed on the child row's own id and nothing in the argument list names the agent or the workspace. Hence the
     * dedicated {@code 'AiAgentChannel'} / {@code 'AiAgentElement'} resource tokens, whose resolvers walk child &rarr;
     * agent &rarr; workspace so a channel id belonging to another workspace's agent resolves to <em>that</em> workspace
     * and is denied.
     *
     * <p>
     * The token half of the expression is as load-bearing as the scope half here, which is why it is pinned character
     * for character: spelling this {@code hasPermission(#channelId, 'AiAgent', …)} would hand the channel id to the
     * agent resolver, which would read it as an agent id — resolving some unrelated agent that happens to share the
     * number, or nothing at all. Silently checking the wrong row is worse than not checking.
     */
    @Test
    void testUpdateAgentChannelRequiresAgentEditOnTheChannelsOwnWorkspace() {
        assertExpression(CHANNEL_AGENT_EDIT, "updateAgentChannel", long.class, Map.class, Long.class);
    }

    @Test
    void testDeleteAgentChannelRequiresAgentEditOnTheChannelsOwnWorkspace() {
        assertExpression(CHANNEL_AGENT_EDIT, "deleteAgentChannel", long.class);
    }

    @Test
    void testUpdateAgentElementRequiresAgentEditOnTheElementsOwnWorkspace() {
        assertExpression(ELEMENT_AGENT_EDIT, "updateAgentElement", long.class, Map.class, Long.class);
    }

    @Test
    void testDeleteAgentElementRequiresAgentEditOnTheElementsOwnWorkspace() {
        assertExpression(ELEMENT_AGENT_EDIT, "deleteAgentElement", long.class);
    }

    /**
     * The registry read that legitimately keeps {@code isAuthenticated()}. Pinned positively rather than left out, so
     * that a later change tightening or loosening it has to come through this test — and so the exemption in
     * {@link #UNGATED_METHOD_NAMES} cannot quietly grow to cover a method that does touch an entity.
     */
    @Test
    void testGetAgentChannelDefinitionsStaysAuthenticatedOnly() {
        assertExpression("isAuthenticated()", "getAgentChannelDefinitions");
    }

    /**
     * The broad backstop. Every method of the {@code AiAgentFacade} interface must carry a {@code hasPermission}
     * expression, with {@link #UNGATED_METHOD_NAMES} the single named exemption.
     *
     * <p>
     * Discovery runs over the interface rather than over {@code AiAgentFacadeImpl.class.getDeclaredMethods()} so that
     * private helpers and bridge methods cannot dilute it, and the discovered count is asserted first: a filter that
     * silently matched nothing would make the "all gated" assertion below pass while checking nothing, which is the
     * exact failure mode this file exists to prevent.
     */
    @Test
    void testEveryFacadeMethodIsGated() {
        List<Method> facadeMethods = Arrays.stream(AiAgentFacade.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .map(method -> findMethod(method.getName(), method.getParameterTypes()))
            .toList();

        assertThat(facadeMethods)
            .as("methods of AiAgentFacade (empty here means the discovery filter is broken, not that all is well)")
            .hasSize(24);

        assertThat(
            facadeMethods.stream()
                .filter(method -> !UNGATED_METHOD_NAMES.contains(method.getName()))
                .filter(method -> {
                    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                    return preAuthorize == null || !preAuthorize.value()
                        .startsWith("hasPermission(");
                })
                .map(Method::getName)
                .sorted()
                .toList())
                    .as("AiAgentFacade methods without a hasPermission gate")
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
