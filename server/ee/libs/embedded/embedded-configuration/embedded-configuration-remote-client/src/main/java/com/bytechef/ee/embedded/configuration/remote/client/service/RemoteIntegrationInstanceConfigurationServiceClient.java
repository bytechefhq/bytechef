/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdsRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceConfigurationsFilterRequest;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteIntegrationInstanceConfigurationServiceClient implements IntegrationInstanceConfigurationService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteIntegrationInstanceConfigurationServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.embeddedConfigurationProtoMapper = new EmbeddedConfigurationProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public IntegrationInstanceConfiguration create(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIntegrationInstanceConfigurationEnabled(long integrationInstanceConfigurationId) {
        BoolResponse boolResponse = getStub().isIntegrationInstanceConfigurationEnabled(
            toIdRequest(integrationInstanceConfigurationId));

        return boolResponse.getValue();
    }

    @Override
    public List<IntegrationInstanceConfiguration>
        getIntegrationInstanceConfigurations(Environment environment, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(long id) {
        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfiguration(
            getStub().getIntegrationInstanceConfiguration(toIdRequest(id)));
    }

    @Override
    public List<IntegrationInstanceConfiguration> getEnabledIntegrationInstanceConfigurations(
        long integrationId, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getIntegrationIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(long integrationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(List<Long> ids) {
        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurations(
            getStub().getIntegrationInstanceConfigurations(
                IdsRequest.newBuilder()
                    .addAllIds(ids)
                    .build()));
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(
        @Nullable Environment environment, @Nullable Long integrationId, @Nullable Long tagId) {

        IntegrationInstanceConfigurationsFilterRequest.Builder builder =
            IntegrationInstanceConfigurationsFilterRequest.newBuilder();

        if (environment != null) {
            builder.setEnvironment(environment.ordinal());
        }

        if (integrationId != null) {
            builder.setIntegrationId(integrationId);
        }

        if (tagId != null) {
            builder.setTagId(tagId);
        }

        return embeddedConfigurationProtoMapper.toIntegrationInstanceConfigurations(
            getStub().getIntegrationInstanceConfigurationsByFilter(builder.build()));
    }

    @Override
    public IntegrationInstanceConfiguration update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfiguration update(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    private IntegrationInstanceConfigurationRemoteServiceGrpc.IntegrationInstanceConfigurationRemoteServiceBlockingStub
        getStub() {
        return IntegrationInstanceConfigurationRemoteServiceGrpc
            .newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
