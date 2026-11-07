/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ApiClientFacadeImpl implements ApiClientFacade {

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiClientFacadeImpl(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    public String create(ApiClient apiClient) {
        return apiClientService.create(apiClient);
    }

    @Override
    @PreAuthorize("isResourceOwner(#id, 'ApiClient')")
    public void delete(long id) {
        apiClientService.delete(id);
    }

    @Override
    @PreAuthorize("isResourceOwner(#id, 'ApiClient')")
    public ApiClient getApiClient(long id) {
        return apiClientService.getApiClient(id);
    }

    @Override
    @PostFilter("isResourceOwner(filterObject.id, 'ApiClient')")
    public List<ApiClient> getApiClients() {
        return new ArrayList<>(apiClientService.getApiClients());
    }

    @Override
    @PreAuthorize("isResourceOwner(#apiClient.id, 'ApiClient')")
    public ApiClient update(ApiClient apiClient) {
        return apiClientService.update(apiClient);
    }
}
