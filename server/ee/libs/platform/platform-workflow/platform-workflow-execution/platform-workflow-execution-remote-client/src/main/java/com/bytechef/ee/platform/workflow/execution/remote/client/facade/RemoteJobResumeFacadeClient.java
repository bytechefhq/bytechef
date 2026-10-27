/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobResumeFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobStreamingRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ResumeJobStreamingResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.google.protobuf.Empty;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.BlockingClientCall;
import java.util.Map;
import java.util.function.LongConsumer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteJobResumeFacadeClient implements JobResumeFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteJobResumeFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public boolean cancelApproval(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isApprovalResumeId(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobResumeOutcome resumeExpiredJob(String id) {
        ResumeJobResponse resumeJobResponse = getStub().resumeExpiredJob(
            ResumeJobRequest.newBuilder()
                .setId(id)
                .build());

        return JobResumeOutcome.valueOf(resumeJobResponse.getOutcome());
    }

    @Override
    public JobResumeOutcome resumeApproval(String token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        ResumeJobResponse resumeJobResponse = getStub().resumeJob(
            ResumeJobRequest.newBuilder()
                .setData(platformWorkflowExecutionProtoMapper.toBytes(data))
                .setId(id)
                .build());

        return JobResumeOutcome.valueOf(resumeJobResponse.getOutcome());
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data, String approvedBy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobResumeOutcome resumeJobStreaming(
        String id, Map<String, Object> data, LongConsumer jobIdConsumer) {

        BlockingClientCall<ResumeJobStreamingRequest, ResumeJobStreamingResponse> blockingClientCall =
            JobResumeFacadeRemoteServiceGrpc.newBlockingV2Stub(grpcChannels.getChannel(EXECUTION_APP))
                .resumeJobStreaming();

        try {
            blockingClientCall.write(
                ResumeJobStreamingRequest.newBuilder()
                    .setResumeJob(
                        ResumeJobRequest.newBuilder()
                            .setData(platformWorkflowExecutionProtoMapper.toBytes(data))
                            .setId(id))
                    .build());

            ResumeJobStreamingResponse resumeJobStreamingResponse = blockingClientCall.read();

            if (resumeJobStreamingResponse != null && resumeJobStreamingResponse.hasJobId()) {
                jobIdConsumer.accept(resumeJobStreamingResponse.getJobId());

                blockingClientCall.write(
                    ResumeJobStreamingRequest.newBuilder()
                        .setJobIdAccepted(Empty.getDefaultInstance())
                        .build());

                resumeJobStreamingResponse = blockingClientCall.read();
            }

            blockingClientCall.halfClose();

            if (resumeJobStreamingResponse == null || !resumeJobStreamingResponse.hasOutcome()) {
                throw new IllegalStateException("The resume of " + id + " ended without an outcome");
            }

            return JobResumeOutcome.valueOf(resumeJobStreamingResponse.getOutcome());
        } catch (StatusException statusException) {
            throw new StatusRuntimeException(statusException.getStatus(), statusException.getTrailers());
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            blockingClientCall.cancel("Interrupted", interruptedException);

            throw new IllegalStateException("Interrupted while resuming " + id, interruptedException);
        } catch (RuntimeException exception) {
            blockingClientCall.cancel("The resume failed on the caller", exception);

            throw exception;
        }
    }

    private JobResumeFacadeRemoteServiceGrpc.JobResumeFacadeRemoteServiceBlockingStub getStub() {
        return JobResumeFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }
}
