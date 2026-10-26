/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.scheduler.remote.grpc.ConnectionRefreshSchedulerRemoteServiceGrpcServer;
import com.bytechef.ee.platform.scheduler.remote.grpc.TriggerSchedulerRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SchedulerRemoteServiceGrpcWireTest {

    private static final Instant INSTANT = Instant.parse("2026-10-26T10:15:30.123Z");

    private final ConnectionRefreshScheduler connectionRefreshScheduler = mock(ConnectionRefreshScheduler.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);

    private ManagedChannel managedChannel;
    private RemoteConnectionRefreshSchedulerClient remoteConnectionRefreshSchedulerClient;
    private RemoteTriggerSchedulerClient remoteTriggerSchedulerClient;
    private Server server;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new TriggerSchedulerRemoteServiceGrpcServer(triggerScheduler))
            .addService(new ConnectionRefreshSchedulerRemoteServiceGrpcServer(connectionRefreshScheduler))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteConnectionRefreshSchedulerClient = new RemoteConnectionRefreshSchedulerClient(fixedGrpcChannels);
        remoteTriggerSchedulerClient = new RemoteTriggerSchedulerClient(fixedGrpcChannels);
        workflowExecutionId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 11L, "workflow-uuid", "trigger_1");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testCancelCalls() {
        remoteTriggerSchedulerClient.cancelDynamicWebhookTriggerRefresh("execution-1");
        remoteTriggerSchedulerClient.cancelOneTimeTask(15L);
        remoteTriggerSchedulerClient.cancelPollingTrigger("execution-2");
        remoteTriggerSchedulerClient.cancelScheduleTrigger("execution-3");

        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh("execution-1");
        verify(triggerScheduler).cancelOneTimeTask(15L);
        verify(triggerScheduler).cancelPollingTrigger("execution-2");
        verify(triggerScheduler).cancelScheduleTrigger("execution-3");
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefresh() {
        remoteTriggerSchedulerClient.scheduleDynamicWebhookTriggerRefresh(
            INSTANT, "github", 1, workflowExecutionId, null);

        verify(triggerScheduler).scheduleDynamicWebhookTriggerRefresh(
            eq(INSTANT), eq("github"), eq(1), argThat(this::isWorkflowExecutionId), eq(null));
    }

    @Test
    void testSchedulePollingTrigger() {
        remoteTriggerSchedulerClient.schedulePollingTrigger(workflowExecutionId);

        verify(triggerScheduler).schedulePollingTrigger(argThat(this::isWorkflowExecutionId));
    }

    @Test
    void testScheduleScheduleTriggerPassesOutputUnchanged() {
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("datetime", "2026-10-26T10:00");
        output.put("hour", 10);
        output.put("dayOfWeek", List.of(1, 2));

        remoteTriggerSchedulerClient.scheduleScheduleTrigger("0 * * * * ?", "Europe/Zagreb", output,
            workflowExecutionId);

        verify(triggerScheduler).scheduleScheduleTrigger(
            eq("0 * * * * ?"), eq("Europe/Zagreb"), eq(output), argThat(this::isWorkflowExecutionId));
    }

    @Test
    void testScheduleOneTimeTask() {
        remoteTriggerSchedulerClient.scheduleOneTimeTask(INSTANT, Map.of("approved", true), 15L);

        verify(triggerScheduler).scheduleOneTimeTask(INSTANT, Map.of("approved", true), 15L);
    }

    @Test
    void testConnectionRefreshCalls() {
        remoteConnectionRefreshSchedulerClient.scheduleConnectionRefresh(7L, INSTANT, "public");
        remoteConnectionRefreshSchedulerClient.cancelConnectionRefresh(7L, "public");

        verify(connectionRefreshScheduler).scheduleConnectionRefresh(7L, INSTANT, "public");
        verify(connectionRefreshScheduler).cancelConnectionRefresh(7L, "public");
    }

    private boolean isWorkflowExecutionId(WorkflowExecutionId actual) {
        return actual != null && workflowExecutionId.toString()
            .equals(actual.toString());
    }
}
