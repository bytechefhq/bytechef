/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import io.grpc.BindableService;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;

/**
 * Starts an in-process server and a channel to it for tests.
 *
 * @version ee
 */
public final class InProcessGrpc implements AutoCloseable {

    private final ManagedChannel channel;
    private final Server server;

    public InProcessGrpc(
        BindableService bindableService, List<ServerInterceptor> serverInterceptors,
        List<ClientInterceptor> clientInterceptors) throws IOException {

        String name = InProcessServerBuilder.generateName();

        this.server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(ServerInterceptors.intercept(bindableService, serverInterceptors))
            .build()
            .start();
        this.channel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .intercept(clientInterceptors)
            .build();
    }

    public ManagedChannel channel() {
        return channel;
    }

    @Override
    public void close() {
        channel.shutdownNow();
        server.shutdownNow();
    }
}
