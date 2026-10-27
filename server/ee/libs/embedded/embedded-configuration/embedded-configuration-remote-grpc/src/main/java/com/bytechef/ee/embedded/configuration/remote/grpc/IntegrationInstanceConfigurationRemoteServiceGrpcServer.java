/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationsFilterRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteIntegrationInstanceConfigurations;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceConfigurationRemoteServiceGrpcServer
    extends IntegrationInstanceConfigurationRemoteServiceGrpc.IntegrationInstanceConfigurationRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationRemoteServiceGrpcServer(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
    }

    @Override
    public void getIntegrationInstanceConfiguration(
        IdRequest request, StreamObserver<RemoteIntegrationInstanceConfiguration> responseObserver) {
        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toProto(
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurations(
        IdsRequest request, StreamObserver<RemoteIntegrationInstanceConfigurations> responseObserver) {

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationInstanceConfigurations(
                integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(request.getIdsList())));
        responseObserver.onCompleted();
    }

    @Override
    public void getIntegrationInstanceConfigurationsByFilter(
        IntegrationInstanceConfigurationsFilterRequest request,
        StreamObserver<RemoteIntegrationInstanceConfigurations> responseObserver) {

        Environment environment = request.hasEnvironment()
            ? EmbeddedConfigurationProtoMapper.toEnvironment(request.getEnvironment()) : null;
        Long integrationId = request.hasIntegrationId() ? request.getIntegrationId() : null;
        Long tagId = request.hasTagId() ? request.getTagId() : null;

        responseObserver.onNext(
            embeddedConfigurationProtoMapper.toRemoteIntegrationInstanceConfigurations(
                integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                    environment, integrationId, tagId)));
        responseObserver.onCompleted();
    }

    @Override
    public void isIntegrationInstanceConfigurationEnabled(
        IdRequest request, StreamObserver<BoolResponse> responseObserver) {

        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(
                    integrationInstanceConfigurationService.isIntegrationInstanceConfigurationEnabled(request.getId()))
                .build());
        responseObserver.onCompleted();
    }
}
