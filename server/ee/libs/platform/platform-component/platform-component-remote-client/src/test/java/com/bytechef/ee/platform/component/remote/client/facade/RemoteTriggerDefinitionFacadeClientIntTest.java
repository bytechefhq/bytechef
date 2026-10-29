/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.ITEM_HOOK_TRIGGER;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.NEW_ITEM_TRIGGER;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.REFRESHED_WEBHOOK_EXPIRATION_DATE;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.WEBHOOK_EXPIRATION_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ComponentRemoteClientIntTestConfiguration.class, properties = "spring.quartz.auto-startup=false")
class RemoteTriggerDefinitionFacadeClientIntTest {

    private static final String WORKFLOW_EXECUTION_ID = String.valueOf(
        WorkflowExecutionId.of(PlatformType.AUTOMATION, 11L, "workflow-uuid", "trigger_1"));

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Scheduler scheduler;

    private ComponentWorker componentWorker;
    private RemoteTriggerDefinitionFacadeClient remoteTriggerDefinitionFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        remoteTriggerDefinitionFacadeClient = new RemoteTriggerDefinitionFacadeClient(
            applicationProperties, ComponentWorker.grpcChannels(componentWorker),
            ComponentWorker.discoveryClient(componentWorker), objectMapper);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        componentWorker.close();

        connectionRepository.deleteAll();
        scheduler.clear();

        GrpcTestComponentHandler.clearDisabledWebhooks();
    }

    @Test
    void testExecuteDynamicPropertiesReturnsTheComponentsProperties() {
        List<Property> properties = remoteTriggerDefinitionFacadeClient.executeDynamicProperties(
            COMPONENT_NAME, 1, NEW_ITEM_TRIGGER, "filters", Map.of("status", "open"), List.of("status"),
            createConnection());

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("openSince");
    }

    @Test
    void testExecuteOptionsFiltersBySearchText() {
        assertThat(
            remoteTriggerDefinitionFacadeClient.executeOptions(
                COMPONENT_NAME, 1, NEW_ITEM_TRIGGER, "status", Map.of(), List.of(), "clo", null))
                    .extracting(Option::getLabel, Option::getValue)
                    .containsExactly(tuple("Closed", "closed"));
    }

    @Test
    void testExecuteOutputReturnsTheComponentsSample() {
        OutputResponse outputResponse = remoteTriggerDefinitionFacadeClient.executeOutput(
            COMPONENT_NAME, 1, NEW_ITEM_TRIGGER, Map.of("status", "open"), createConnection());

        assertThat(outputResponse.sampleOutput()).isEqualTo(Map.of("id", "item-1"));
        assertThat(outputResponse.outputSchema()).isNotNull();
    }

    @Test
    void testExecuteTriggerPollsFromTheGivenState() {
        TriggerOutput triggerOutput = remoteTriggerDefinitionFacadeClient.executeTrigger(
            COMPONENT_NAME, 1, NEW_ITEM_TRIGGER, 11L, "workflow-uuid", 15L, Map.of("status", "open"),
            Map.of("cursor", 1), null, createConnection(), 2L, PlatformType.AUTOMATION, false);

        assertThat(triggerOutput.value()).isEqualTo(
            List.of(Map.of("id", "item-2", "status", "open"), Map.of("id", "item-3", "status", "open")));
        assertThat(triggerOutput.state()).isEqualTo(Map.of("cursor", 3));
    }

    @Test
    void testExecuteTriggerHandsTheWebhookRequestToTheComponent() {
        TriggerOutput triggerOutput = remoteTriggerDefinitionFacadeClient.executeTrigger(
            "webhook", 1, "autoRespondWithHTTP200", null, "workflow-uuid", null, Map.of(), null, webhookRequest(),
            null, null, PlatformType.EMBEDDED, false);

        assertThat(triggerOutput.value()).isEqualTo(
            Map.of(
                "body", Map.of("name", "x"), "headers", Map.of("X-Csrf-Token", "token-1"), "method", "POST",
                "parameters", Map.of("challenge", "abc")));
    }

    @Test
    void testExecuteWebhookEnableReturnsTheComponentsOutput() {
        WebhookEnableOutput webhookEnableOutput = remoteTriggerDefinitionFacadeClient.executeWebhookEnable(
            COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER, Map.of("event", "created"), WORKFLOW_EXECUTION_ID, null,
            "https://example.com/webhooks/1", 2L);

        assertThat(webhookEnableOutput).isEqualTo(
            new WebhookEnableOutput(
                Map.of(
                    "event", "created", "url", "https://example.com/webhooks/1", "webhookId",
                    "hook-" + WORKFLOW_EXECUTION_ID),
                WEBHOOK_EXPIRATION_DATE));
    }

    @Test
    void testExecuteWebhookDisableHandsTheEnableOutputToTheComponent() {
        remoteTriggerDefinitionFacadeClient.executeWebhookDisable(
            COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER, Map.of("event", "created"), WORKFLOW_EXECUTION_ID,
            Map.of("webhookId", "hook-1"), createConnection());

        assertThat(GrpcTestComponentHandler.getDisabledWebhooks()).containsExactly(
            Map.of("event", "created", "webhookId", "hook-1", "workflowExecutionId", WORKFLOW_EXECUTION_ID));
    }

    @Test
    void testExecuteDynamicWebhookRefreshReturnsTheRefreshedOutput() {
        WebhookEnableOutput webhookEnableOutput = remoteTriggerDefinitionFacadeClient.executeDynamicWebhookRefresh(
            COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER, Map.of("webhookId", "hook-1"), null);

        assertThat(webhookEnableOutput).isEqualTo(
            new WebhookEnableOutput(Map.of("refreshed", true, "webhookId", "hook-1"),
                REFRESHED_WEBHOOK_EXPIRATION_DATE));
    }

    @Test
    void testExecuteWebhookValidateAcceptsAMatchingCsrfToken() {
        assertThat(
            remoteTriggerDefinitionFacadeClient.executeWebhookValidate(
                "webhook", 1, "validateAndRespond", Map.of("csrfToken", "token-1"), webhookRequest(), null)
                .status()).isEqualTo(200);
        assertThat(
            remoteTriggerDefinitionFacadeClient.executeWebhookValidate(
                "webhook", 1, "validateAndRespond", Map.of("csrfToken", "other"), webhookRequest(), null)
                .status()).isEqualTo(400);
    }

    @Test
    void testExecuteWebhookValidateOnEnableReturnsTheComponentsResponse() {
        WebhookValidateResponse webhookValidateResponse =
            remoteTriggerDefinitionFacadeClient.executeWebhookValidateOnEnable(
                COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER, Map.of("event", "created"), webhookRequest(),
                createConnection());

        assertThat(webhookValidateResponse).isEqualTo(
            new WebhookValidateResponse(Map.of("challenge", "abc"), Map.of("X-Event", List.of("created")), 200));
    }

    @Test
    void testExecuteListenerEnableAndDisableScheduleAndCancelTheQuartzJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(WORKFLOW_EXECUTION_ID, "ScheduleTrigger");

        remoteTriggerDefinitionFacadeClient.executeListenerEnable(
            "schedule", 1, "cron", Map.of("expression", "15 10 * * ?", "timezone", "Europe/Zagreb"),
            WORKFLOW_EXECUTION_ID, null);

        assertThat(scheduler.checkExists(jobKey)).isTrue();

        CronTrigger cronTrigger = (CronTrigger) scheduler.getTrigger(
            TriggerKey.triggerKey(WORKFLOW_EXECUTION_ID, "ScheduleTrigger"));

        assertThat(cronTrigger.getCronExpression()).isEqualTo("0 15 10 * * ?");
        assertThat(cronTrigger.getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Zagreb"));

        remoteTriggerDefinitionFacadeClient.executeListenerDisable(
            "schedule", 1, "cron", Map.of("expression", "15 10 * * ?", "timezone", "Europe/Zagreb"),
            WORKFLOW_EXECUTION_ID, null);

        assertThat(scheduler.checkExists(jobKey)).isFalse();
    }

    private long createConnection() {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .componentName(COMPONENT_NAME)
                .connectionVersion(1)
                .name("gRPC test")
                .parameters(Map.of("region", "us", Authorization.ACCESS_TOKEN, "access"))
                .type(PlatformType.AUTOMATION)
                .build());

        return connection.getId();
    }

    private static WebhookRequest webhookRequest() {
        return new WebhookRequest(
            Map.of("X-Csrf-Token", List.of("token-1")), Map.of("challenge", List.of("abc")),
            new WebhookRequest.WebhookBodyImpl(
                Map.of("name", "x"), ContentType.JSON, "application/json", "{\"name\":\"x\"}"),
            WebhookMethod.POST);
    }
}
