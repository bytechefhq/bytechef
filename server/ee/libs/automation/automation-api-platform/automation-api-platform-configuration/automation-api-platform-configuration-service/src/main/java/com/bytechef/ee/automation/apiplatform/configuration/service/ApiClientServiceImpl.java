/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiClientRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiClientServiceImpl implements ApiClientService {

    private final ApiClientRepository apiClientRepository;

    public ApiClientServiceImpl(ApiClientRepository apiClientRepository) {
        this.apiClientRepository = apiClientRepository;
    }

    @Override
    public String create(ApiClient apiKey) {
        Assert.isTrue(apiKey.getId() == null, "'id' must be null");
        Assert.notNull(apiKey.getName(), "'name' must not be null");

        apiKey.setSecretKey(String.valueOf(TenantKey.of()));

        apiKey = apiClientRepository.save(apiKey);

        return apiKey.getSecretKey();
    }

    @Override
    @PreAuthorize("isResourceOwner(#id, 'ApiClient')")
    public void delete(long id) {
        apiClientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiClient> fetchApiClient(long id) {
        return apiClientRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiClient> fetchApiClient(String secretKey, long environmentId) {
        return apiClientRepository.findBySecretKeyAndEnvironment(secretKey, (int) environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isResourceOwner(#id, 'ApiClient')")
    public ApiClient getApiClient(long id) {
        return OptionalUtils.get(apiClientRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiClient> getApiClients() {
        List<ApiClient> apiClients = apiClientRepository.findAll();

        // Owner-isolation (EE): a non-admin caller sees only the API clients they created. Tenant admins see all.
        // This mirrors the per-id ApiClient:ResourceOwner gating; a listing cannot be expressed as a single
        // hasPermission gate, so it is filtered here.
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            return apiClients;
        }

        String currentUserLogin = SecurityUtils.fetchCurrentUserLogin()
            .orElse(null);

        if (currentUserLogin == null) {
            return List.of();
        }

        return apiClients.stream()
            .filter(apiClient -> currentUserLogin.equals(apiClient.getCreatedBy()))
            .toList();
    }

    @Override
    @PreAuthorize("isResourceOwner(#apiClient.id, 'ApiClient')")
    public ApiClient update(ApiClient apiClient) {
        Assert.notNull(apiClient.getId(), "id");
        Assert.notNull(apiClient.getName(), "name");

        ApiClient curApiClient = getApiClient(apiClient.getId());

        curApiClient.setName(apiClient.getName());

        return apiClientRepository.save(curApiClient);
    }
}
