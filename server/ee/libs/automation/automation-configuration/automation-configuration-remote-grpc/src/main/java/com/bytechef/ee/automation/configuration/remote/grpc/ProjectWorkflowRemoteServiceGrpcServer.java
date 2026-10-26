/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ProjectWorkflowRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.WorkflowUuidRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectWorkflowRemoteServiceGrpcServer
    extends ProjectWorkflowRemoteServiceGrpc.ProjectWorkflowRemoteServiceImplBase {

    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowRemoteServiceGrpcServer(ProjectWorkflowService projectWorkflowService) {
        this.projectWorkflowService = projectWorkflowService;
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
}
