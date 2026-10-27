/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstances;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;

/**
 * @version ee
 */
public class IntegrationInstanceRemoteServiceGrpcServer
    extends IntegrationInstanceRemoteServiceGrpc.IntegrationInstanceRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceRemoteServiceGrpcServer(
        IntegrationInstanceService integrationInstanceService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public void getConnectedUserIntegrationInstances(
        ConnectedUserIntegrationInstancesRequest request, StreamObserver<RemoteIntegrationInstances> responseObserver) {

        List<IntegrationInstance> integrationInstances;

        if (request.hasEnabled()) {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId(), request.getEnabled());
        } else if (request.hasEnvironment()) {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId(), toEnvironment(request.getEnvironment()));
        } else {
            integrationInstances = integrationInstanceService.getConnectedUserIntegrationInstances(
                request.getConnectedUserId());
        }

        responseObserver.onNext(embeddedConfigurationProtoMapper.toProto(integrationInstances));
        responseObserver.onCompleted();
    }

    private static Environment toEnvironment(int ordinal) {
        Environment[] environments = Environment.values();

        if (ordinal < 0 || ordinal >= environments.length) {
            throw new IllegalArgumentException("Unknown environment: " + ordinal);
        }

        return environments[ordinal];
    }
}
