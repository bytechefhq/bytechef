/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.client.ConnectedUserProjectRemoteClientIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserWorkflowCopyResolverRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
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
class RemoteConnectedUserWorkflowCopyResolverClientIntTest {

    private static final String TEMPLATE_DEFINITION = """
        {"label":"Sync leads","description":"Copies new leads","triggers":[],"tasks":[]}
        """;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteConnectedUserWorkflowCopyResolverClient remoteConnectedUserWorkflowCopyResolverClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new ConnectedUserWorkflowCopyResolverRemoteServiceGrpcServer(connectedUserWorkflowCopyResolver));

        remoteConnectedUserWorkflowCopyResolverClient = new RemoteConnectedUserWorkflowCopyResolverClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels());
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();
    }

    @Test
    void testGetWorkflowCopyUuidsReturnsTheCopiesEveryConnectedUserMadeOfTheTemplate() {
        String templateWorkflowUuid = createPublishedTemplate();
        String otherTemplateWorkflowUuid = createPublishedTemplate();

        String firstCopyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            createConnectedUser().getExternalId(), templateWorkflowUuid, Environment.PRODUCTION);
        String secondCopyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            createConnectedUser().getExternalId(), templateWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.copyWorkflowTemplate(
            createConnectedUser().getExternalId(), otherTemplateWorkflowUuid, Environment.PRODUCTION);

        assertThat(remoteConnectedUserWorkflowCopyResolverClient.getWorkflowCopyUuids(templateWorkflowUuid))
            .containsExactlyInAnyOrder(firstCopyWorkflowUuid, secondCopyWorkflowUuid);
    }

    @Test
    void testGetWorkflowCopyUuidsOfATemplateNobodyCopiedIsEmpty() {
        String templateWorkflowUuid = createPublishedTemplate();

        assertThat(remoteConnectedUserWorkflowCopyResolverClient.getWorkflowCopyUuids(templateWorkflowUuid))
            .isEmpty();
    }

    private ConnectedUser createConnectedUser() {
        return connectedUserService.createConnectedUser(
            "remote-copy-resolver-user-" + UUID.randomUUID(), Environment.PRODUCTION);
    }

    private String createPublishedTemplate() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "Remote copy resolver " + UUID.randomUUID(), "", null, List.of(), null);

        String workflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            projectId, TEMPLATE_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(projectId);

        automationWorkflowProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), 1, true, true);

        return workflowUuid;
    }
}
