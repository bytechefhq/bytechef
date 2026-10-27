/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetUnifiedApiComponentDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.UnifiedApiDefinitionRemoteServiceGrpc;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class UnifiedApiDefinitionRemoteServiceGrpcServer
    extends UnifiedApiDefinitionRemoteServiceGrpc.UnifiedApiDefinitionRemoteServiceImplBase {

    private final ComponentProtoMapper componentProtoMapper;
    private final UnifiedApiDefinitionService unifiedApiDefinitionService;

    public UnifiedApiDefinitionRemoteServiceGrpcServer(
        UnifiedApiDefinitionService unifiedApiDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.componentProtoMapper = componentProtoMapper;
        this.unifiedApiDefinitionService = unifiedApiDefinitionService;
    }

    @Override
    public void getUnifiedApiComponentDefinitions(
        GetUnifiedApiComponentDefinitionsRequest request, StreamObserver<RemoteComponentDefinitions> responseObserver) {

        List<ComponentDefinition> componentDefinitions = unifiedApiDefinitionService.getUnifiedApiComponentDefinitions(
            request.hasCategory() ? UnifiedApiCategory.valueOf(request.getCategory()) : null);

        RemoteComponentDefinitions.Builder builder = RemoteComponentDefinitions.newBuilder();

        for (ComponentDefinition componentDefinition : componentDefinitions) {
            builder.addComponentDefinitions(componentProtoMapper.toProto(componentDefinition));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
