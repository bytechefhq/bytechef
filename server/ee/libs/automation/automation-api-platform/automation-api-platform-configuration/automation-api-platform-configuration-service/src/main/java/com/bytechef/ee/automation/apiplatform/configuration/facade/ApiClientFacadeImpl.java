/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API platform clients belong to the tenant, not to a workspace, so only a tenant admin manages them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiClientFacadeImpl implements ApiClientFacade {

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiClientFacadeImpl(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public String createApiClient(ApiClient apiClient) {
        return apiClientService.create(apiClient);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteApiClient(long id) {
        apiClientService.delete(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public ApiClient getApiClient(long id) {
        return apiClientService.getApiClient(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<ApiClient> getApiClients() {
        return apiClientService.getApiClients();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ApiClient updateApiClient(ApiClient apiClient) {
        return apiClientService.update(apiClient);
    }
}
