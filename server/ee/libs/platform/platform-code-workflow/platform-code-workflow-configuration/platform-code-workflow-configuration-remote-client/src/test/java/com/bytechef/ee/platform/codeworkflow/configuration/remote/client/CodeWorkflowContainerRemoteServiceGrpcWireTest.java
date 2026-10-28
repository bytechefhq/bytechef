/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.client.service.RemoteCodeWorkflowContainerServiceClient;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.CodeWorkflowContainerProtoMapper;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.CodeWorkflowContainerRemoteServiceGrpcServer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.file.storage.domain.FileEntry;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowContainerRemoteServiceGrpcWireTest {

    private static final String CODE_WORKFLOW_CONTAINER_UUID = "3f1b4b52-7a8e-4c55-9f0e-2d2b8c1a6e01";
    private static final String CODE_WORKFLOW_ID = "8c2d0a4e-1b7f-4e9a-a3c6-5f4e2b1d0c9a";

    private final CodeWorkflowContainerService codeWorkflowContainerService = mock(
        CodeWorkflowContainerService.class);

    private ManagedChannel managedChannel;
    private RemoteCodeWorkflowContainerServiceClient remoteCodeWorkflowContainerServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new CodeWorkflowContainerRemoteServiceGrpcServer(
                    codeWorkflowContainerService, new CodeWorkflowContainerProtoMapper()))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteCodeWorkflowContainerServiceClient = new RemoteCodeWorkflowContainerServiceClient(
            new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetCodeWorkflowContainer() {
        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(
            UUID.fromString(CODE_WORKFLOW_CONTAINER_UUID));

        codeWorkflowContainer.addCodeWorkflow(UUID.fromString(CODE_WORKFLOW_ID), "hello");
        codeWorkflowContainer.setExternalVersion("1.0.0");
        codeWorkflowContainer.setId(12L);
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("greetings");
        codeWorkflowContainer.setWorkflows(
            new FileEntry(
                "workflows.js", "js", "text/javascript",
                "file:///opt/bytechef/data/file-storage/code_workflows/workflows.js"));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(CODE_WORKFLOW_CONTAINER_UUID))
            .thenReturn(codeWorkflowContainer);

        CodeWorkflowContainer remoteCodeWorkflowContainer =
            remoteCodeWorkflowContainerServiceClient.getCodeWorkflowContainer(CODE_WORKFLOW_CONTAINER_UUID);

        assertThat(remoteCodeWorkflowContainer.getUuid()).isEqualTo(CODE_WORKFLOW_CONTAINER_UUID);
        assertThat(remoteCodeWorkflowContainer.getId()).isEqualTo(12L);
        assertThat(remoteCodeWorkflowContainer.getExternalVersion()).isEqualTo("1.0.0");
        assertThat(remoteCodeWorkflowContainer.getLanguage()).isEqualTo(Language.JAVASCRIPT);
        assertThat(remoteCodeWorkflowContainer.getName()).isEqualTo("greetings");
        assertThat(remoteCodeWorkflowContainer.getWorkflows()).isEqualTo(codeWorkflowContainer.getWorkflows());
        assertThat(remoteCodeWorkflowContainer.getWorkflowNameIds()).isEqualTo(Map.of("hello", CODE_WORKFLOW_ID));
        assertThat(remoteCodeWorkflowContainer).hasToString(codeWorkflowContainer.toString());
    }
}
