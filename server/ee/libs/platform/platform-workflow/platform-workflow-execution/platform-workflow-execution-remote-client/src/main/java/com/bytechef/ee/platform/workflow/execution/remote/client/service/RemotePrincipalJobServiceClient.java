/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.GetJobIdsPageRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobIdsResponse;
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
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePrincipalJobServiceClient implements PrincipalJobService {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;
    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;

    public RemotePrincipalJobServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
    }

    @Override
    public PrincipalJob create(long jobId, long principalId, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deletePrincipalJobs(long jobId, PlatformType type) {
        getStub().deletePrincipalJobs(toJobTypeRequest(jobId, type));
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
        OptionalIdResponse optionalIdResponse = getStub().fetchJobPrincipalId(toJobTypeRequest(jobId, type));

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
        JobIdsResponse jobIdsResponse = getStub().getJobIds(toPrincipalTypeRequest(principalId, List.of(), type));

        return List.copyOf(jobIdsResponse.getJobIdsList());
    }

    @Override
    public Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, PlatformType type,
        List<String> workflowIds, boolean onlyRootJobs, int pageNumber) {

        GetJobIdsPageRequest.Builder builder = GetJobIdsPageRequest.newBuilder()
            .setOnlyRootJobs(onlyRootJobs)
            .setPageNumber(pageNumber)
            .setPlatformType(type.name());

        if (status != null) {
            builder.setStatus(status.name());
        }

        if (startDate != null) {
            builder.setStartDate(platformWorkflowExecutionProtoMapper.toTimestamp(startDate));
        }

        if (endDate != null) {
            builder.setEndDate(platformWorkflowExecutionProtoMapper.toTimestamp(endDate));
        }

        if (principalIds != null) {
            builder.addAllPrincipalIds(principalIds);
        }

        if (workflowIds != null) {
            builder.addAllWorkflowIds(workflowIds);
        }

        return platformWorkflowExecutionProtoMapper.toJobIdsPage(getStub().getJobIdsPage(builder.build()));
    }

    private PrincipalJobRemoteServiceGrpc.PrincipalJobRemoteServiceBlockingStub getStub() {
        return PrincipalJobRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static JobTypeRequest toJobTypeRequest(long jobId, PlatformType type) {
        return JobTypeRequest.newBuilder()
            .setJobId(jobId)
            .setPlatformType(type.name())
            .build();
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
