/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.client.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.config.TaskHandlerRegistryConfiguration;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.TaskHandlerRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentIntTest
@Import(TaskHandlerRegistryConfiguration.class)
@TestPropertySource(
    properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration")
class RemoteTaskHandlerClientIntTest {

    private static final String WORKER_APP = "worker-app";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskHandlerRegistry taskHandlerRegistry;

    private ManagedChannel managedChannel;
    private final List<String> requestedTargets = new CopyOnWriteArrayList<>();
    private Server server;
    private SimpleDiscoveryProperties simpleDiscoveryProperties;
    private final AtomicInteger workerExecutorRuns = new AtomicInteger();

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor("grpc-worker-");

        TaskExecutor workerExecutor = runnable -> {
            workerExecutorRuns.incrementAndGet();

            simpleAsyncTaskExecutor.execute(runnable);
        };

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TaskHandlerRemoteServiceGrpcServer(
                        taskHandlerRegistry, new TaskExecutionProtoMapper(objectMapper), workerExecutor),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        simpleDiscoveryProperties = new SimpleDiscoveryProperties();

        simpleDiscoveryProperties.setInstances(
            Map.of(
                WORKER_APP,
                List.of(
                    serviceInstance("worker-app:1", "[{\"name\":\"var\"}]"),
                    serviceInstance("worker-app:2", "[{\"name\":\"mathHelper\"}]"))));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testHandleRunsTheComponentActionOnTheWorker() throws TaskExecutionException {
        Object output = remoteTaskHandlerClient().handle(
            "mathHelper/v1/addition",
            taskExecution("mathHelper/v1/addition", Map.of("firstNumber", 2.5, "secondNumber", 4)));

        assertThat(output).isEqualTo(6.5);
        assertThat(workerExecutorRuns.get()).isEqualTo(1);
    }

    @Test
    void testHandleKeepsNumberTypes() throws TaskExecutionException {
        Map<String, Object> value = new LinkedHashMap<>();

        value.put("count", 5);
        value.put("id", 9_007_199_254_740_993L);
        value.put("ratio", 5.0);
        value.put("nested", Map.of("items", List.of(1, "two", true)));

        Object output = remoteTaskHandlerClient().handle(
            "var/v1/set", taskExecution("var/v1/set", Map.of("type", "OBJECT", "value", value)));

        assertThat(output).isEqualTo(value);
        assertThat(((Map<?, ?>) output).get("count")).isInstanceOf(Integer.class);
        assertThat(((Map<?, ?>) output).get("id")).isEqualTo(9_007_199_254_740_993L);
        assertThat(((Map<?, ?>) output).get("ratio")).isEqualTo(5.0);
    }

    @Test
    void testHandleFailureCarriesTheWorkerMessage() {
        assertThatThrownBy(
            () -> remoteTaskHandlerClient().handle(
                "mathHelper/v1/division",
                taskExecution("mathHelper/v1/division", Map.of("firstNumber", 1, "secondNumber", 0))))
                    .isInstanceOf(TaskExecutionException.class)
                    .hasMessageContaining("Division by zero is not allowed.");
    }

    @Test
    void testUnknownComponentFailsNamingTheComponent() {
        assertThatThrownBy(
            () -> remoteTaskHandlerClient().handle(
                "missingComponent/v1/missingAction",
                taskExecution("missingComponent/v1/missingAction", Map.of())))
                    .isInstanceOf(TaskExecutionException.class)
                    .hasMessageContaining("missingComponent");
    }

    @Test
    void testHandleRoutesToTheWorkerOfferingTheComponent() throws TaskExecutionException {
        remoteTaskHandlerClient().handle(
            "mathHelper/v1/addition",
            taskExecution("mathHelper/v1/addition", Map.of("firstNumber", 1, "secondNumber", 1)));

        assertThat(requestedTargets).containsExactly("worker-app:2");
    }

    @Test
    void testHandleSpreadsTasksOverEveryWorkerOfferingTheComponent() throws TaskExecutionException {
        simpleDiscoveryProperties.setInstances(
            Map.of(
                WORKER_APP,
                List.of(
                    serviceInstance("worker-app:1", "[{\"name\":\"mathHelper\"}]"),
                    serviceInstance("worker-app:2", "[{\"name\":\"mathHelper\"}]"))));

        RemoteTaskHandlerClient remoteTaskHandlerClient = remoteTaskHandlerClient();

        for (int index = 0; index < 4; index++) {
            remoteTaskHandlerClient.handle(
                "mathHelper/v1/addition",
                taskExecution("mathHelper/v1/addition", Map.of("firstNumber", index, "secondNumber", 1)));
        }

        assertThat(requestedTargets).containsExactlyInAnyOrder(
            "worker-app:1", "worker-app:1", "worker-app:2", "worker-app:2");
    }

    @Test
    void testHandleWithoutAdvertisingWorkerUsesTheServiceChannel() throws TaskExecutionException {
        simpleDiscoveryProperties.setInstances(
            Map.of(WORKER_APP, List.of(serviceInstance("worker-app:1", "[{\"name\":\"httpClient\"}]"))));

        Object output = remoteTaskHandlerClient().handle(
            "var/v1/set", taskExecution("var/v1/set", Map.of("type", "STRING", "value", "hello")));

        assertThat(output).isEqualTo("hello");
        assertThat(requestedTargets).containsExactly(WORKER_APP);
    }

    private RemoteTaskHandlerClient remoteTaskHandlerClient() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        grpc.setExecutionDeadline(Duration.ofSeconds(30));

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

        return new RemoteTaskHandlerClient(
            applicationProperties, recordingGrpcChannels, new SimpleDiscoveryClient(simpleDiscoveryProperties),
            objectMapper);
    }

    private static InstanceProperties serviceInstance(String instanceId, String components) {
        InstanceProperties instanceProperties = new InstanceProperties();

        instanceProperties.setHost(instanceId.replace(":", "-"));
        instanceProperties.setInstanceId(instanceId);
        instanceProperties.setMetadata(Map.of("components", components, "grpc-port", "9090"));
        instanceProperties.setPort(8080);
        instanceProperties.setServiceId(WORKER_APP);

        return instanceProperties;
    }

    private static TaskExecution taskExecution(String type, Map<String, ?> parameters) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(1L);
        taskExecution.setJobId(2L);
        taskExecution.setWorkflowTask(
            new WorkflowTask(Map.of("name", "task_1", "type", type, "parameters", parameters)));

        return taskExecution;
    }
}
