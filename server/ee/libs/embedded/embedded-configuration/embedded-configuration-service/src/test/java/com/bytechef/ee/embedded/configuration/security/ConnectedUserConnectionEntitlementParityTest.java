/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacadeImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Pins the property that the "appears in the list, then 403s" symptom is made of: what
 * {@code ConnectedUserConnectionFacadeImpl.getConnections} SHOWS a connected user and what
 * {@link ConnectedUserResourceMembershipResolver} GRANTS that same user must be one set.
 *
 * <p>
 * Both are built here over ONE {@link ConnectedUserConnectionMembership} and one set of stubbed services, so the test
 * compares the two production code paths rather than two restatements of the rule. Nothing about the shape of the
 * entitlement is asserted directly -- only that the two answers agree, for entitled ids and unentitled ones alike.
 * Reinstate either hand-written union and this goes red as soon as the two drift.
 *
 * <p>
 * The parity is over the READ scopes, and deliberately so: the picker lists what the caller may see and use, and the
 * resolver's grant for a mutating scope is narrower on purpose -- a configuration-level connection is shared with every
 * connected user on that configuration, so it is listed but not theirs to change. That narrowing is pinned in
 * {@code ConnectedUserResourceMembershipResolverTest}; what belongs here is that listing and read authorization agree.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserConnectionEntitlementParityTest {

    private static final String EXTERNAL_USER_ID = "connected-user-1";
    private static final long CONNECTED_USER_ID = 9L;
    private static final long PROJECT_ID = 100L;
    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 500L;

    private static final long OWN_INSTANCE_CONNECTION_ID = 10L;
    private static final long OWN_CREATED_CONNECTION_ID = 11L;
    private static final long CONFIGURATION_SHARED_CONNECTION_ID = 12L;
    private static final long FOREIGN_CONNECTION_ID = 99L;

    private ConnectedUserConnectionFacadeImpl facade;
    private ConnectedUserResourceMembershipResolver resolver;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), new User(EXTERNAL_USER_ID, "", List.of())));

        securityUtilsMock = mockStatic(SecurityUtils.class);

        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(EXTERNAL_USER_ID));

        ConnectedUserConnectionService connectedUserConnectionService = mock(ConnectedUserConnectionService.class);
        ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
        ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
        ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService =
            mock(IntegrationInstanceConfigurationWorkflowService.class);
        IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);

        when(integrationInstanceService.getConnectedUserIntegrationInstances(
            CONNECTED_USER_ID, Environment.PRODUCTION))
                .thenReturn(List.of(integrationInstance()));
        when(connectedUserConnectionService.getConnectionIds(CONNECTED_USER_ID))
            .thenReturn(List.of(OWN_CREATED_CONNECTION_ID));
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
            List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                .thenReturn(List.of(integrationInstanceConfigurationWorkflow()));

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);

        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(new ConnectedUser()));
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject()));

        // Echoes back one ConnectionDTO per id it is asked for, so the picker's output is exactly the id set the
        // facade decided on and the comparison below is between the two entitlement computations, nothing else.
        when(connectionFacade.getConnections(any(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> requestedConnectionIds = invocation.getArgument(0);

                return requestedConnectionIds.stream()
                    .map(connectionId -> ConnectionDTO.builder()
                        .id(connectionId)
                        .build())
                    .toList();
            });

        ConnectedUserConnectionMembership connectedUserConnectionMembership = new ConnectedUserConnectionMembership(
            connectedUserConnectionService, integrationInstanceConfigurationWorkflowService,
            integrationInstanceService);

        facade = new ConnectedUserConnectionFacadeImpl(
            connectedUserConnectionMembership, connectedUserConnectionService, connectedUserService, connectionFacade);

        resolver = new ConnectedUserResourceMembershipResolver(
            mock(AutomationWorkflowProjectFacade.class), connectedUserConnectionMembership, connectedUserProjectService,
            mock(ConnectedUserProjectWorkflowService.class), connectedUserService, mock(JobService.class),
            mock(PrincipalJobService.class), mock(ProjectDeploymentService.class), mock(ProjectService.class),
            mock(ProjectWorkflowService.class));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();

        SecurityContextHolder.clearContext();
    }

    /**
     * The mutating half of the same picture: everything the picker lists is readable, but only the OWNED subset is
     * mutable. Without this the parity assertion above would still hold if the scope split were removed.
     */
    @Test
    void testTheListedSetIsReadableButOnlyTheOwnedSubsetIsMutable() {
        Set<Long> shownConnectionIds = shownConnectionIds();

        assertThat(shownConnectionIds).contains(CONFIGURATION_SHARED_CONNECTION_ID, OWN_INSTANCE_CONNECTION_ID);

        assertThat(resolver.resolve(CONFIGURATION_SHARED_CONNECTION_ID, "Connection", "CONNECTION_USE"))
            .isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(CONFIGURATION_SHARED_CONNECTION_ID, "Connection", "CONNECTION_EDIT"))
            .isEqualTo(Decision.DENIED);

        assertThat(resolver.resolve(OWN_INSTANCE_CONNECTION_ID, "Connection", "CONNECTION_EDIT"))
            .isEqualTo(Decision.GRANTED);
    }

    @Test
    void testThePickerAndTheResolverAgreeOnEveryConnectionId() {
        Set<Long> shownConnectionIds = shownConnectionIds();

        List<Long> candidateConnectionIds = List.of(
            OWN_INSTANCE_CONNECTION_ID, OWN_CREATED_CONNECTION_ID, CONFIGURATION_SHARED_CONNECTION_ID,
            FOREIGN_CONNECTION_ID);

        assertThat(candidateConnectionIds)
            .allSatisfy(connectionId -> {
                Decision decision = resolver.resolve(connectionId, "Connection", "CONNECTION_USE");

                assertThat(decision == Decision.GRANTED)
                    .as("connection id=%s: picker shows=%s, resolver=%s", connectionId,
                        shownConnectionIds.contains(connectionId), decision)
                    .isEqualTo(shownConnectionIds.contains(connectionId));
            });
    }

    /**
     * The same agreement, stated as the set it is about: all three entitlement sources are present on both sides and
     * the foreign id on neither. Without this a resolver and a picker that both denied everything would agree.
     */
    @Test
    void testTheAgreedSetIsTheUnionOfAllThreeSources() {
        Set<Long> shownConnectionIds = shownConnectionIds();

        assertThat(shownConnectionIds).containsExactlyInAnyOrder(
            OWN_INSTANCE_CONNECTION_ID, OWN_CREATED_CONNECTION_ID, CONFIGURATION_SHARED_CONNECTION_ID);

        assertThat(resolver.resolve(CONFIGURATION_SHARED_CONNECTION_ID, "Connection", "CONNECTION_USE"))
            .isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(FOREIGN_CONNECTION_ID, "Connection", "CONNECTION_USE"))
            .isEqualTo(Decision.DENIED);
    }

    private Set<Long> shownConnectionIds() {
        return facade.getConnections(CONNECTED_USER_ID, null, List.of())
            .stream()
            .map(ConnectionDTO::id)
            .collect(Collectors.toSet());
    }

    private static ConnectedUserProject connectedUserProject() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(1L);
        connectedUserProject.setConnectedUserId(CONNECTED_USER_ID);
        connectedUserProject.setProjectId(PROJECT_ID);

        return connectedUserProject;
    }

    private static IntegrationInstance integrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(OWN_INSTANCE_CONNECTION_ID);
        integrationInstance.setIntegrationInstanceConfigurationId(INTEGRATION_INSTANCE_CONFIGURATION_ID);

        return integrationInstance;
    }

    private static IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow() {
        IntegrationInstanceConfigurationWorkflowConnection workflowConnection =
            new IntegrationInstanceConfigurationWorkflowConnection(
                CONFIGURATION_SHARED_CONNECTION_ID, "connection", "node");

        return new IntegrationInstanceConfigurationWorkflow(List.of(workflowConnection), Map.of(), "workflow-1");
    }
}
