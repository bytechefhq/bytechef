/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionListProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionRemoteServiceGrpc;
import com.bytechef.ee.platform.connection.remote.grpc.GetConnectionRequest;
import com.bytechef.ee.platform.connection.remote.grpc.GetConnectionsRequest;
import com.bytechef.ee.platform.connection.remote.grpc.PlatformTypeProto;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.InternalServiceTokenClientInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.mapper.ConnectionProtoMapper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannelBuilder;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC counterpart of {@link RemoteConnectionServiceClient}. Active only when {@code bytechef.remote.transport=grpc};
 * otherwise the REST client is used. Delegates the two read methods to the generated blocking stub over a
 * static-address channel carrying the internal service token + current tenant via client interceptors, and maps proto
 * results back to the domain. Write methods are not part of the remote surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(name = "bytechef.remote.transport", havingValue = "grpc")
public final class GrpcConnectionServiceClient implements ConnectionService {

    private final ConnectionRemoteServiceGrpc.ConnectionRemoteServiceBlockingStub blockingStub;

    // CT_CONSTRUCTOR_THROW: the class is final and the fail-fast validation guards a security
    // invariant (no plaintext to a non-loopback target), so the partially-constructed-object concern
    // does not apply.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public GrpcConnectionServiceClient(
        @Value("${bytechef.grpc.connection-app.address:}") String address,
        @Value("${bytechef.internal.service-token:}") String serviceToken,
        @Value("${bytechef.grpc.plaintext-enabled:false}") boolean plaintextEnabled) {

        // Secure by default: the internal service token is a bearer credential, so TLS is required
        // unless plaintext is explicitly enabled (local development against a loopback target only).
        // round_robin distributes calls across all addresses a discovery:/// target resolves to.
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(address)
            .defaultLoadBalancingPolicy("round_robin");

        if (plaintextEnabled) {
            if (!isLoopbackTarget(address)) {
                throw new IllegalStateException(
                    "bytechef.grpc.plaintext-enabled is only permitted for a loopback target, not: " + address);
            }

            channelBuilder.usePlaintext();
        } else {
            channelBuilder.useTransportSecurity();
        }

        blockingStub = ConnectionRemoteServiceGrpc.newBlockingStub(
            channelBuilder
                .intercept(
                    new TenantContextClientInterceptor(),
                    new InternalServiceTokenClientInterceptor(serviceToken))
                .build());
    }

    private static boolean isLoopbackTarget(String address) {
        String host = address;

        int schemeIndex = host.indexOf("://");

        if (schemeIndex >= 0) {
            host = host.substring(schemeIndex + 3);
        }

        int portIndex = host.lastIndexOf(':');

        if (portIndex >= 0 && host.indexOf(']') < portIndex) {
            host = host.substring(0, portIndex);
        }

        host = host.replace("[", "")
            .replace("]", "");

        try {
            return InetAddress.getByName(host)
                .isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    @Override
    public Connection getConnection(long id) {
        ConnectionProto proto = blockingStub.getConnection(
            GetConnectionRequest.newBuilder()
                .setId(id)
                .build());

        return ConnectionProtoMapper.toDomain(proto);
    }

    @Override
    public Optional<Connection> fetchConnection(long id) {
        // The gRPC contract has no not-found variant, so absence cannot be distinguished here. Callers on this
        // path address ids the coordinator already resolved.
        return Optional.ofNullable(getConnection(id));
    }

    @Override
    public List<Connection> getConnections(PlatformType type) {
        ConnectionListProto list = blockingStub.getConnections(
            GetConnectionsRequest.newBuilder()
                .setType(toPlatformTypeProto(type))
                .build());

        return list.getConnectionsList()
            .stream()
            .map(ConnectionProtoMapper::toDomain)
            .toList();
    }

    @Override
    public Connection create(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection create(
        AuthorizationType authorizationType, String componentName, int connectionVersion, int environmentId,
        String name, Map<String, Object> parameters, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnectionsByVisibility(ResourceVisibility visibility, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long typeId, Long environmentId, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getAiProviderConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Connection registerExisting(Connection connection, CredentialStoreType storeType, String credentialRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, Connection.CredentialStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateCreatedBy(long id, String newCreatedBy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateVisibility(long id, ResourceVisibility visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getInactiveConnections(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateConnectionsActive(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    private static PlatformTypeProto toPlatformTypeProto(@Nullable PlatformType type) {
        if (type == null) {
            return PlatformTypeProto.PLATFORM_TYPE_UNSPECIFIED;
        }

        return PlatformTypeProto.valueOf(type.name());
    }
}
