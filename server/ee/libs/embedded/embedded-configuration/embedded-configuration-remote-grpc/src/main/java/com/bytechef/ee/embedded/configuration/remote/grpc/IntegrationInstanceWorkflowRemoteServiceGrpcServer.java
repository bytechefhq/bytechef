/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceWorkflowRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceWorkflowRemoteServiceGrpcServer
    extends IntegrationInstanceWorkflowRemoteServiceGrpc.IntegrationInstanceWorkflowRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceWorkflowRemoteServiceGrpcServer(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
    }

    @Override
    public void getIntegrationInstanceWorkflow(
        IntegrationInstanceWorkflowRequest request,
        StreamObserver<RemoteIntegrationInstanceWorkflow> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                    request.getIntegrationInstanceId(), request.getWorkflowId())));
        responseObserver.onCompleted();
    }
}
