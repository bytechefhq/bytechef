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
import com.bytechef.ee.platform.component.remote.grpc.proto.CodeEditorScriptInputFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.CodeEditorScriptInputFacadeRemoteServiceGrpc.CodeEditorScriptInputFacadeRemoteServiceBlockingStub;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetScriptInputParametersRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.facade.CodeEditorScriptInputFacade;
import java.time.Duration;
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
public class RemoteCodeEditorScriptInputFacadeClient implements CodeEditorScriptInputFacade {

    private final ComponentProtoMapper componentProtoMapper;
    private final Duration executionDeadline;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteCodeEditorScriptInputFacadeClient(
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
    public Map<String, Object> getScriptInputParameters(
        String rootComponentName, int rootComponentVersion, String sourceComponentName, int sourceComponentVersion,
        String sourceClusterElementName, Map<String, ?> sourceInputParameters, @Nullable Long sourceConnectionId) {

        GetScriptInputParametersRequest.Builder builder = GetScriptInputParametersRequest.newBuilder()
            .setRootComponentName(rootComponentName)
            .setRootComponentVersion(rootComponentVersion)
            .setSourceComponentName(sourceComponentName)
            .setSourceComponentVersion(sourceComponentVersion)
            .setSourceClusterElementName(sourceClusterElementName)
            .setSourceInputParameters(componentProtoMapper.toBytes(sourceInputParameters));

        if (sourceConnectionId != null) {
            builder.setSourceConnectionId(sourceConnectionId);
        }

        CodeEditorScriptInputFacadeRemoteServiceBlockingStub stub = getStub(rootComponentName);

        ValueResponse valueResponse = stub.getScriptInputParameters(builder.build());

        Map<String, Object> scriptInputParameters = componentProtoMapper.toMap(valueResponse.getValue());

        return scriptInputParameters == null ? Map.of() : scriptInputParameters;
    }

    private CodeEditorScriptInputFacadeRemoteServiceBlockingStub getStub(String rootComponentName) {
        CodeEditorScriptInputFacadeRemoteServiceBlockingStub stub =
            CodeEditorScriptInputFacadeRemoteServiceGrpc.newBlockingStub(
                grpcChannels.getChannel(workerInstances.getInstance(rootComponentName)));

        if (executionDeadline == null) {
            return stub;
        }

        return stub.withDeadlineAfter(executionDeadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}
