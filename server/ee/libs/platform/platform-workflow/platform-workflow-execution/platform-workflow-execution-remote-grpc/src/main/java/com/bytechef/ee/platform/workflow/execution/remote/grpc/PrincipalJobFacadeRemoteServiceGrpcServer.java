/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJob;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreateChildJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreatePrincipalJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreatePrincipalLinkedJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalJobFacadeRemoteServiceGrpc;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 */
public class PrincipalJobFacadeRemoteServiceGrpcServer
    extends PrincipalJobFacadeRemoteServiceGrpc.PrincipalJobFacadeRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final PrincipalJobFacade principalJobFacade;

    @SuppressFBWarnings("EI")
    public PrincipalJobFacadeRemoteServiceGrpcServer(
        PrincipalJobFacade principalJobFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.principalJobFacade = principalJobFacade;
    }

    @Override
    public void createChildJob(CreateChildJobRequest request, StreamObserver<IdResponse> responseObserver) {
        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(
                    principalJobFacade.createChildJob(
                        request.getParentJobId(),
                        platformWorkflowExecutionProtoMapper.toJobParametersDTO(request.getJobParameters()),
                        PlatformType.valueOf(request.getPlatformType())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createJob(CreatePrincipalJobRequest request, StreamObserver<IdResponse> responseObserver) {
        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(
                    principalJobFacade.createJob(
                        platformWorkflowExecutionProtoMapper.toJobParametersDTO(request.getJobParameters()),
                        request.getJobPrincipalId(),
                        PlatformType.valueOf(request.getPlatformType())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createPrincipalLinkedJob(
        CreatePrincipalLinkedJobRequest request, StreamObserver<IdResponse> responseObserver) {

        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(
                    principalJobFacade.createPrincipalLinkedJob(
                        request.getReferenceJobId(),
                        platformWorkflowExecutionProtoMapper.toJobParametersDTO(request.getJobParameters()),
                        PlatformType.valueOf(request.getPlatformType())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createSyncJob(CreatePrincipalJobRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(
            platformWorkflowExecutionProtoMapper.toProto(
                principalJobFacade.createSyncJob(
                    platformWorkflowExecutionProtoMapper.toJobParametersDTO(request.getJobParameters()),
                    request.getJobPrincipalId(),
                    PlatformType.valueOf(request.getPlatformType()))));
        responseObserver.onCompleted();
    }
}
