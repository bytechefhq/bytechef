/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.ActionDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ActionExecutePerformRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeRemoteServiceGrpcServer
    extends ActionDefinitionFacadeRemoteServiceGrpc.ActionDefinitionFacadeRemoteServiceImplBase {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ComponentProtoMapper componentProtoMapper;

    public ActionDefinitionFacadeRemoteServiceGrpcServer(
        ActionDefinitionFacade actionDefinitionFacade, ComponentProtoMapper componentProtoMapper) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void executeDynamicProperties(
        ActionExecuteDynamicPropertiesRequest request, StreamObserver<RemoteProperties> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toRemoteProperties(
                actionDefinitionFacade.executeDynamicProperties(
                    request.getComponentName(), request.getComponentVersion(), request.getActionName(),
                    request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                    List.copyOf(request.getLookupDependsOnPathsList()),
                    request.hasWorkflowId() ? request.getWorkflowId() : null,
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOptions(ActionExecuteOptionsRequest request, StreamObserver<RemoteOptions> responseObserver) {
        List<Option> options;

        if (request.getMultipleConnections()) {
            options = actionDefinitionFacade.executeOptions(
                request.getComponentName(), request.getComponentVersion(), request.getActionName(),
                request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                List.copyOf(request.getLookupDependsOnPathsList()),
                request.hasSearchText() ? request.getSearchText() : null, request.getConnectionIdsMap(),
                request.hasExtensions() ? componentProtoMapper.toMap(request.getExtensions()) : null);
        } else {
            options = actionDefinitionFacade.executeOptions(
                request.getComponentName(), request.getComponentVersion(), request.getActionName(),
                request.getPropertyName(), componentProtoMapper.toMap(request.getInputParameters()),
                List.copyOf(request.getLookupDependsOnPathsList()),
                request.hasSearchText() ? request.getSearchText() : null,
                request.hasConnectionId() ? request.getConnectionId() : null);
        }

        responseObserver.onNext(componentProtoMapper.toRemoteOptions(options));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOutput(
        ActionExecuteOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        OutputResponse outputResponse = actionDefinitionFacade.executeOutput(
            request.getComponentName(), request.getComponentVersion(), request.getActionName(),
            componentProtoMapper.toMap(request.getInputParameters()), request.getConnectionIdsMap());

        responseObserver.onNext(componentProtoMapper.toOutputResponseResult(outputResponse));
        responseObserver.onCompleted();
    }

    @Override
    public void executePerform(ActionExecutePerformRequest request, StreamObserver<ValueResponse> responseObserver) {
        Object result = actionDefinitionFacade.executePerform(
            request.getComponentName(), request.getComponentVersion(), request.getActionName(),
            request.hasJobPrincipalId() ? request.getJobPrincipalId() : null,
            request.hasJobPrincipalWorkflowId() ? request.getJobPrincipalWorkflowId() : null,
            request.hasJobId() ? request.getJobId() : null,
            request.hasTaskExecutionId() ? request.getTaskExecutionId() : null,
            request.hasWorkflowId() ? request.getWorkflowId() : null,
            componentProtoMapper.toMap(request.getInputParameters()),
            request.getConnectionIdsMap(),
            request.hasExtensions() ? componentProtoMapper.toMap(request.getExtensions()) : null,
            request.hasEnvironmentId() ? request.getEnvironmentId() : null,
            request.hasPlatformType() ? PlatformType.valueOf(request.getPlatformType()) : null,
            request.getEditorEnvironment(),
            request.hasContinueParameters() ? componentProtoMapper.toMap(request.getContinueParameters()) : null,
            request.hasResumeData() ? componentProtoMapper.toMap(request.getResumeData()) : null,
            request.hasSuspendExpiresAt() ? toInstant(request.getSuspendExpiresAt()) : null);

        responseObserver.onNext(
            ValueResponse.newBuilder()
                .setValue(componentProtoMapper.toBytes(result))
                .build());
        responseObserver.onCompleted();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
