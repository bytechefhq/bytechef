/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.remote.client.service;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.CodeWorkflowContainerProtoMapper;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.CodeWorkflowContainerRemoteServiceGrpc;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.GetCodeWorkflowContainerRequest;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteCodeWorkflowContainerServiceClient implements CodeWorkflowContainerService {

    private static final String SERVICE_ID = "configuration-app";

    private final CodeWorkflowContainerProtoMapper codeWorkflowContainerProtoMapper =
        new CodeWorkflowContainerProtoMapper();
    private final GrpcChannels grpcChannels;

    public RemoteCodeWorkflowContainerServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public CodeWorkflowContainer create(CodeWorkflowContainer codeWorkflowContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeWorkflowContainer getCodeWorkflowContainer(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeWorkflowContainer getCodeWorkflowContainer(String codeWorkflowContainerUuid) {
        return codeWorkflowContainerProtoMapper.toCodeWorkflowContainer(
            getStub().getCodeWorkflowContainer(
                GetCodeWorkflowContainerRequest.newBuilder()
                    .setCodeWorkflowContainerUuid(codeWorkflowContainerUuid)
                    .build()));
    }

    private CodeWorkflowContainerRemoteServiceGrpc.CodeWorkflowContainerRemoteServiceBlockingStub getStub() {
        return CodeWorkflowContainerRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}
