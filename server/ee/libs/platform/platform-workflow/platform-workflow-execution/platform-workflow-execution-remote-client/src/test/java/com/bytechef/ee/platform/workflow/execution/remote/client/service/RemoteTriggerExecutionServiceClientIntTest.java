/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerExecutionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.repository.TriggerExecutionRepository;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
class RemoteTriggerExecutionServiceClientIntTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1");

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TriggerExecutionRepository triggerExecutionRepository;

    @Autowired
    private TriggerExecutionService triggerExecutionService;

    private ManagedChannel managedChannel;
    private RemoteTriggerExecutionServiceClient remoteTriggerExecutionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TriggerExecutionRemoteServiceGrpcServer(
                        triggerExecutionService, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTriggerExecutionServiceClient = new RemoteTriggerExecutionServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        triggerExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheTriggerExecution() {
        long jobId = saveJob();

        TriggerExecution triggerExecution = newTriggerExecution();

        triggerExecution.addJobId(jobId);
        triggerExecution.setBatch(true);
        triggerExecution.setMaxRetries(2);
        triggerExecution.setPriority(4);
        triggerExecution.setState(Map.of("cursor", 9_007_199_254_740_993L));

        TriggerExecution createdTriggerExecution = remoteTriggerExecutionServiceClient.create(triggerExecution);

        assertThat(createdTriggerExecution.getId()).isNotNull();
        assertThat(createdTriggerExecution.isBatch()).isTrue();
        assertThat(createdTriggerExecution.getState()).isEqualTo(Map.of("cursor", 9_007_199_254_740_993L));

        TriggerExecution storedTriggerExecution = triggerExecutionService.getTriggerExecution(
            Objects.requireNonNull(createdTriggerExecution.getId()));

        assertThat(storedTriggerExecution.getWorkflowExecutionId())
            .hasToString(WORKFLOW_EXECUTION_ID.toString());
        assertThat(storedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.CREATED);
        assertThat(storedTriggerExecution.getJobIds()).containsExactly(jobId);
        assertThat(storedTriggerExecution.getMaxRetries()).isEqualTo(2);
        assertThat(storedTriggerExecution.getPriority()).isEqualTo(4);
        assertThat(storedTriggerExecution.getWorkflowTrigger()
            .getName()).isEqualTo("trigger_1");
        assertThat(storedTriggerExecution.getWorkflowTrigger()
            .getType()).isEqualTo("webhook/v1/autoRespondWithHTTP200");
    }

    @Test
    void testCreateWithAnIdFails() {
        TriggerExecution triggerExecution = newTriggerExecution();

        triggerExecution.setId(Long.MAX_VALUE);

        assertThatThrownBy(() -> remoteTriggerExecutionServiceClient.create(triggerExecution))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("'triggerExecution.id' must be null");
        assertThat(triggerExecutionRepository.count()).isZero();
    }

    @Test
    void testGetTriggerExecution() {
        TriggerExecution triggerExecution = triggerExecutionService.create(newTriggerExecution());

        TriggerExecution remoteTriggerExecution = remoteTriggerExecutionServiceClient.getTriggerExecution(
            Objects.requireNonNull(triggerExecution.getId()));

        assertThat(remoteTriggerExecution.getId()).isEqualTo(triggerExecution.getId());
        assertThat(remoteTriggerExecution.getWorkflowExecutionId())
            .hasToString(WORKFLOW_EXECUTION_ID.toString());
        assertThat(remoteTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.CREATED);
        assertThat(remoteTriggerExecution.getWorkflowTrigger()
            .getParameters()).isEqualTo(Map.of("count", 5));
        assertThat(remoteTriggerExecution.getCreatedDate()).isEqualTo(triggerExecution.getCreatedDate());
    }

    @Test
    void testUpdateStoresStatusOutputAndError() {
        Instant startDate = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        TriggerExecution triggerExecution = triggerExecutionService.create(newTriggerExecution());

        TriggerExecution remoteTriggerExecution = remoteTriggerExecutionServiceClient.getTriggerExecution(
            Objects.requireNonNull(triggerExecution.getId()));

        remoteTriggerExecution.setEndDate(startDate.plusSeconds(3));
        remoteTriggerExecution.setError(new ExecutionError("Bad payload", List.of("line 1")));
        remoteTriggerExecution.setExecutionTime(3000);
        remoteTriggerExecution.setOutput(new FileEntry("output.json", "base64:///eyJhIjoxfQ=="));
        remoteTriggerExecution.setStartDate(startDate);
        remoteTriggerExecution.setStatus(TriggerExecution.Status.FAILED);

        TriggerExecution updatedTriggerExecution = remoteTriggerExecutionServiceClient.update(remoteTriggerExecution);

        assertThat(updatedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.FAILED);

        TriggerExecution storedTriggerExecution = triggerExecutionService.getTriggerExecution(
            Objects.requireNonNull(triggerExecution.getId()));

        assertThat(storedTriggerExecution.getStatus()).isEqualTo(TriggerExecution.Status.FAILED);
        assertThat(storedTriggerExecution.getEndDate()).isEqualTo(startDate.plusSeconds(3));
        assertThat(storedTriggerExecution.getExecutionTime()).isEqualTo(3000);
        assertThat(storedTriggerExecution.getError()
            .getMessage()).isEqualTo("Bad payload");
        assertThat(storedTriggerExecution.getOutput()
            .getUrl()).isEqualTo("base64:///eyJhIjoxfQ==");
        assertThat(storedTriggerExecution.getWorkflowExecutionId())
            .hasToString(WORKFLOW_EXECUTION_ID.toString());
    }

    @Test
    void testDeleteJobTriggerExecution() {
        long jobId = saveJob();
        long otherJobId = saveJob();

        TriggerExecution triggerExecution = newTriggerExecution();

        triggerExecution.addJobId(jobId);

        triggerExecution = triggerExecutionService.create(triggerExecution);

        TriggerExecution otherTriggerExecution = newTriggerExecution();

        otherTriggerExecution.addJobId(otherJobId);

        otherTriggerExecution = triggerExecutionService.create(otherTriggerExecution);

        remoteTriggerExecutionServiceClient.deleteJobTriggerExecution(jobId);

        assertThat(triggerExecutionRepository.findById(Objects.requireNonNull(triggerExecution.getId()))).isEmpty();
        assertThat(triggerExecutionService.fetchJobTriggerExecution(jobId)).isEmpty();
        assertThat(triggerExecutionService.fetchJobTriggerExecution(otherJobId))
            .get()
            .extracting(TriggerExecution::getId)
            .isEqualTo(otherTriggerExecution.getId());
    }

    private static TriggerExecution newTriggerExecution() {
        return TriggerExecution.builder()
            .status(TriggerExecution.Status.CREATED)
            .workflowExecutionId(WORKFLOW_EXECUTION_ID)
            .workflowTrigger(
                new WorkflowTrigger(
                    Map.of(
                        "name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "parameters",
                        Map.of("count", 5))))
            .build();
    }

    private long saveJob() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("workflow-1");

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }
}
