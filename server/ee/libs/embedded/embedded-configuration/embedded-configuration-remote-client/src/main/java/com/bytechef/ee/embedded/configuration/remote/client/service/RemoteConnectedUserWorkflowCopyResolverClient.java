/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserWorkflowCopyResolverRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.StringsResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.WorkflowUuidRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserWorkflowCopyResolverClient implements ConnectedUserWorkflowCopyResolver {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectedUserWorkflowCopyResolverClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public List<String> getWorkflowCopyUuids(String workflowUuid) {
        ConnectedUserWorkflowCopyResolverRemoteServiceGrpc.ConnectedUserWorkflowCopyResolverRemoteServiceBlockingStub stub =
            ConnectedUserWorkflowCopyResolverRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(CONFIGURATION_APP));

        StringsResponse stringsResponse = stub.getWorkflowCopyUuids(
            WorkflowUuidRequest.newBuilder()
                .setWorkflowUuid(workflowUuid)
                .build());

        return stringsResponse.getValuesList();
    }
}
