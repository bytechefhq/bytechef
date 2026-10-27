/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.OptionalStringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowUuidRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteProjectWorkflows;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringsResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowIdsRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowUuidRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectWorkflowRemoteServiceGrpcServer
    extends ProjectWorkflowRemoteServiceGrpc.ProjectWorkflowRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowRemoteServiceGrpcServer(
        ProjectWorkflowService projectWorkflowService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public void fetchProjectWorkflowWorkflowId(
        WorkflowUuidRequest request, StreamObserver<OptionalStringResponse> responseObserver) {

        Optional<String> workflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(
            request.getProjectDeploymentId(), request.getWorkflowUuid());

        OptionalStringResponse.Builder builder = OptionalStringResponse.newBuilder();

        workflowId.ifPresent(builder::setValue);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLastWorkflowId(WorkflowUuidRequest request, StreamObserver<StringResponse> responseObserver) {
        responseObserver.onNext(
            StringResponse.newBuilder()
                .setValue(projectWorkflowService.getLastWorkflowId(request.getWorkflowUuid()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectWorkflowWorkflowId(
        WorkflowUuidRequest request, StreamObserver<StringResponse> responseObserver) {

        responseObserver.onNext(
            StringResponse.newBuilder()
                .setValue(
                    projectWorkflowService.getProjectWorkflowWorkflowId(
                        request.getProjectDeploymentId(), request.getWorkflowUuid()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectUuidProjectWorkflows(
        ProjectWorkflowUuidRequest request, StreamObserver<RemoteProjectWorkflows> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectWorkflows(
                projectWorkflowService.getProjectWorkflows(
                    request.hasProjectId() ? request.getProjectId() : null, request.getWorkflowUuid())));
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectWorkflowIds(IdRequest request, StreamObserver<StringsResponse> responseObserver) {
        responseObserver.onNext(
            StringsResponse.newBuilder()
                .addAllValues(projectWorkflowService.getProjectWorkflowIds(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getProjectsProjectWorkflows(
        IdsRequest request, StreamObserver<RemoteProjectWorkflows> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectWorkflows(
                projectWorkflowService.getProjectWorkflows(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowProjectWorkflows(
        WorkflowIdsRequest request, StreamObserver<RemoteProjectWorkflows> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectWorkflows(
                projectWorkflowService.getWorkflowProjectWorkflows(request.getWorkflowIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowUuidProjectWorkflows(
        WorkflowUuidRequest request, StreamObserver<RemoteProjectWorkflows> responseObserver) {

        responseObserver.onNext(
            automationConfigurationProtoMapper.toRemoteProjectWorkflows(
                projectWorkflowService.getWorkflowUuidProjectWorkflows(request.getWorkflowUuid())));
        responseObserver.onCompleted();
    }
}
