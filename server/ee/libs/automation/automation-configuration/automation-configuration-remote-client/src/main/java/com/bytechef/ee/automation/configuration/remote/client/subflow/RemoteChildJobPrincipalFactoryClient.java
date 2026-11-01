/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.subflow;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ChildJobPrincipalFactoryRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.CreateChildJobRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.CreatePrincipalLinkedJobRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteChildJobPrincipalFactoryClient implements ChildJobPrincipalFactory {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteChildJobPrincipalFactoryClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.automationConfigurationProtoMapper = new AutomationConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
        IdResponse idResponse = getStub().createChildJob(
            CreateChildJobRequest.newBuilder()
                .setJobParameters(automationConfigurationProtoMapper.toProto(jobParametersDTO))
                .setParentJobId(parentJobId)
                .build());

        return idResponse.getId();
    }

    @Override
    public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
        IdResponse idResponse = getStub().createPrincipalLinkedJob(
            CreatePrincipalLinkedJobRequest.newBuilder()
                .setJobParameters(automationConfigurationProtoMapper.toProto(jobParametersDTO))
                .setReferenceJobId(referenceJobId)
                .build());

        return idResponse.getId();
    }

    private ChildJobPrincipalFactoryRemoteServiceGrpc.ChildJobPrincipalFactoryRemoteServiceBlockingStub getStub() {
        return ChildJobPrincipalFactoryRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}
