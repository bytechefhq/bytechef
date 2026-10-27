/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationWorkflowFacadeRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationWorkflowDTOs;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowFacadeRemoteServiceGrpcServer
    extends IntegrationWorkflowFacadeRemoteServiceGrpc.IntegrationWorkflowFacadeRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationWorkflowFacade integrationWorkflowFacade;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowFacadeRemoteServiceGrpcServer(
        IntegrationWorkflowFacade integrationWorkflowFacade,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationWorkflowFacade = integrationWorkflowFacade;
    }

    @Override
    public void getIntegrationWorkflows(Empty request, StreamObserver<RemoteIntegrationWorkflowDTOs> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationWorkflowDTOs(
                integrationWorkflowFacade.getIntegrationWorkflows()));
        responseObserver.onCompleted();
    }
}
