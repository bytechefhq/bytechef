/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.remote.grpc.ExternalInvocationEventProtoMapper;
import com.bytechef.ee.platform.external.invocation.log.remote.grpc.ExternalInvocationLogRemoteServiceGrpcServer;
import com.bytechef.ee.platform.external.invocation.log.repository.ExternalInvocationLogRepository;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
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
    classes = RemoteExternalInvocationEventListenerIntTest.RemoteExternalInvocationEventListenerIntTestConfiguration.class,
    properties = "bytechef.edition=ee")
class RemoteExternalInvocationEventListenerIntTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExternalInvocationLogRepository externalInvocationLogRepository;

    private ManagedChannel managedChannel;
    private RemoteExternalInvocationEventListener remoteExternalInvocationEventListener;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ExternalInvocationLogRemoteServiceGrpcServer(
                        applicationEventPublisher, new ExternalInvocationEventProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteExternalInvocationEventListener = new RemoteExternalInvocationEventListener(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        externalInvocationLogRepository.deleteAll();
    }

    @Test
    void testEventIsRecordedByTheServingApp() {
        remoteExternalInvocationEventListener.onExternalInvocationEvent(
            ExternalInvocationEvent
                .builder(
                    PlatformType.AUTOMATION, ExternalInvocationSource.MCP_SERVER, ExternalInvocationKind.WORKFLOW,
                    "send_report")
                .mcpServerId(3L)
                .workspaceId(4L)
                .jobId(11L)
                .outcome(ExternalInvocationOutcome.ERROR)
                .errorType("IllegalStateException")
                .errorMessage("failed")
                .durationMs(42)
                .build());

        await().atMost(Duration.ofSeconds(10))
            .until(() -> externalInvocationLogRepository.count() == 1);

        ExternalInvocationLog externalInvocationLog = externalInvocationLogRepository.findAll()
            .getFirst();

        assertThat(externalInvocationLog.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(externalInvocationLog.getSource()).isEqualTo(ExternalInvocationSource.MCP_SERVER);
        assertThat(externalInvocationLog.getKind()).isEqualTo(ExternalInvocationKind.WORKFLOW);
        assertThat(externalInvocationLog.getToolName()).isEqualTo("send_report");
        assertThat(externalInvocationLog.getMcpServerId()).isEqualTo(3L);
        assertThat(externalInvocationLog.getWorkspaceId()).isEqualTo(4L);
        assertThat(externalInvocationLog.getJobId()).isEqualTo(11L);
        assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
        assertThat(externalInvocationLog.getErrorMessage()).isEqualTo("failed");
        assertThat(externalInvocationLog.getDurationMs()).isEqualTo(42);
    }

    @Test
    void testUnreachableServingAppDoesNotFailTheToolCall() {
        server.shutdownNow();

        assertThatCode(
            () -> remoteExternalInvocationEventListener.onExternalInvocationEvent(
                ExternalInvocationEvent
                    .builder(
                        PlatformType.AUTOMATION, ExternalInvocationSource.MCP_SERVER, ExternalInvocationKind.COMPONENT,
                        "list_projects")
                    .build()))
                        .doesNotThrowAnyException();

        assertThat(externalInvocationLogRepository.count()).isZero();
    }

    @ComponentScan(
        basePackages = "com.bytechef.ee.platform.external.invocation.log",
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
    static class RemoteExternalInvocationEventListenerIntTestConfiguration {

        @Bean
        JobService jobService() {
            return mock(JobService.class);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        @Configuration
        static class RemoteExternalInvocationEventListenerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            @Override
            protected List<?> userConverters() {
                return List.of();
            }
        }
    }
}
