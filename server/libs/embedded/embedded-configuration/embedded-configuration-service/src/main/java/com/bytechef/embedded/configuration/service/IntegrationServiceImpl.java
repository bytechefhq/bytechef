/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.repository.IntegrationRepository;
import java.util.List;
import java.util.Optional;
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
    public long countIntegrations() {
        return integrationRepository.count();
    }

    @Override
    public Integration create(Integration integration) {
        Validate.notNull(integration, "'integration' must not be null");
        Validate.isTrue(integration.getId() == null, "'id' must be null");
        Validate.notNull(integration.getComponentName(), "'componentName' must not be null");

        integration.setIntegrationVersion(1);
        integration.setStatus(Integration.Status.UNPUBLISHED);

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
    public Optional<Integration> fetchIntegration(String name) {
        return integrationRepository.findByComponentNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIntegrationEnabled(long integrationId) {
        Integration integration = getIntegration(integrationId);

        return integration.getPublishedDate() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integration getIntegrationInstanceIntegration(long integrationInstanceId) {
        return integrationRepository.findByIntegrationInstanceId(integrationInstanceId);
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
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations(Long categoryId, List<Long> ids, Long tagId, Boolean published) {
        Iterable<Integration> integrationIterable;

        if (categoryId == null && tagId == null) {
            integrationIterable = integrationRepository.findAll(Sort.by("componentName"));
        } else if (categoryId != null && tagId == null) {
            integrationIterable = integrationRepository.findAllByCategoryIdOrderByComponentName(categoryId);
        } else if (categoryId == null) {
            integrationIterable = integrationRepository.findAllByTagIdOrderByName(tagId);
        } else {
            integrationIterable = integrationRepository.findAllByCategoryIdAndTagIdOrderByComponentName(
                categoryId, tagId);
        }

        List<Integration> integrations = CollectionUtils.toList(integrationIterable);

        if (published != null) {
            if (published) {
                integrations = CollectionUtils.filter(
                    integrations, integration -> integration.getPublishedDate() != null);
            } else {
                integrations = CollectionUtils.filter(
                    integrations, integration -> integration.getPublishedDate() == null);
            }
        }

        if (ids != null) {
            integrations = integrations.stream()
                .filter(integration -> ids.contains(integration.getId()))
                .toList();
        }

        return integrations;
    }

    @Override
    public Integration getWorkflowIntegration(String workflowId) {
        return OptionalUtils.get(integrationRepository.findByWorkflowId(workflowId));
    }

    @Override
    public Integration publish(long id) {
        return null;
    }

    @Override
    public void removeWorkflow(long id, String workflowId) {
        Integration integration = getIntegration(id);

        integration.removeWorkflow(workflowId);

        update(integration);
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

        curIntegration.setCategoryId(integration.getCategoryId());
        curIntegration.setId(integration.getId());
        curIntegration.setOverview(integration.getOverview());
        curIntegration.setTagIds(integration.getTagIds());
        curIntegration.setWorkflowIds(integration.getWorkflowIds());

        return integrationRepository.save(curIntegration);
    }
}
