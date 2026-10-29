/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import static com.bytechef.platform.scheduler.constant.QuartzConnectionRefreshConstant.TOKEN_REFRESH_OFFSET_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.scheduler.remote.client.config.SchedulerRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.scheduler.remote.grpc.ConnectionRefreshSchedulerRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.util.QuartzConnectionRefreshUtils;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = SchedulerRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteConnectionRefreshSchedulerClientIntTest {

    private static final Instant TOKEN_EXPIRATION_TIME = Instant.parse("2099-10-26T10:15:30Z");

    @Autowired
    private ConnectionRefreshScheduler connectionRefreshScheduler;

    @Autowired
    private Scheduler scheduler;

    private ManagedChannel managedChannel;
    private RemoteConnectionRefreshSchedulerClient remoteConnectionRefreshSchedulerClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ConnectionRefreshSchedulerRemoteServiceGrpcServer(connectionRefreshScheduler))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectionRefreshSchedulerClient = new RemoteConnectionRefreshSchedulerClient(
            new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        managedChannel.shutdownNow();
        server.shutdownNow();

        scheduler.clear();
    }

    @Test
    void testScheduleConnectionRefreshStoresTheRefreshJobBeforeTheTokenExpires() throws SchedulerException {
        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, TOKEN_EXPIRATION_TIME, "tenant_a");

        JobKey jobKey = QuartzConnectionRefreshUtils.buildJobKey(7L, "tenant_a");

        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getLong("connectionId")).isEqualTo(7L);
        assertThat(jobDataMap.getString("tenantId")).isEqualTo("tenant_a");

        Trigger trigger = scheduler.getTrigger(QuartzConnectionRefreshUtils.buildTriggerKey(7L, "tenant_a"));

        assertThat(trigger).isNotNull();
        assertThat(trigger.getStartTime()).isEqualTo(
            Date.from(TOKEN_EXPIRATION_TIME.minus(Duration.ofMinutes(TOKEN_REFRESH_OFFSET_MINUTES))));
    }

    @Test
    void testScheduleConnectionRefreshAgainReplacesTheTrigger() throws SchedulerException {
        Instant laterTokenExpirationTime = TOKEN_EXPIRATION_TIME.plus(Duration.ofHours(1));

        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, TOKEN_EXPIRATION_TIME, "tenant_a");
        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, laterTokenExpirationTime, "tenant_a");

        Trigger trigger = scheduler.getTrigger(QuartzConnectionRefreshUtils.buildTriggerKey(7L, "tenant_a"));

        assertThat(trigger.getStartTime()).isEqualTo(
            Date.from(laterTokenExpirationTime.minus(Duration.ofMinutes(TOKEN_REFRESH_OFFSET_MINUTES))));
    }

    @Test
    void testCancelConnectionRefreshRemovesOnlyThatTenantsJob() throws SchedulerException {
        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, TOKEN_EXPIRATION_TIME, "tenant_a");
        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, TOKEN_EXPIRATION_TIME, "tenant_b");

        remoteConnectionRefreshSchedulerClient.cancelConnectionRefresh(7L, "tenant_a");

        assertThat(scheduler.checkExists(QuartzConnectionRefreshUtils.buildJobKey(7L, "tenant_a"))).isFalse();
        assertThat(scheduler.checkExists(QuartzConnectionRefreshUtils.buildJobKey(7L, "tenant_b"))).isTrue();
    }
}
