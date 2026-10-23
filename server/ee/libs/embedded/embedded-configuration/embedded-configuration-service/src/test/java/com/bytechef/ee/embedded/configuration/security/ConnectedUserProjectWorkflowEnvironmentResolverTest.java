/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectWorkflowEnvironmentResolverTest {

    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository =
        mock(ConnectedUserProjectWorkflowRepository.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final ConnectUserProjectRepository connectUserProjectRepository = mock(ConnectUserProjectRepository.class);
    private final ConnectedUserProjectWorkflowEnvironmentResolver resolver =
        new ConnectedUserProjectWorkflowEnvironmentResolver(
            connectedUserProjectWorkflowRepository, connectedUserService, connectUserProjectRepository);

    private void stubWorkflowAndProject() {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(21L);

        when(connectedUserProjectWorkflowRepository.findById(7L))
            .thenReturn(Optional.of(connectedUserProjectWorkflow));
        when(connectUserProjectRepository.findById(21L))
            .thenReturn(Optional.of(new ConnectedUserProject(31L, 21L, 41L, 0)));
    }

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("ConnectedUserProjectWorkflow");
    }

    @Test
    void testTheEnvironmentComesFromTheConnectedUser() {
        stubWorkflowAndProject();

        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnvironment(Environment.STAGING);

        when(connectedUserService.fetchConnectedUser(31L)).thenReturn(Optional.of(connectedUser));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.STAGING);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        when(connectedUserProjectWorkflowRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testAMissingConnectedUserHasNoEnvironment() {
        stubWorkflowAndProject();

        when(connectedUserService.fetchConnectedUser(31L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }
}
