/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import java.util.List;
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
public class ApiCollectionEndpointServiceImpl implements ApiCollectionEndpointService {

    private final ApiEndpointRepository apiEndpointRepository;

    public ApiCollectionEndpointServiceImpl(ApiEndpointRepository apiEndpointRepository) {
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Override
    public ApiCollectionEndpoint create(@NonNull ApiCollectionEndpoint apiCollectionEndpoint) {
        Validate.notNull(apiCollectionEndpoint, "'apiCollectionEndpoint' must not be null");
        Validate.notNull(apiCollectionEndpoint.getApiCollectionId(), "'apiCollectionId' must not be null");
        Validate.notNull(apiCollectionEndpoint.getHttpMethod(), "'httpMethod' must not be null");
        Validate.isTrue(apiCollectionEndpoint.getId() == null, "'id' must be null");
        Validate.notNull(apiCollectionEndpoint.getPath(), "'path' must not be null");
        Validate.notNull(apiCollectionEndpoint.getWorkflowReferenceCode(), "'workflowReferenceCode' must not be null");

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
    public ApiCollectionEndpoint update(@NonNull ApiCollectionEndpoint apiCollectionEndpoint) {
        Validate.notNull(apiCollectionEndpoint, "'apiCollectionEndpoint' must not be null");

        ApiCollectionEndpoint curApiCollectionEndpoint =
            getOpenApiEndpoint(Validate.notNull(apiCollectionEndpoint.getId(), "id"));

        curApiCollectionEndpoint.setHttpMethod(apiCollectionEndpoint.getHttpMethod());
        curApiCollectionEndpoint.setName(apiCollectionEndpoint.getName());
        curApiCollectionEndpoint.setPath(apiCollectionEndpoint.getPath());

        return apiEndpointRepository.save(curApiCollectionEndpoint);
    }
}
