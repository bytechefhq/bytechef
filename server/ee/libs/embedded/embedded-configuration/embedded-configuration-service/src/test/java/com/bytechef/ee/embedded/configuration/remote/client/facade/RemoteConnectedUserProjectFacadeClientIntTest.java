/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.client.ConnectedUserProjectRemoteClientIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.ConnectedUserProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
class RemoteConnectedUserProjectFacadeClientIntTest {

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
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    private ConnectedUser connectedUser;
    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteConnectedUserProjectFacadeClient remoteConnectedUserProjectFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        connectedUser = connectedUserService.createConnectedUser(
            "remote-project-facade-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new ConnectedUserProjectFacadeRemoteServiceGrpcServer(connectedUserProjectFacade));

        remoteConnectedUserProjectFacadeClient = new RemoteConnectedUserProjectFacadeClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels());
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();
    }

    @Test
    void testCopyWorkflowTemplateCopiesThePublishedTemplateIntoTheConnectedUsersProject() {
        String templateWorkflowUuid = createPublishedTemplate();

        String copiedWorkflowUuid = remoteConnectedUserProjectFacadeClient.copyWorkflowTemplate(
            connectedUser.getExternalId(), templateWorkflowUuid, Environment.PRODUCTION);

        assertThat(copiedWorkflowUuid).isNotBlank()
            .isNotEqualTo(templateWorkflowUuid);

        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows =
            connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUser.getId());

        assertThat(connectedUserProjectWorkflows)
            .singleElement()
            .satisfies(connectedUserProjectWorkflow -> {
                assertThat(connectedUserProjectWorkflow.getCopiedFromWorkflowUuid()).isEqualTo(templateWorkflowUuid);

                ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
                    connectedUserProjectWorkflow.getProjectWorkflowId());

                assertThat(projectWorkflow.getUuidAsString()).isEqualTo(copiedWorkflowUuid);
                assertThat(workflowService.getWorkflow(projectWorkflow.getWorkflowId())
                    .getLabel()).isEqualTo("Sync leads");
            });
    }

    @Test
    void testCopyWorkflowTemplateOfAnUnpublishedTemplateFailsWithTheServiceMessage() {
        String unknownWorkflowUuid = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(() -> remoteConnectedUserProjectFacadeClient.copyWorkflowTemplate(
            connectedUser.getExternalId(), unknownWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    StatusRuntimeException.class,
                    statusRuntimeException -> {
                        Status status = statusRuntimeException.getStatus();

                        assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                        assertThat(status.getDescription()).isEqualTo(
                            "Not a published automation workflow template: " + unknownWorkflowUuid);
                    });

        assertThat(connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUser.getId())).isEmpty();
    }

    private String createPublishedTemplate() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "Remote copy " + UUID.randomUUID(), "", null, List.of(), null);

        String workflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            projectId, TEMPLATE_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(projectId);

        automationWorkflowProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), 1, true, true);

        return workflowUuid;
    }
}
