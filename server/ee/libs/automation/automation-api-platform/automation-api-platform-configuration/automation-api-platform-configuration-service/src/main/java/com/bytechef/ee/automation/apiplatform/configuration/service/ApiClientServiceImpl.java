/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiClientRepository;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public String create(@NonNull ApiClient apiKey) {
        Validate.isTrue(apiKey.getId() == null, "'id' must be null");
        Validate.notNull(apiKey.getName(), "'name' must not be null");

        apiKey.setSecretKey(String.valueOf(TenantKey.of()));

        apiKey = apiClientRepository.save(apiKey);

        return apiKey.getSecretKey();
    }

    @Override
    public void delete(long id) {
        apiClientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiClient> fetchApiClient(@NonNull String secretKey) {
        return apiClientRepository.findBySecretKey(secretKey);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiClient getApiClient(long id) {
        return OptionalUtils.get(apiClientRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiClient> getApiClients() {
        return apiClientRepository.findAll();
    }

    @Override
    public ApiClient update(@NonNull ApiClient apiClient) {
        ApiClient curApiClient = getApiClient(Validate.notNull(apiClient.getId(), "id"));

        curApiClient.setName(Validate.notNull(apiClient.getName(), "name"));

        return apiClientRepository.save(curApiClient);
    }
}
