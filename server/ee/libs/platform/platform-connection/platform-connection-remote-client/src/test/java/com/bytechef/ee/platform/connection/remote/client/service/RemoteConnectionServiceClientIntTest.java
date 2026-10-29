/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.client.config.ConnectionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.tag.service.TagService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConnectionRemoteClientIntTestConfiguration.class)
class RemoteConnectionServiceClientIntTest {

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

    private ManagedChannel managedChannel;
    private RemoteConnectionServiceClient remoteConnectionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionRemoteServiceGrpcServer(connectionService, new ConnectionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectionServiceClient = new RemoteConnectionServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheConnectionWithItsTagsAndParameters() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));

        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName("airtable")
            .connectionVersion(1)
            .name("My Airtable")
            .parameters(Map.of("key", "secret", "count", 5))
            .tagIds(List.of(tags.getFirst()
                .getId()))
            .type(PlatformType.AUTOMATION)
            .build();

        connection.setEnvironmentId(2);

        Connection createdConnection = remoteConnectionServiceClient.create(connection);

        assertThat(createdConnection.getId()).isNotNull();
        assertThat(createdConnection.getCreatedBy()).isEqualTo("system");
        assertThat(createdConnection.getCreatedDate()).isNotNull();

        Connection storedConnection = connectionService.getConnection(createdConnection.getId());

        assertThat(createdConnection)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(storedConnection);
        assertThat(storedConnection.getAuthorizationType()).isEqualTo(AuthorizationType.API_KEY);
        assertThat(storedConnection.getComponentName()).isEqualTo("airtable");
        assertThat(storedConnection.getEnvironmentId()).isEqualTo(2);
        assertThat(storedConnection.getName()).isEqualTo("My Airtable");
        assertThat(storedConnection.getParameters()).isEqualTo(Map.of("key", "secret", "count", 5));
        assertThat(storedConnection.getTagIds()).containsExactly(tags.getFirst()
            .getId());
        assertThat(storedConnection.getType()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testCreateWithParametersStoresTheConnection() {
        Connection createdConnection = remoteConnectionServiceClient.create(
            AuthorizationType.BEARER_TOKEN, "httpClient", 1, 2, "My HTTP", Map.of("token", "abc"),
            PlatformType.EMBEDDED);

        Connection storedConnection = connectionService.getConnection(createdConnection.getId());

        assertThat(storedConnection.getAuthorizationType()).isEqualTo(AuthorizationType.BEARER_TOKEN);
        assertThat(storedConnection.getComponentName()).isEqualTo("httpClient");
        assertThat(storedConnection.getConnectionVersion()).isEqualTo(1);
        assertThat(storedConnection.getEnvironmentId()).isEqualTo(2);
        assertThat(storedConnection.getName()).isEqualTo("My HTTP");
        assertThat(storedConnection.getParameters()).isEqualTo(Map.of("token", "abc"));
        assertThat(storedConnection.getType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testCreateWithParametersWithoutAuthorizationTypeKeepsItEmpty() {
        Connection createdConnection = remoteConnectionServiceClient.create(
            null, "httpClient", 1, 0, "No auth", Map.of(), PlatformType.AUTOMATION);

        assertThat(connectionService.getConnection(createdConnection.getId())
            .getAuthorizationType()).isNull();
    }

    @Test
    void testDeleteRemovesTheConnection() {
        Connection connection = createConnection("To delete", "airtable", PlatformType.AUTOMATION, 0);

        remoteConnectionServiceClient.delete(connection.getId());

        assertThat(connectionRepository.findById(connection.getId())).isEmpty();
    }

    @Test
    void testGetConnectionReadsTheStoredConnection() {
        Connection connection = createConnection("Readable", "airtable", PlatformType.AUTOMATION, 2);

        assertThat(remoteConnectionServiceClient.getConnection(connection.getId()))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(connectionService.getConnection(connection.getId()));
    }

    @Test
    void testGetConnectionKeepsParameterNumberTypes() {
        Connection connection = connectionService.create(
            Connection.builder()
                .componentName("postgresql")
                .connectionVersion(1)
                .name("Numbers")
                .parameters(Map.of("big", 9_007_199_254_740_993L, "port", 5432, "ratio", 5.5))
                .type(PlatformType.AUTOMATION)
                .build());

        Map<String, ?> readParameters = remoteConnectionServiceClient.getConnection(connection.getId())
            .getParameters();

        assertThat(readParameters).isEqualTo(Map.of("big", 9_007_199_254_740_993L, "port", 5432, "ratio", 5.5));
        assertThat(readParameters.get("big")).isInstanceOf(Long.class);
        assertThat(readParameters.get("port")).isInstanceOf(Integer.class);
        assertThat(readParameters.get("ratio")).isInstanceOf(Double.class);
    }

    @Test
    void testGetConnectionOfUnknownIdFailsWithTheServiceMessage() {
        assertThatThrownBy(() -> remoteConnectionServiceClient.getConnection(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining("Connection does not exist for id=" + Long.MAX_VALUE);
    }

    @Test
    void testFetchConnectionReturnsTheStoredConnectionOrEmpty() {
        Connection connection = createConnection("Fetchable", "airtable", PlatformType.AUTOMATION, 0);

        assertThat(remoteConnectionServiceClient.fetchConnection(connection.getId()))
            .get()
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(connectionService.getConnection(connection.getId()));
        assertThat(remoteConnectionServiceClient.fetchConnection(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetConnectionsByTypeReturnsOnlyThatType() {
        Connection automationConnection = createConnection("Automation", "airtable", PlatformType.AUTOMATION, 0);

        createConnection("Embedded", "airtable", PlatformType.EMBEDDED, 0);

        assertThat(remoteConnectionServiceClient.getConnections(PlatformType.AUTOMATION))
            .extracting(Connection::getId)
            .containsExactly(automationConnection.getId());
    }

    @Test
    void testGetConnectionsOfComponentVersionAndType() {
        Connection airtableConnection = createConnection("Airtable", "airtable", PlatformType.AUTOMATION, 0);

        createConnection("Slack", "slack", PlatformType.AUTOMATION, 0);
        createConnection("Embedded airtable", "airtable", PlatformType.EMBEDDED, 0);

        assertThat(remoteConnectionServiceClient.getConnections("airtable", 1, PlatformType.AUTOMATION))
            .extracting(Connection::getId)
            .containsExactly(airtableConnection.getId());
    }

    @Test
    void testGetConnectionsFilteredByComponentTagAndEnvironment() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Long tagId = tags.getFirst()
            .getId();

        Connection taggedConnection = createConnection("Tagged", "airtable", PlatformType.EMBEDDED, 2);

        connectionService.update(taggedConnection.getId(), List.of(tagId));

        createConnection("Untagged", "airtable", PlatformType.EMBEDDED, 2);

        Connection otherEnvironmentConnection = createConnection(
            "Other environment", "airtable", PlatformType.EMBEDDED, 1);

        connectionService.update(otherEnvironmentConnection.getId(), List.of(tagId));

        assertThat(remoteConnectionServiceClient.getConnections("airtable", null, tagId, 2L, PlatformType.EMBEDDED))
            .extracting(Connection::getId)
            .containsExactly(taggedConnection.getId());
        assertThat(remoteConnectionServiceClient.getConnections(null, null, null, 2L, PlatformType.EMBEDDED))
            .extracting(Connection::getName)
            .containsExactly("Tagged", "Untagged");
        assertThat(remoteConnectionServiceClient.getConnections("airtable", 1, null, null, PlatformType.EMBEDDED))
            .hasSize(3);
    }

    @Test
    void testGetConnectionsByIdsReturnsOnlyThoseConnections() {
        Connection firstConnection = createConnection("First", "airtable", PlatformType.AUTOMATION, 0);
        Connection secondConnection = createConnection("Second", "airtable", PlatformType.AUTOMATION, 0);

        createConnection("Third", "airtable", PlatformType.AUTOMATION, 0);

        assertThat(
            remoteConnectionServiceClient.getConnections(List.of(firstConnection.getId(), secondConnection.getId())))
                .extracting(Connection::getId)
                .containsExactlyInAnyOrder(firstConnection.getId(), secondConnection.getId());
    }

    @Test
    void testUpdateTagIdsReplacesTheTags() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm"), new Tag("sales")));
        Connection connection = createConnection("Tagged", "airtable", PlatformType.AUTOMATION, 0);

        List<Long> tagIds = tags.stream()
            .map(Tag::getId)
            .toList();

        Connection updatedConnection = remoteConnectionServiceClient.update(connection.getId(), tagIds);

        assertThat(updatedConnection.getTagIds()).containsExactlyInAnyOrderElementsOf(tagIds);
        assertThat(connectionService.getConnection(connection.getId())
            .getTagIds()).containsExactlyInAnyOrderElementsOf(tagIds);
    }

    @Test
    void testUpdateRenamesAndReplacesTags() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Original", "airtable", PlatformType.AUTOMATION, 0);
        Long tagId = tags.getFirst()
            .getId();

        Connection updatedConnection = remoteConnectionServiceClient.update(
            connection.getId(), "Renamed", List.of(tagId), connection.getVersion());

        assertThat(updatedConnection.getName()).isEqualTo("Renamed");

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getName()).isEqualTo("Renamed");
        assertThat(storedConnection.getTagIds()).containsExactly(tagId);
        assertThat(storedConnection.getVersion()).isEqualTo(connection.getVersion() + 1);
    }

    @Test
    void testUpdateWithoutNameOrTagsLeavesThemUnchanged() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Original", "airtable", PlatformType.AUTOMATION, 0);
        Long tagId = tags.getFirst()
            .getId();

        connection = connectionService.update(connection.getId(), List.of(tagId));

        remoteConnectionServiceClient.update(connection.getId(), null, null, connection.getVersion());

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getName()).isEqualTo("Original");
        assertThat(storedConnection.getTagIds()).containsExactly(tagId);
    }

    @Test
    void testUpdateWithEmptyTagsClearsThem() {
        List<Tag> tags = tagService.save(List.of(new Tag("crm")));
        Connection connection = createConnection("Original", "airtable", PlatformType.AUTOMATION, 0);

        connection = connectionService.update(connection.getId(), List.of(tags.getFirst()
            .getId()));

        remoteConnectionServiceClient.update(connection.getId(), "Renamed", List.of(), connection.getVersion());

        assertThat(connectionService.getConnection(connection.getId())
            .getTagIds()).isEmpty();
    }

    @Test
    void testUpdateWithStaleVersionFailsAsAborted() {
        Connection connection = createConnection("Original", "airtable", PlatformType.AUTOMATION, 0);

        remoteConnectionServiceClient.update(connection.getId(), "First rename", null, connection.getVersion());

        assertThatThrownBy(
            () -> remoteConnectionServiceClient.update(
                connection.getId(), "Second rename", null, connection.getVersion()))
                    .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        statusRuntimeException -> assertThat(statusRuntimeException.getStatus()
                            .getCode()).isEqualTo(Status.Code.ABORTED));

        assertThat(connectionService.getConnection(connection.getId())
            .getName()).isEqualTo("First rename");
    }

    @Test
    void testUpdateConnectionCredentialStatusStoresIt() {
        Connection connection = createConnection("Credentials", "airtable", PlatformType.AUTOMATION, 0);

        Connection updatedConnection = remoteConnectionServiceClient.updateConnectionCredentialStatus(
            connection.getId(), Connection.CredentialStatus.INVALID);

        assertThat(updatedConnection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
        assertThat(connectionService.getConnection(connection.getId())
            .getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
    }

    @Test
    void testUpdateConnectionStatusStoresIt() {
        Connection connection = createConnection("Status", "airtable", PlatformType.AUTOMATION, 0);

        Connection updatedConnection = remoteConnectionServiceClient.updateConnectionStatus(
            connection.getId(), ConnectionStatus.REVOKED);

        assertThat(updatedConnection.getStatus()).isEqualTo(ConnectionStatus.REVOKED);
        assertThat(connectionService.getConnection(connection.getId())
            .getStatus()).isEqualTo(ConnectionStatus.REVOKED);
    }

    @Test
    void testUpdateConnectionStatusRejectsAnInvalidTransition() {
        Connection connection = createConnection("Status", "airtable", PlatformType.AUTOMATION, 0);

        connectionService.updateConnectionStatus(connection.getId(), ConnectionStatus.REVOKED);

        assertThatThrownBy(
            () -> remoteConnectionServiceClient.updateConnectionStatus(connection.getId(), ConnectionStatus.ACTIVE))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Cannot transition connection status from REVOKED to ACTIVE");

        assertThat(connectionService.getConnection(connection.getId())
            .getStatus()).isEqualTo(ConnectionStatus.REVOKED);
    }

    @Test
    void testUpdateCreatedByHandsTheConnectionToTheConnectedUser() {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.BEARER_TOKEN)
                .componentName("httpClient")
                .connectionVersion(1)
                .name("provisioned")
                .parameters(Map.of("token", "secret"))
                .type(PlatformType.EMBEDDED)
                .build());

        assertThat(connection.getCreatedBy()).isEqualTo("system");

        Connection updatedConnection = remoteConnectionServiceClient.updateCreatedBy(
            connection.getId(), "external-user");

        assertThat(updatedConnection.getId()).isEqualTo(connection.getId());
        assertThat(updatedConnection.getCreatedBy()).isEqualTo("external-user");

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getCreatedBy()).isEqualTo("external-user");
        assertThat(storedConnection.getName()).isEqualTo("provisioned");
        assertThat(storedConnection.getParameters()).isEqualTo(Map.of("token", "secret"));
    }

    @Test
    void testUpdateConnectionParametersMergesThem() {
        Connection connection = createConnection("Parameters", "airtable", PlatformType.AUTOMATION, 0);

        Connection updatedConnection = remoteConnectionServiceClient.updateConnectionParameters(
            connection.getId(), Map.of("token", "rotated", "expires_in", 3600));

        Map<String, Object> expectedParameters = Map.of("key", "secret", "token", "rotated", "expires_in", 3600);

        assertThat(updatedConnection.getParameters()).isEqualTo(expectedParameters);
        assertThat(connectionService.getConnection(connection.getId())
            .getParameters()).isEqualTo(expectedParameters);
    }

    @Test
    void testReplaceConnectionParametersDropsTheOldOnes() {
        Connection connection = createConnection("Parameters", "airtable", PlatformType.AUTOMATION, 0);

        Connection updatedConnection = remoteConnectionServiceClient.replaceConnectionParameters(
            connection.getId(), Map.of("token", "only"));

        assertThat(updatedConnection.getParameters()).isEqualTo(Map.of("token", "only"));
        assertThat(connectionService.getConnection(connection.getId())
            .getParameters()).isEqualTo(Map.of("token", "only"));
    }

    private Connection createConnection(String name, String componentName, PlatformType type, int environmentId) {
        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName(componentName)
            .connectionVersion(1)
            .name(name)
            .parameters(Map.of("key", "secret"))
            .type(type)
            .build();

        connection.setEnvironmentId(environmentId);

        return connectionService.create(connection);
    }
}
