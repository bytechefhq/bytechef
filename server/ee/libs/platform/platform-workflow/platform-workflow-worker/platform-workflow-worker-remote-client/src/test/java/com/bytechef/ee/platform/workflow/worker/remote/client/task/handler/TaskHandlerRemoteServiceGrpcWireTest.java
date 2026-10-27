/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.client.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.TaskHandlerRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskHandlerRemoteServiceGrpcWireTest {

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final AtomicReference<TaskExecution> receivedTaskExecution = new AtomicReference<>();
    private final List<String> requestedTargets = new ArrayList<>();
    private final AtomicInteger workerExecutorRuns = new AtomicInteger();
    private final TaskHandlerRegistry taskHandlerRegistry = type -> "randomHelper/v1/missing".equals(type) ? null
        : taskExecution -> {
            receivedTaskExecution.set(taskExecution);

            if ("randomHelper/v1/broken".equals(type)) {
                throw new IllegalStateException("Handler broke");
            }

            if ("failing/v1/fail".equals(type)) {
                throw new TaskExecutionException("Bad input");
            }

            if ("bigNumber/v1/get".equals(type)) {
                return Map.of("id", 9_007_199_254_740_993L);
            }

            return Map.of("type", type, "value", 0.5);
        };

    private ManagedChannel managedChannel;
    private RemoteTaskHandlerClient remoteTaskHandlerClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TaskHandlerRemoteServiceGrpcServer(
                        taskHandlerRegistry, new TaskExecutionProtoMapper(jsonMapper), runnable -> {
                            workerExecutorRuns.incrementAndGet();

                            Thread thread = new Thread(runnable);

                            thread.start();
                        }),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        when(discoveryClient.getInstances("worker-app")).thenReturn(
            List.of(
                new DefaultServiceInstance(
                    "worker-app:1", "worker-app", "worker-1", 8080, false,
                    Map.of("components", "[{\"name\":\"httpClient\"}]", "grpc-port", "9090")),
                new DefaultServiceInstance(
                    "worker-app:2", "worker-app", "worker-2", 8080, false,
                    Map.of("components", "[{\"name\":\"randomHelper\"}]", "grpc-port", "9090"))));

        GrpcChannels recordingGrpcChannels = new GrpcChannels() {

            @Override
            public Channel getChannel(String serviceId) {
                requestedTargets.add(serviceId);

                return managedChannel;
            }

            @Override
            public Channel getChannel(ServiceInstance serviceInstance) {
                requestedTargets.add(serviceInstance.getInstanceId());

                return managedChannel;
            }
        };

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getRemote()
            .getGrpc()
            .setExecutionDeadline(Duration.ofSeconds(5));

        remoteTaskHandlerClient = new RemoteTaskHandlerClient(
            applicationProperties, recordingGrpcChannels, discoveryClient, jsonMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testHandleRunsOnTheWorkerExecutor() throws TaskExecutionException {
        remoteTaskHandlerClient.handle(
            "randomHelper/v1/randomFloat", taskExecution("randomHelper/v1/randomFloat", Map.of()));

        assertThat(workerExecutorRuns.get()).isEqualTo(1);
    }

    @Test
    void testUnknownTaskTypeFailsNamingTheType() {
        assertThatThrownBy(
            () -> remoteTaskHandlerClient.handle(
                "randomHelper/v1/missing", taskExecution("randomHelper/v1/missing", Map.of())))
                    .isInstanceOf(TaskExecutionException.class)
                    .hasMessageContaining("randomHelper/v1/missing");
    }

    @Test
    void testUnexpectedHandlerFailureIsReportedInsteadOfHanging() {
        long startNanos = System.nanoTime();

        assertThatThrownBy(
            () -> remoteTaskHandlerClient.handle(
                "randomHelper/v1/broken", taskExecution("randomHelper/v1/broken", Map.of())))
                    .isInstanceOf(TaskExecutionException.class)
                    .hasMessageContaining("Handler broke");
        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void testHandleRoutesToTheWorkerOfferingTheComponent() throws TaskExecutionException {
        Object output = remoteTaskHandlerClient.handle(
            "randomHelper/v1/randomFloat", taskExecution("randomHelper/v1/randomFloat", Map.of()));

        assertThat(output).isEqualTo(Map.of("type", "randomHelper/v1/randomFloat", "value", 0.5));
        assertThat(requestedTargets).containsExactly("worker-app:2");
    }

    @Test
    void testHandleSpreadsTasksOverEveryWorkerOfferingTheComponent() throws TaskExecutionException {
        when(discoveryClient.getInstances("worker-app")).thenReturn(
            List.of(
                new DefaultServiceInstance(
                    "worker-app:1", "worker-app", "worker-1", 8080, false,
                    Map.of("components", "[{\"name\":\"randomHelper\"}]", "grpc-port", "9090")),
                new DefaultServiceInstance(
                    "worker-app:2", "worker-app", "worker-2", 8080, false,
                    Map.of("components", "[{\"name\":\"randomHelper\"}]", "grpc-port", "9090"))));

        for (int index = 0; index < 4; index++) {
            remoteTaskHandlerClient.handle(
                "randomHelper/v1/randomFloat", taskExecution("randomHelper/v1/randomFloat", Map.of()));
        }

        assertThat(requestedTargets).containsOnly("worker-app:1", "worker-app:2");
        assertThat(requestedTargets).containsExactlyInAnyOrder(
            "worker-app:1", "worker-app:1", "worker-app:2", "worker-app:2");
    }

    @Test
    void testHandleWithoutAdvertisingWorkerUsesTheServiceChannel() throws TaskExecutionException {
        remoteTaskHandlerClient.handle("var/v1/set", taskExecution("var/v1/set", Map.of()));

        assertThat(requestedTargets).containsExactly("worker-app");
    }

    @Test
    void testHandleKeepsNumberTypes() throws TaskExecutionException {
        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("count", 5);
        parameters.put("id", 9_007_199_254_740_993L);
        parameters.put("ratio", 5.0);

        Object output = remoteTaskHandlerClient.handle(
            "bigNumber/v1/get", taskExecution("bigNumber/v1/get", parameters));

        TaskExecution taskExecution = receivedTaskExecution.get();
        WorkflowTask workflowTask = taskExecution.getWorkflowTask();
        Map<String, ?> receivedParameters = workflowTask.getParameters();

        assertThat(receivedParameters.get("count")).isEqualTo(5);
        assertThat(receivedParameters.get("id")).isEqualTo(9_007_199_254_740_993L);
        assertThat(receivedParameters.get("ratio")).isEqualTo(5.0);
        assertThat(output).isEqualTo(Map.of("id", 9_007_199_254_740_993L));
    }

    @Test
    void testHandleFailureCarriesTheWorkerMessage() {
        assertThatThrownBy(() -> remoteTaskHandlerClient.handle(
            "failing/v1/fail", taskExecution("failing/v1/fail", Map.of())))
                .isInstanceOf(TaskExecutionException.class)
                .hasMessage("Bad input");
    }

    private static TaskExecution taskExecution(String type, Map<String, ?> parameters) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);
        taskExecution.setRetryDelay("1s");
        taskExecution.setWorkflowTask(
            new WorkflowTask(Map.of("name", "task_1", "type", type, "parameters", parameters)));

        return taskExecution;
    }
}
