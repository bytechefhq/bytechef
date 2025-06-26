/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import java.util.List;
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
public class ApiCollectionEndpointServiceImpl implements ApiCollectionEndpointService {

    private final ApiEndpointRepository apiEndpointRepository;

    public ApiCollectionEndpointServiceImpl(ApiEndpointRepository apiEndpointRepository) {
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Override
    public ApiCollectionEndpoint create(ApiCollectionEndpoint apiCollectionEndpoint) {
        Assert.notNull(apiCollectionEndpoint, "'apiCollectionEndpoint' must not be null");
        Assert.notNull(apiCollectionEndpoint.getApiCollectionId(), "'apiCollectionId' must not be null");
        Assert.notNull(apiCollectionEndpoint.getHttpMethod(), "'httpMethod' must not be null");
        Assert.isTrue(apiCollectionEndpoint.getId() == null, "'id' must be null");
        Assert.notNull(apiCollectionEndpoint.getPath(), "'path' must not be null");

        return apiEndpointRepository.save(apiCollectionEndpoint);
    }

    @Override
    public void delete(long id) {
        apiEndpointRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiCollectionEndpoint getOpenApiEndpoint(long id) {
        return OptionalUtils.get(apiEndpointRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiCollectionEndpoint> getApiEndpoints(long apiCollectionId) {
        return apiEndpointRepository.findByApiCollectionIdOrderByHttpMethod(apiCollectionId);
    }

    @Override
    public ApiCollectionEndpoint update(ApiCollectionEndpoint apiCollectionEndpoint) {
        Assert.notNull(apiCollectionEndpoint, "'apiCollectionEndpoint' must not be null");
        Assert.notNull(apiCollectionEndpoint.getId(), "id");

        ApiCollectionEndpoint curApiCollectionEndpoint = getOpenApiEndpoint(apiCollectionEndpoint.getId());

        curApiCollectionEndpoint.setHttpMethod(apiCollectionEndpoint.getHttpMethod());
        curApiCollectionEndpoint.setName(apiCollectionEndpoint.getName());
        curApiCollectionEndpoint.setPath(apiCollectionEndpoint.getPath());

        return apiEndpointRepository.save(curApiCollectionEndpoint);
    }
}
