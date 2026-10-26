/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionVersionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetComponentDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetConnectionComponentDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.SearchComponentDefinitionsRequest;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentDefinitionRemoteServiceGrpcServer
    extends ComponentDefinitionRemoteServiceGrpc.ComponentDefinitionRemoteServiceImplBase {

    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentProtoMapper componentProtoMapper;

    public ComponentDefinitionRemoteServiceGrpcServer(
        ComponentDefinitionService componentDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.componentDefinitionService = componentDefinitionService;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void fetchComponentDefinition(
        GetComponentDefinitionRequest request, StreamObserver<ComponentDefinitionResult> responseObserver) {

        Optional<ComponentDefinition> componentDefinition = componentDefinitionService.fetchComponentDefinition(
            request.getName(), request.hasVersion() ? request.getVersion() : null);

        ComponentDefinitionResult.Builder builder = ComponentDefinitionResult.newBuilder();

        componentDefinition.ifPresent(
            curComponentDefinition -> builder
                .setComponentDefinition(componentProtoMapper.toProto(curComponentDefinition)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getComponentDefinition(
        GetComponentDefinitionRequest request, StreamObserver<RemoteComponentDefinition> responseObserver) {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            request.getName(), request.hasVersion() ? request.getVersion() : null);

        responseObserver.onNext(componentProtoMapper.toProto(componentDefinition));
        responseObserver.onCompleted();
    }

    @Override
    public void getComponentDefinitions(
        GetComponentDefinitionsRequest request, StreamObserver<RemoteComponentDefinitions> responseObserver) {

        List<ComponentDefinition> componentDefinitions;

        if (!request.hasActionDefinitions() && !request.hasClusterElementDefinitions() &&
            !request.hasConnectionDefinitions() && !request.hasTriggerDefinitions() &&
            request.getIncludeCount() == 0) {

            componentDefinitions = componentDefinitionService.getComponentDefinitions();
        } else {
            componentDefinitions = componentDefinitionService.getComponentDefinitions(
                request.hasActionDefinitions() ? request.getActionDefinitions() : null,
                request.hasClusterElementDefinitions() ? request.getClusterElementDefinitions() : null,
                request.hasConnectionDefinitions() ? request.getConnectionDefinitions() : null,
                request.hasTriggerDefinitions() ? request.getTriggerDefinitions() : null,
                request.getIncludeCount() == 0 ? null : List.copyOf(request.getIncludeList()), null);
        }

        responseObserver.onNext(toRemoteComponentDefinitions(componentDefinitions));
        responseObserver.onCompleted();
    }

    @Override
    public void getComponentDefinitionVersions(
        GetComponentDefinitionVersionsRequest request, StreamObserver<RemoteComponentDefinitions> responseObserver) {

        responseObserver.onNext(
            toRemoteComponentDefinitions(componentDefinitionService.getComponentDefinitionVersions(request.getName())));
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectionComponentDefinition(
        GetConnectionComponentDefinitionRequest request, StreamObserver<RemoteComponentDefinition> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toProto(
                componentDefinitionService.getConnectionComponentDefinition(
                    request.getName(), request.getConnectionVersion())));
        responseObserver.onCompleted();
    }

    @Override
    public void searchComponentDefinitions(
        SearchComponentDefinitionsRequest request, StreamObserver<RemoteComponentDefinitions> responseObserver) {

        responseObserver.onNext(
            toRemoteComponentDefinitions(componentDefinitionService.getComponentDefinitions(request.getQuery(), null)));
        responseObserver.onCompleted();
    }

    private RemoteComponentDefinitions toRemoteComponentDefinitions(List<ComponentDefinition> componentDefinitions) {
        RemoteComponentDefinitions.Builder builder = RemoteComponentDefinitions.newBuilder();

        for (ComponentDefinition componentDefinition : componentDefinitions) {
            builder.addComponentDefinitions(componentProtoMapper.toProto(componentDefinition));
        }

        return builder.build();
    }
}
