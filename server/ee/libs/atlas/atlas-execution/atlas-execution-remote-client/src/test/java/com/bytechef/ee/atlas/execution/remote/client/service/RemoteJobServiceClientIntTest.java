/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.JobRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.LinkedHashMap;
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
    classes = AtlasExecutionRemoteClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class RemoteJobServiceClientIntTest {

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private ManagedChannel managedChannel;
    private RemoteJobServiceClient remoteJobServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new JobRemoteServiceGrpcServer(jobService, new AtlasExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJobServiceClient = new RemoteJobServiceClient(new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void testGetJobKeepsInputsAndNumberTypes() {
        long jobId = saveJob("workflow-1", Job.Status.CREATED);

        Job job = jobService.getJob(jobId);

        Job remoteJob = remoteJobServiceClient.getJob(jobId);

        assertThat(remoteJob.getId()).isEqualTo(jobId);
        assertThat(remoteJob.getLabel()).isEqualTo("Orders");
        assertThat(remoteJob.getStatus()).isEqualTo(Job.Status.CREATED);
        assertThat(remoteJob.getWorkflowId()).isEqualTo("workflow-1");
        assertThat(remoteJob.getPriority()).isEqualTo(3);
        assertThat(remoteJob.getInputs()).isEqualTo(job.getInputs());
        assertThat(remoteJob.getInputs()
            .get("id")).isInstanceOf(Long.class);
        assertThat(remoteJob.getInputs()
            .get("ratio")).isInstanceOf(Double.class);
        assertThat(remoteJob.getMetadata()).isEqualTo(Map.of("source", "test"));
        assertThat(remoteJob.getVersion()).isEqualTo(job.getVersion());
        assertThat(remoteJob.getCreatedDate()).isEqualTo(job.getCreatedDate());
    }

    @Test
    void testGetUnknownJobFails() {
        assertThatThrownBy(() -> remoteJobServiceClient.getJob(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                .getCode())
            .isEqualTo(Status.Code.INTERNAL);
    }

    @Test
    void testFetchJob() {
        long jobId = saveJob("workflow-1", Job.Status.CREATED);

        assertThat(remoteJobServiceClient.fetchJob(jobId))
            .get()
            .extracting(Job::getId, Job::getWorkflowId)
            .containsExactly(jobId, "workflow-1");
        assertThat(remoteJobServiceClient.fetchJob(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testFetchLastWorkflowJob() {
        saveJob("workflow-1", Job.Status.COMPLETED);

        long lastJobId = saveJob("workflow-1", Job.Status.STARTED);

        saveJob("workflow-2", Job.Status.STARTED);

        assertThat(remoteJobServiceClient.fetchLastWorkflowJob("workflow-1"))
            .get()
            .extracting(Job::getId)
            .isEqualTo(lastJobId);
        assertThat(remoteJobServiceClient.fetchLastWorkflowJob("workflow-3")).isEmpty();
    }

    @Test
    void testFetchAndGetTaskExecutionJob() {
        long jobId = saveJob("workflow-1", Job.Status.STARTED);

        long taskExecutionId = saveTaskExecution(jobId);

        assertThat(remoteJobServiceClient.fetchTaskExecutionJob(taskExecutionId))
            .get()
            .extracting(Job::getId)
            .isEqualTo(jobId);
        assertThat(remoteJobServiceClient.getTaskExecutionJob(taskExecutionId)
            .getId()).isEqualTo(jobId);
        assertThat(remoteJobServiceClient.fetchTaskExecutionJob(Long.MAX_VALUE)).isEmpty();
        assertThatThrownBy(() -> remoteJobServiceClient.getTaskExecutionJob(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("Unable to locate job with taskExecutionId: " + Long.MAX_VALUE);
    }

    @Test
    void testSetStatusToStartedAndStopped() {
        long jobId = saveJob("workflow-1", Job.Status.CREATED);

        Job startedJob = remoteJobServiceClient.setStatusToStarted(jobId);

        assertThat(startedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(startedJob.getStartDate()).isNotNull();
        assertThat(jobService.getJob(jobId)
            .getStatus()).isEqualTo(Job.Status.STARTED);

        Job stoppedJob = remoteJobServiceClient.setStatusToStopped(jobId);

        assertThat(stoppedJob.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(stoppedJob.getEndDate()).isNotNull();

        Job storedJob = jobService.getJob(jobId);

        assertThat(storedJob.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(storedJob.getEndDate()).isNotNull();
    }

    @Test
    void testSetStatusToStoppedOfAJobThatIsNotStartedFails() {
        long jobId = saveJob("workflow-1", Job.Status.COMPLETED);

        assertThatThrownBy(() -> remoteJobServiceClient.setStatusToStopped(jobId))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("can not be stopped as it is COMPLETED")
            .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                .getCode())
            .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(jobService.getJob(jobId)
            .getStatus()).isEqualTo(Job.Status.COMPLETED);
    }

    @Test
    void testResumeToStatusStarted() {
        long jobId = saveJob("workflow-1", Job.Status.STOPPED);

        Job resumedJob = remoteJobServiceClient.resumeToStatusStarted(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(resumedJob.getEndDate()).isNull();
        assertThat(jobService.getJob(jobId)
            .getStatus()).isEqualTo(Job.Status.STARTED);
    }

    @Test
    void testUpdate() {
        Job job = remoteJobServiceClient.getJob(saveJob("workflow-1", Job.Status.STARTED));

        job.setCurrentTask(4);
        job.setLabel("Updated orders");
        job.setStatus(Job.Status.COMPLETED);

        Job updatedJob = remoteJobServiceClient.update(job);

        assertThat(updatedJob.getLabel()).isEqualTo("Updated orders");
        assertThat(updatedJob.getVersion()).isEqualTo(job.getVersion() + 1);

        Job storedJob = jobService.getJob(Objects.requireNonNull(job.getId()));

        assertThat(storedJob.getCurrentTask()).isEqualTo(4);
        assertThat(storedJob.getLabel()).isEqualTo("Updated orders");
        assertThat(storedJob.getStatus()).isEqualTo(Job.Status.COMPLETED);
        assertThat(storedJob.getInputs()).isEqualTo(job.getInputs());
    }

    private long saveJob(String workflowId, Job.Status status) {
        Job job = new Job();

        Map<String, Object> inputs = new LinkedHashMap<>();

        inputs.put("count", 5);
        inputs.put("id", 9_007_199_254_740_993L);
        inputs.put("ratio", 5.0);

        job.setInputs(inputs);
        job.setLabel("Orders");
        job.setMetadata(Map.of("source", "test"));
        job.setPriority(3);
        job.setStatus(status);
        job.setWorkflowId(workflowId);

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }

    private long saveTaskExecution(long jobId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .workflowTask(new WorkflowTask(Map.of("name", "setValue", "type", "var/v1/set")))
            .build();

        taskExecution.setTaskNumber(1);

        TaskExecution createdTaskExecution = taskExecutionService.create(taskExecution);

        return Objects.requireNonNull(createdTaskExecution.getId());
    }
}
