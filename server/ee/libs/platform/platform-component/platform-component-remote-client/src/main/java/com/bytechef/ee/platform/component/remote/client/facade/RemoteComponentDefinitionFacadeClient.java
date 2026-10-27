/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.ComponentDefinitionFacadeRemoteServiceGrpc.ComponentDefinitionFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.ExecuteWorkflowInputOptionsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentDefinitionFacadeClient implements ComponentDefinitionFacade {

    private final ComponentProtoMapper componentProtoMapper;
    private final Duration executionDeadline;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteComponentDefinitionFacadeClient(
        ApplicationProperties applicationProperties, GrpcChannels grpcChannels, DiscoveryClient discoveryClient,
        ObjectMapper objectMapper) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();
        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.executionDeadline = grpc.getExecutionDeadline();
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId) {

        ExecuteWorkflowInputOptionsRequest.Builder builder = ExecuteWorkflowInputOptionsRequest.newBuilder()
            .setComponentName(componentName)
            .setComponentVersion(componentVersion)
            .setGroupName(groupName)
            .setPropertyName(propertyName)
            .setInputParameters(componentProtoMapper.toBytes(inputParameters))
            .addAllLookupDependsOnPaths(lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths);

        if (searchText != null) {
            builder.setSearchText(searchText);
        }

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        return componentProtoMapper.toOptions(getStub(componentName).executeWorkflowInputOptions(builder.build()));
    }

    private ComponentDefinitionFacadeRemoteServiceBlockingStub getStub(String componentName) {
        ComponentDefinitionFacadeRemoteServiceBlockingStub stub =
            ComponentDefinitionFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(workerInstances.getInstance(componentName)));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}
