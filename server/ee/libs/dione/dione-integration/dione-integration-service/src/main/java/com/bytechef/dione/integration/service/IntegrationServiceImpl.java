
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

package com.bytechef.dione.integration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.dione.integration.domain.Integration;
import com.bytechef.dione.integration.repository.IntegrationRepository;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Service
public class IntegrationServiceImpl implements IntegrationService {

    private final IntegrationRepository integrationRepository;

    public IntegrationServiceImpl(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public Integration addWorkflow(long id, String workflowId) {
        Assert.notNull(workflowId, "'workflowId' must not be null");

        Integration integration = getIntegration(id);

        integration.addWorkflowId(workflowId);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration create(Integration integration) {
        Assert.notNull(integration, "'integration' must not be null");

        Assert.isNull(integration.getId(), "'id' must be null");
        Assert.notNull(integration.getName(), "'name' must not be null");

        integration.setIntegrationVersion(1);
        integration.setStatus(Integration.Status.UNPUBLISHED);

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(long id) {
        integrationRepository.delete(getIntegration(id));
    }

    @Override
    public Integration getIntegration(long id) {
        return OptionalUtils.get(integrationRepository.findById(id));
    }

    @Override
    public List<Integration> searchIntegrations(List<Long> categoryIds, List<Long> tagIds) {
        Iterable<Integration> integrationIterable;

        if (CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            integrationIterable = integrationRepository.findAll(Sort.by("name"));
        } else if (!CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            integrationIterable = integrationRepository.findAllByCategoryIdInOrderByName(categoryIds);
        } else if (CollectionUtils.isEmpty(categoryIds)) {
            integrationIterable = integrationRepository.findAllByTagIdInOrderByName(tagIds);
        } else {
            integrationIterable = integrationRepository.findAllByCategoryIdsAndTagIdsOrderByName(categoryIds, tagIds);
        }

        return com.bytechef.commons.util.CollectionUtils.toList(integrationIterable);
    }

    @Override
    public Integration update(long id, List<Long> tagIds) {
        Integration integration = getIntegration(id);

        integration.setTagIds(tagIds);

        return integrationRepository.save(integration);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Integration update(
        long id, Long categoryId, String description, String name, List<Long> tagIds, List<String> workflowIds) {

        Assert.notNull(name, "'name' must not be null");

        Integration integration = getIntegration(id);

        integration.setCategoryId(categoryId);
        integration.setDescription(description);
        integration.setId(id);
        integration.setName(name);
        integration.setTagIds(tagIds);
        integration.setWorkflowIds(workflowIds);

        return integrationRepository.save(integration);
    }
}
