/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.ConnectionLifecycleFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.scheduler.util.QuartzConnectionRefreshUtils;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
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
    classes = PlatformWorkflowExecutionRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.approval.signed-token.secret=Ynl0ZWNoZWYtYXBwcm92YWwtdG9rZW4tdGVzdC1zZWNyZXQh",
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.quartz.auto-startup=false"
    })
@Import(PostgreSQLContainerConfiguration.class)
class RemoteConnectionLifecycleFacadeClientIntTest {

    private static final long CONNECTION_ID = 1050L;
    private static final String TENANT_ID = "public";

    @Autowired
    private ConnectionLifecycleFacade connectionLifecycleFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Scheduler scheduler;

    private ManagedChannel managedChannel;
    private RemoteConnectionLifecycleFacadeClient remoteConnectionLifecycleFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionLifecycleFacadeRemoteServiceGrpcServer(
                        connectionLifecycleFacade, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectionLifecycleFacadeClient = new RemoteConnectionLifecycleFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        managedChannel.shutdownNow();
        server.shutdownNow();

        scheduler.clear();
    }

    @Test
    void testScheduleConnectionRefreshSchedulesTheRefreshJob() throws SchedulerException {
        Instant beforeScheduling = Instant.now();

        remoteConnectionLifecycleFacadeClient.scheduleConnectionRefresh(
            CONNECTION_ID, Map.of("access_token", "abc", "expires_in", 3600), TENANT_ID);

        JobKey jobKey = QuartzConnectionRefreshUtils.buildJobKey(CONNECTION_ID, TENANT_ID);

        assertThat(scheduler.checkExists(jobKey)).isTrue();

        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.get("connectionId")).isEqualTo(CONNECTION_ID);
        assertThat(jobDataMap.get("tenantId")).isEqualTo(TENANT_ID);

        Trigger trigger = scheduler.getTrigger(QuartzConnectionRefreshUtils.buildTriggerKey(CONNECTION_ID, TENANT_ID));

        Instant startTime = trigger.getStartTime()
            .toInstant();

        assertThat(startTime).isAfter(beforeScheduling.plus(Duration.ofMinutes(54)))
            .isBefore(beforeScheduling.plus(Duration.ofMinutes(56)));
    }

    @Test
    void testScheduleConnectionRefreshWithoutExpiryDoesNotSchedule() throws SchedulerException {
        remoteConnectionLifecycleFacadeClient.scheduleConnectionRefresh(
            CONNECTION_ID, Map.of("access_token", "abc"), TENANT_ID);

        assertThat(scheduler.checkExists(QuartzConnectionRefreshUtils.buildJobKey(CONNECTION_ID, TENANT_ID)))
            .isFalse();
    }

    @Test
    void testDeleteScheduledConnectionRefreshRemovesTheRefreshJob() throws SchedulerException {
        long otherConnectionId = 1051L;

        connectionLifecycleFacade.scheduleConnectionRefresh(CONNECTION_ID, Map.of("expires_in", 3600), TENANT_ID);
        connectionLifecycleFacade.scheduleConnectionRefresh(otherConnectionId, Map.of("expires_in", 3600), TENANT_ID);

        remoteConnectionLifecycleFacadeClient.deleteScheduledConnectionRefresh(CONNECTION_ID, TENANT_ID);

        assertThat(scheduler.checkExists(QuartzConnectionRefreshUtils.buildJobKey(CONNECTION_ID, TENANT_ID)))
            .isFalse();
        assertThat(scheduler.checkExists(QuartzConnectionRefreshUtils.buildJobKey(otherConnectionId, TENANT_ID)))
            .isTrue();
    }
}
