/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.CONNECTION_ID;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.ONE_TIME_TASK;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.POLLING_TRIGGER;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.SCHEDULE_TRIGGER;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.scheduler.remote.client.config.SchedulerRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.scheduler.remote.grpc.TriggerSchedulerRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.scheduler.job.OneTimeSchedulerJob;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = SchedulerRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteTriggerSchedulerClientIntTest {

    private static final Instant EXECUTE_AT = Instant.parse("2099-10-26T10:15:30Z");

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 11L, "workflow-uuid", "trigger_1");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TriggerScheduler triggerScheduler;

    private ManagedChannel managedChannel;
    private RemoteTriggerSchedulerClient remoteTriggerSchedulerClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new TriggerSchedulerRemoteServiceGrpcServer(triggerScheduler, objectMapper))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteTriggerSchedulerClient = new RemoteTriggerSchedulerClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        managedChannel.shutdownNow();
        server.shutdownNow();

        scheduler.clear();
    }

    @Test
    void testScheduleScheduleTriggerStoresTheCronTriggerWithItsOutput() throws SchedulerException {
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("datetime", "2026-10-26T10:00");
        output.put("hour", 10);
        output.put("dayOfWeek", List.of(1, 2));

        remoteTriggerSchedulerClient.scheduleScheduleTrigger(
            "0 0 10 * * ?", "Europe/Zagreb", output, WORKFLOW_EXECUTION_ID);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey(SCHEDULE_TRIGGER));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getString("workflowExecutionId")).isEqualTo(WORKFLOW_EXECUTION_ID.toString());
        assertThat(objectMapper.readValue(jobDataMap.getString("output"), Map.class)).isEqualTo(output);

        Trigger trigger = scheduler.getTrigger(triggerKey(SCHEDULE_TRIGGER));

        assertThat(trigger).isInstanceOf(CronTrigger.class);

        CronTrigger cronTrigger = (CronTrigger) trigger;

        assertThat(cronTrigger.getCronExpression()).isEqualTo("0 0 10 * * ?");
        assertThat(cronTrigger.getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Zagreb"));
    }

    @Test
    void testCancelScheduleTriggerRemovesTheJob() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleScheduleTrigger(
            "0 0 10 * * ?", "UTC", Map.of("hour", 10), WORKFLOW_EXECUTION_ID);

        assertThat(scheduler.checkExists(jobKey(SCHEDULE_TRIGGER))).isTrue();

        remoteTriggerSchedulerClient.cancelScheduleTrigger(WORKFLOW_EXECUTION_ID.toString());

        assertThat(scheduler.checkExists(jobKey(SCHEDULE_TRIGGER))).isFalse();
    }

    @Test
    void testSchedulePollingTriggerRepeatsEveryConfiguredCheckPeriod() throws SchedulerException {
        remoteTriggerSchedulerClient.schedulePollingTrigger(WORKFLOW_EXECUTION_ID);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey(POLLING_TRIGGER));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getString("workflowExecutionId")).isEqualTo(WORKFLOW_EXECUTION_ID.toString());

        Trigger trigger = scheduler.getTrigger(triggerKey(POLLING_TRIGGER));

        assertThat(trigger).isInstanceOf(SimpleTrigger.class);

        SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;

        assertThat(simpleTrigger.getRepeatInterval()).isEqualTo(
            Duration.ofMinutes(SchedulerRemoteClientIntTestConfiguration.POLLING_CHECK_PERIOD_MINUTES)
                .toMillis());
        assertThat(simpleTrigger.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);
    }

    @Test
    void testCancelPollingTriggerRemovesTheJob() throws SchedulerException {
        remoteTriggerSchedulerClient.schedulePollingTrigger(WORKFLOW_EXECUTION_ID);

        assertThat(scheduler.checkExists(jobKey(POLLING_TRIGGER))).isTrue();

        remoteTriggerSchedulerClient.cancelPollingTrigger(WORKFLOW_EXECUTION_ID.toString());

        assertThat(scheduler.checkExists(jobKey(POLLING_TRIGGER))).isFalse();
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefreshStartsAtTheWebhookExpiration() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleDynamicWebhookTriggerRefresh(
            EXECUTE_AT, "github", 1, WORKFLOW_EXECUTION_ID, 456L);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey(DYNAMIC_WEBHOOK_TRIGGER_REFRESH));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getString("workflowExecutionId")).isEqualTo(WORKFLOW_EXECUTION_ID.toString());
        assertThat(jobDataMap.get(CONNECTION_ID)).isEqualTo(456L);

        Trigger trigger = scheduler.getTrigger(triggerKey(DYNAMIC_WEBHOOK_TRIGGER_REFRESH));

        assertThat(trigger.getStartTime()).isEqualTo(Date.from(EXECUTE_AT));
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefreshWithoutConnectionStoresNoConnectionId() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleDynamicWebhookTriggerRefresh(
            EXECUTE_AT, "github", 1, WORKFLOW_EXECUTION_ID, null);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey(DYNAMIC_WEBHOOK_TRIGGER_REFRESH));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.get(CONNECTION_ID)).isNull();
    }

    @Test
    void testCancelDynamicWebhookTriggerRefreshRemovesTheJob() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleDynamicWebhookTriggerRefresh(
            EXECUTE_AT, "github", 1, WORKFLOW_EXECUTION_ID, 456L);

        assertThat(scheduler.checkExists(jobKey(DYNAMIC_WEBHOOK_TRIGGER_REFRESH))).isTrue();

        remoteTriggerSchedulerClient.cancelDynamicWebhookTriggerRefresh(WORKFLOW_EXECUTION_ID.toString());

        assertThat(scheduler.checkExists(jobKey(DYNAMIC_WEBHOOK_TRIGGER_REFRESH))).isFalse();
    }

    @Test
    void testScheduleOneTimeTaskStoresTheJobAndItsContinueParameters() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleOneTimeTask(EXECUTE_AT, Map.of("approved", true, "count", 3), 15L);

        JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey("15", ONE_TIME_TASK));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getLong("jobId")).isEqualTo(15L);
        assertThat(objectMapper.readValue(jobDataMap.getString(OneTimeSchedulerJob.CONTINUE_PARAMETERS), Map.class))
            .isEqualTo(Map.of("approved", true, "count", 3));

        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("15", ONE_TIME_TASK));

        assertThat(trigger.getStartTime()).isEqualTo(Date.from(EXECUTE_AT));
    }

    @Test
    void testScheduleOneTimeTaskWithoutOutputStoresNoContinueParameters() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleOneTimeTask(EXECUTE_AT, null, 16L);

        JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey("16", ONE_TIME_TASK));

        assertThat(jobDetail).isNotNull();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.containsKey(OneTimeSchedulerJob.CONTINUE_PARAMETERS)).isFalse();
    }

    @Test
    void testCancelOneTimeTaskRemovesOnlyThatJob() throws SchedulerException {
        remoteTriggerSchedulerClient.scheduleOneTimeTask(EXECUTE_AT, null, 15L);
        remoteTriggerSchedulerClient.scheduleOneTimeTask(EXECUTE_AT, null, 16L);

        remoteTriggerSchedulerClient.cancelOneTimeTask(15L);

        assertThat(scheduler.checkExists(JobKey.jobKey("15", ONE_TIME_TASK))).isFalse();
        assertThat(scheduler.checkExists(JobKey.jobKey("16", ONE_TIME_TASK))).isTrue();
    }

    private static JobKey jobKey(String group) {
        return JobKey.jobKey(WORKFLOW_EXECUTION_ID.toString(), group);
    }

    private static TriggerKey triggerKey(String group) {
        return TriggerKey.triggerKey(WORKFLOW_EXECUTION_ID.toString(), group);
    }
}
