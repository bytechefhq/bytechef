/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.ContextRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
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
    classes = AtlasExecutionRemoteClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class RemoteContextServiceClientIntTest {

    private static final long STACK_ID = 4_242L;

    @Autowired
    private JdbcContextRepository contextRepository;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskFileStorage taskFileStorage;

    private ManagedChannel managedChannel;
    private RemoteContextServiceClient remoteContextServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ContextRemoteServiceGrpcServer(contextService, new AtlasExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteContextServiceClient = new RemoteContextServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        contextRepository.deleteAll();
    }

    @Test
    void testPushAndPeekWithoutSubStack() {
        FileEntry fileEntry = taskFileStorage.storeContextValue(
            STACK_ID, Context.Classname.JOB, Map.of("id", 9_007_199_254_740_993L, "name", "orders"));

        remoteContextServiceClient.push(STACK_ID, Context.Classname.JOB, fileEntry);

        FileEntry storedFileEntry = contextService.peek(STACK_ID, Context.Classname.JOB);

        assertThat(storedFileEntry.getUrl()).isEqualTo(fileEntry.getUrl());
        assertThat(taskFileStorage.readContextValue(storedFileEntry))
            .isEqualTo(Map.of("id", 9_007_199_254_740_993L, "name", "orders"));

        FileEntry remoteFileEntry = remoteContextServiceClient.peek(STACK_ID, Context.Classname.JOB);

        assertThat(remoteFileEntry)
            .usingRecursiveComparison()
            .isEqualTo(storedFileEntry);
    }

    @Test
    void testPushAndPeekWithSubStack() {
        FileEntry firstFileEntry = taskFileStorage.storeContextValue(
            STACK_ID, 1, Context.Classname.TASK_EXECUTION, Map.of("item", "first"));
        FileEntry secondFileEntry = taskFileStorage.storeContextValue(
            STACK_ID, 2, Context.Classname.TASK_EXECUTION, Map.of("item", "second"));

        remoteContextServiceClient.push(STACK_ID, 1, Context.Classname.TASK_EXECUTION, firstFileEntry);
        remoteContextServiceClient.push(STACK_ID, 2, Context.Classname.TASK_EXECUTION, secondFileEntry);

        assertThat(taskFileStorage.readContextValue(contextService.peek(STACK_ID, 1, Context.Classname.TASK_EXECUTION)))
            .isEqualTo(Map.of("item", "first"));
        assertThat(
            taskFileStorage.readContextValue(
                remoteContextServiceClient.peek(STACK_ID, 2, Context.Classname.TASK_EXECUTION)))
                    .isEqualTo(Map.of("item", "second"));
        assertThatThrownBy(() -> contextService.peek(STACK_ID, Context.Classname.JOB))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPeekWithoutContextFailsWithItsMessage() {
        assertThatThrownBy(() -> remoteContextServiceClient.peek(STACK_ID, Context.Classname.JOB))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("Unable to locate context with stackId: " + STACK_ID)
            .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                .getCode())
            .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThatThrownBy(() -> remoteContextServiceClient.peek(STACK_ID, 3, Context.Classname.TASK_EXECUTION))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("Unable to locate context with stackId: %s, subStackId: 3".formatted(STACK_ID));
    }
}
