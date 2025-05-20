/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
public class IntegrationServiceImpl implements IntegrationService {

    private final IntegrationRepository integrationRepository;

    public IntegrationServiceImpl(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public Integration create(Integration integration) {
        Assert.notNull(integration, "'integration' must not be null");
        Assert.isTrue(integration.getId() == null, "'id' must be null");
        Assert.notNull(integration.getComponentName(), "'componentName' must not be null");

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(long id) {
        integrationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Integration getIntegration(long id) {
        return OptionalUtils.get(integrationRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Integration getIntegrationInstanceIntegration(long integrationInstanceId) {
        return integrationRepository.findByIntegrationInstanceId(integrationInstanceId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Integration not found for integrationInstanceId: " + integrationInstanceId));
    }

    @Override
    public Integration getIntegrationInstanceConfigurationIntegration(long integrationInstanceConfigurationId) {
        return integrationRepository.findByIntegrationInstanceConfigurationId(integrationInstanceConfigurationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Integration not found for integrationInstanceConfigurationId: " + integrationInstanceConfigurationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations() {
        return CollectionUtils.toList(integrationRepository.findAll(Sort.by("componentName")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations(List<Long> ids) {
        return CollectionUtils.toList(integrationRepository.findAllById(ids));
    }

    @Override
    public List<IntegrationVersion> getIntegrationVersions(Long id) {
        Integration integration = getIntegration(id);

        return integration.getIntegrationVersions()
            .stream()
            .sorted((o1, o2) -> Integer.compare(o2.getVersion(), o1.getVersion()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations(Long categoryId, List<Long> ids, Long tagId, Status status) {
        return integrationRepository.findAllIntegrations(
            categoryId, ids, tagId, status == null ? null : status.ordinal());
    }

    @Override
    public Integration getWorkflowIntegration(String workflowId) {
        return OptionalUtils.get(integrationRepository.findByWorkflowId(workflowId));
    }

    @Override
    public int publishIntegration(long id, String description) {
        Integration integration = getIntegration(id);

        int newVersion = integration.publish(description);

        integrationRepository.save(integration);

        return newVersion;
    }

    @Override
    public Integration update(long id, List<Long> tagIds) {
        Integration integration = getIntegration(id);

        integration.setTagIds(tagIds);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration update(Integration integration) {
        Integration curIntegration = getIntegration(Validate.notNull(integration.getId(), "id"));

        curIntegration.setMultipleInstances(integration.isMultipleInstances());
        curIntegration.setCategoryId(integration.getCategoryId());
        curIntegration.setDescription(integration.getDescription());
        curIntegration.setName(integration.getName());
        curIntegration.setTagIds(integration.getTagIds());
        curIntegration.setVersion(integration.getVersion());

        return integrationRepository.save(curIntegration);
    }
}
