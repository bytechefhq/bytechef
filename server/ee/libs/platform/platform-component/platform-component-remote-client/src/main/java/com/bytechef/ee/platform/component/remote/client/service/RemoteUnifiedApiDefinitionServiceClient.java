/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.ee.platform.component.remote.client.WorkerInstances;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetUnifiedApiComponentDefinitionsRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinition;
import com.bytechef.ee.platform.component.remote.grpc.proto.RemoteComponentDefinitions;
import com.bytechef.ee.platform.component.remote.grpc.proto.UnifiedApiDefinitionRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.UnifiedApiDefinitionRemoteServiceGrpc.UnifiedApiDefinitionRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteUnifiedApiDefinitionServiceClient implements UnifiedApiDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(RemoteUnifiedApiDefinitionServiceClient.class);

    private final ComponentProtoMapper componentProtoMapper;
    private final GrpcChannels grpcChannels;
    private final WorkerInstances workerInstances;

    public RemoteUnifiedApiDefinitionServiceClient(
        GrpcChannels grpcChannels, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.componentProtoMapper = new ComponentProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.workerInstances = new WorkerInstances(discoveryClient);
    }

    @Override
    public List<ComponentDefinition> getUnifiedApiComponentDefinitions(UnifiedApiCategory category) {
        GetUnifiedApiComponentDefinitionsRequest.Builder builder =
            GetUnifiedApiComponentDefinitionsRequest.newBuilder();

        if (category != null) {
            builder.setCategory(category.name());
        }

        GetUnifiedApiComponentDefinitionsRequest getUnifiedApiComponentDefinitionsRequest = builder.build();

        Map<String, ComponentDefinition> componentDefinitionMap = new LinkedHashMap<>();
        Set<ServiceInstance> serviceInstances = workerInstances.getInstances();
        StatusRuntimeException lastStatusRuntimeException = null;
        int failedInstanceCount = 0;

        for (ServiceInstance serviceInstance : serviceInstances) {
            RemoteComponentDefinitions remoteComponentDefinitions;

            try {
                remoteComponentDefinitions = getStub(serviceInstance).getUnifiedApiComponentDefinitions(
                    getUnifiedApiComponentDefinitionsRequest);
            } catch (StatusRuntimeException statusRuntimeException) {
                log.warn(
                    "Skipping worker instance {} while listing unified API component definitions: {}",
                    serviceInstance.getInstanceId(), statusRuntimeException.getStatus());

                lastStatusRuntimeException = statusRuntimeException;
                failedInstanceCount++;

                continue;
            }

            for (RemoteComponentDefinition remoteComponentDefinition : remoteComponentDefinitions
                .getComponentDefinitionsList()) {

                String key = remoteComponentDefinition.getName() + ":" + remoteComponentDefinition.getVersion();

                if (!componentDefinitionMap.containsKey(key)) {
                    componentDefinitionMap.put(
                        key, componentProtoMapper.toComponentDefinition(remoteComponentDefinition));
                }
            }
        }

        if (lastStatusRuntimeException != null && failedInstanceCount == serviceInstances.size()) {
            throw lastStatusRuntimeException;
        }

        return new ArrayList<>(componentDefinitionMap.values());
    }

    @Override
    public ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel>
        getUnifiedApiProviderModelAdapter(
            String componentName, UnifiedApiCategory category, ModelType modelType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public
        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>
        getUnifiedApiProviderModelMapper(
            String componentName, UnifiedApiCategory category, ModelType modelType) {

        throw new UnsupportedOperationException();
    }

    private UnifiedApiDefinitionRemoteServiceBlockingStub getStub(ServiceInstance serviceInstance) {
        return UnifiedApiDefinitionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(serviceInstance));
    }
}
