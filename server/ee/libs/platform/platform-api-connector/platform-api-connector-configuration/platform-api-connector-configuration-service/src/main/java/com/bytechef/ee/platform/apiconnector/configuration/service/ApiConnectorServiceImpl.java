/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.repository.ApiConnectorRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
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
@ConditionalOnEEVersion
public class ApiConnectorServiceImpl implements ApiConnectorService {

    private final ApiConnectorRepository apiConnectorRepository;

    public ApiConnectorServiceImpl(ApiConnectorRepository apiConnectorRepository) {
        this.apiConnectorRepository = apiConnectorRepository;
    }

    @Override
    public ApiConnector create(ApiConnector apiConnector) {
        Assert.notNull(apiConnector, "'openApiConnector' must not be null");
        Assert.notNull(apiConnector.getDefinition(), "'definition' must not be null");
        Assert.isTrue(apiConnector.getId() == null, "'id' must be null");
        Assert.notNull(apiConnector.getName(), "'componentName' must not be null");
        Assert.notNull(apiConnector.getSpecification(), "'specification' must not be null");

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
        return apiConnectorRepository.findAll(Sort.by("title", "name", "version"));
    }

    @Override
    public ApiConnector update(ApiConnector apiConnector) {
        Assert.notNull(apiConnector, "'openApiConnector' must not be null");
        Assert.notNull(apiConnector.getName(), "name");
        Assert.notNull(apiConnector.getId(), "id");

        ApiConnector curApiConnector = getApiConnector(apiConnector.getId());

        curApiConnector.setDescription(apiConnector.getDescription());
        curApiConnector.setSpecification(apiConnector.getSpecification());
        curApiConnector.setIcon(curApiConnector.getIcon());
        curApiConnector.setName(apiConnector.getName());

        return apiConnectorRepository.save(curApiConnector);
    }

    @Override
    public ApiConnector enableApiConnector(long id, boolean enable) {
        ApiConnector apiConnector = getApiConnector(id);

        apiConnector.setEnabled(enable);

        return apiConnectorRepository.save(apiConnector);
    }
}
