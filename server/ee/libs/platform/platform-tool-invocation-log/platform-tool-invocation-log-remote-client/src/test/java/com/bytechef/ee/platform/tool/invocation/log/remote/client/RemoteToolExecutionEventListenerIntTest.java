/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.ToolExecutionEventProtoMapper;
import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.ToolInvocationLogRemoteServiceGrpcServer;
import com.bytechef.ee.platform.tool.invocation.log.repository.ToolInvocationLogRepository;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.tenant.single.SingleTenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RemoteToolExecutionEventListenerIntTest.RemoteToolExecutionEventListenerIntTestConfiguration.class,
    properties = "bytechef.edition=ee")
class RemoteToolExecutionEventListenerIntTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToolInvocationLogRepository toolInvocationLogRepository;

    private ManagedChannel managedChannel;
    private RemoteToolExecutionEventListener remoteToolExecutionEventListener;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ToolInvocationLogRemoteServiceGrpcServer(
                        applicationEventPublisher, new ToolExecutionEventProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteToolExecutionEventListener = new RemoteToolExecutionEventListener(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        toolInvocationLogRepository.deleteAll();
    }

    @Test
    void testEventIsRecordedByTheServingApp() {
        remoteToolExecutionEventListener.onToolExecutionEvent(
            ToolExecutionEvent
                .builder(
                    PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.WORKFLOW,
                    "send_report")
                .mcpServerId(3L)
                .workspaceId(4L)
                .jobId(11L)
                .outcome(ToolExecutionOutcome.ERROR)
                .errorType("IllegalStateException")
                .errorMessage("failed")
                .durationMs(42)
                .build());

        await().atMost(Duration.ofSeconds(10))
            .until(() -> toolInvocationLogRepository.count() == 1);

        ToolInvocationLog toolInvocationLog = toolInvocationLogRepository.findAll()
            .getFirst();

        assertThat(toolInvocationLog.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.MCP_AUTOMATION);
        assertThat(toolInvocationLog.getKind()).isEqualTo(ToolExecutionKind.WORKFLOW);
        assertThat(toolInvocationLog.getToolName()).isEqualTo("send_report");
        assertThat(toolInvocationLog.getMcpServerId()).isEqualTo(3L);
        assertThat(toolInvocationLog.getWorkspaceId()).isEqualTo(4L);
        assertThat(toolInvocationLog.getJobId()).isEqualTo(11L);
        assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(toolInvocationLog.getErrorMessage()).isEqualTo("failed");
        assertThat(toolInvocationLog.getDurationMs()).isEqualTo(42);
    }

    @Test
    void testUnreachableServingAppDoesNotFailTheToolCall() {
        server.shutdownNow();

        assertThatCode(
            () -> remoteToolExecutionEventListener.onToolExecutionEvent(
                ToolExecutionEvent
                    .builder(
                        PlatformType.AUTOMATION, ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT,
                        "list_projects")
                    .build()))
                        .doesNotThrowAnyException();

        assertThat(toolInvocationLogRepository.count()).isZero();
    }

    @ComponentScan(
        basePackages = "com.bytechef.ee.platform.tool.invocation.log",
        excludeFilters = {
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
            @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.bytechef\\.ee\\.platform\\.tool\\.invocation\\.log\\.remote\\..*")
        })
    @Import({
        LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class, SingleTenantService.class
    })
    @EnableAutoConfiguration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Configuration
    static class RemoteToolExecutionEventListenerIntTestConfiguration {

        @Bean
        JobService jobService() {
            return mock(JobService.class);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        @Configuration
        static class RemoteToolExecutionEventListenerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            @Override
            protected List<?> userConverters() {
                return List.of();
            }
        }
    }
}
