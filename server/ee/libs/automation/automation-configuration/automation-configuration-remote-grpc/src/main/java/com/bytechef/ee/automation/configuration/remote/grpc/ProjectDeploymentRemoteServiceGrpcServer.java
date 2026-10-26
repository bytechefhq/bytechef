/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectDeploymentRemoteServiceGrpcServer
    extends ProjectDeploymentRemoteServiceGrpc.ProjectDeploymentRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentRemoteServiceGrpcServer(
        ProjectDeploymentService projectDeploymentService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public void getProjectDeployment(IdRequest request, StreamObserver<RemoteProjectDeployment> responseObserver) {
        responseObserver.onNext(
            automationConfigurationProtoMapper.toProto(projectDeploymentService.getProjectDeployment(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void isProjectDeploymentEnabled(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(projectDeploymentService.isProjectDeploymentEnabled(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}
