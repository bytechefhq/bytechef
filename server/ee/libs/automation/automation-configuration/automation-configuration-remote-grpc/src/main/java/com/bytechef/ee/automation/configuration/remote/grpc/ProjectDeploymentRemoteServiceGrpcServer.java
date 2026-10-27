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
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentsFilterRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployment;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeployments;
import com.bytechef.platform.configuration.domain.Environment;
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
    public void getProjectDeployments(IdsRequest request, StreamObserver<RemoteProjectDeployments> responseObserver) {
        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectDeployments(
                projectDeploymentService.getProjectDeployments(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectDeploymentsByFilter(
        ProjectDeploymentsFilterRequest request, StreamObserver<RemoteProjectDeployments> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectDeployments(
                projectDeploymentService.getProjectDeployments(
                    request.hasEmbedded() ? request.getEmbedded() : null,
                    request.hasEnvironment() ? Environment.valueOf(request.getEnvironment()) : null,
                    request.hasProjectId() ? request.getProjectId() : null,
                    request.hasTagId() ? request.getTagId() : null,
                    request.hasWorkspaceId() ? request.getWorkspaceId() : null)));
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
