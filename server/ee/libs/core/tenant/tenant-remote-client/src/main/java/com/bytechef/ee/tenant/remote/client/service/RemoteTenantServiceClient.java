/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.constant.Tenancy;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * Multi-tenant {@link TenantService} binding for apps that have no datasource of their own. Resolves the tenant list
 * over REST from configuration-app instead of reading the database directly, so an app such as coordinator-app — whose
 * per-tenant sweeps (orphaned-job recovery, job timeout, retention, approval expiry/reminder/escalation) iterate
 * {@link #getTenantIds()} — can run multi-tenant while staying datasource-free.
 *
 * <p>
 * Only the read used by those sweeps is implemented. Tenant lifecycle operations (create/delete/changelog) and
 * user-lookup reads belong to the app that owns the datasource and are intentionally unsupported here, matching the
 * convention used by the other remote clients.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMultiTenant
public class RemoteTenantServiceClient implements TenantService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String TENANT_SERVICE = "/remote/tenant-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTenantServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<String> getTenantIds() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(TENANT_SERVICE + "/get-tenant-ids")
                .build(),
            new ParameterizedTypeReference<>() {});
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
