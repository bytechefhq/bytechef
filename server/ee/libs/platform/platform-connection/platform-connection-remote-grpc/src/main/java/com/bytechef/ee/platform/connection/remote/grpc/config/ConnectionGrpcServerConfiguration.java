/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.config;

import com.bytechef.ee.platform.connection.remote.grpc.interceptor.InternalServiceTokenServerInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.TenantContextServerInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.server.ConnectionRemoteServiceGrpcServer;
import com.bytechef.platform.connection.service.ConnectionService;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Connection gRPC service, wrapped with the auth + tenant server interceptors, as a Spring
 * {@link BindableService} bean so the spring-grpc server auto-configuration picks it up and serves it. Active only when
 * {@code bytechef.remote.transport=grpc}; the spring-grpc server itself is additionally gated by
 * {@code spring.grpc.server.enabled} so it stays dormant by default.
 *
 * <p>
 * The interceptors are bound directly to the service definition (rather than relying on a global-interceptor mechanism)
 * so ordering and applicability are explicit and framework-version independent.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.remote.transport", havingValue = "grpc")
public class ConnectionGrpcServerConfiguration {

    @Bean
    BindableService connectionRemoteBindableService(
        ConnectionService connectionService,
        @Value("${bytechef.internal.service-token:}") String serviceToken) {

        ServerServiceDefinition serviceDefinition = ServerInterceptors.intercept(
            new ConnectionRemoteServiceGrpcServer(connectionService),
            new TenantContextServerInterceptor(),
            new InternalServiceTokenServerInterceptor(serviceToken));

        return () -> serviceDefinition;
    }
}
