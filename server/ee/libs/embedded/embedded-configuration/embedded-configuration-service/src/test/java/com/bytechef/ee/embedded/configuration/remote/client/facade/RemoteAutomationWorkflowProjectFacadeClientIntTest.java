/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
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
    private AutomationProjectFacade automationProjectFacade;

    @Autowired
    private ObjectMapper objectMapper;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteAutomationProjectFacadeClient remoteAutomationProjectFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(
                automationProjectFacade, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteAutomationProjectFacadeClient = new RemoteAutomationProjectFacadeClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();
    }

    @Test
    void testGetPublishedProjectsReturnsThePublishedProjectsWithTheirWorkflowTemplates() {
        String projectName = "Remote published " + UUID.randomUUID();

        long projectId = automationProjectFacade.createProject(
            projectName, "Published for connected users", null, List.of(), null);

        String leadsWorkflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, """
                {"label":"Sync leads","description":"Copies new leads","triggers":[],"tasks":[]}
                """, null);
        String ordersWorkflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, """
                {"label":"Sync orders","description":"Copies new orders","triggers":[],"tasks":[]}
                """, "connectedUser.metadata.plan == 'gold'");

        automationProjectFacade.publishProject(projectId);

        long draftProjectId = automationProjectFacade.createProject(
            "Remote draft " + UUID.randomUUID(), "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(draftProjectId, null, null);

        List<AutomationProjectDTO> remoteAutomationWorkflowProjectDTOs =
            remoteAutomationProjectFacadeClient.getPublishedProjects();

        assertThat(remoteAutomationWorkflowProjectDTOs).usingRecursiveComparison()
            .isEqualTo(automationProjectFacade.getPublishedProjects());

        AutomationProjectDTO automationProjectDTO = remoteAutomationWorkflowProjectDTOs.stream()
            .filter(remoteAutomationWorkflowProjectDTO -> remoteAutomationWorkflowProjectDTO.id() == projectId)
            .findFirst()
            .orElseThrow();

        assertThat(automationProjectDTO.name()).isEqualTo(projectName);
        assertThat(automationProjectDTO.lastPublishedVersion()).isEqualTo(1);
        assertThat(automationProjectDTO.workflowTemplates())
            .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid, ConnectedUserWorkflowTemplateDTO::label)
            .containsExactlyInAnyOrder(
                tuple(leadsWorkflowUuid, "Sync leads"),
                tuple(ordersWorkflowUuid, "Sync orders"));
    }
}
