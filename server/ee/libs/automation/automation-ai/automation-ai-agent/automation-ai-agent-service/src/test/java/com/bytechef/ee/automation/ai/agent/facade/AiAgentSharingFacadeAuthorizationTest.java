/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} expressions on {@link AiAgentSharingFacadeImpl}. The guards live on the facade, never
 * on {@code AiAgentSharingGraphQlController}, so they protect every caller rather than the GraphQL entry point alone.
 *
 * <p>
 * All four take the same expression, and every part of it matters. The first disjunct is {@code AGENT_EDIT} and not the
 * project sibling's {@code isResourceOwner}, because {@code isResourceOwner('AiAgent', …)} is unconditionally false —
 * {@code AiAgentOwnershipResolver} answers with a workspace and no owner user — so that expression would lock an
 * agent's creator out of sharing it. It is keyed on {@code 'AiAgent'} and not {@code 'Workspace'}, because only the
 * agent-keyed token carries the visibility precondition {@code AiAgentVisibilityProvider} registers. And it is
 * {@code AGENT_EDIT} on the grants READ too, because the audience of a withheld agent is not part of seeing it.
 *
 * <p>
 * The second disjunct is the recovery path, and it is not decoration: that same visibility precondition denies a
 * WORKSPACE admin who is neither the agent's creator nor a grantee — the resolver's admin bypass is TENANT admin — so
 * without it a withheld agent would be re-shareable by nobody but its creator, while the identical person could
 * re-share a withheld PROJECT because {@code ProjectSharingFacadeImpl}'s gate never consults visibility.
 * {@code PermissionServiceAgentVisibilityTest.testWorkspaceAdminIsRescuedByTheResourceRoleDisjunct} pins the halves
 * behaviourally: the first disjunct denies her and the second allows her.
 *
 * <p>
 * The expression is the whole of neither gate: {@code ProjectSharingFacadeImpl}'s owner-or-admin still runs on the
 * delegated call, so the effective rule is this AND that. {@code AiAgentSharingFacadeTest} pins the delegation that
 * makes the second half reachable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentSharingFacadeAuthorizationTest {

    private static final String AGENT_EDIT_OR_WORKSPACE_ADMIN =
        "hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT') || " +
            "@permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')";

    @ParameterizedTest
    @ValueSource(strings = {
        "getAgentGrants", "grantAgentAccess", "revokeAgentAccess", "setAgentVisibility"
    })
    void testSharingMethodsRequireAgentEditOrWorkspaceAdmin(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize", methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' must be gated on AGENT_EDIT on the agent, or workspace ADMIN", methodName)
            .isEqualTo(AGENT_EDIT_OR_WORKSPACE_ADMIN);
    }

    /**
     * The backstop: a method added to this facade without being listed above fails here rather than shipping unnoticed
     * with whatever gate — or none — it was given.
     */
    @Test
    void testEverySharingMethodIsCovered() {
        List<String> annotatedMethods = Arrays.stream(AiAgentSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(annotatedMethods)
            .containsExactly("getAgentGrants", "grantAgentAccess", "revokeAgentAccess", "setAgentVisibility");
    }

    /**
     * The primitive {@code long} is load-bearing: {@code #agentId} is only a usable gate key while the parameter cannot
     * be null, since a boxed {@code null} reaches {@code AutomationPermissionEvaluator} as a null target id. Both
     * editions' {@code PermissionServiceImpl} would then deny, but that is an accident of how
     * {@code ResourceOwnershipResolver.resolveOwner(Serializable)} handles a non-{@code Number}; the parameter type is
     * the real guarantee.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "getAgentGrants", "grantAgentAccess", "revokeAgentAccess", "setAgentVisibility"
    })
    void testSharingMethodsKeyOnAPrimitiveAgentId(String methodName) {
        assertThat(findMethod(methodName).getParameterTypes()[0]).isEqualTo(long.class);
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(AiAgentSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getName()
                .equals(methodName))
            .filter(method -> !method.isSynthetic())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected '" + methodName + "' on AiAgentSharingFacadeImpl"));
    }
}
