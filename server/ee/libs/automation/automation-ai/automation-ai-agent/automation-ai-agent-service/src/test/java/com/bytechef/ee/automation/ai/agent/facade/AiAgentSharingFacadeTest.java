/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.ee.automation.configuration.facade.ProjectSharingFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins that the agent-keyed sharing surface is a delegation and not a second implementation.
 *
 * <p>
 * The four behavioural tests say the project facade is reached, with the agent resolved to its backing project and its
 * workspace. On their own they are not enough: they would still pass if the facade ALSO kept a private copy of the
 * sharing rules, or grew one later. {@link #testHoldsNothingItCouldReimplementSharingWith()} is what closes that —
 * reimplementing any of the four operations means injecting {@code ResourceGrantService}, {@code ProjectService},
 * {@code WorkspaceUserService} or a policy registry, and the field list is asserted exactly, so the drift fails here
 * before it can be written. It is the same job {@code ProjectDeploymentWorkflowGraphQlControllerAuthorizationTest}
 * gives its {@code verifyNoInteractions}, in the only form available against a class that would have to GAIN a
 * collaborator rather than quietly use one it already holds.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentSharingFacadeTest {

    private static final long AGENT_ID = 3L;
    private static final long PROJECT_ID = 5L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    private final AiAgentService aiAgentService = mock(AiAgentService.class);
    private final ProjectSharingFacade projectSharingFacade = mock(ProjectSharingFacade.class);

    private AiAgentSharingFacadeImpl aiAgentSharingFacade;

    @BeforeEach
    void setUp() {
        AiAgent agent = new AiAgent();

        agent.setId(AGENT_ID);
        agent.setProjectId(PROJECT_ID);
        agent.setWorkspaceId(WORKSPACE_ID);

        when(aiAgentService.fetchAgent(AGENT_ID)).thenReturn(Optional.of(agent));

        aiAgentSharingFacade = new AiAgentSharingFacadeImpl(aiAgentService, projectSharingFacade);
    }

    @Test
    void testGetAgentGrantsDelegatesToTheProjectFacade() {
        when(projectSharingFacade.getProjectGrants(WORKSPACE_ID, PROJECT_ID)).thenReturn(List.of(USER_ID));

        assertThat(aiAgentSharingFacade.getAgentGrants(AGENT_ID)).containsExactly(USER_ID);

        verify(projectSharingFacade).getProjectGrants(WORKSPACE_ID, PROJECT_ID);
    }

    @Test
    void testGrantAgentAccessDelegatesToTheProjectFacade() {
        aiAgentSharingFacade.grantAgentAccess(AGENT_ID, USER_ID);

        verify(projectSharingFacade).grantProjectAccess(WORKSPACE_ID, PROJECT_ID, USER_ID);
    }

    @Test
    void testRevokeAgentAccessDelegatesToTheProjectFacade() {
        aiAgentSharingFacade.revokeAgentAccess(AGENT_ID, USER_ID);

        verify(projectSharingFacade).revokeProjectAccess(WORKSPACE_ID, PROJECT_ID, USER_ID);
    }

    @Test
    void testSetAgentVisibilityDelegatesToTheProjectFacade() {
        aiAgentSharingFacade.setAgentVisibility(AGENT_ID, ResourceVisibility.PRIVATE);

        verify(projectSharingFacade).setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.PRIVATE);
    }

    /**
     * The rung is handed on untouched rather than validated here. {@code ProjectSharingFacadeImpl} rejects an
     * unsupported one, and duplicating that check would give agents a second answer to "which rungs exist" that could
     * disagree with the project's.
     */
    @Test
    void testAnUnsupportedRungIsTheProjectFacadesToRefuse() {
        aiAgentSharingFacade.setAgentVisibility(AGENT_ID, ResourceVisibility.ORGANIZATION);

        verify(projectSharingFacade).setProjectVisibility(WORKSPACE_ID, PROJECT_ID, ResourceVisibility.ORGANIZATION);
    }

    @Test
    void testAnUnknownAgentIsRejectedBeforeTheProjectFacadeIsReached() {
        when(aiAgentService.fetchAgent(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiAgentSharingFacade.setAgentVisibility(404L, ResourceVisibility.PRIVATE))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AiAgentErrorType.INVALID_AGENT.getErrorKey()));

        verifyNoInteractions(projectSharingFacade);
    }

    /**
     * {@code ai_agent.workspace_id} is nullable, and the project facade's first argument has to come from somewhere.
     * Substituting a default would hand a foreign workspace's sharing rules an agent nobody's workspace owns.
     */
    @Test
    void testAWorkspacelessAgentIsRejectedBeforeTheProjectFacadeIsReached() {
        AiAgent orphanAgent = new AiAgent();

        orphanAgent.setId(9L);
        orphanAgent.setProjectId(PROJECT_ID);

        when(aiAgentService.fetchAgent(9L)).thenReturn(Optional.of(orphanAgent));

        assertThatThrownBy(() -> aiAgentSharingFacade.grantAgentAccess(9L, USER_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AiAgentErrorType.INVALID_AGENT.getErrorKey()));

        verifyNoInteractions(projectSharingFacade);
    }

    /**
     * Exactly two collaborators: the one that resolves an agent to its project, and the one that owns the sharing
     * rules. Anything else on this list would be a sharing primitive, and a sharing primitive here is a second
     * implementation waiting to disagree with the project's.
     */
    @Test
    void testHoldsNothingItCouldReimplementSharingWith() {
        List<Class<?>> fieldTypes = Arrays.stream(AiAgentSharingFacadeImpl.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();

        assertThat(fieldTypes).containsExactlyInAnyOrder(AiAgentService.class, ProjectSharingFacade.class);
    }
}
