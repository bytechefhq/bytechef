/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobFacadeRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJobParameters;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 */
public class JobFacadeRemoteServiceGrpcServer extends JobFacadeRemoteServiceGrpc.JobFacadeRemoteServiceImplBase {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public JobFacadeRemoteServiceGrpcServer(
        JobFacade jobFacade, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        this.atlasExecutionProtoMapper = atlasExecutionProtoMapper;
        this.jobFacade = jobFacade;
    }

    @Override
    public void createJob(RemoteJobParameters request, StreamObserver<IdResponse> responseObserver) {
        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(jobFacade.createJob(atlasExecutionProtoMapper.toJobParametersDTO(request)))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteJob(IdRequest request, StreamObserver<Empty> responseObserver) {
        jobFacade.deleteJob(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void stopJob(IdRequest request, StreamObserver<Empty> responseObserver) {
        jobFacade.stopJob(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
