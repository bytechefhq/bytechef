
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.dione.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.dione.configuration.domain.Integration;
import com.bytechef.dione.configuration.repository.IntegrationRepository;

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
