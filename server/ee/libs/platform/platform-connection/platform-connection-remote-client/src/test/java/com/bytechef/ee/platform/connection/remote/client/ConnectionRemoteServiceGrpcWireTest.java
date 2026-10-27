/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.client.fasade.RemoteConnectionFacadeClient;
import com.bytechef.ee.platform.connection.remote.client.service.RemoteConnectionServiceClient;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectionRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final Map<String, Object> PARAMETERS = Map.of("key", "secret", "count", 5);

    private final ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);

    private ManagedChannel managedChannel;
    private RemoteConnectionFacadeClient remoteConnectionFacadeClient;
    private RemoteConnectionServiceClient remoteConnectionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        ConnectionProtoMapper connectionProtoMapper = new ConnectionProtoMapper(JSON_MAPPER);
        GrpcExceptionHandlerInterceptor grpcExceptionHandlerInterceptor = new GrpcExceptionHandlerInterceptor(
            new GrpcRemoteExceptionHandler());

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionRemoteServiceGrpcServer(connectionService, connectionProtoMapper),
                    grpcExceptionHandlerInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionFacadeRemoteServiceGrpcServer(connectionFacade, connectionProtoMapper),
                    grpcExceptionHandlerInterceptor))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteConnectionFacadeClient = new RemoteConnectionFacadeClient(fixedGrpcChannels, JSON_MAPPER);
        remoteConnectionServiceClient = new RemoteConnectionServiceClient(fixedGrpcChannels, JSON_MAPPER);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testServiceCreateAndRead() {
        Connection connection = connection(7L);

        when(connectionService.create(argThat(curConnection -> "airtable".equals(curConnection.getComponentName()))))
            .thenReturn(connection);
        when(connectionService.create(
            AuthorizationType.API_KEY, "airtable", 1, 2, "My Airtable", PARAMETERS, PlatformType.AUTOMATION))
                .thenReturn(connection);
        when(connectionService.getConnection(7L)).thenReturn(connection);
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection));
        when(connectionService.fetchConnection(8L)).thenReturn(Optional.empty());

        assertThat(remoteConnectionServiceClient.create(connection)).usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.create(
            AuthorizationType.API_KEY, "airtable", 1, 2, "My Airtable", PARAMETERS, PlatformType.AUTOMATION))
                .usingRecursiveComparison()
                .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.getConnection(7L)).usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.fetchConnection(7L)).get()
            .usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.fetchConnection(8L)).isEmpty();
    }

    @Test
    void testConnectionParametersKeepNumberTypes() {
        Map<String, Object> parameters = Map.of("big", 9_007_199_254_740_993L, "port", 5432, "ratio", 5.0);

        Connection connection = connection(7L);

        connection.setParameters(parameters);

        when(connectionService.getConnection(7L)).thenReturn(connection);

        Map<String, ?> readParameters = remoteConnectionServiceClient.getConnection(7L)
            .getParameters();

        assertThat(readParameters).isEqualTo(parameters);
        assertThat(readParameters.get("big")).isInstanceOf(Long.class);
        assertThat(readParameters.get("port")).isInstanceOf(Integer.class);
        assertThat(readParameters.get("ratio")).isInstanceOf(Double.class);
    }

    @Test
    void testServiceListCalls() {
        List<Connection> connections = List.of(connection(7L), connection(8L));

        when(connectionService.getConnections(PlatformType.AUTOMATION)).thenReturn(connections);
        when(connectionService.getConnections("airtable", 1, PlatformType.AUTOMATION)).thenReturn(connections);
        when(connectionService.getConnections("airtable", null, null, 2L, PlatformType.EMBEDDED))
            .thenReturn(connections);
        when(connectionService.getConnections(List.of(7L, 8L))).thenReturn(connections);

        assertThat(remoteConnectionServiceClient.getConnections(PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(connections);
        assertThat(remoteConnectionServiceClient.getConnections("airtable", 1, PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(connections);
        assertThat(remoteConnectionServiceClient.getConnections("airtable", null, null, 2L, PlatformType.EMBEDDED))
            .usingRecursiveComparison()
            .isEqualTo(connections);
        assertThat(remoteConnectionServiceClient.getConnections(List.of(7L, 8L)))
            .usingRecursiveComparison()
            .isEqualTo(connections);
    }

    @Test
    void testServiceUpdates() {
        Connection connection = connection(7L);

        when(connectionService.update(7L, List.of(1L, 2L))).thenReturn(connection);
        when(connectionService.update(7L, "Renamed", List.of(1L), 3)).thenReturn(connection);
        when(connectionService.updateConnectionCredentialStatus(7L, Connection.CredentialStatus.INVALID))
            .thenReturn(connection);
        when(connectionService.updateConnectionStatus(7L, ConnectionStatus.REVOKED)).thenReturn(connection);
        when(connectionService.updateConnectionParameters(7L, PARAMETERS)).thenReturn(connection);
        when(connectionService.replaceConnectionParameters(7L, PARAMETERS)).thenReturn(connection);

        assertThat(remoteConnectionServiceClient.update(7L, List.of(1L, 2L))).usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.update(7L, "Renamed", List.of(1L), 3)).usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(
            remoteConnectionServiceClient.updateConnectionCredentialStatus(7L, Connection.CredentialStatus.INVALID))
                .usingRecursiveComparison()
                .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.updateConnectionStatus(7L, ConnectionStatus.REVOKED))
            .usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.updateConnectionParameters(7L, PARAMETERS))
            .usingRecursiveComparison()
            .isEqualTo(connection);
        assertThat(remoteConnectionServiceClient.replaceConnectionParameters(7L, PARAMETERS))
            .usingRecursiveComparison()
            .isEqualTo(connection);

        remoteConnectionServiceClient.delete(7L);

        verify(connectionService).delete(7L);
    }

    @Test
    void testUpdateWithoutNameOrTagsLeavesThemUnchanged() {
        Connection connection = connection(7L);

        when(connectionService.update(7L, null, null, 3)).thenReturn(connection);

        assertThat(remoteConnectionServiceClient.update(7L, null, null, 3)).usingRecursiveComparison()
            .isEqualTo(connection);

        remoteConnectionFacadeClient.update(7L, null, null, 3);

        verify(connectionFacade).update(7L, null, null, 3);
    }

    @Test
    void testUpdateWithEmptyTagsClearsThem() {
        Connection connection = connection(7L);

        when(connectionService.update(7L, "Renamed", List.of(), 3)).thenReturn(connection);

        assertThat(remoteConnectionServiceClient.update(7L, "Renamed", List.of(), 3)).usingRecursiveComparison()
            .isEqualTo(connection);

        remoteConnectionFacadeClient.update(7L, "Renamed", List.of(), 3);

        verify(connectionFacade).update(7L, "Renamed", List.of(), 3);
    }

    @Test
    void testGetConnectionOfUnknownIdFailsWithItsMessage() {
        when(connectionService.getConnection(9L)).thenThrow(new IllegalArgumentException("Connection 9 not found"));

        assertThatThrownBy(() -> remoteConnectionServiceClient.getConnection(9L))
            .hasMessageContaining("Connection 9 not found");
    }

    @Test
    void testCreateAndListConnectionWithoutTags() {
        ConnectionDTO connectionDTO = connectionDTO(List.of());

        when(connectionFacade.create(
            argThat(curConnectionDTO -> "My Airtable".equals(curConnectionDTO.name())), eq(PlatformType.AUTOMATION)))
                .thenReturn(7L);
        when(connectionFacade.getConnections("airtable", null, null, null, null, PlatformType.AUTOMATION))
            .thenReturn(List.of(connectionDTO));

        assertThat(remoteConnectionFacadeClient.create(connectionDTO, PlatformType.AUTOMATION)).isEqualTo(7L);

        List<ConnectionDTO> connectionDTOs = remoteConnectionFacadeClient.getConnections(
            "airtable", null, null, null, null, PlatformType.AUTOMATION);

        assertThat(connectionDTOs).singleElement()
            .satisfies(curConnectionDTO -> assertThat(curConnectionDTO.tags()).isEmpty());
    }

    @Test
    void testFacadeReadCalls() {
        ConnectionDTO connectionDTO = connectionDTO(List.of(tag()));
        List<Connection> connections = List.of(connection(7L));

        when(connectionFacade.getConnection(7L)).thenReturn(connectionDTO);
        when(connectionFacade.getConnections(List.of(7L), PlatformType.AUTOMATION)).thenReturn(List.of(connectionDTO));
        when(connectionFacade.getConnections(null, PlatformType.AUTOMATION)).thenReturn(List.of(connectionDTO));
        when(connectionFacade.getConnections("airtable", 1, List.of(7L), 4L, 2L, PlatformType.EMBEDDED))
            .thenReturn(List.of(connectionDTO));
        when(connectionFacade.toConnectionDTOs(
            argThat(curConnections -> curConnections.size() == 1 && curConnections.getFirst()
                .getId() == 7L)))
                    .thenReturn(List.of(connectionDTO));
        when(connectionFacade.getConnectionTags(PlatformType.AUTOMATION)).thenReturn(List.of(tag()));
        when(connectionFacade.executeConnectionRefresh(7L)).thenReturn(3);

        assertThat(remoteConnectionFacadeClient.getConnection(7L)).usingRecursiveComparison()
            .isEqualTo(connectionDTO);
        assertThat(remoteConnectionFacadeClient.getConnections(List.of(7L), PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(List.of(connectionDTO));
        assertThat(remoteConnectionFacadeClient.getConnections(null, PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(List.of(connectionDTO));
        assertThat(remoteConnectionFacadeClient.getConnections("airtable", 1, List.of(7L), 4L, 2L,
            PlatformType.EMBEDDED))
                .usingRecursiveComparison()
                .isEqualTo(List.of(connectionDTO));
        assertThat(remoteConnectionFacadeClient.toConnectionDTOs(connections)).usingRecursiveComparison()
            .isEqualTo(List.of(connectionDTO));
        assertThat(remoteConnectionFacadeClient.getConnectionTags(PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(List.of(tag()));
        assertThat(remoteConnectionFacadeClient.executeConnectionRefresh(7L)).isEqualTo(3);
    }

    @Test
    void testFacadeWriteCalls() {
        remoteConnectionFacadeClient.update(7L, List.of(tag()));
        remoteConnectionFacadeClient.update(7L, "Renamed", List.of(tag()), 3);
        remoteConnectionFacadeClient.replaceAuthorizationParameters(7L, PARAMETERS);
        remoteConnectionFacadeClient.delete(7L);

        verify(connectionFacade).update(eq(7L), argThat(tags -> tags.size() == 1 && "crm".equals(tags.getFirst()
            .getName())));
        verify(connectionFacade).update(eq(7L), eq("Renamed"), argThat(tags -> tags.size() == 1), eq(3));
        verify(connectionFacade).replaceAuthorizationParameters(7L, PARAMETERS);
        verify(connectionFacade).delete(7L);
    }

    private static Connection connection(long id) {
        return JSON_MAPPER.readValue(
            """
                {"authorizationType":"API_KEY","componentName":"airtable","connectionVersion":1,
                "createdBy":"admin@localhost.com","createdDate":"2026-10-26T10:00:00Z","credentialStatus":"VALID",
                "environmentId":2,"id":CONNECTION_ID,"name":"My Airtable","parameters":{"key":"secret","count":5},
                "status":"ACTIVE","tagIds":[1],"type":"AUTOMATION","version":3}
                """.replace("CONNECTION_ID", String.valueOf(id)),
            Connection.class);
    }

    private static ConnectionDTO connectionDTO(List<Tag> tags) {
        return new ConnectionDTO(
            true, AuthorizationType.API_KEY, Map.of("token", "abc"), "https://api.airtable.com", "airtable",
            Map.of("key", "secret"), 1, "admin@localhost.com", Instant.parse("2026-10-26T10:00:00Z"),
            Connection.CredentialStatus.VALID, 2, 7L, "user@localhost.com", Instant.parse("2026-10-26T11:00:00Z"),
            "My Airtable", PARAMETERS, ConnectionStatus.ACTIVE, tags, 3);
    }

    private static Tag tag() {
        return new Tag(4L, "crm");
    }
}
