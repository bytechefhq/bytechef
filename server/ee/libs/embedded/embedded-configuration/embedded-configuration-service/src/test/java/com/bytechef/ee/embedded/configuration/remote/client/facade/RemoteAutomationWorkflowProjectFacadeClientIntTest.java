/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.client.ConnectedUserProjectRemoteClientIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.AutomationWorkflowProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConnectedUserProjectRemoteClientIntTestConfiguration
class RemoteAutomationWorkflowProjectFacadeClientIntTest {

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ObjectMapper objectMapper;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteAutomationWorkflowProjectFacadeClient remoteAutomationWorkflowProjectFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(
                automationWorkflowProjectFacade, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteAutomationWorkflowProjectFacadeClient = new RemoteAutomationWorkflowProjectFacadeClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();
    }

    @Test
    void testGetPublishedProjectsReturnsThePublishedProjectsWithTheirWorkflowTemplates() {
        String projectName = "Remote published " + UUID.randomUUID();

        long projectId = automationWorkflowProjectFacade.createProject(
            projectName, "Published for connected users", null, List.of(), null);

        String leadsWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            projectId, """
                {"label":"Sync leads","description":"Copies new leads","triggers":[],"tasks":[]}
                """, null);
        String ordersWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            projectId, """
                {"label":"Sync orders","description":"Copies new orders","triggers":[],"tasks":[]}
                """, "connectedUser.metadata.plan == 'gold'");

        automationWorkflowProjectFacade.publishProject(projectId);

        long draftProjectId = automationWorkflowProjectFacade.createProject(
            "Remote draft " + UUID.randomUUID(), "", null, List.of(), null);

        automationWorkflowProjectFacade.createProjectWorkflow(draftProjectId, null, null);

        List<AutomationWorkflowProjectDTO> remoteAutomationWorkflowProjectDTOs =
            remoteAutomationWorkflowProjectFacadeClient.getPublishedProjects();

        assertThat(remoteAutomationWorkflowProjectDTOs).usingRecursiveComparison()
            .isEqualTo(automationWorkflowProjectFacade.getPublishedProjects());

        AutomationWorkflowProjectDTO automationWorkflowProjectDTO = remoteAutomationWorkflowProjectDTOs.stream()
            .filter(remoteAutomationWorkflowProjectDTO -> remoteAutomationWorkflowProjectDTO.id() == projectId)
            .findFirst()
            .orElseThrow();

        assertThat(automationWorkflowProjectDTO.name()).isEqualTo(projectName);
        assertThat(automationWorkflowProjectDTO.lastPublishedVersion()).isEqualTo(1);
        assertThat(automationWorkflowProjectDTO.workflowTemplates())
            .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid, ConnectedUserWorkflowTemplateDTO::label)
            .containsExactlyInAnyOrder(
                tuple(leadsWorkflowUuid, "Sync leads"),
                tuple(ordersWorkflowUuid, "Sync orders"));
    }
}
