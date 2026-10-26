/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.AutomationWorkflowProjectFacadeRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteAutomationWorkflowProjects;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationWorkflowProjectFacadeRemoteServiceGrpcServer
    extends AutomationWorkflowProjectFacadeRemoteServiceGrpc.AutomationWorkflowProjectFacadeRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    public void getPublishedProjects(Empty request, StreamObserver<RemoteAutomationWorkflowProjects> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteAutomationWorkflowProjects(
                automationWorkflowProjectFacade.getPublishedProjects()));
        responseObserver.onCompleted();
    }
}
