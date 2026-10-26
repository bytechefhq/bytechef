/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.grpc;

import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ConnectionRefreshRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ConnectionRefreshSchedulerRemoteServiceGrpc;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionRefreshSchedulerRemoteServiceGrpcServer
    extends ConnectionRefreshSchedulerRemoteServiceGrpc.ConnectionRefreshSchedulerRemoteServiceImplBase {

    private final ConnectionRefreshScheduler connectionRefreshScheduler;

    @SuppressFBWarnings("EI")
    public ConnectionRefreshSchedulerRemoteServiceGrpcServer(ConnectionRefreshScheduler connectionRefreshScheduler) {
        this.connectionRefreshScheduler = connectionRefreshScheduler;
    }

    @Override
    public void cancelConnectionRefresh(ConnectionRefreshRequest request, StreamObserver<Empty> responseObserver) {
        connectionRefreshScheduler.cancelConnectionRefresh(
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasTenantId() ? request.getTenantId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void scheduleConnectionRefresh(ConnectionRefreshRequest request, StreamObserver<Empty> responseObserver) {
        connectionRefreshScheduler.scheduleConnectionRefresh(
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasTokenExpirationTime()
                ? SchedulerProtoTimestamps.toInstant(request.getTokenExpirationTime()) : null,
            request.hasTenantId() ? request.getTenantId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
