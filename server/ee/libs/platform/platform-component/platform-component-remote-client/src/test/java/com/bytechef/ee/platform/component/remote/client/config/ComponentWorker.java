/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.config;

import com.bytechef.ee.platform.component.remote.grpc.ActionDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ActionDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ClusterElementDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ClusterElementDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.CodeEditorScriptInputFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.ComponentProtoMapper;
import com.bytechef.ee.platform.component.remote.grpc.ConnectionDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.TriggerDefinitionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.TriggerDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.component.remote.grpc.UnifiedApiDefinitionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.CodeEditorScriptInputFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ComponentWorker implements AutoCloseable {

    public static final String WORKER_APP = "worker-app";

    private final Map<String, AtomicInteger> callCounts = new ConcurrentHashMap<>();
    private final String componentsMetadata;
    private final ComponentWorkerServices componentWorkerServices;
    private final String instanceId;
    private final ManagedChannel managedChannel;
    private final Server server;

    public ComponentWorker(
        String instanceId, ComponentWorkerServices componentWorkerServices, ObjectMapper objectMapper)
        throws IOException {

        String name = InProcessServerBuilder.generateName();
        ComponentProtoMapper componentProtoMapper = new ComponentProtoMapper(objectMapper);

        List<BindableService> bindableServices = List.of(
            new ActionDefinitionFacadeRemoteServiceGrpcServer(
                componentWorkerServices.actionDefinitionFacade(), componentProtoMapper),
            new ActionDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.actionDefinitionService(), componentProtoMapper),
            new ClusterElementDefinitionFacadeRemoteServiceGrpcServer(
                componentWorkerServices.clusterElementDefinitionFacade(), componentProtoMapper),
            new ClusterElementDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.clusterElementDefinitionService(), componentProtoMapper),
            new CodeEditorScriptInputFacadeRemoteServiceGrpcServer(
                componentWorkerServices.codeEditorScriptInputFacade(), componentProtoMapper),
            new ComponentDefinitionFacadeRemoteServiceGrpcServer(
                componentWorkerServices.componentDefinitionFacade(), componentProtoMapper),
            new ComponentDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.componentDefinitionService(), componentProtoMapper),
            new ConnectionDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.connectionDefinitionService(), componentProtoMapper),
            new TriggerDefinitionFacadeRemoteServiceGrpcServer(
                componentWorkerServices.triggerDefinitionFacade(), componentProtoMapper),
            new TriggerDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.triggerDefinitionService(), componentProtoMapper),
            new UnifiedApiDefinitionRemoteServiceGrpcServer(
                componentWorkerServices.unifiedApiDefinitionService(), componentProtoMapper));

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(name)
            .directExecutor();

        GrpcExceptionHandlerInterceptor grpcExceptionHandlerInterceptor = new GrpcExceptionHandlerInterceptor(
            new GrpcRemoteExceptionHandler());

        for (BindableService bindableService : bindableServices) {
            inProcessServerBuilder.addService(
                ServerInterceptors.intercept(
                    bindableService, grpcExceptionHandlerInterceptor, new CallCountingServerInterceptor()));
        }

        this.server = inProcessServerBuilder.build()
            .start();

        this.managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        List<Map<String, String>> components = new ArrayList<>();

        for (String componentName : componentWorkerServices.componentNames()) {
            components.add(Map.of("name", componentName));
        }

        this.componentsMetadata = objectMapper.writeValueAsString(components);
        this.componentWorkerServices = componentWorkerServices;
        this.instanceId = instanceId;
    }

    public static DiscoveryClient discoveryClient(ComponentWorker... componentWorkers) {
        SimpleDiscoveryProperties simpleDiscoveryProperties = new SimpleDiscoveryProperties();

        register(simpleDiscoveryProperties, componentWorkers);

        return new SimpleDiscoveryClient(simpleDiscoveryProperties);
    }

    public static GrpcChannels grpcChannels(ComponentWorker... componentWorkers) {
        Map<String, Channel> channels = new LinkedHashMap<>();

        for (ComponentWorker componentWorker : componentWorkers) {
            channels.put(componentWorker.instanceId, componentWorker.managedChannel);
        }

        return new InstanceGrpcChannels(channels, componentWorkers[0].managedChannel);
    }

    public static void register(
        SimpleDiscoveryProperties simpleDiscoveryProperties, ComponentWorker... componentWorkers) {

        List<InstanceProperties> instanceProperties = new ArrayList<>();

        for (ComponentWorker componentWorker : componentWorkers) {
            instanceProperties.add(componentWorker.toInstanceProperties(componentWorker.componentsMetadata));
        }

        simpleDiscoveryProperties.setInstances(Map.of(WORKER_APP, instanceProperties));
    }

    public int getCallCount(String fullMethodName) {
        AtomicInteger callCount = callCounts.get(fullMethodName);

        return callCount == null ? 0 : callCount.get();
    }

    public ComponentWorkerServices getComponentWorkerServices() {
        return componentWorkerServices;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public ManagedChannel getManagedChannel() {
        return managedChannel;
    }

    public InstanceProperties toInstanceProperties(String components) {
        InstanceProperties instanceProperties = new InstanceProperties();

        instanceProperties.setHost(instanceId);
        instanceProperties.setInstanceId(instanceId);
        instanceProperties.setMetadata(Map.of("components", components, "grpc-port", "9090"));
        instanceProperties.setPort(8080);
        instanceProperties.setServiceId(WORKER_APP);

        return instanceProperties;
    }

    @Override
    public void close() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @SuppressFBWarnings("EI")
    public record ComponentWorkerServices(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        CodeEditorScriptInputFacade codeEditorScriptInputFacade, ComponentDefinitionFacade componentDefinitionFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, UnifiedApiDefinitionService unifiedApiDefinitionService,
        List<String> componentNames) {
    }

    private final class CallCountingServerInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

            AtomicInteger callCount = callCounts.computeIfAbsent(
                serverCall.getMethodDescriptor()
                    .getFullMethodName(),
                fullMethodName -> new AtomicInteger());

            callCount.incrementAndGet();

            return serverCallHandler.startCall(serverCall, metadata);
        }
    }

    @SuppressFBWarnings("EI")
    private record InstanceGrpcChannels(Map<String, Channel> channels, Channel defaultChannel)
        implements GrpcChannels {

        @Override
        public Channel getChannel(String serviceId) {
            return defaultChannel;
        }

        @Override
        public Channel getChannel(ServiceInstance serviceInstance) {
            Channel channel = channels.get(serviceInstance.getInstanceId());

            if (channel == null) {
                throw new IllegalStateException("No channel for worker " + serviceInstance.getInstanceId());
            }

            return channel;
        }
    }
}
