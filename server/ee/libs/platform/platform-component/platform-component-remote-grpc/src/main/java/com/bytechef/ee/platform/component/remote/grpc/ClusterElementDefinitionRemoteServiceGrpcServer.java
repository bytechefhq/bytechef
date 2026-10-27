/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteWorkflowNodeDescriptionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetClusterElementTypeRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteClusterElementType;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ClusterElementDefinitionRemoteServiceGrpcServer
    extends ClusterElementDefinitionRemoteServiceGrpc.ClusterElementDefinitionRemoteServiceImplBase {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentProtoMapper componentProtoMapper;

    public ClusterElementDefinitionRemoteServiceGrpcServer(
        ClusterElementDefinitionService clusterElementDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void executeWorkflowNodeDescription(
        ClusterElementExecuteWorkflowNodeDescriptionRequest request, StreamObserver<StringResponse> responseObserver) {

        String workflowNodeDescription = clusterElementDefinitionService.executeWorkflowNodeDescription(
            request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
            componentProtoMapper.toMap(request.getInputParameters()));

        StringResponse.Builder builder = StringResponse.newBuilder();

        if (workflowNodeDescription != null) {
            builder.setValue(workflowNodeDescription);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getClusterElementDefinition(
        GetClusterElementDefinitionRequest request, StreamObserver<RemoteClusterElementDefinition> responseObserver) {

        ClusterElementDefinition clusterElementDefinition;

        if (!request.hasComponentVersion()) {
            clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
                request.getComponentName(), request.getClusterElementName());
        } else if (request.hasClusterElementTypeName()) {
            clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
                request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
                request.getClusterElementTypeName());
        } else {
            clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
                request.getComponentName(), request.getComponentVersion(), request.getClusterElementName());
        }

        responseObserver.onNext(componentProtoMapper.toProto(clusterElementDefinition));
        responseObserver.onCompleted();
    }

    @Override
    public void getClusterElementDefinitions(
        GetClusterElementDefinitionsRequest request, StreamObserver<RemoteClusterElementDefinitions> responseObserver) {

        ClusterElementType clusterElementType = componentProtoMapper.toClusterElementType(
            request.getClusterElementType());
        List<ClusterElementDefinition> clusterElementDefinitions;

        if (request.hasComponentName()) {
            clusterElementDefinitions = clusterElementDefinitionService.getClusterElementDefinitions(
                request.getComponentName(), request.getComponentVersion(), clusterElementType);
        } else {
            clusterElementDefinitions = clusterElementDefinitionService.getClusterElementDefinitions(
                clusterElementType);
        }

        RemoteClusterElementDefinitions.Builder builder = RemoteClusterElementDefinitions.newBuilder();

        for (ClusterElementDefinition clusterElementDefinition : clusterElementDefinitions) {
            builder.addClusterElementDefinitions(componentProtoMapper.toProto(clusterElementDefinition));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getClusterElementType(
        GetClusterElementTypeRequest request, StreamObserver<RemoteClusterElementType> responseObserver) {

        ClusterElementType clusterElementType = clusterElementDefinitionService.getClusterElementType(
            request.getRootComponentName(), request.getRootComponentVersion(), request.getClusterElementTypeName());

        responseObserver.onNext(componentProtoMapper.toProto(clusterElementType));
        responseObserver.onCompleted();
    }
}
