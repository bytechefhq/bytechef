/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.ComponentConnectionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteComponentConnections;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowNodeRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentConnectionFacadeRemoteServiceGrpcServer
    extends ComponentConnectionFacadeRemoteServiceGrpc.ComponentConnectionFacadeRemoteServiceImplBase {

    private final ComponentConnectionFacade componentConnectionFacade;
    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;

    @SuppressFBWarnings("EI")
    public ComponentConnectionFacadeRemoteServiceGrpcServer(
        ComponentConnectionFacade componentConnectionFacade,
        PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.platformConfigurationProtoMapper = platformConfigurationProtoMapper;
    }

    @Override
    public void getWorkflowTaskComponentConnections(
        WorkflowNodeRequest request, StreamObserver<RemoteComponentConnections> responseObserver) {

        responseObserver.onNext(
            platformConfigurationProtoMapper.toRemoteComponentConnections(
                componentConnectionFacade.getComponentConnections(new WorkflowTask(toNodeMap(request)))));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowTriggerComponentConnections(
        WorkflowNodeRequest request, StreamObserver<RemoteComponentConnections> responseObserver) {

        responseObserver.onNext(
            platformConfigurationProtoMapper.toRemoteComponentConnections(
                componentConnectionFacade.getComponentConnections(new WorkflowTrigger(toNodeMap(request)))));
        responseObserver.onCompleted();
    }

    private Map<String, Object> toNodeMap(WorkflowNodeRequest request) {
        return Objects.requireNonNullElse(platformConfigurationProtoMapper.toMap(request.getNode()), Map.of());
    }
}
