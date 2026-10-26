/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentVersionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetActionDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteActionDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteActionDefinitions;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ActionDefinitionRemoteServiceGrpcServer
    extends ActionDefinitionRemoteServiceGrpc.ActionDefinitionRemoteServiceImplBase {

    private final ActionDefinitionService actionDefinitionService;
    private final ComponentProtoMapper componentProtoMapper;

    public ActionDefinitionRemoteServiceGrpcServer(
        ActionDefinitionService actionDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.actionDefinitionService = actionDefinitionService;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void getActionDefinition(
        GetActionDefinitionRequest request, StreamObserver<RemoteActionDefinition> responseObserver) {

        ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
            request.getComponentName(), request.getComponentVersion(), request.getActionName());

        responseObserver.onNext(componentProtoMapper.toProto(actionDefinition));
        responseObserver.onCompleted();
    }

    @Override
    public void getActionDefinitions(
        ComponentVersionRequest request, StreamObserver<RemoteActionDefinitions> responseObserver) {

        RemoteActionDefinitions.Builder builder = RemoteActionDefinitions.newBuilder();

        for (ActionDefinition actionDefinition : actionDefinitionService.getActionDefinitions(
            request.getComponentName(), request.getComponentVersion())) {

            builder.addActionDefinitions(componentProtoMapper.toProto(actionDefinition));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
