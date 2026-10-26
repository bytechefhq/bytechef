/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.OutputResponseResult;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteOptions;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteProperties;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteTriggerOutput;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteWebhookValidateResponse;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteDynamicPropertiesRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteDynamicWebhookRefreshRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteListenerRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteOptionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteOutputRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteTriggerRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookDisableRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookEnableRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.TriggerExecuteWebhookValidateRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.WebhookEnableOutputResult;
import com.bytechef.ee.remote.grpc.ProtoValues;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeRemoteServiceGrpcServer
    extends TriggerDefinitionFacadeRemoteServiceGrpc.TriggerDefinitionFacadeRemoteServiceImplBase {

    private final ComponentProtoMapper componentProtoMapper;
    private final TriggerDefinitionFacade triggerDefinitionFacade;

    public TriggerDefinitionFacadeRemoteServiceGrpcServer(
        TriggerDefinitionFacade triggerDefinitionFacade, ComponentProtoMapper componentProtoMapper) {

        this.componentProtoMapper = componentProtoMapper;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @Override
    public void executeDynamicProperties(
        TriggerExecuteDynamicPropertiesRequest request, StreamObserver<RemoteProperties> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toRemoteProperties(
                triggerDefinitionFacade.executeDynamicProperties(
                    request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
                    request.getPropertyName(), ProtoValues.toMap(request.getInputParameters()),
                    List.copyOf(request.getLookupDependsOnPathsList()),
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void executeDynamicWebhookRefresh(
        TriggerExecuteDynamicWebhookRefreshRequest request,
        StreamObserver<WebhookEnableOutputResult> responseObserver) {

        WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeDynamicWebhookRefresh(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getOutputParameters()),
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(componentProtoMapper.toWebhookEnableOutputResult(webhookEnableOutput));
        responseObserver.onCompleted();
    }

    @Override
    public void executeListenerDisable(TriggerExecuteListenerRequest request, StreamObserver<Empty> responseObserver) {
        triggerDefinitionFacade.executeListenerDisable(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getInputParameters()),
            request.hasWorkflowExecutionId() ? request.getWorkflowExecutionId() : null,
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void executeListenerEnable(TriggerExecuteListenerRequest request, StreamObserver<Empty> responseObserver) {
        triggerDefinitionFacade.executeListenerEnable(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getInputParameters()),
            request.hasWorkflowExecutionId() ? request.getWorkflowExecutionId() : null,
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void executeOptions(TriggerExecuteOptionsRequest request, StreamObserver<RemoteOptions> responseObserver) {
        responseObserver.onNext(
            componentProtoMapper.toRemoteOptions(
                triggerDefinitionFacade.executeOptions(
                    request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
                    request.getPropertyName(), ProtoValues.toMap(request.getInputParameters()),
                    List.copyOf(request.getLookupDependsOnPathsList()),
                    request.hasSearchText() ? request.getSearchText() : null,
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void executeOutput(
        TriggerExecuteOutputRequest request, StreamObserver<OutputResponseResult> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toOutputResponseResult(
                triggerDefinitionFacade.executeOutput(
                    request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
                    ProtoValues.toMap(request.getInputParameters()),
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }

    @Override
    public void executeTrigger(
        TriggerExecuteTriggerRequest request, StreamObserver<RemoteTriggerOutput> responseObserver) {

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            request.hasJobPrincipalId() ? request.getJobPrincipalId() : null,
            request.hasWorkflowUuid() ? request.getWorkflowUuid() : null,
            request.hasTriggerExecutionId() ? request.getTriggerExecutionId() : null,
            ProtoValues.toMap(request.getInputParameters()),
            request.hasTriggerState() ? ProtoValues.toObject(request.getTriggerState()) : null,
            request.hasWebhookRequest() ? componentProtoMapper.toWebhookRequest(request.getWebhookRequest()) : null,
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasEnvironmentId() ? request.getEnvironmentId() : null,
            request.hasPlatformType() ? PlatformType.valueOf(request.getPlatformType()) : null,
            request.getEditorEnvironment());

        responseObserver.onNext(componentProtoMapper.toProto(triggerOutput));
        responseObserver.onCompleted();
    }

    @Override
    public void executeWebhookDisable(
        TriggerExecuteWebhookDisableRequest request, StreamObserver<Empty> responseObserver) {

        triggerDefinitionFacade.executeWebhookDisable(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getInputParameters()),
            request.hasWorkflowExecutionId() ? request.getWorkflowExecutionId() : null,
            ProtoValues.toMap(request.getOutputParameters()),
            request.hasConnectionId() ? request.getConnectionId() : null);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void executeWebhookEnable(
        TriggerExecuteWebhookEnableRequest request, StreamObserver<WebhookEnableOutputResult> responseObserver) {

        WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeWebhookEnable(
            request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
            ProtoValues.toMap(request.getInputParameters()),
            request.hasWorkflowExecutionId() ? request.getWorkflowExecutionId() : null,
            request.hasConnectionId() ? request.getConnectionId() : null,
            request.hasWebhookUrl() ? request.getWebhookUrl() : null, request.getEnvironmentId());

        responseObserver.onNext(componentProtoMapper.toWebhookEnableOutputResult(webhookEnableOutput));
        responseObserver.onCompleted();
    }

    @Override
    public void executeWebhookValidate(
        TriggerExecuteWebhookValidateRequest request, StreamObserver<RemoteWebhookValidateResponse> responseObserver) {

        responseObserver.onNext(
            componentProtoMapper.toProto(
                triggerDefinitionFacade.executeWebhookValidate(
                    request.getComponentName(), request.getComponentVersion(), request.getTriggerName(),
                    ProtoValues.toMap(request.getInputParameters()),
                    request.hasWebhookRequest() ? componentProtoMapper.toWebhookRequest(request.getWebhookRequest())
                        : null,
                    request.hasConnectionId() ? request.getConnectionId() : null)));
        responseObserver.onCompleted();
    }
}
