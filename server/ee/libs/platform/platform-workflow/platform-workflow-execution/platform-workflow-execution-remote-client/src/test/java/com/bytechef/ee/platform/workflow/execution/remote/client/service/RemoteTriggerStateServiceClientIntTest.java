/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerStateRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformWorkflowExecutionRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.approval.signed-token.secret=Ynl0ZWNoZWYtYXBwcm92YWwtdG9rZW4tdGVzdC1zZWNyZXQh",
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.quartz.auto-startup=false"
    })
@Import(PostgreSQLContainerConfiguration.class)
class RemoteTriggerStateServiceClientIntTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TriggerStateRepository triggerStateRepository;

    @Autowired
    private TriggerStateService triggerStateService;

    private ManagedChannel managedChannel;
    private RemoteTriggerStateServiceClient remoteTriggerStateServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TriggerStateRemoteServiceGrpcServer(
                        triggerStateService, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTriggerStateServiceClient = new RemoteTriggerStateServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        triggerStateRepository.deleteAll();
    }

    @Test
    void testSaveStoresTheValueAndFetchKeepsItsType() {
        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "hook-1", "count", 5), Instant.parse("2026-11-01T10:00:00Z"));

        remoteTriggerStateServiceClient.save(WORKFLOW_EXECUTION_ID, webhookEnableOutput);

        Optional<WebhookEnableOutput> storedValue = triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID);

        assertThat(storedValue).contains(webhookEnableOutput);

        Optional<WebhookEnableOutput> remoteValue = remoteTriggerStateServiceClient.fetchValue(WORKFLOW_EXECUTION_ID);

        assertThat(remoteValue)
            .containsInstanceOf(WebhookEnableOutput.class)
            .contains(webhookEnableOutput);
    }

    @Test
    void testSaveReplacesTheValueAndKeepsNumberTypes() {
        triggerStateService.save(WORKFLOW_EXECUTION_ID, Map.of("cursor", 1));

        remoteTriggerStateServiceClient.save(WORKFLOW_EXECUTION_ID, 9_007_199_254_740_993L);

        assertThat(triggerStateService.<Object>fetchValue(WORKFLOW_EXECUTION_ID)).contains(9_007_199_254_740_993L);
        assertThat(remoteTriggerStateServiceClient.<Object>fetchValue(WORKFLOW_EXECUTION_ID))
            .containsInstanceOf(Long.class)
            .contains(9_007_199_254_740_993L);
        assertThat(triggerStateRepository.count()).isEqualTo(1);
    }

    @Test
    void testFetchMissingValueIsEmpty() {
        assertThat(remoteTriggerStateServiceClient.<Object>fetchValue(WORKFLOW_EXECUTION_ID)).isEmpty();
    }

    @Test
    void testDeleteRemovesTheValue() {
        WorkflowExecutionId otherWorkflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_2");

        triggerStateService.save(WORKFLOW_EXECUTION_ID, Map.of("cursor", 5));
        triggerStateService.save(otherWorkflowExecutionId, Map.of("cursor", 6));

        remoteTriggerStateServiceClient.delete(WORKFLOW_EXECUTION_ID);

        assertThat(triggerStateService.<Object>fetchValue(WORKFLOW_EXECUTION_ID)).isEmpty();
        assertThat(triggerStateService.<Object>fetchValue(otherWorkflowExecutionId)).contains(Map.of("cursor", 6));
    }
}
