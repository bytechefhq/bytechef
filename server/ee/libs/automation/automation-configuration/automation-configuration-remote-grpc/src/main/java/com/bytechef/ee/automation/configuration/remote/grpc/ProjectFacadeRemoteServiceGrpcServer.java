/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectFacadeRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflowDTOs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectFacadeRemoteServiceGrpcServer
    extends ProjectFacadeRemoteServiceGrpc.ProjectFacadeRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public ProjectFacadeRemoteServiceGrpcServer(
        ProjectFacade projectFacade, AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.projectFacade = projectFacade;
    }

    @Override
    public void getWorkspaceProjectWorkflows(
        IdRequest request, StreamObserver<RemoteProjectWorkflowDTOs> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectWorkflowDTOs(
                projectFacade.getWorkspaceProjectWorkflows(request.getId())));
        responseObserver.onCompleted();
    }
}
