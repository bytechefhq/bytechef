/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobTypeRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.OptionalIdResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalJobRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalTypeRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePrincipalJobServiceClient implements PrincipalJobService {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;

    public RemotePrincipalJobServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public PrincipalJob create(long jobId, long principalId, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deletePrincipalJobs(long jobId, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchLastJobId(long principalId, PlatformType type) {
        OptionalIdResponse optionalIdResponse =
            getStub().fetchLastJobId(toPrincipalTypeRequest(principalId, List.of(), type));

        return optionalIdResponse.hasId() ? Optional.of(optionalIdResponse.getId()) : Optional.empty();
    }

    @Override
    public Optional<Long> fetchLastWorkflowJobId(long principalId, List<String> workflowIds, PlatformType type) {
        OptionalIdResponse optionalIdResponse =
            getStub().fetchLastWorkflowJobId(toPrincipalTypeRequest(principalId, workflowIds, type));

        return optionalIdResponse.hasId() ? Optional.of(optionalIdResponse.getId()) : Optional.empty();
    }

    @Override
    public Optional<Long> fetchJobPrincipalId(long jobId, PlatformType type) {
        OptionalIdResponse optionalIdResponse = getStub().fetchJobPrincipalId(
            JobTypeRequest.newBuilder()
                .setJobId(jobId)
                .setPlatformType(type.name())
                .build());

        return optionalIdResponse.hasId() ? Optional.of(optionalIdResponse.getId()) : Optional.empty();
    }

    @Override
    public List<PrincipalJob> getPrincipalJobs(List<Long> jobIds, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getJobPrincipalId(long jobId, PlatformType type) {
        Optional<Long> jobPrincipalIdOptional = fetchJobPrincipalId(jobId, type);

        return jobPrincipalIdOptional.orElseThrow();
    }

    @Override
    public List<Long> getJobIds(long principalId, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, boolean onlyRootJobs, int pageNumber) {

        throw new UnsupportedOperationException();
    }

    private PrincipalJobRemoteServiceGrpc.PrincipalJobRemoteServiceBlockingStub getStub() {
        return PrincipalJobRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static PrincipalTypeRequest toPrincipalTypeRequest(
        long principalId, List<String> workflowIds, PlatformType type) {

        return PrincipalTypeRequest.newBuilder()
            .addAllWorkflowIds(workflowIds)
            .setPlatformType(type.name())
            .setPrincipalId(principalId)
            .build();
    }
}
