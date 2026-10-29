/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import static com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.webhook.WebhookComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.config.ComponentRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorkerFactory;
import com.bytechef.ee.platform.component.remote.client.config.GrpcTestComponentHandler;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class RemoteConnectionDefinitionServiceClientIntTest {

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

    private final List<ComponentWorker> componentWorkers = new ArrayList<>();
    private ConnectionDefinitionService connectionDefinitionService;
    private ComponentWorker componentWorker;
    private RemoteConnectionDefinitionServiceClient remoteConnectionDefinitionServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        componentWorker = componentWorkerFactory.start("worker-1");

        componentWorkers.add(componentWorker);

        connectionDefinitionService = componentWorker.getComponentWorkerServices()
            .connectionDefinitionService();
        remoteConnectionDefinitionServiceClient = new RemoteConnectionDefinitionServiceClient(
            ComponentWorker.grpcChannels(componentWorker), ComponentWorker.discoveryClient(componentWorker),
            objectMapper);
    }

    @AfterEach
    void afterEach() {
        componentWorkers.forEach(ComponentWorker::close);

        connectionRepository.deleteAll();
    }

    @Test
    void testGetConnectionDefinitionReturnsWhatTheWorkerServes() {
        ConnectionDefinition connectionDefinition = remoteConnectionDefinitionServiceClient.getConnectionDefinition(
            COMPONENT_NAME, 1);

        assertThat(connectionDefinition)
            .usingRecursiveComparison()
            .isEqualTo(connectionDefinitionService.getConnectionDefinition(COMPONENT_NAME, 1));
        assertThat(connectionDefinition.getAuthorizations())
            .extracting(authorization -> authorization.getType())
            .containsExactly(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    @Test
    void testGetComponentConnectionDefinitionsAndConnectionConnectionDefinition() {
        assertThat(remoteConnectionDefinitionServiceClient.getConnectionDefinitions(COMPONENT_NAME, null))
            .usingRecursiveComparison()
            .isEqualTo(connectionDefinitionService.getConnectionDefinitions(COMPONENT_NAME, null));
        assertThat(remoteConnectionDefinitionServiceClient.getConnectionConnectionDefinition(COMPONENT_NAME, 1))
            .usingRecursiveComparison()
            .isEqualTo(connectionDefinitionService.getConnectionConnectionDefinition(COMPONENT_NAME, 1));
    }

    @Test
    void testGetConnectionDefinitionsAreMergedAcrossWorkers() throws IOException {
        ComponentWorker secondComponentWorker = componentWorkerFactory.start(
            "worker-2",
            ComponentRemoteClientIntTestConfiguration.componentDefinitionRegistry(
                applicationProperties,
                List.of(
                    new GrpcTestComponentHandler(1, "gRPC Test"), new GrpcTestComponentHandler(2, "gRPC Test v2"),
                    new WebhookComponentHandler())));

        componentWorkers.add(secondComponentWorker);

        RemoteConnectionDefinitionServiceClient mergingRemoteConnectionDefinitionServiceClient =
            new RemoteConnectionDefinitionServiceClient(
                ComponentWorker.grpcChannels(componentWorker, secondComponentWorker),
                ComponentWorker.discoveryClient(componentWorker, secondComponentWorker), objectMapper);

        assertThat(mergingRemoteConnectionDefinitionServiceClient.getConnectionDefinitions())
            .extracting(ConnectionDefinition::getComponentName, ConnectionDefinition::getVersion)
            .containsExactlyInAnyOrder(tuple(COMPONENT_NAME, 1));
    }

    @Test
    void testGetAuthorizationTypeReturnsTheAuthorizationsType() {
        assertThat(
            remoteConnectionDefinitionServiceClient.getAuthorizationType(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .isEqualTo(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    @Test
    void testGetOAuth2AuthorizationParametersRunsTheComponentsFunctions() {
        OAuth2AuthorizationParameters oAuth2AuthorizationParameters =
            remoteConnectionDefinitionServiceClient.getOAuth2AuthorizationParameters(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
                Map.of(Authorization.CLIENT_ID, "client-1"));

        assertThat(oAuth2AuthorizationParameters.authorizationUrl())
            .isEqualTo("https://auth.grpc-test.example.com/authorize");
        assertThat(oAuth2AuthorizationParameters.clientId()).isEqualTo("client-1");
        assertThat(oAuth2AuthorizationParameters.scopes()).isEqualTo(Map.of("items.read", true));
    }

    @Test
    void testGetOAuth2AuthorizationParametersForAnUnknownAuthorizationFailsAsInvalidArgument() {
        assertThatThrownBy(
            () -> remoteConnectionDefinitionServiceClient.getOAuth2AuthorizationParameters(
                COMPONENT_NAME, 1, AuthorizationType.API_KEY, Map.of()))
                    .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        statusRuntimeException -> assertThat(statusRuntimeException.getStatus()
                            .getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void testExecuteBaseUriRunsTheComponentsFunction() {
        Optional<String> baseUri = remoteConnectionDefinitionServiceClient.executeBaseUri(
            COMPONENT_NAME,
            new ComponentConnection(
                COMPONENT_NAME, 1, 7L, Map.of("region", "ap"), AuthorizationType.OAUTH2_AUTHORIZATION_CODE));

        assertThat(baseUri).hasValue("https://ap.grpc-test.example.com");
    }

    @Test
    void testExecuteConnectionRefreshStoresTheRefreshedToken() {
        Connection connection = createConnection(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        ComponentConnection componentConnection = remoteConnectionDefinitionServiceClient.executeConnectionRefresh(
            new ComponentConnection(
                COMPONENT_NAME, 1, connection.getId(), connection.getParameters(),
                AuthorizationType.OAUTH2_AUTHORIZATION_CODE));

        assertThat(componentConnection.getParameters())
            .extracting(Authorization.ACCESS_TOKEN, Authorization.REFRESH_TOKEN, "region")
            .containsExactly("refreshed-first-refresh-token", "next-refresh-token", "us");
        assertThat(connectionService.getConnection(connection.getId())
            .getParameters())
                .extracting(Authorization.ACCESS_TOKEN)
                .isEqualTo("refreshed-first-refresh-token");
    }

    @Test
    void testExecuteConnectionRefreshFailureCarriesItsMessageAndMarksTheCredentialsInvalid() {
        Connection connection = createConnection(null);

        assertThatThrownBy(
            () -> remoteConnectionDefinitionServiceClient.executeConnectionRefresh(
                new ComponentConnection(COMPONENT_NAME, 1, connection.getId(), connection.getParameters(), null)))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("NullPointerException");

        assertThat(connectionService.getConnection(connection.getId())
            .getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
    }

    private Connection createConnection(AuthorizationType authorizationType) {
        return connectionService.create(
            Connection.builder()
                .authorizationType(authorizationType)
                .componentName(COMPONENT_NAME)
                .connectionVersion(1)
                .name("gRPC test")
                .parameters(
                    Map.of(
                        "region", "us", Authorization.ACCESS_TOKEN, "first-access-token", Authorization.REFRESH_TOKEN,
                        "first-refresh-token"))
                .type(PlatformType.AUTOMATION)
                .build());
    }
}
