
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.service;

import com.bytechef.athena.configuration.domain.Integration;
import com.bytechef.athena.configuration.repository.IntegrationRepository;
import com.bytechef.commons.util.OptionalUtils;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
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
    public Integration addWorkflow(long id, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        Integration integration = getIntegration(id);

        integration.addWorkflowId(workflowId);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration create(Integration integration) {
        Validate.notNull(integration, "'integration' must not be null");
        Validate.isTrue(integration.getId() == null, "'id' must be null");
        Validate.notNull(integration.getName(), "'name' must not be null");

        integration.setIntegrationVersion(1);
        integration.setStatus(Integration.Status.UNPUBLISHED);

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(long id) {
        integrationRepository.delete(getIntegration(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Integration getIntegration(long id) {
        return OptionalUtils.get(integrationRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations(Long categoryId, Long tagId) {
        Iterable<Integration> integrationIterable;

        if (categoryId == null && tagId == null) {
            integrationIterable = integrationRepository.findAll(Sort.by("name"));
        } else if (categoryId != null && tagId == null) {
            integrationIterable = integrationRepository.findAllByCategoryIdOrderByName(categoryId);
        } else if (categoryId == null) {
            integrationIterable = integrationRepository.findAllByTagIdOrderByName(tagId);
        } else {
            integrationIterable = integrationRepository.findAllByCategoryIdAndTagIdOrderByName(categoryId, tagId);
        }

        return com.bytechef.commons.util.CollectionUtils.toList(integrationIterable);
    }

    @Override
    @Transactional
    public void removeWorkflow(long id, String workflowId) {
        Integration integration = getIntegration(id);

        integration.removeWorkflow(workflowId);

        update(integration);
    }

    @Override
    @Transactional
    public Integration update(long id, List<Long> tagIds) {
        Integration integration = getIntegration(id);

        integration.setTagIds(tagIds);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration update(Integration integration) {
        Integration curIntegration = getIntegration(Validate.notNull(integration.getId(), "id"));

        curIntegration.setCategoryId(integration.getCategoryId());
        curIntegration.setDescription(integration.getDescription());
        curIntegration.setId(integration.getId());
        curIntegration.setName(Validate.notNull(integration.getName(), "name"));
        curIntegration.setTagIds(integration.getTagIds());
        curIntegration.setWorkflowIds(integration.getWorkflowIds());

        return integrationRepository.save(curIntegration);
    }
}
