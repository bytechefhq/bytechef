/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.facade;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobFacadeRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import java.util.Map;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteJobFacadeClient implements JobFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteJobFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long createJob(JobParametersDTO jobParametersDTO) {
        IdResponse idResponse = getStub().createJob(atlasExecutionProtoMapper.toProto(jobParametersDTO));

        return idResponse.getId();
    }

    @Override
    public long createJob(JobParametersDTO jobParametersDTO, LongConsumer jobCreatedCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJob(long id) {
        loadBalancedRestClient.delete(
            uriBuilder -> uriBuilder
                .host("execution-app")
                .path("/remote/job-facade/delete-job/{id}")
                .build(id));
    }

    @Override
    public void resumeApproval(long jobId, String uuid, boolean approved) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeJob(long id, long taskExecutionId, @Nullable Map<String, ?> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeClaimedJob(long id, @Nullable Long taskExecutionId, @Nullable Map<String, ?> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopJob(long id) {
        throw new UnsupportedOperationException();
    }

    private JobFacadeRemoteServiceGrpc.JobFacadeRemoteServiceBlockingStub getStub() {
        return JobFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }
}
