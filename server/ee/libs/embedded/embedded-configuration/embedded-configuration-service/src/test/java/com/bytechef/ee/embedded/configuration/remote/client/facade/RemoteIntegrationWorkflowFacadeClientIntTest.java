/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.remote.client.ConnectedUserProjectRemoteClientIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationWorkflowFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
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
class RemoteIntegrationWorkflowFacadeClientIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowService workflowService;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationWorkflowFacadeClient remoteIntegrationWorkflowFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationWorkflowFacadeRemoteServiceGrpcServer(
                integrationWorkflowFacade, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationWorkflowFacadeClient = new RemoteIntegrationWorkflowFacadeClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationWorkflowsReturnsEveryIntegrationWorkflowWithItsStoredWorkflow() {
        long airtableIntegrationId = createIntegration("airtable");
        long slackIntegrationId = createIntegration("slack");

        IntegrationWorkflow airtableIntegrationWorkflow = addWorkflow(airtableIntegrationId, "Sync records");
        IntegrationWorkflow slackIntegrationWorkflow = addWorkflow(slackIntegrationId, "Post digest");

        integrationWorkflowService.addWorkflow(slackIntegrationId, 1, "missing-workflow");

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs = remoteIntegrationWorkflowFacadeClient
            .getIntegrationWorkflows();

        assertThat(integrationWorkflowDTOs).usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*createdBy", ".*createdDate", ".*lastModifiedBy")
            .isEqualTo(integrationWorkflowFacade.getIntegrationWorkflows());
        assertThat(integrationWorkflowDTOs)
            .extracting(
                IntegrationWorkflowDTO::getIntegrationWorkflowId, IntegrationWorkflowDTO::getWorkflowUuid,
                IntegrationWorkflowDTO::getLabel)
            .containsExactlyInAnyOrder(
                tuple(
                    airtableIntegrationWorkflow.getId(), airtableIntegrationWorkflow.getUuidAsString(),
                    "Sync records"),
                tuple(slackIntegrationWorkflow.getId(), slackIntegrationWorkflow.getUuidAsString(), "Post digest"));
    }

    private IntegrationWorkflow addWorkflow(long integrationId, String label) {
        Workflow workflow = workflowService.create(
            "{\"label\":\"" + label + "\",\"description\":\"" + label + " description\",\"triggers\":[],\"tasks\":[]}",
            Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return integrationWorkflowService.addWorkflow(integrationId, 1, workflow.getId());
    }

    private long createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName + "-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
