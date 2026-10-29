/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.ITEM_HOOK_TRIGGER;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.NEW_ITEM_TRIGGER;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class RemoteTriggerDefinitionServiceClientIntTest {

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private ComponentWorker componentWorker;
    private RemoteTriggerDefinitionServiceClient remoteTriggerDefinitionServiceClient;
    private TriggerDefinitionService triggerDefinitionService;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        triggerDefinitionService = componentWorker.getComponentWorkerServices()
            .triggerDefinitionService();
        remoteTriggerDefinitionServiceClient = new RemoteTriggerDefinitionServiceClient(
            ComponentWorker.grpcChannels(componentWorker), ComponentWorker.discoveryClient(componentWorker),
            objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorker.close();
    }

    @Test
    void testGetTriggerDefinitionReturnsWhatTheWorkerServes() {
        TriggerDefinition triggerDefinition = remoteTriggerDefinitionServiceClient.getTriggerDefinition(
            COMPONENT_NAME, 1, NEW_ITEM_TRIGGER);

        assertThat(triggerDefinition)
            .usingRecursiveComparison()
            .isEqualTo(triggerDefinitionService.getTriggerDefinition(COMPONENT_NAME, 1, NEW_ITEM_TRIGGER));
        assertThat(triggerDefinition.getTitle()).isEqualTo("New item");
    }

    @Test
    void testGetTriggerDefinitionsReturnsAllTriggersOfTheComponent() {
        assertThat(remoteTriggerDefinitionServiceClient.getTriggerDefinitions("webhook", 1))
            .usingRecursiveComparison()
            .isEqualTo(triggerDefinitionService.getTriggerDefinitions("webhook", 1));
        assertThat(remoteTriggerDefinitionServiceClient.getTriggerDefinitions(COMPONENT_NAME, 1))
            .extracting(TriggerDefinition::getName)
            .containsExactly(NEW_ITEM_TRIGGER, ITEM_HOOK_TRIGGER);
    }

    @Test
    void testGetWebhookTriggerFlagsReturnsTheTriggersFlags() {
        WebhookTriggerFlags webhookTriggerFlags = remoteTriggerDefinitionServiceClient.getWebhookTriggerFlags(
            "webhook", 1, "validateAndRespond");

        assertThat(webhookTriggerFlags)
            .usingRecursiveComparison()
            .isEqualTo(triggerDefinitionService.getWebhookTriggerFlags("webhook", 1, "validateAndRespond"));
        assertThat(
            remoteTriggerDefinitionServiceClient.getWebhookTriggerFlags(COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER))
                .usingRecursiveComparison()
                .isEqualTo(triggerDefinitionService.getWebhookTriggerFlags(COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER));
    }

    @Test
    void testIsDynamicOutputDefinedOnlyForTriggersWithAnOutputFunction() {
        assertThat(remoteTriggerDefinitionServiceClient.isDynamicOutputDefined(COMPONENT_NAME, 1, NEW_ITEM_TRIGGER))
            .isTrue();
        assertThat(remoteTriggerDefinitionServiceClient.isDynamicOutputDefined(COMPONENT_NAME, 1, ITEM_HOOK_TRIGGER))
            .isFalse();
    }

    @Test
    void testExecuteWorkflowNodeDescriptionRunsTheComponentsFunction() {
        assertThat(
            remoteTriggerDefinitionServiceClient.executeWorkflowNodeDescription(
                COMPONENT_NAME, 1, NEW_ITEM_TRIGGER, Map.of("status", "open")))
                    .isEqualTo("New open item");
    }
}
