/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.registry;

import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteSubflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ResolveSubflowRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.SubflowResolverRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteSubflowResolverClient implements SubflowResolver {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteSubflowResolverClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Subflow resolveSubflow(String workflowUuid, String triggerName, boolean editorEnvironment) {
        SubflowResolverRemoteServiceGrpc.SubflowResolverRemoteServiceBlockingStub stub =
            SubflowResolverRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));

        RemoteSubflow remoteSubflow = stub.resolveSubflow(
            ResolveSubflowRequest.newBuilder()
                .setEditorEnvironment(editorEnvironment)
                .setTriggerName(triggerName)
                .setWorkflowUuid(workflowUuid)
                .build());

        return new Subflow(remoteSubflow.getWorkflowId(), remoteSubflow.getInputsName());
    }
}
