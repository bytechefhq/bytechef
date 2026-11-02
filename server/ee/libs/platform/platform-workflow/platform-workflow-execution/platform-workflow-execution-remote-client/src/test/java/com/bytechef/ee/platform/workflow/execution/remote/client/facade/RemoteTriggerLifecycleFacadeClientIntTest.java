/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.schedule.ScheduleComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerLifecycleFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.scheduler.QuartzTriggerScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacadeImpl;
import com.bytechef.platform.workflow.execution.facade.TriggerStateFacade;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * Enables and disables triggers through the real trigger lifecycle facade, the real component definitions and an
 * in-memory Quartz scheduler. Trigger state is only touched by dynamic webhook triggers, which need an external system,
 * so it is a mock that must stay untouched.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentIntTest
@Import({
    RemoteTriggerLifecycleFacadeClientIntTest.TriggerLifecycleIntTestConfiguration.class,
    ScheduleComponentHandler.class
})
@TestPropertySource(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration,"
            + "com.bytechef.scheduler.lock.config.SchedulerLockAutoConfiguration",
        "spring.quartz.auto-startup=false"
    })
class RemoteTriggerLifecycleFacadeClientIntTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockitoBean
    private TriggerStateFacade triggerStateFacade;

    private ManagedChannel managedChannel;
    private RemoteTriggerLifecycleFacadeClient remoteTriggerLifecycleFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new TriggerLifecycleFacadeRemoteServiceGrpcServer(
                        triggerLifecycleFacade, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        grpc.setExecutionDeadline(Duration.ofSeconds(30));

        remoteTriggerLifecycleFacadeClient = new RemoteTriggerLifecycleFacadeClient(
            applicationProperties, new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        managedChannel.shutdownNow();
        server.shutdownNow();

        scheduler.clear();
    }

    @Test
    void testEnableAndDisableListenerTriggerSchedulesAndCancelsTheSchedule() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(WORKFLOW_EXECUTION_ID.toString(), "ScheduleTrigger");

        remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("schedule", 1, "cron"),
            Map.of("expression", "30 9 ? * MON-FRI", "timezone", "Europe/Zagreb"), null, null, 0L);

        assertThat(scheduler.checkExists(jobKey)).isTrue();

        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getString("workflowExecutionId")).isEqualTo(WORKFLOW_EXECUTION_ID.toString());
        assertThat(JsonUtils.readMap(jobDataMap.getString("output")))
            .isEqualTo(Map.of("expression", "30 9 ? * MON-FRI", "timezone", "Europe/Zagreb"));

        remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("schedule", 1, "cron"),
            Map.of("expression", "30 9 ? * MON-FRI", "timezone", "Europe/Zagreb"), null);

        assertThat(scheduler.checkExists(jobKey)).isFalse();
        verifyNoInteractions(triggerStateFacade);
    }

    @Test
    void testEnableAndDisablePollingTriggerSchedulesAndCancelsThePolling() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(WORKFLOW_EXECUTION_ID.toString(), "PollingTrigger");

        remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("beamer", 1, "newPost"), Map.of(), null, null,
            0L);

        assertThat(scheduler.checkExists(jobKey)).isTrue();
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        assertThat(jobDataMap.getString("workflowExecutionId")).isEqualTo(WORKFLOW_EXECUTION_ID.toString());

        remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("beamer", 1, "newPost"), Map.of(), null);

        assertThat(scheduler.checkExists(jobKey)).isFalse();
        verifyNoInteractions(triggerStateFacade);
    }

    @Test
    void testEnableStaticWebhookTriggerKeepsNoState() throws SchedulerException {
        remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("webhook", 1, "autoRespondWithHTTP200"),
            Map.of("count", 5), null, "https://example.com/webhooks/abc", 7L);

        assertThat(scheduler.getJobGroupNames()).isEmpty();
        verifyNoInteractions(triggerStateFacade);
    }

    @Test
    void testEnableUnknownTriggerFails() {
        assertThatThrownBy(
            () -> remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
                "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("missingComponent", 1, "missingTrigger"),
                Map.of(), null, null, 0L))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("missingComponent");
    }

    @Test
    void testDisableUnknownTriggerIsIgnored() {
        remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", WORKFLOW_EXECUTION_ID, new WorkflowNodeType("missingComponent", 1, "missingTrigger"),
            Map.of(), null);

        verifyNoInteractions(triggerStateFacade);
    }

    @TestConfiguration
    static class TriggerLifecycleIntTestConfiguration {

        @Bean
        TriggerLifecycleFacade triggerLifecycleFacade(
            TriggerScheduler triggerScheduler, TriggerDefinitionFacade triggerDefinitionFacade,
            TriggerDefinitionService triggerDefinitionService, TriggerStateFacade triggerStateFacade) {

            return new TriggerLifecycleFacadeImpl(
                triggerScheduler, triggerDefinitionFacade, triggerDefinitionService, triggerStateFacade);
        }

        @Bean
        TriggerScheduler triggerScheduler(ApplicationProperties applicationProperties, Scheduler scheduler) {
            ApplicationProperties.Coordinator coordinator = applicationProperties.getCoordinator();

            ApplicationProperties.Coordinator.Trigger trigger = coordinator.getTrigger();

            ApplicationProperties.Coordinator.Trigger.Polling polling = trigger.getPolling();

            polling.setCheckPeriod(5);

            return new QuartzTriggerScheduler(polling, scheduler);
        }
    }
}
