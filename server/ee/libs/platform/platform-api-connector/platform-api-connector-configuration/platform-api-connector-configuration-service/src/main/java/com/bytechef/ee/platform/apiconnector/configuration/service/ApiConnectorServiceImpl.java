/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.repository.ApiConnectorRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
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
public class ApiConnectorServiceImpl implements ApiConnectorService {

    private final ApiConnectorRepository apiConnectorRepository;

    public ApiConnectorServiceImpl(ApiConnectorRepository apiConnectorRepository) {
        this.apiConnectorRepository = apiConnectorRepository;
    }

    @Override
    public ApiConnector create(@NonNull ApiConnector apiConnector) {
        Validate.notNull(apiConnector, "'openApiConnector' must not be null");
        Validate.notNull(apiConnector.getDefinition(), "'definition' must not be null");
        Validate.isTrue(apiConnector.getId() == null, "'id' must be null");
        Validate.notNull(apiConnector.getName(), "'componentName' must not be null");
        Validate.notNull(apiConnector.getSpecification(), "'specification' must not be null");

        return apiConnectorRepository.save(apiConnector);
    }

    @Override
    public void delete(long id) {
        apiConnectorRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiConnector getApiConnector(long id) {
        return OptionalUtils.get(apiConnectorRepository.findById(id));
    }

    @Override
    public Optional<ApiConnector> fetchApiConnector(String name, int connectorVersion) {
        return apiConnectorRepository.findByNameAndConnectorVersion(name, connectorVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiConnector> getApiConnectors() {
        return apiConnectorRepository.findAll(Sort.by("title", "componentName", "componentVersion"));
    }

    @Override
    public ApiConnector update(@NonNull ApiConnector apiConnector) {
        Validate.notNull(apiConnector, "'openApiConnector' must not be null");

        ApiConnector curApiConnector = getApiConnector(Validate.notNull(apiConnector.getId(), "id"));

        curApiConnector.setDescription(apiConnector.getDescription());
        curApiConnector.setSpecification(apiConnector.getSpecification());
        curApiConnector.setIcon(curApiConnector.getIcon());
        curApiConnector.setName(Validate.notNull(apiConnector.getName(), "componentName"));

        return apiConnectorRepository.save(curApiConnector);
    }

    @Override
    public ApiConnector enableApiConnector(Long id, boolean enable) {
        ApiConnector apiConnector = getApiConnector(Validate.notNull(id, "id"));

        apiConnector.setEnabled(enable);

        return apiConnectorRepository.save(apiConnector);
    }
}
