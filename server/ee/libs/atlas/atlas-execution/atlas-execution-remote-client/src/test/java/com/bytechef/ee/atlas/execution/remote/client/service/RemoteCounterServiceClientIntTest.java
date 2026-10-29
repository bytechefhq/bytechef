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
import com.bytechef.atlas.execution.repository.jdbc.JdbcCounterRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.CounterRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AtlasExecutionRemoteClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class RemoteCounterServiceClientIntTest {

    @Autowired
    private JdbcCounterRepository counterRepository;

    @Autowired
    private CounterService counterService;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    private long counterId;
    private ManagedChannel managedChannel;
    private RemoteCounterServiceClient remoteCounterServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new CounterRemoteServiceGrpcServer(counterService),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteCounterServiceClient = new RemoteCounterServiceClient(new FixedGrpcChannels(managedChannel));

        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("workflow-1");

        job = jobRepository.save(job);

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(job.getId())
            .workflowTask(new WorkflowTask(Map.of("name", "fork", "type", "forkJoin/v1")))
            .build();

        taskExecution.setTaskNumber(1);

        counterId = Objects.requireNonNull(taskExecutionService.create(taskExecution)
            .getId());
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        counterRepository.deleteAll();
        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void testSetStoresTheValue() {
        remoteCounterServiceClient.set(counterId, 3L);

        assertThat(counterRepository.findValueByIdForUpdate(counterId)).contains(3L);

        remoteCounterServiceClient.set(counterId, 5L);

        assertThat(counterRepository.findValueByIdForUpdate(counterId)).contains(5L);
    }

    @Test
    void testDecrementReturnsAndStoresTheNewValue() {
        counterService.set(counterId, 3L);

        assertThat(remoteCounterServiceClient.decrement(counterId)).isEqualTo(2L);
        assertThat(remoteCounterServiceClient.decrement(counterId)).isEqualTo(1L);
        assertThat(counterRepository.findValueByIdForUpdate(counterId)).contains(1L);
    }

    @Test
    void testDeleteRemovesTheCounter() {
        counterService.set(counterId, 3L);

        remoteCounterServiceClient.delete(counterId);

        assertThat(counterRepository.findValueByIdForUpdate(counterId)).isEmpty();
    }
}
