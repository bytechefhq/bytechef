/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.TaskDispatcherDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc.TaskDispatcherProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcherDefinitionFactory;
import com.bytechef.task.dispatcher.loop.LoopTaskDispatcherDefinitionFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskDispatcherDefinitionRemoteServiceGrpcWireTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService =
        mock(TaskDispatcherDefinitionService.class);

    private ManagedChannel managedChannel;
    private RemoteTaskDispatcherDefinitionServiceClient remoteTaskDispatcherDefinitionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new TaskDispatcherDefinitionRemoteServiceGrpcServer(
                    taskDispatcherDefinitionService, new TaskDispatcherProtoMapper(jsonMapper)))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTaskDispatcherDefinitionServiceClient = new RemoteTaskDispatcherDefinitionServiceClient(
            new FixedGrpcChannels(managedChannel), jsonMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetTaskDispatcherDefinitionWithAndWithoutVersion() {
        TaskDispatcherDefinition loopTaskDispatcherDefinition = loop();

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", null))
            .thenReturn(loopTaskDispatcherDefinition);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(loopTaskDispatcherDefinition);

        assertThat(remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition("loop", null))
            .usingRecursiveComparison()
            .isEqualTo(loopTaskDispatcherDefinition);
        assertThat(remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition("loop", 1))
            .usingRecursiveComparison()
            .isEqualTo(loopTaskDispatcherDefinition);
    }

    @Test
    void testGetTaskDispatcherDefinitionsAndVersions() {
        List<TaskDispatcherDefinition> taskDispatcherDefinitions = List.of(condition(), loop());

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinitions()).thenReturn(taskDispatcherDefinitions);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions("loop")).thenReturn(List.of(loop()));

        assertThat(remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitions())
            .usingRecursiveComparison()
            .isEqualTo(taskDispatcherDefinitions);
        assertThat(remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitionVersions("loop"))
            .extracting(TaskDispatcherDefinition::getName)
            .containsExactly("loop");
    }

    @Test
    void testExecuteOutput() {
        OutputResponse outputResponse = new OutputResponse(
            loop().getProperties()
                .getFirst(),
            Map.of("item", "x", "index", 1), null);
        Map<String, Object> inputParameters = Map.of("items", List.of(1, 2));

        when(taskDispatcherDefinitionService.executeOutput("loop", 1, inputParameters)).thenReturn(outputResponse);

        assertThat(remoteTaskDispatcherDefinitionServiceClient.executeOutput("loop", 1, inputParameters))
            .usingRecursiveComparison()
            .isEqualTo(outputResponse);
    }

    private static TaskDispatcherDefinition condition() {
        return new TaskDispatcherDefinition(new ConditionTaskDispatcherDefinitionFactory().getDefinition());
    }

    private static TaskDispatcherDefinition loop() {
        return new TaskDispatcherDefinition(new LoopTaskDispatcherDefinitionFactory().getDefinition());
    }
}
