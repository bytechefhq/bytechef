/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.client.ConnectedUserProjectRemoteClientIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserProjectRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConnectedUserProjectRemoteClientIntTestConfiguration
class RemoteConnectedUserProjectServiceClientIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {"label":"Leads","description":"Copies leads","triggers":[],"tasks":[]}
        """;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteConnectedUserProjectServiceClient remoteConnectedUserProjectServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new ConnectedUserProjectRemoteServiceGrpcServer(connectedUserProjectService));

        remoteConnectedUserProjectServiceClient = new RemoteConnectedUserProjectServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels());
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();
    }

    @Test
    void testContainsProjectDeploymentIsTrueOnlyForTheDeploymentOfAConnectedUsersProject() {
        ConnectedUser connectedUser = createConnectedUser();

        long projectDeploymentId = createPublishedProjectDeployment(connectedUser);

        assertThat(remoteConnectedUserProjectServiceClient.containsProjectDeployment(projectDeploymentId)).isTrue();
        assertThat(remoteConnectedUserProjectServiceClient.containsProjectDeployment(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void testFetchConnectedUserIdReturnsTheConnectedUserOwningTheDeployment() {
        ConnectedUser connectedUser = createConnectedUser();
        ConnectedUser otherConnectedUser = createConnectedUser();

        long projectDeploymentId = createPublishedProjectDeployment(connectedUser);
        long otherProjectDeploymentId = createPublishedProjectDeployment(otherConnectedUser);

        assertThat(remoteConnectedUserProjectServiceClient.fetchConnectedUserId(projectDeploymentId))
            .contains(connectedUser.getId());
        assertThat(remoteConnectedUserProjectServiceClient.fetchConnectedUserId(otherProjectDeploymentId))
            .contains(otherConnectedUser.getId());
        assertThat(remoteConnectedUserProjectServiceClient.fetchConnectedUserId(Long.MAX_VALUE)).isEmpty();
    }

    private ConnectedUser createConnectedUser() {
        return connectedUserService.createConnectedUser(
            "remote-project-service-user-" + UUID.randomUUID(), Environment.PRODUCTION);
    }

    private long createPublishedProjectDeployment(ConnectedUser connectedUser) {
        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            connectedUser.getExternalId(), WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(
            connectedUser.getExternalId(), workflowUuid, "published", null);

        List<ConnectedUserProject> connectedUserProjects =
            connectedUserProjectService.getConnectedUserConnectedUserProjects(connectedUser.getId());

        ConnectedUserProject connectedUserProject = connectedUserProjects.getFirst();

        return projectDeploymentService.getProjectDeploymentId(
            connectedUserProject.getProjectId(), Environment.PRODUCTION);
    }
}
