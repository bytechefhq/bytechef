/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentVersionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ExecuteWorkflowNodeDescriptionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetTriggerDefinitionRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookTriggerFlags;
import com.bytechef.ee.platform.component.remote.grpc.proto.StringResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerDefinitionRemoteServiceGrpcServer
    extends TriggerDefinitionRemoteServiceGrpc.TriggerDefinitionRemoteServiceImplBase {

    private final ComponentProtoMapper componentProtoMapper;
    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionRemoteServiceGrpcServer(
        TriggerDefinitionService triggerDefinitionService, ComponentProtoMapper componentProtoMapper) {

        this.componentProtoMapper = componentProtoMapper;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public void executeWorkflowNodeDescription(
        ExecuteWorkflowNodeDescriptionRequest request, StreamObserver<StringResponse> responseObserver) {

        String workflowNodeDescription = triggerDefinitionService.executeWorkflowNodeDescription(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getInputParameters()));

        StringResponse.Builder builder = StringResponse.newBuilder();

        if (workflowNodeDescription != null) {
            builder.setValue(workflowNodeDescription);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTriggerDefinition(
        GetTriggerDefinitionRequest request, StreamObserver<RemoteTriggerDefinition> responseObserver) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName());

        responseObserver.onNext(componentProtoMapper.toProto(triggerDefinition));
        responseObserver.onCompleted();
    }

    @Override
    public void getTriggerDefinitions(
        ComponentVersionRequest request, StreamObserver<RemoteTriggerDefinitions> responseObserver) {

        RemoteTriggerDefinitions.Builder builder = RemoteTriggerDefinitions.newBuilder();

        for (TriggerDefinition triggerDefinition : triggerDefinitionService.getTriggerDefinitions(
            request.getComponentName(), request.getComponentVersion())) {

            builder.addTriggerDefinitions(componentProtoMapper.toProto(triggerDefinition));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWebhookTriggerFlags(
        GetTriggerDefinitionRequest request, StreamObserver<RemoteWebhookTriggerFlags> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toProto(
                triggerDefinitionService.getWebhookTriggerFlags(
                    request.getComponentName(), request.getComponentVersion(), request.getTriggerName())));
        responseObserver.onCompleted();
    }

    @Override
    public void isDynamicOutputDefined(
        GetTriggerDefinitionRequest request, StreamObserver<BoolResponse> responseObserver) {

        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(
                    triggerDefinitionService.isDynamicOutputDefined(
                        request.getComponentName(), request.getComponentVersion(), request.getTriggerName()))
                .build());
        responseObserver.onCompleted();
    }
}
