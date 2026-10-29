/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TaskStateRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.repository.TaskStateRepository;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class RemoteTaskStateServiceClientIntTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskStateRepository taskStateRepository;

    @Autowired
    private TaskStateService taskStateService;

    private ManagedChannel managedChannel;
    private RemoteTaskStateServiceClient remoteTaskStateServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TaskStateRemoteServiceGrpcServer(
                        taskStateService, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTaskStateServiceClient = new RemoteTaskStateServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        taskStateRepository.deleteAll();
    }

    @Test
    void testSaveStoresTheValueAndFetchKeepsItsType() {
        JobResumeId jobResumeId = JobResumeId.of("public", 7L);
        Suspend suspend = new Suspend(
            Map.of("approvalResume", true, "fields", List.of("name")), Instant.parse("2026-10-28T10:00:00Z"));

        remoteTaskStateServiceClient.save(jobResumeId, suspend);

        assertThat(taskStateService.<Suspend>fetchValue(jobResumeId)).contains(suspend);
        assertThat(remoteTaskStateServiceClient.<Suspend>fetchValue(jobResumeId))
            .containsInstanceOf(Suspend.class)
            .contains(suspend);
    }

    @Test
    void testSaveReplacesTheValue() {
        JobResumeId jobResumeId = JobResumeId.of("public", 7L);

        taskStateService.save(jobResumeId, Map.of("step", 1));

        remoteTaskStateServiceClient.save(jobResumeId, Map.of("step", 2, "id", 9_007_199_254_740_993L));

        assertThat(taskStateService.<Map<String, Object>>fetchValue(jobResumeId))
            .contains(Map.of("step", 2, "id", 9_007_199_254_740_993L));
        assertThat(taskStateRepository.count()).isEqualTo(1);
    }

    @Test
    void testFetchMissingValueIsEmpty() {
        assertThat(remoteTaskStateServiceClient.<Suspend>fetchValue(JobResumeId.of("public", 7L))).isEmpty();
    }

    @Test
    void testDeleteRemovesTheValue() {
        JobResumeId jobResumeId = JobResumeId.of("public", 7L);
        JobResumeId otherJobResumeId = JobResumeId.of("public", 8L);

        taskStateService.save(jobResumeId, Map.of("step", 1));
        taskStateService.save(otherJobResumeId, Map.of("step", 2));

        remoteTaskStateServiceClient.delete(jobResumeId);

        assertThat(taskStateService.<Object>fetchValue(jobResumeId)).isEmpty();
        assertThat(taskStateService.<Object>fetchValue(otherJobResumeId)).contains(Map.of("step", 2));
    }
}
