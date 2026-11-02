/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.client.service;

import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.tenant.remote.grpc.proto.TenantRemoteServiceGrpc;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.constant.Tenancy;
import com.bytechef.tenant.service.TenantService;
import com.google.protobuf.Empty;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMultiTenant
public class RemoteTenantServiceClient implements TenantService {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteTenantServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public List<String> getTenantIds() {
        TenantRemoteServiceGrpc.TenantRemoteServiceBlockingStub stub = TenantRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(CONFIGURATION_APP));

        return List.copyOf(
            stub.getTenantIds(Empty.getDefaultInstance())
                .getTenantIdsList());
    }

    @Override
    public boolean isMultiTenantEnabled() {
        return true;
    }

    @Override
    public String createTenant() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTenant(String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantIdByUserActivationKey(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTenantIdsByUserEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTenantIdsByUserLogin(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantIdByUserResetKey(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadChangelog(List<String> tenantIds, Tenancy tenancy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tenantIdsByUserEmailExist(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tenantIdsByUserLoginExist(String email) {
        throw new UnsupportedOperationException();
    }
}
