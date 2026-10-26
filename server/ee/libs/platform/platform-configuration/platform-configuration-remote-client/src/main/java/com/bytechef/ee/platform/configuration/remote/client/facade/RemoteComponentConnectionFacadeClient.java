/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.ComponentConnectionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.ComponentConnectionFacadeRemoteServiceGrpc.ComponentConnectionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.WorkflowNodeRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentConnectionFacadeClient implements ComponentConnectionFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;
    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper;

    public RemoteComponentConnectionFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.platformConfigurationProtoMapper = new PlatformConfigurationProtoMapper(objectMapper);
    }

    @Override
    public ComponentConnection getClusterElementComponentConnection(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName,
        String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentConnection> getClusterElementComponentConnections(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ComponentConnection getComponentConnection(String workflowId, String workflowNodeName, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTask workflowTask) {
        return platformConfigurationProtoMapper.toComponentConnections(
            getStub().getWorkflowTaskComponentConnections(toWorkflowNodeRequest(workflowTask.toMap())));
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTrigger workflowTrigger) {
        return platformConfigurationProtoMapper.toComponentConnections(
            getStub().getWorkflowTriggerComponentConnections(toWorkflowNodeRequest(workflowTrigger.toMap())));
    }

    @Override
    public List<ComponentConnection> getWorkflowNodeComponentConnections(String workflowId, String workflowNodeName) {
        throw new UnsupportedOperationException();
    }

    private ComponentConnectionFacadeRemoteServiceBlockingStub getStub() {
        return ComponentConnectionFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private WorkflowNodeRequest toWorkflowNodeRequest(Map<String, ?> node) {
        return WorkflowNodeRequest.newBuilder()
            .setNode(platformConfigurationProtoMapper.toBytes(node))
            .build();
    }
}
