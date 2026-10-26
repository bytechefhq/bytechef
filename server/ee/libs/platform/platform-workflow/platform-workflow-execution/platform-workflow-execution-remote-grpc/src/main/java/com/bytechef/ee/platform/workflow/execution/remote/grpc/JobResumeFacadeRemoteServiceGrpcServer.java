/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobResumeFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobResponse;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 */
public class JobResumeFacadeRemoteServiceGrpcServer
    extends JobResumeFacadeRemoteServiceGrpc.JobResumeFacadeRemoteServiceImplBase {

    private final JobResumeFacade jobResumeFacade;
    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;

    @SuppressFBWarnings("EI")
    public JobResumeFacadeRemoteServiceGrpcServer(
        JobResumeFacade jobResumeFacade, PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.jobResumeFacade = jobResumeFacade;
        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
    }

    @Override
    public void resumeExpiredJob(ResumeJobRequest request, StreamObserver<ResumeJobResponse> responseObserver) {
        responseObserver.onNext(
            ResumeJobResponse.newBuilder()
                .setOutcome(String.valueOf(jobResumeFacade.resumeExpiredJob(request.getId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void resumeJob(ResumeJobRequest request, StreamObserver<ResumeJobResponse> responseObserver) {
        responseObserver.onNext(
            ResumeJobResponse.newBuilder()
                .setOutcome(String.valueOf(
                    jobResumeFacade.resumeJob(
                        request.getId(),
                        Objects.requireNonNullElse(platformWorkflowExecutionProtoMapper.toMap(request.getData()),
                            Map.of()))))
                .build());
        responseObserver.onCompleted();
    }
}
