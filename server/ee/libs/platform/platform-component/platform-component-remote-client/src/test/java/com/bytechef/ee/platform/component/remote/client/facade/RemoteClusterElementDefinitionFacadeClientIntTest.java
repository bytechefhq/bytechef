/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.LOOKUP_TOOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import io.grpc.StatusRuntimeException;
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
class RemoteClusterElementDefinitionFacadeClientIntTest {

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
    private RemoteClusterElementDefinitionFacadeClient remoteClusterElementDefinitionFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        remoteClusterElementDefinitionFacadeClient = new RemoteClusterElementDefinitionFacadeClient(
            applicationProperties, ComponentWorker.grpcChannels(componentWorker),
            ComponentWorker.discoveryClient(componentWorker), objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorker.close();

        connectionRepository.deleteAll();
    }

    @Test
    void testExecuteDynamicPropertiesReturnsTheComponentsProperties() {
        List<Property> properties = remoteClusterElementDefinitionFacadeClient.executeDynamicProperties(
            COMPONENT_NAME, 1, LOOKUP_TOOL, "columns", Map.of("query", "customers"), List.of("query"),
            createConnection());

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("customersColumn");
    }

    @Test
    void testExecuteDynamicPropertiesWithClusterElements() {
        List<Property> properties = remoteClusterElementDefinitionFacadeClient.executeDynamicProperties(
            COMPONENT_NAME, 1, LOOKUP_TOOL, "columns", Map.of("query", "orders"), Map.of("parent", "agent"),
            List.of("query"), null, Map.of(COMPONENT_NAME, createConnection()),
            Map.of(COMPONENT_NAME, Map.of("temperature", 1)));

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("ordersColumn");
    }

    @Test
    void testExecuteOptionsFiltersBySearchText() {
        List<Option> options = remoteClusterElementDefinitionFacadeClient.executeOptions(
            COMPONENT_NAME, 1, LOOKUP_TOOL, "query", Map.of(), Map.of("parent", "agent"), List.of(), "ord",
            createConnection(), Map.of(), Map.of());

        assertThat(options)
            .extracting(Option::getValue)
            .containsExactly("orders");
    }

    @Test
    void testExecuteOutputReturnsTheComponentsSample() {
        OutputResponse outputResponse = remoteClusterElementDefinitionFacadeClient.executeOutput(
            COMPONENT_NAME, 1, LOOKUP_TOOL, Map.of("query", "customers"), createConnection());

        assertThat(outputResponse).isNotNull();
        assertThat(outputResponse.sampleOutput()).isEqualTo(Map.of("match", "row-1"));
    }

    @Test
    void testExecuteToolRunsTheToolWithItsConnection() {
        assertThat(
            remoteClusterElementDefinitionFacadeClient.executeTool(
                COMPONENT_NAME, 1, LOOKUP_TOOL, Map.of("query", "customers"), createConnection()))
                    .isEqualTo(Map.of("match", "customers-row", "region", "us"));
        assertThat(
            remoteClusterElementDefinitionFacadeClient.executeTool(
                COMPONENT_NAME, 1, LOOKUP_TOOL, Map.of("query", "orders"), null, true))
                    .isEqualTo(Map.of("match", "orders-row", "region", "none"));
    }

    @Test
    void testExecuteToolWithoutVersionUsesTheLatestComponent() {
        assertThat(
            remoteClusterElementDefinitionFacadeClient.executeTool(
                COMPONENT_NAME, LOOKUP_TOOL, Map.of("query", "orders"), null))
                    .isEqualTo(Map.of("match", "orders-row", "region", "none"));
    }

    @Test
    void testExecuteToolOfAnUnknownClusterElementFailsWithItsMessage() {
        assertThatThrownBy(
            () -> remoteClusterElementDefinitionFacadeClient.executeTool(
                COMPONENT_NAME, 1, "unknown", Map.of(), null))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("unknown");
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
}
