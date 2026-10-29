/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.io.IOException;
import java.util.List;
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
class RemoteComponentDefinitionFacadeClientIntTest {

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

    private ComponentWorker componentWorker;
    private RemoteComponentDefinitionFacadeClient remoteComponentDefinitionFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        remoteComponentDefinitionFacadeClient = new RemoteComponentDefinitionFacadeClient(
            applicationProperties, ComponentWorker.grpcChannels(componentWorker),
            ComponentWorker.discoveryClient(componentWorker), objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorker.close();

        connectionRepository.deleteAll();
    }

    @Test
    void testExecuteWorkflowInputOptionsFiltersBySearchTextWithAConnection() {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .componentName(COMPONENT_NAME)
                .connectionVersion(1)
                .name("gRPC test")
                .parameters(Map.of("region", "us"))
                .type(PlatformType.AUTOMATION)
                .build());

        assertThat(
            remoteComponentDefinitionFacadeClient.executeWorkflowInputOptions(
                COMPONENT_NAME, 1, "inputs", "status", Map.of(), List.of(), "op", connection.getId()))
                    .extracting(Option::getLabel, Option::getValue)
                    .containsExactly(org.assertj.core.api.Assertions.tuple("Open", "open"));
    }

    @Test
    void testExecuteWorkflowInputOptionsWithoutConnectionOrSearchText() {
        assertThat(
            remoteComponentDefinitionFacadeClient.executeWorkflowInputOptions(
                COMPONENT_NAME, 1, "inputs", "status", Map.of(), List.of(), null, null))
                    .extracting(Option::getValue)
                    .containsExactly("open", "closed");
    }
}
