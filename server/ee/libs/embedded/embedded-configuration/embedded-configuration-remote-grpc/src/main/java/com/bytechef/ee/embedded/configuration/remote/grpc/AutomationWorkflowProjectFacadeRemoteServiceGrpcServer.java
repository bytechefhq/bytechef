/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
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
    private final AutomationProjectFacade automationProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(
        AutomationProjectFacade automationProjectFacade,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.automationProjectFacade = automationProjectFacade;
    }

    @Override
    public void getPublishedProjects(Empty request, StreamObserver<RemoteAutomationWorkflowProjects> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteAutomationWorkflowProjects(
                automationProjectFacade.getPublishedProjects()));
        responseObserver.onCompleted();
    }
}
