/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ConnectionLifecycleFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ConnectionRefreshRequest;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 */
public class ConnectionLifecycleFacadeRemoteServiceGrpcServer
    extends ConnectionLifecycleFacadeRemoteServiceGrpc.ConnectionLifecycleFacadeRemoteServiceImplBase {

    private final ConnectionLifecycleFacade connectionLifecycleFacade;
    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;

    @SuppressFBWarnings("EI")
    public ConnectionLifecycleFacadeRemoteServiceGrpcServer(
        ConnectionLifecycleFacade connectionLifecycleFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.connectionLifecycleFacade = connectionLifecycleFacade;
        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
    }

    @Override
    public void
        deleteScheduledConnectionRefresh(ConnectionRefreshRequest request, StreamObserver<Empty> responseObserver) {
        connectionLifecycleFacade.deleteScheduledConnectionRefresh(
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasTenantId() ? request.getTenantId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void scheduleConnectionRefresh(ConnectionRefreshRequest request, StreamObserver<Empty> responseObserver) {
        connectionLifecycleFacade.scheduleConnectionRefresh(
            request.hasConnectionId() ? request.getConnectionId() : null,
            Objects.requireNonNullElse(platformWorkflowExecutionProtoMapper.toMap(request.getParameters()), Map.of()),
            request.hasTenantId() ? request.getTenantId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
