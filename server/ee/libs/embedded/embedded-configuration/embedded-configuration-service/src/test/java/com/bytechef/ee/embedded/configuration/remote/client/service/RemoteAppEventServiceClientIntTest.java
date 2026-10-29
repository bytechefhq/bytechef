/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.AppEventRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.repository.AppEventRepository;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE", "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class RemoteAppEventServiceClientIntTest {

    private static final String SCHEMA = """
        {"type":"object","properties":{"orderId":{"type":"string"}}}""";

    @Autowired
    private AppEventRepository appEventRepository;

    @Autowired
    private AppEventService appEventService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteAppEventServiceClient remoteAppEventServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new AppEventRemoteServiceGrpcServer(appEventService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteAppEventServiceClient = new RemoteAppEventServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        appEventRepository.deleteAll();
    }

    @Test
    void testGetAppEventReturnsTheStoredAppEvent() {
        AppEvent appEvent = createAppEvent("order.created");

        createAppEvent("order.cancelled");

        AppEvent remoteAppEvent = remoteAppEventServiceClient.getAppEvent(appEvent.getId());

        assertThat(remoteAppEvent).usingRecursiveComparison()
            .isEqualTo(appEventService.getAppEvent(appEvent.getId()));
        assertThat(remoteAppEvent.getName()).isEqualTo("order.created");
        assertThat(remoteAppEvent.getSchema()).isEqualTo(SCHEMA);
    }

    private AppEvent createAppEvent(String name) {
        AppEvent appEvent = new AppEvent();

        appEvent.setName(name);
        appEvent.setSchema(SCHEMA);

        return appEventService.create(appEvent);
    }
}
