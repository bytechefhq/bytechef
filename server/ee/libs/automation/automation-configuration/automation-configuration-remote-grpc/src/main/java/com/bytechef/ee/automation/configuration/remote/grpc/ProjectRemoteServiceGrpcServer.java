/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProject;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjects;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectRemoteServiceGrpcServer extends ProjectRemoteServiceGrpc.ProjectRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectRemoteServiceGrpcServer(
        ProjectService projectService, AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.projectService = projectService;
    }

    @Override
    public void getProject(IdRequest request, StreamObserver<RemoteProject> responseObserver) {
        responseObserver.onNext(automationConfigurationProtoMapper.toProto(projectService.getProject(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectDeploymentProject(IdRequest request, StreamObserver<RemoteProject> responseObserver) {
        responseObserver.onNext(
            automationConfigurationProtoMapper.toProto(projectService.getProjectDeploymentProject(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getProjects(Empty request, StreamObserver<RemoteProjects> responseObserver) {
        responseObserver.onNext(automationConfigurationProtoMapper.toRemoteProjects(projectService.getProjects()));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowProject(WorkflowIdRequest request, StreamObserver<RemoteProject> responseObserver) {
        responseObserver.onNext(
            automationConfigurationProtoMapper.toProto(projectService.getWorkflowProject(request.getWorkflowId())));
        responseObserver.onCompleted();
    }
}
