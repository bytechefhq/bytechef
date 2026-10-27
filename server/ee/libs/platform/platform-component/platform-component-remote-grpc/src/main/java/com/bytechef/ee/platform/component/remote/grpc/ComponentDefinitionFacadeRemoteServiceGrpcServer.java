/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ExecuteWorkflowInputOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentDefinitionFacadeRemoteServiceGrpcServer
    extends ComponentDefinitionFacadeRemoteServiceGrpc.ComponentDefinitionFacadeRemoteServiceImplBase {

    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentProtoMapper componentProtoMapper;

    public ComponentDefinitionFacadeRemoteServiceGrpcServer(
        ComponentDefinitionFacade componentDefinitionFacade, ComponentProtoMapper componentProtoMapper) {

        this.componentDefinitionFacade = componentDefinitionFacade;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void executeWorkflowInputOptions(
        ExecuteWorkflowInputOptionsRequest request, StreamObserver<RemoteOptions> responseObserver) {

        Map<String, Object> inputParameters = componentProtoMapper.toMap(request.getInputParameters());

        responseObserver.onNext(
            componentProtoMapper.toRemoteOptions(
                componentDefinitionFacade.executeWorkflowInputOptions(
                    request.getComponentName(), request.getComponentVersion(), request.getGroupName(),
                    request.getPropertyName(), inputParameters == null ? Map.of() : inputParameters,
                    List.copyOf(request.getLookupDependsOnPathsList()),
                    request.hasSearchText() ? request.getSearchText() : null,
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }
}
