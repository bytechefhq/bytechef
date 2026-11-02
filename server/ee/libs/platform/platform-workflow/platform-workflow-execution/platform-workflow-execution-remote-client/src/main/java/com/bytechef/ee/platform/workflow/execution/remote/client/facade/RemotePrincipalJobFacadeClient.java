/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreateChildJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreatePrincipalJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.CreatePrincipalLinkedJobRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalJobFacadeRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePrincipalJobFacadeClient implements PrincipalJobFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final @Nullable Duration executionDeadline;
    private final GrpcChannels grpcChannels;

    public RemotePrincipalJobFacadeClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.executionDeadline = grpc.getExecutionDeadline();
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {
        IdResponse idResponse = getStub().createChildJob(
            CreateChildJobRequest.newBuilder()
                .setJobParameters(platformWorkflowExecutionProtoMapper.toProto(jobParametersDTO))
                .setParentJobId(parentJobId)
                .setPlatformType(platformType.name())
                .build());

        return idResponse.getId();
    }

    @Override
    public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        IdResponse idResponse = getStub().createJob(
            toCreatePrincipalJobRequest(jobParametersDTO, jobPrincipalId, type));

        return idResponse.getId();
    }

    @Override
    public Job createSyncJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        return platformWorkflowExecutionProtoMapper.toJob(
            getStub().createSyncJob(toCreatePrincipalJobRequest(jobParametersDTO, jobPrincipalId, type)));
    }

    @Override
    public long createPrincipalLinkedJob(
        long referenceJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {

        IdResponse idResponse = getStub().createPrincipalLinkedJob(
            CreatePrincipalLinkedJobRequest.newBuilder()
                .setJobParameters(platformWorkflowExecutionProtoMapper.toProto(jobParametersDTO))
                .setPlatformType(platformType.name())
                .setReferenceJobId(referenceJobId)
                .build());

        return idResponse.getId();
    }

    @Override
    public long createPrincipalLinkedJob(
        JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType platformType) {

        IdResponse idResponse = getStub().createPrincipalLinkedJobForPrincipal(
            toCreatePrincipalJobRequest(jobParametersDTO, jobPrincipalId, platformType));

        return idResponse.getId();
    }

    private PrincipalJobFacadeRemoteServiceGrpc.PrincipalJobFacadeRemoteServiceBlockingStub getStub() {
        PrincipalJobFacadeRemoteServiceGrpc.PrincipalJobFacadeRemoteServiceBlockingStub stub =
            PrincipalJobFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(EXECUTION_APP));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private CreatePrincipalJobRequest toCreatePrincipalJobRequest(
        JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {

        return CreatePrincipalJobRequest.newBuilder()
            .setJobParameters(platformWorkflowExecutionProtoMapper.toProto(jobParametersDTO))
            .setJobPrincipalId(jobPrincipalId)
            .setPlatformType(type.name())
            .build();
    }
}
