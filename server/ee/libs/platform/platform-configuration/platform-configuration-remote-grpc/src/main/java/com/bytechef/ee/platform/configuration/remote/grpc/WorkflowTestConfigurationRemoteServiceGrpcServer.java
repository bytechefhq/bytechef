/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.ee.platform.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.InputsResponse;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowTestConfigurationInputsRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowTestConfigurationRemoteServiceGrpc;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowTestConfigurationRemoteServiceGrpcServer
    extends WorkflowTestConfigurationRemoteServiceGrpc.WorkflowTestConfigurationRemoteServiceImplBase {

    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowTestConfigurationRemoteServiceGrpcServer(
        WorkflowTestConfigurationService workflowTestConfigurationService,
        PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        this.platformConfigurationProtoMapper = platformConfigurationProtoMapper;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void getWorkflowTestConfigurationInputs(
        WorkflowTestConfigurationInputsRequest request, StreamObserver<InputsResponse> responseObserver) {

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            request.getWorkflowId(), request.getEnvironmentId());

        InputsResponse.Builder builder = InputsResponse.newBuilder();

        if (inputs != null) {
            builder.setInputs(platformConfigurationProtoMapper.toBytes(inputs));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void isConnectionUsed(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(workflowTestConfigurationService.isConnectionUsed(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}
