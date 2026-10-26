/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteWorkflow;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteWorkflows;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowIdsRequest;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowRemoteServiceGrpc;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowRemoteServiceGrpcServer extends WorkflowRemoteServiceGrpc.WorkflowRemoteServiceImplBase {

    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowRemoteServiceGrpcServer(
        WorkflowService workflowService, PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        this.platformConfigurationProtoMapper = platformConfigurationProtoMapper;
        this.workflowService = workflowService;
    }

    @Override
    public void fetchWorkflow(WorkflowIdRequest request, StreamObserver<WorkflowResult> responseObserver) {
        Optional<Workflow> workflow = workflowService.fetchWorkflow(request.getId());

        WorkflowResult.Builder builder = WorkflowResult.newBuilder();

        workflow.ifPresent(curWorkflow -> builder.setWorkflow(platformConfigurationProtoMapper.toProto(curWorkflow)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflow(WorkflowIdRequest request, StreamObserver<RemoteWorkflow> responseObserver) {
        responseObserver.onNext(platformConfigurationProtoMapper.toProto(workflowService.getWorkflow(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflows(WorkflowIdsRequest request, StreamObserver<RemoteWorkflows> responseObserver) {
        responseObserver.onNext(
            platformConfigurationProtoMapper.toRemoteWorkflows(workflowService.getWorkflows(request.getIdsList())));
        responseObserver.onCompleted();
    }
}
