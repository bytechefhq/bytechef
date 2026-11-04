/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskExecutionLockedUpdatesImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionLockedUpdatesRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
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
class RemoteTaskExecutionLockedUpdatesClientIntTest {

    private static final int CONCURRENT_WRITERS = 8;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    private ManagedChannel managedChannel;
    private RemoteTaskExecutionLockedUpdatesClient remoteTaskExecutionLockedUpdatesClient;
    private Server server;
    private long taskExecutionId;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .executor(Executors.newFixedThreadPool(CONCURRENT_WRITERS))
            .addService(
                ServerInterceptors.intercept(
                    new TaskExecutionLockedUpdatesRemoteServiceGrpcServer(
                        new TaskExecutionLockedUpdatesImpl(taskExecutionService, taskFileStorage),
                        new AtlasExecutionProtoMapper(objectMapper), objectMapper),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .build();

        remoteTaskExecutionLockedUpdatesClient = new RemoteTaskExecutionLockedUpdatesClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("workflow-1");

        job = jobRepository.save(job);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(Objects.requireNonNull(job.getId()))
            .workflowTask(new WorkflowTask(Map.of("name", "forkJoin_1", "type", "forkJoin/v1")))
            .build();

        taskExecution.setTaskNumber(1);

        taskExecutionId = Objects.requireNonNull(taskExecutionService.create(taskExecution)
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
    void testPutOutputEntryAddsEachEntryToTheOutput() {
        remoteTaskExecutionLockedUpdatesClient.putOutputEntry(taskExecutionId, "branch_0", Map.of("name", "first"));

        TaskExecution taskExecution = remoteTaskExecutionLockedUpdatesClient.putOutputEntry(
            taskExecutionId, "branch_1", "second");

        Map<String, Object> expectedOutput = Map.of("branch_0", Map.of("name", "first"), "branch_1", "second");

        assertThat(readOutput(taskExecution)).isEqualTo(expectedOutput);
        assertThat(readOutput(taskExecutionService.getTaskExecution(taskExecutionId))).isEqualTo(expectedOutput);
    }

    @Test
    void testPutOutputEntryStoresANullValue() {
        TaskExecution taskExecution = remoteTaskExecutionLockedUpdatesClient.putOutputEntry(
            taskExecutionId, "branch_0", null);

        assertThat(readOutput(taskExecution)).containsEntry("branch_0", null)
            .hasSize(1);
    }

    @Test
    void testConcurrentPutOutputEntryKeepsEveryEntry() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_WRITERS)) {
            for (int index = 0; index < CONCURRENT_WRITERS; index++) {
                int branch = index;

                futures.add(executorService.submit(() -> {
                    startLatch.await();

                    return remoteTaskExecutionLockedUpdatesClient.putOutputEntry(
                        taskExecutionId, "branch_" + branch, branch);
                }));
            }

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(readOutput(taskExecutionService.getTaskExecution(taskExecutionId)))
            .containsOnlyKeys(
                IntStream.range(0, CONCURRENT_WRITERS)
                    .mapToObj(branch -> "branch_" + branch)
                    .toList());
    }

    @Test
    void testSetEndDateSetsTheEndDate() {
        Instant endDate = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        TaskExecution taskExecution = remoteTaskExecutionLockedUpdatesClient.setEndDate(taskExecutionId, endDate);

        assertThat(taskExecution.getEndDate()).isEqualTo(endDate);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        assertThat(storedTaskExecution.getEndDate()).isEqualTo(endDate);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readOutput(TaskExecution taskExecution) {
        FileEntry output = Objects.requireNonNull(taskExecution.getOutput());

        return (Map<String, Object>) taskFileStorage.readTaskExecutionOutput(output);
    }
}
