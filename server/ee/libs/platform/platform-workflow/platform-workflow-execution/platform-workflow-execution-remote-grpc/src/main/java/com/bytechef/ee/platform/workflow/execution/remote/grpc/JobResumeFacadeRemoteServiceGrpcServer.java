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
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobStreamingRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobStreamingResponse;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @version ee
 */
public class JobResumeFacadeRemoteServiceGrpcServer
    extends JobResumeFacadeRemoteServiceGrpc.JobResumeFacadeRemoteServiceImplBase {

    private final GrpcRemoteExceptionHandler grpcRemoteExceptionHandler = new GrpcRemoteExceptionHandler();
    private final JobResumeFacade jobResumeFacade;
    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final Executor resumeExecutor;

    @SuppressFBWarnings("EI")
    public JobResumeFacadeRemoteServiceGrpcServer(
        JobResumeFacade jobResumeFacade, PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper,
        Executor resumeExecutor) {

        this.jobResumeFacade = jobResumeFacade;
        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.resumeExecutor = resumeExecutor;
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

    @Override
    public StreamObserver<ResumeJobStreamingRequest> resumeJobStreaming(
        StreamObserver<ResumeJobStreamingResponse> responseObserver) {

        CompletableFuture<Void> jobIdAcceptedFuture = new CompletableFuture<>();

        return new StreamObserver<>() {

            @Override
            public void onNext(ResumeJobStreamingRequest request) {
                if (request.hasJobIdAccepted()) {
                    jobIdAcceptedFuture.complete(null);

                    return;
                }

                ResumeJobRequest resumeJobRequest = request.getResumeJob();

                resumeExecutor.execute(
                    () -> resumeJobStreaming(resumeJobRequest, jobIdAcceptedFuture, responseObserver));
            }

            @Override
            public void onError(Throwable throwable) {
                jobIdAcceptedFuture.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
                jobIdAcceptedFuture.completeExceptionally(
                    new IllegalStateException("The caller closed the resume before accepting the job id"));
            }
        };
    }

    private void resumeJobStreaming(
        ResumeJobRequest resumeJobRequest, CompletableFuture<Void> jobIdAcceptedFuture,
        StreamObserver<ResumeJobStreamingResponse> responseObserver) {

        JobResumeOutcome jobResumeOutcome;

        try {
            jobResumeOutcome = jobResumeFacade.resumeJobStreaming(
                resumeJobRequest.getId(),
                Objects.requireNonNullElse(
                    platformWorkflowExecutionProtoMapper.toMap(resumeJobRequest.getData()), Map.of()),
                jobId -> {
                    responseObserver.onNext(
                        ResumeJobStreamingResponse.newBuilder()
                            .setJobId(jobId)
                            .build());

                    jobIdAcceptedFuture.join();
                });
        } catch (RuntimeException exception) {
            responseObserver.onError(grpcRemoteExceptionHandler.handleException(exception));

            return;
        }

        responseObserver.onNext(
            ResumeJobStreamingResponse.newBuilder()
                .setOutcome(jobResumeOutcome.name())
                .build());
        responseObserver.onCompleted();
    }
}
