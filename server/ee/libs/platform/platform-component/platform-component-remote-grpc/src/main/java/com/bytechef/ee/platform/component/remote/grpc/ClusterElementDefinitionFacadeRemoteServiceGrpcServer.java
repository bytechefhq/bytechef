/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ClusterElementExecuteToolRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.domain.OutputResponse;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ClusterElementDefinitionFacadeRemoteServiceGrpcServer
    extends ClusterElementDefinitionFacadeRemoteServiceGrpc.ClusterElementDefinitionFacadeRemoteServiceImplBase {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ComponentProtoMapper componentProtoMapper;

    public ClusterElementDefinitionFacadeRemoteServiceGrpcServer(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade, ComponentProtoMapper componentProtoMapper) {

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void executeDynamicProperties(
        ClusterElementExecuteDynamicPropertiesRequest request, StreamObserver<RemoteProperties> responseObserver) {

        List<Property> properties;

        if (request.getClusterElements()) {
            properties = clusterElementDefinitionFacade.executeDynamicProperties(
                request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
                request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                request.hasExtensions() ? componentProtoMapper.toMap(request.getExtensions()) : null,
                List.copyOf(request.getLookupDependsOnPathsList()),
                request.hasConnectionId() ? request.getConnectionId() : null,
                request.getClusterElementConnectionIdsMap(),
                toClusterElementInputParameters(
                    request.hasClusterElementInputParameters() ? request.getClusterElementInputParameters() : null));
        } else {
            properties = clusterElementDefinitionFacade.executeDynamicProperties(
                request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
                request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                List.copyOf(request.getLookupDependsOnPathsList()),
                request.hasConnectionId() ? request.getConnectionId() : null);
        }

        responseObserver.onNext(componentProtoMapper.toRemoteProperties(properties));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOptions(
        ClusterElementExecuteOptionsRequest request, StreamObserver<RemoteOptions> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toRemoteOptions(
                clusterElementDefinitionFacade.executeOptions(
                    request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
                    request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                    request.hasExtensions() ? componentProtoMapper.toMap(request.getExtensions()) : null,
                    List.copyOf(request.getLookupDependsOnPathsList()),
                    request.hasSearchText() ? request.getSearchText() : null,
                    request.hasConnectionId() ? request.getConnectionId() : null,
                    request.getClusterElementConnectionIdsMap(),
                    toClusterElementInputParameters(
                        request.hasClusterElementInputParameters() ? request.getClusterElementInputParameters()
                            : null))));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOutput(
        ClusterElementExecuteOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        OutputResponse outputResponse = clusterElementDefinitionFacade.executeOutput(
            request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
            componentProtoMapper.toMap(request.getInputParameters()),
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(componentProtoMapper.toOutputResponseResult(outputResponse));
        responseObserver.onCompleted();
    }

    @Override
    public void executeTool(ClusterElementExecuteToolRequest request, StreamObserver<ValueResponse> responseObserver) {
        Long connectionId = request.hasConnectionId() ? request.getConnectionId() : null;
        Object result;

        if (request.hasComponentVersion()) {
            result = clusterElementDefinitionFacade.executeTool(
                request.getComponentName(), request.getComponentVersion(), request.getClusterElementName(),
                componentProtoMapper.toMap(request.getInputParameters()), connectionId);
        } else {
            result = clusterElementDefinitionFacade.executeTool(
                request.getComponentName(), request.getClusterElementName(),
                componentProtoMapper.toMap(request.getInputParameters()), connectionId);
        }

        responseObserver.onNext(
            ValueResponse.newBuilder()
                .setValue(componentProtoMapper.toBytes(result))
                .build());
        responseObserver.onCompleted();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, ?>> toClusterElementInputParameters(@Nullable ByteString bytes) {
        if (bytes == null) {
            return Map.of();
        }

        Map<String, ?> clusterElementInputParameters = componentProtoMapper.toMap(bytes);

        return clusterElementInputParameters == null ? Map.of()
            : (Map<String, Map<String, ?>>) clusterElementInputParameters;
    }
}
