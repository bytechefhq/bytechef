/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client;

import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

/**
 * Serves the given gRPC services in process behind the exception handler the microservices register, and hands out a
 * channel to them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmbeddedConfigurationGrpcTestServer {

    private final ManagedChannel managedChannel;
    private final Server server;

    private EmbeddedConfigurationGrpcTestServer(ManagedChannel managedChannel, Server server) {
        this.managedChannel = managedChannel;
        this.server = server;
    }

    public static EmbeddedConfigurationGrpcTestServer start(BindableService... bindableServices) throws IOException {
        String name = InProcessServerBuilder.generateName();
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(name)
            .directExecutor();

        for (BindableService bindableService : bindableServices) {
            inProcessServerBuilder.addService(ServerInterceptors.intercept(bindableService, serverInterceptor));
        }

        Server server = inProcessServerBuilder.build()
            .start();

        ManagedChannel managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        return new EmbeddedConfigurationGrpcTestServer(managedChannel, server);
    }

    public GrpcChannels getGrpcChannels() {
        return new FixedGrpcChannels(managedChannel);
    }

    public ManagedChannel getManagedChannel() {
        return managedChannel;
    }

    public void shutdown() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }
}
