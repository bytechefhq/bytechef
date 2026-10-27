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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Starts an in-process server and a channel to it for tests. Server calls run on their own thread, as they do over a
 * real transport, so anything thread-bound must cross the wire to reach them.
 *
 * @version ee
 */
public final class InProcessGrpc implements AutoCloseable {

    private final ManagedChannel channel;
    private final Server server;
    private final ExecutorService serverExecutorService = Executors.newSingleThreadExecutor();

    public InProcessGrpc(
        BindableService bindableService, List<ServerInterceptor> serverInterceptors,
        List<ClientInterceptor> clientInterceptors) throws IOException {

        String name = InProcessServerBuilder.generateName();

        this.server = InProcessServerBuilder.forName(name)
            .executor(serverExecutorService)
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

    /**
     * The single thread every server call runs on, so tests can inspect what a call leaves behind on it.
     */
    public ExecutorService serverExecutorService() {
        return serverExecutorService;
    }

    @Override
    public void close() {
        channel.shutdownNow();
        server.shutdownNow();
        serverExecutorService.shutdownNow();
    }
}
