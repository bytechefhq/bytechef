/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.ECHO_ACTION;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.SLOW_ECHO_ACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import io.grpc.StatusRuntimeException;
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
class RemoteActionDefinitionServiceClientIntTest {

    @Autowired
    private ComponentWorkerFactory componentWorkerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private ActionDefinitionService actionDefinitionService;
    private ComponentWorker componentWorker;
    private RemoteActionDefinitionServiceClient remoteActionDefinitionServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        actionDefinitionService = componentWorker.getComponentWorkerServices()
            .actionDefinitionService();
        remoteActionDefinitionServiceClient = new RemoteActionDefinitionServiceClient(
            ComponentWorker.grpcChannels(componentWorker), ComponentWorker.discoveryClient(componentWorker),
            objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorker.close();
    }

    @Test
    void testGetActionDefinitionReturnsWhatTheWorkerServes() {
        ActionDefinition actionDefinition = remoteActionDefinitionServiceClient.getActionDefinition(
            COMPONENT_NAME, 1, ECHO_ACTION);

        assertThat(actionDefinition)
            .usingRecursiveComparison()
            .isEqualTo(actionDefinitionService.getActionDefinition(COMPONENT_NAME, 1, ECHO_ACTION));
        assertThat(actionDefinition.getTitle()).isEqualTo("Echo");
    }

    @Test
    void testGetActionDefinitionsReturnsAllActionsOfTheComponent() {
        assertThat(remoteActionDefinitionServiceClient.getActionDefinitions(COMPONENT_NAME, 1))
            .usingRecursiveComparison()
            .isEqualTo(actionDefinitionService.getActionDefinitions(COMPONENT_NAME, 1));
        assertThat(remoteActionDefinitionServiceClient.getActionDefinitions(COMPONENT_NAME, 1))
            .extracting(ActionDefinition::getName)
            .containsExactly(ECHO_ACTION, SLOW_ECHO_ACTION);
    }

    @Test
    void testIsDynamicOutputDefinedOnlyForActionsWithAnOutputFunction() {
        assertThat(remoteActionDefinitionServiceClient.isDynamicOutputDefined(COMPONENT_NAME, 1, ECHO_ACTION))
            .isTrue();
        assertThat(remoteActionDefinitionServiceClient.isDynamicOutputDefined(COMPONENT_NAME, 1, SLOW_ECHO_ACTION))
            .isFalse();
    }

    @Test
    void testExecuteWorkflowNodeDescriptionRunsTheComponentsFunction() {
        assertThat(
            remoteActionDefinitionServiceClient.executeWorkflowNodeDescription(
                COMPONENT_NAME, 1, ECHO_ACTION, Map.of("name", "ada")))
                    .isEqualTo("Echo ada");
    }

    @Test
    void testGetActionDefinitionOfAnUnknownActionFailsWithItsMessage() {
        assertThatThrownBy(() -> remoteActionDefinitionServiceClient.getActionDefinition(COMPONENT_NAME, 1, "nope"))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("nope");
    }
}
