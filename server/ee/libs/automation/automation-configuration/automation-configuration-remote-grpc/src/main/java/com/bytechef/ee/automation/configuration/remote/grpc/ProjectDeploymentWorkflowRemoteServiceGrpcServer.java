/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowConnectionResult;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectDeploymentWorkflowRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectDeploymentWorkflowConnections;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectDeploymentWorkflowRemoteServiceGrpcServer
    extends ProjectDeploymentWorkflowRemoteServiceGrpc.ProjectDeploymentWorkflowRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowRemoteServiceGrpcServer(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void fetchProjectDeploymentWorkflowConnection(
        ProjectDeploymentWorkflowRequest request,
        StreamObserver<ProjectDeploymentWorkflowConnectionResult> responseObserver) {

        Optional<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnection =
            projectDeploymentWorkflowService.fetchProjectDeploymentWorkflowConnection(
                request.getProjectDeploymentId(), request.getWorkflowId(), request.getWorkflowNodeName(),
                request.getWorkflowConnectionKey());

        ProjectDeploymentWorkflowConnectionResult.Builder builder =
            ProjectDeploymentWorkflowConnectionResult.newBuilder();

        projectDeploymentWorkflowConnection.ifPresent(
            curProjectDeploymentWorkflowConnection -> builder.setConnection(
                automationConfigurationProtoMapper.toProto(curProjectDeploymentWorkflowConnection)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectDeploymentWorkflow(
        ProjectDeploymentWorkflowRequest request, StreamObserver<RemoteProjectDeploymentWorkflow> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toProto(
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    request.getProjectDeploymentId(), request.getWorkflowId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectDeploymentWorkflowConnections(
        ProjectDeploymentWorkflowRequest request,
        StreamObserver<RemoteProjectDeploymentWorkflowConnections> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectDeploymentWorkflowConnections(
                projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
                    request.getProjectDeploymentId(), request.getWorkflowId(), request.getWorkflowNodeName())));
        responseObserver.onCompleted();
    }

    @Override
    public void isConnectionUsed(IdRequest request, StreamObserver<BoolResponse> responseObserver) {
        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(projectDeploymentWorkflowService.isConnectionUsed(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void isProjectDeploymentWorkflowEnabled(
        ProjectDeploymentWorkflowRequest request, StreamObserver<BoolResponse> responseObserver) {

        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(
                    projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                        request.getProjectDeploymentId(), request.getWorkflowId()))
                .build());
        responseObserver.onCompleted();
    }
}
