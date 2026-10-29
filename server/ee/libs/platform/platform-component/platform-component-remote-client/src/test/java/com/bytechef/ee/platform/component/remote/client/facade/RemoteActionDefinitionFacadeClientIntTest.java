/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.ECHO_ACTION;
import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.SLOW_ECHO_ACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.interceptor.DeadlineClientInterceptor;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import io.grpc.ClientInterceptors;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
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
class RemoteActionDefinitionFacadeClientIntTest {

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
    private RemoteActionDefinitionFacadeClient remoteActionDefinitionFacadeClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        remoteActionDefinitionFacadeClient = new RemoteActionDefinitionFacadeClient(
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
        List<Property> properties = remoteActionDefinitionFacadeClient.executeDynamicProperties(
            COMPONENT_NAME, 1, ECHO_ACTION, "fields", Map.of("name", "ada", "fieldNames", List.of("email", "phone")),
            List.of("name"), "workflow-1", null);

        assertThat(properties)
            .extracting(Property::getName, property -> ((ValueProperty<?>) property).getLabel())
            .containsExactly(
                tuple("email", "email of ada"),
                tuple("phone", "phone of ada"));
    }

    @Test
    void testExecuteOptionsWithAConnectionUsesItsParameters() {
        long connectionId = createConnection("us");

        assertThat(
            remoteActionDefinitionFacadeClient.executeOptions(
                COMPONENT_NAME, 1, ECHO_ACTION, "name", Map.of(), List.of(), null, connectionId))
                    .extracting(Option::getLabel, Option::getValue)
                    .containsExactly(
                        tuple("Ada in us", "ada"),
                        tuple("Grace in us", "grace"));
        assertThat(
            remoteActionDefinitionFacadeClient.executeOptions(
                COMPONENT_NAME, 1, ECHO_ACTION, "name", Map.of(), List.of(), "gra", connectionId))
                    .extracting(Option::getValue)
                    .containsExactly("grace");
    }

    @Test
    void testExecuteOptionsWithoutAConnectionHasNoRegion() {
        assertThat(
            remoteActionDefinitionFacadeClient.executeOptions(
                COMPONENT_NAME, 1, ECHO_ACTION, "name", Map.of(), List.of(), "ada", null))
                    .extracting(Option::getLabel)
                    .containsExactly("Ada in none");
    }

    @Test
    void testExecuteOptionsWithConnectionsMapAndExtensions() {
        long connectionId = createConnection("ap");

        assertThat(
            remoteActionDefinitionFacadeClient.executeOptions(
                COMPONENT_NAME, 1, ECHO_ACTION, "name", Map.of(), List.of(), null,
                Map.of(COMPONENT_NAME, connectionId), Map.of("tool", "x")))
                    .extracting(Option::getLabel)
                    .containsExactly("Ada in ap", "Grace in ap");
    }

    @Test
    void testExecuteOutputReturnsTheComponentsOutputSchemaAndSample() {
        long connectionId = createConnection("eu");

        OutputResponse outputResponse = remoteActionDefinitionFacadeClient.executeOutput(
            COMPONENT_NAME, 1, ECHO_ACTION, Map.of("name", "ada"), Map.of(COMPONENT_NAME, connectionId));

        assertThat(outputResponse.sampleOutput()).isEqualTo(Map.of("name", "ada", "count", 1));
        assertThat(outputResponse)
            .usingRecursiveComparison()
            .isEqualTo(
                componentWorker.getComponentWorkerServices()
                    .actionDefinitionFacade()
                    .executeOutput(
                        COMPONENT_NAME, 1, ECHO_ACTION, Map.of("name", "ada"), Map.of(COMPONENT_NAME, connectionId)));
    }

    @Test
    void testExecutePerformRunsTheActionWithItsConnection() {
        long connectionId = createConnection("us");

        Object result = remoteActionDefinitionFacadeClient.executePerform(
            COMPONENT_NAME, 1, ECHO_ACTION, 11L, 12L, 13L, 14L, "workflow-1", Map.of("name", "ada", "count", 5),
            Map.of(COMPONENT_NAME, connectionId), Map.of("tool", "x"), 2L, PlatformType.AUTOMATION, true, null, null,
            null);

        assertThat(result).isEqualTo(
            Map.of(
                "count", 5, "name", "ada", "nested", Map.of("rows", List.of(Map.of("id", 1), Map.of("id", 2))),
                "region", "us"));
    }

    @Test
    void testExecutePerformWithNullOptionalArguments() {
        Object result = remoteActionDefinitionFacadeClient.executePerform(
            COMPONENT_NAME, 1, ECHO_ACTION, null, null, null, null, null, Map.of("name", "grace"), Map.of(), Map.of(),
            null, null, false, null, null, null);

        assertThat(result)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("name", "grace")
            .containsEntry("region", null);
    }

    @Test
    void testExecutePerformIsNotCutOffByTheLookupDeadline() {
        RemoteActionDefinitionFacadeClient deadlineRemoteActionDefinitionFacadeClient =
            new RemoteActionDefinitionFacadeClient(
                applicationProperties,
                new FixedGrpcChannels(
                    ClientInterceptors.intercept(
                        componentWorker.getManagedChannel(), new DeadlineClientInterceptor(Duration.ofMillis(100)))),
                ComponentWorker.discoveryClient(componentWorker), objectMapper);

        assertThat(
            deadlineRemoteActionDefinitionFacadeClient.executePerform(
                COMPONENT_NAME, 1, SLOW_ECHO_ACTION, null, null, null, null, null, Map.of("millis", 500), Map.of(),
                Map.of(), null, null, false, null, null, null))
                    .isEqualTo("done");
    }

    @Test
    void testExecutePerformOfAnUnknownConnectionFailsWithItsMessage() {
        assertThatThrownBy(
            () -> remoteActionDefinitionFacadeClient.executePerform(
                COMPONENT_NAME, 1, ECHO_ACTION, null, null, null, null, null, Map.of(),
                Map.of(COMPONENT_NAME, Long.MAX_VALUE), Map.of(), null, null, false, null, null, null))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("Connection does not exist for id=" + Long.MAX_VALUE);
    }

    private long createConnection(String region) {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .componentName(COMPONENT_NAME)
                .connectionVersion(1)
                .name("gRPC test " + region)
                .parameters(
                    Map.of(
                        "region", region, Authorization.ACCESS_TOKEN, "access", Authorization.REFRESH_TOKEN,
                        "refresh"))
                .type(PlatformType.AUTOMATION)
                .build());

        return connection.getId();
    }
}
