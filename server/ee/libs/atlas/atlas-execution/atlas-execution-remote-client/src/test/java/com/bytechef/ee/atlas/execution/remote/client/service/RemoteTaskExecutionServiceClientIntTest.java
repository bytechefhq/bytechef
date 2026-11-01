/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
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
    classes = AtlasExecutionRemoteClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class RemoteTaskExecutionServiceClientIntTest {

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private long jobId;
    private ManagedChannel managedChannel;
    private RemoteTaskExecutionServiceClient remoteTaskExecutionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TaskExecutionRemoteServiceGrpcServer(
                        taskExecutionService, new AtlasExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTaskExecutionServiceClient = new RemoteTaskExecutionServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("workflow-1");

        jobId = Objects.requireNonNull(jobRepository.save(job)
            .getId());
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheTaskExecution() {
        TaskExecution taskExecution = newTaskExecution("setValue", null, 1);

        taskExecution.setMaxRetries(3);
        taskExecution.setMetadata(Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0));
        taskExecution.setPriority(7);
        taskExecution.setRetryDelay("2s");
        taskExecution.setStatus(TaskExecution.Status.CREATED);

        TaskExecution createdTaskExecution = remoteTaskExecutionServiceClient.create(taskExecution);

        assertThat(createdTaskExecution.getId()).isNotNull();

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(createdTaskExecution.getId()));

        assertThat(storedTaskExecution.getJobId()).isEqualTo(jobId);
        assertThat(storedTaskExecution.getMaxRetries()).isEqualTo(3);
        assertThat(storedTaskExecution.getMetadata()).isEqualTo(Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0));
        assertThat(storedTaskExecution.getPriority()).isEqualTo(7);
        assertThat(storedTaskExecution.getRetryDelay()).isEqualTo("2s");
        assertThat(storedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CREATED);
        assertThat(storedTaskExecution.getTaskNumber()).isEqualTo(1);
        assertThat(storedTaskExecution.getWorkflowTask()
            .toMap()).isEqualTo(taskExecution.getWorkflowTask()
                .toMap());
    }

    @Test
    void testCreateWithAnIdFails() {
        TaskExecution taskExecution = newTaskExecution("setValue", null, 1);

        taskExecution.setId(Long.MAX_VALUE);

        assertThatThrownBy(() -> remoteTaskExecutionServiceClient.create(taskExecution))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("'taskExecution.id' must be null");
        assertThat(taskExecutionService.getJobTaskExecutions(jobId)).isEmpty();
    }

    @Test
    void testGetTaskExecutionAndForUpdate() {
        TaskExecution taskExecution = createTaskExecution("setValue", null, 1, TaskExecution.Status.STARTED);

        TaskExecution remoteTaskExecution = remoteTaskExecutionServiceClient.getTaskExecution(id(taskExecution));

        assertThat(remoteTaskExecution.getId()).isEqualTo(taskExecution.getId());
        assertThat(remoteTaskExecution.getJobId()).isEqualTo(jobId);
        assertThat(remoteTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);
        assertThat(remoteTaskExecution.getWorkflowTask()
            .toMap()).isEqualTo(taskExecution.getWorkflowTask()
                .toMap());
        assertThat(remoteTaskExecution.getCreatedDate()).isEqualTo(taskExecution.getCreatedDate());
        assertThat(remoteTaskExecutionServiceClient.getTaskExecutionForUpdate(id(taskExecution))
            .getId()).isEqualTo(taskExecution.getId());
        assertThatThrownBy(() -> remoteTaskExecutionServiceClient.getTaskExecutionForUpdate(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("TaskExecution with id '%s' not found".formatted(Long.MAX_VALUE));
    }

    @Test
    void testGetJobAndParentTaskExecutions() {
        TaskExecution parentTaskExecution = createTaskExecution("loop", null, 1, TaskExecution.Status.STARTED);
        TaskExecution secondChildTaskExecution = createTaskExecution(
            "second", parentTaskExecution.getId(), 2, TaskExecution.Status.CREATED);
        TaskExecution firstChildTaskExecution = createTaskExecution(
            "first", parentTaskExecution.getId(), 1, TaskExecution.Status.CREATED);

        assertThat(remoteTaskExecutionServiceClient.getJobTaskExecutions(jobId))
            .extracting(TaskExecution::getId)
            .containsExactly(
                parentTaskExecution.getId(), secondChildTaskExecution.getId(), firstChildTaskExecution.getId());
        assertThat(remoteTaskExecutionServiceClient.getParentTaskExecutions(id(parentTaskExecution)))
            .extracting(TaskExecution::getId, TaskExecution::getParentId)
            .containsExactly(
                tuple(firstChildTaskExecution.getId(), parentTaskExecution.getId()),
                tuple(secondChildTaskExecution.getId(), parentTaskExecution.getId()));
        assertThat(remoteTaskExecutionServiceClient.getJobTaskExecutions(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetJobUnfinishedTaskExecutions() {
        createTaskExecution("completed", null, 1, TaskExecution.Status.COMPLETED);

        TaskExecution startedTaskExecution = createTaskExecution("started", null, 2, TaskExecution.Status.STARTED);
        TaskExecution createdTaskExecution = createTaskExecution("created", null, 3, TaskExecution.Status.CREATED);

        assertThat(remoteTaskExecutionServiceClient.getJobUnfinishedTaskExecutions(jobId))
            .extracting(TaskExecution::getId)
            .containsExactlyInAnyOrder(startedTaskExecution.getId(), createdTaskExecution.getId());
        assertThat(remoteTaskExecutionServiceClient.getJobUnfinishedTaskExecutions(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testFetchLastJobTaskExecution() {
        createTaskExecution("first", null, 1, TaskExecution.Status.COMPLETED);

        TaskExecution lastTaskExecution = createTaskExecution("second", null, 2, TaskExecution.Status.STARTED);

        assertThat(remoteTaskExecutionServiceClient.fetchLastJobTaskExecution(jobId))
            .get()
            .extracting(TaskExecution::getId)
            .isEqualTo(lastTaskExecution.getId());
        assertThat(remoteTaskExecutionServiceClient.fetchLastJobTaskExecution(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testCancelIfUnfinished() {
        TaskExecution startedTaskExecution = createTaskExecution("started", null, 1, TaskExecution.Status.STARTED);
        TaskExecution completedTaskExecution = createTaskExecution(
            "completed", null, 2, TaskExecution.Status.COMPLETED);

        assertThat(remoteTaskExecutionServiceClient.cancelIfUnfinished(id(startedTaskExecution))).isTrue();
        assertThat(remoteTaskExecutionServiceClient.cancelIfUnfinished(id(completedTaskExecution))).isFalse();

        TaskExecution cancelledTaskExecution = taskExecutionService.getTaskExecution(id(startedTaskExecution));

        assertThat(cancelledTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(cancelledTaskExecution.getEndDate()).isNotNull();
        assertThat(taskExecutionService.getTaskExecution(id(completedTaskExecution))
            .getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
    }

    @Test
    void testCompleteIfNotCancelled() {
        TaskExecution startedTaskExecution = createTaskExecution("started", null, 1, TaskExecution.Status.STARTED);
        TaskExecution cancelledTaskExecution = createTaskExecution(
            "cancelled", null, 2, TaskExecution.Status.CANCELLED);

        assertThat(remoteTaskExecutionServiceClient.completeIfNotCancelled(id(startedTaskExecution))).isTrue();
        assertThat(remoteTaskExecutionServiceClient.completeIfNotCancelled(id(cancelledTaskExecution))).isFalse();

        assertThat(taskExecutionService.getTaskExecution(id(startedTaskExecution))
            .getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
        assertThat(taskExecutionService.getTaskExecution(id(cancelledTaskExecution))
            .getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
    }

    @Test
    void testUpdateStoresStatusOutputAndError() {
        Instant startDate = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        TaskExecution taskExecution = createTaskExecution("setValue", null, 1, TaskExecution.Status.CREATED);

        TaskExecution remoteTaskExecution = remoteTaskExecutionServiceClient.getTaskExecution(id(taskExecution));

        remoteTaskExecution.setEndDate(startDate.plusSeconds(5));
        remoteTaskExecution.setError(new ExecutionError("Bad input", List.of("line 1", "line 2")));
        remoteTaskExecution.setExecutionTime(5000);
        remoteTaskExecution.setOutput(new FileEntry("output.json", "base64:///eyJhIjoxfQ=="));
        remoteTaskExecution.setProgress(100);
        remoteTaskExecution.setStartDate(startDate);
        remoteTaskExecution.setStatus(TaskExecution.Status.FAILED);

        TaskExecution updatedTaskExecution = remoteTaskExecutionServiceClient.update(remoteTaskExecution);

        assertThat(updatedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(id(taskExecution));

        assertThat(storedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(storedTaskExecution.getEndDate()).isEqualTo(startDate.plusSeconds(5));
        assertThat(storedTaskExecution.getExecutionTime()).isEqualTo(5000);
        assertThat(storedTaskExecution.getProgress()).isEqualTo(100);
        assertThat(storedTaskExecution.getError()
            .getMessage()).isEqualTo("Bad input");
        assertThat(storedTaskExecution.getError()
            .getStackTrace()).containsExactly("line 1", "line 2");
        assertThat(storedTaskExecution.getOutput())
            .extracting(FileEntry::getName, FileEntry::getUrl)
            .containsExactly("output.json", "base64:///eyJhIjoxfQ==");
    }

    private TaskExecution createTaskExecution(
        String name, Long parentId, int taskNumber, TaskExecution.Status status) {

        TaskExecution taskExecution = newTaskExecution(name, parentId, taskNumber);

        taskExecution.setStatus(status);

        return taskExecutionService.create(taskExecution);
    }

    private static long id(TaskExecution taskExecution) {
        return Objects.requireNonNull(taskExecution.getId());
    }

    private TaskExecution newTaskExecution(String name, Long parentId, int taskNumber) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .parentId(parentId)
            .workflowTask(
                new WorkflowTask(Map.of("name", name, "type", "var/v1/set", "parameters", Map.of("value", 1))))
            .build();

        taskExecution.setTaskNumber(taskNumber);

        return taskExecution;
    }
}
