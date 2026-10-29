/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.fasade;

import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.ACCESS_TOKEN_PREFIX;
import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.BASE_URI;
import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.COMPONENT_NAME;
import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.REDIRECT_URI;
import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.REFRESHED_ACCESS_TOKEN;
import static com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration.REFRESHED_EXPIRES_IN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = ConnectionRemoteClientIntTestConfiguration.class)
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class RemoteConnectionFacadeClientIntTest {

    private static final String WORKFLOW_ID = "connection-facade-workflow";

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private ManagedChannel managedChannel;
    private RemoteConnectionFacadeClient remoteConnectionFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionFacadeRemoteServiceGrpcServer(
                        connectionFacade, new ConnectionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectionFacadeClient = new RemoteConnectionFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        workflowTestConfigurationService.delete(WORKFLOW_ID);
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheConnectionWithNewTagsAndWithoutTheOAuth2State() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .environmentId(2)
            .name("Created remotely")
            .parameters(Map.of(Authorization.CLIENT_ID, "client", "region", "eu", "state", "xyz"))
            .tags(List.of(new Tag("crm")))
            .build();

        long connectionId = remoteConnectionFacadeClient.create(connectionDTO, PlatformType.AUTOMATION);

        Connection storedConnection = connectionService.getConnection(connectionId);

        assertThat(storedConnection.getName()).isEqualTo("Created remotely");
        assertThat(storedConnection.getEnvironmentId()).isEqualTo(2);
        assertThat(storedConnection.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(storedConnection.getParameters())
            .isEqualTo(Map.of(Authorization.CLIENT_ID, "client", "region", "eu"));
        assertThat(tagService.getTags(storedConnection.getTagIds()))
            .extracting(Tag::getName)
            .containsExactly("crm");
    }

    @Test
    void testCreateWithAnAuthorizationCodeExchangesItThroughTheComponent() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name("Authorized")
            .parameters(
                Map.of(
                    Authorization.CLIENT_ID, "client", Authorization.CLIENT_SECRET, "secret", Authorization.CODE,
                    "the-code"))
            .build();

        long connectionId = remoteConnectionFacadeClient.create(connectionDTO, PlatformType.AUTOMATION);

        Map<String, ?> storedParameters = connectionService.getConnection(connectionId)
            .getParameters();

        assertThat(storedParameters.get(Authorization.ACCESS_TOKEN)).isEqualTo(ACCESS_TOKEN_PREFIX + "the-code");
        assertThat(storedParameters.get(Authorization.REFRESH_TOKEN)).isEqualTo("refresh-token");
        assertThat(storedParameters.get("redirect_uri_used")).isEqualTo(REDIRECT_URI);
        assertThat(storedParameters.get(Authorization.CLIENT_ID)).isEqualTo("client");
    }

    @Test
    void testGetConnectionReturnsTheDTOTheFacadeBuilds() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Readable", PlatformType.AUTOMATION, 2);

        connectionService.update(connection.getId(), List.of(tags.getFirst()
            .getId()));

        ConnectionDTO connectionDTO = remoteConnectionFacadeClient.getConnection(connection.getId());

        assertThat(connectionDTO)
            .usingRecursiveComparison()
            .isEqualTo(connectionFacade.getConnection(connection.getId()));
        assertThat(connectionDTO.active()).isFalse();
        assertThat(connectionDTO.authorizationParameters())
            .isEqualTo(Map.of(Authorization.CLIENT_ID, "client", Authorization.CLIENT_SECRET, "secret"));
        assertThat(connectionDTO.baseUri()).isEqualTo(BASE_URI);
        assertThat(connectionDTO.connectionParameters()).isEqualTo(Map.of("region", "eu"));
        assertThat(connectionDTO.environmentId()).isEqualTo(2);
        assertThat(connectionDTO.tags())
            .extracting(Tag::getName)
            .containsExactly("crm");
    }

    @Test
    void testGetConnectionIsActiveWhenAWorkflowTestConfigurationUsesIt() {
        Connection connection = createConnection("Used", PlatformType.AUTOMATION, 0);

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            WORKFLOW_ID, "trigger_1", "connection", connection.getId(), false, 0);

        assertThat(remoteConnectionFacadeClient.getConnection(connection.getId())
            .active()).isTrue();
    }

    @Test
    void testGetConnectionsByIdsReturnsOnlyThoseConnections() {
        Connection firstConnection = createConnection("First", PlatformType.AUTOMATION, 0);

        createConnection("Second", PlatformType.AUTOMATION, 0);

        List<ConnectionDTO> connectionDTOs = remoteConnectionFacadeClient.getConnections(
            List.of(firstConnection.getId()), PlatformType.AUTOMATION);

        assertThat(connectionDTOs)
            .usingRecursiveComparison()
            .isEqualTo(connectionFacade.getConnections(List.of(firstConnection.getId()), PlatformType.AUTOMATION));
        assertThat(connectionDTOs)
            .extracting(ConnectionDTO::name)
            .containsExactly("First");
    }

    @Test
    void testGetConnectionsFiltersByComponentIdsTagAndEnvironment() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Long tagId = tags.getFirst()
            .getId();

        Connection taggedConnection = createConnection("Tagged", PlatformType.AUTOMATION, 2);

        connectionService.update(taggedConnection.getId(), List.of(tagId));

        Connection untaggedConnection = createConnection("Untagged", PlatformType.AUTOMATION, 2);

        createConnection("Embedded", PlatformType.EMBEDDED, 2);

        assertThat(
            remoteConnectionFacadeClient.getConnections(
                COMPONENT_NAME, 1, List.of(), tagId, 2L, PlatformType.AUTOMATION))
                    .extracting(ConnectionDTO::name)
                    .containsExactly("Tagged");
        assertThat(
            remoteConnectionFacadeClient.getConnections(
                COMPONENT_NAME, null, List.of(untaggedConnection.getId()), null, null, PlatformType.AUTOMATION))
                    .extracting(ConnectionDTO::name)
                    .containsExactly("Untagged");

        List<ConnectionDTO> connectionDTOs = remoteConnectionFacadeClient.getConnections(
            null, null, List.of(), null, null, PlatformType.AUTOMATION);

        assertThat(connectionDTOs)
            .usingRecursiveComparison()
            .isEqualTo(connectionFacade.getConnections(null, null, List.of(), null, null, PlatformType.AUTOMATION));
        assertThat(connectionDTOs)
            .extracting(ConnectionDTO::name)
            .containsExactly("Tagged", "Untagged");
    }

    @Test
    void testToConnectionDTOsBuildsTheDTOsOfTheGivenConnections() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Converted", PlatformType.AUTOMATION, 0);

        connectionService.update(connection.getId(), List.of(tags.getFirst()
            .getId()));

        List<Connection> connections = List.of(connectionService.getConnection(connection.getId()));

        List<ConnectionDTO> connectionDTOs = remoteConnectionFacadeClient.toConnectionDTOs(connections);

        assertThat(connectionDTOs)
            .usingRecursiveComparison()
            .isEqualTo(connectionFacade.toConnectionDTOs(connections));
        assertThat(connectionDTOs)
            .singleElement()
            .satisfies(connectionDTO -> {
                assertThat(connectionDTO.baseUri()).isEqualTo(BASE_URI);
                assertThat(connectionDTO.tags())
                    .extracting(Tag::getName)
                    .containsExactly("crm");
            });
    }

    @Test
    void testGetConnectionTagsReturnsTheTagsOfThatTypesConnections() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm"), new Tag("internal")));
        Connection automationConnection = createConnection("Automation", PlatformType.AUTOMATION, 0);
        Connection embeddedConnection = createConnection("Embedded", PlatformType.EMBEDDED, 0);

        connectionService.update(automationConnection.getId(), List.of(tags.getFirst()
            .getId()));
        connectionService.update(embeddedConnection.getId(), List.of(tags.getLast()
            .getId()));

        assertThat(remoteConnectionFacadeClient.getConnectionTags(PlatformType.AUTOMATION))
            .extracting(Tag::getName)
            .containsExactly("crm");
    }

    @Test
    void testUpdateTagsSavesNewTagsAndAssignsThem() {
        Connection connection = createConnection("Tagged", PlatformType.AUTOMATION, 0);

        remoteConnectionFacadeClient.update(connection.getId(), List.of(new Tag("sales")));

        assertThat(tagService.getTags(connectionService.getConnection(connection.getId())
            .getTagIds()))
                .extracting(Tag::getName)
                .containsExactly("sales");
    }

    @Test
    void testUpdateRenamesAndReplacesTags() {
        Connection connection = createConnection("Original", PlatformType.AUTOMATION, 0);

        remoteConnectionFacadeClient.update(
            connection.getId(), "Renamed", List.of(new Tag("crm")), connection.getVersion());

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getName()).isEqualTo("Renamed");
        assertThat(tagService.getTags(storedConnection.getTagIds()))
            .extracting(Tag::getName)
            .containsExactly("crm");
    }

    @Test
    void testUpdateWithoutNameOrTagsKeepsTheNameAndClearsTagsAsTheFacadeDoes() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Original", PlatformType.AUTOMATION, 0);
        Long tagId = tags.getFirst()
            .getId();

        connection = connectionService.update(connection.getId(), List.of(tagId));

        remoteConnectionFacadeClient.update(connection.getId(), null, null, connection.getVersion());

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getName()).isEqualTo("Original");
        assertThat(storedConnection.getTagIds()).isEmpty();
        assertThat(storedConnection.getVersion()).isEqualTo(connection.getVersion() + 1);
    }

    @Test
    void testUpdateWithEmptyTagsClearsThem() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Original", PlatformType.AUTOMATION, 0);

        connection = connectionService.update(connection.getId(), List.of(tags.getFirst()
            .getId()));

        remoteConnectionFacadeClient.update(connection.getId(), "Renamed", List.of(), connection.getVersion());

        assertThat(connectionService.getConnection(connection.getId())
            .getTagIds()).isEmpty();
    }

    @Test
    @WithMockUser(username = "user@localhost.com", authorities = AuthorityConstants.USER)
    void testUpdateByANonOwnerFailsAsPermissionDenied() {
        Connection connection = createConnection("Not mine", PlatformType.AUTOMATION, 0);

        assertThatThrownBy(
            () -> remoteConnectionFacadeClient.update(connection.getId(), "Renamed", null, connection.getVersion()))
                .isInstanceOfSatisfying(
                    StatusRuntimeException.class,
                    statusRuntimeException -> assertThat(statusRuntimeException.getStatus()
                        .getCode()).isEqualTo(Status.Code.PERMISSION_DENIED));

        assertThat(connectionService.getConnection(connection.getId())
            .getName()).isEqualTo("Not mine");
    }

    @Test
    void testReplaceAuthorizationParametersKeepsTheOtherParametersAndMarksCredentialsValid() {
        Connection connection = createConnection("Reauthorized", PlatformType.AUTOMATION, 0);

        connectionService.updateConnectionCredentialStatus(connection.getId(), Connection.CredentialStatus.INVALID);

        remoteConnectionFacadeClient.replaceAuthorizationParameters(
            connection.getId(), Map.of(Authorization.CLIENT_ID, "new-client", "state", "ignored"));

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getParameters())
            .isEqualTo(
                Map.of(
                    Authorization.ACCESS_TOKEN, "old-access-token", Authorization.CLIENT_ID, "new-client",
                    Authorization.REFRESH_TOKEN, "old-refresh-token", "region", "eu"));
        assertThat(storedConnection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.VALID);
    }

    @Test
    void testExecuteConnectionRefreshStoresTheRefreshedTokenAndReturnsItsExpiry() {
        Connection connection = createConnection("Refreshable", PlatformType.AUTOMATION, 0);

        Integer expiresIn = remoteConnectionFacadeClient.executeConnectionRefresh(connection.getId());

        assertThat(expiresIn).isEqualTo(REFRESHED_EXPIRES_IN);

        Map<String, ?> storedParameters = connectionService.getConnection(connection.getId())
            .getParameters();

        assertThat(storedParameters.get(Authorization.ACCESS_TOKEN)).isEqualTo(REFRESHED_ACCESS_TOKEN);
        assertThat(storedParameters.get(Authorization.REFRESH_TOKEN)).isEqualTo("refreshed-refresh-token");
        assertThat(storedParameters.get("region")).isEqualTo("eu");
    }

    @Test
    void testDeleteRemovesAnUnusedConnection() {
        Connection connection = createConnection("To delete", PlatformType.AUTOMATION, 0);

        remoteConnectionFacadeClient.delete(connection.getId());

        assertThat(connectionRepository.findById(connection.getId())).isEmpty();
    }

    @Test
    void testDeleteOfAUsedConnectionFailsAndKeepsIt() {
        Connection connection = createConnection("Used", PlatformType.AUTOMATION, 0);

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            WORKFLOW_ID, "trigger_1", "connection", connection.getId(), false, 0);

        assertThatThrownBy(() -> remoteConnectionFacadeClient.delete(connection.getId()))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("Connection id=%s is used".formatted(connection.getId()));

        assertThat(connectionRepository.findById(connection.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = "user@localhost.com", authorities = AuthorityConstants.USER)
    void testDeleteByANonOwnerFailsAsPermissionDenied() {
        Connection connection = createConnection("Not mine", PlatformType.AUTOMATION, 0);

        assertThatThrownBy(() -> remoteConnectionFacadeClient.delete(connection.getId()))
            .isInstanceOfSatisfying(
                StatusRuntimeException.class,
                statusRuntimeException -> assertThat(statusRuntimeException.getStatus()
                    .getCode()).isEqualTo(Status.Code.PERMISSION_DENIED));

        assertThat(connectionRepository.findById(connection.getId())).isPresent();
    }

    private Connection createConnection(String name, PlatformType type, int environmentId) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(Authorization.ACCESS_TOKEN, "old-access-token");
        parameters.put(Authorization.CLIENT_ID, "client");
        parameters.put(Authorization.CLIENT_SECRET, "secret");
        parameters.put(Authorization.REFRESH_TOKEN, "old-refresh-token");
        parameters.put("region", "eu");

        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name(name)
            .parameters(parameters)
            .type(type)
            .build();

        connection.setEnvironmentId(environmentId);

        return connectionService.create(connection);
    }
}
