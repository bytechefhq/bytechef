
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

package com.bytechef.hermes.integration.service;

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.repository.IntegrationRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.bytechef.tag.domain.Tag;
import org.springframework.lang.NonNull;
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
    public Integration create(
        @NonNull String name, String description, String category, @NonNull Set<String> workflowIds, Set<Tag> tags) {
        Assert.notNull(name, "'name' must not be null.");
        Assert.notEmpty(workflowIds, "'workflowIds' must not be empty.");

        Integration integration = new Integration();

        integration.setCategory(category);
        integration.setDescription(description);
        integration.setName(name);
        integration.setTags(tags);
        integration.setWorkflowIds(workflowIds);

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(@NonNull Long id) {
        Assert.notNull(id, "'id' must not be null.");

        integrationRepository.deleteById(id);
    }

    @Override
    public Integration getIntegration(@NonNull Long id) {
        Assert.notNull(id, "'id' must not be null.");

        return integrationRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public List<Integration> getIntegrations() {
        return StreamSupport.stream(integrationRepository.findAll()
            .spliterator(), false)
            .toList();
    }

    @Override
    public Integration update(
        @NonNull Long id, String name, String description, String category, Set<String> workflowIds, Set<Tag> tags) {

        Integration integration = getIntegration(id);

        if (name != null) {
            integration.setName(name);
        }

        if (category != null) {
            integration.setCategory(category);
        }

        if (description != null) {
            integration.setDescription(description);
        }

        if (tags != null) {
            integration.setTags(tags);
        }

        if (!CollectionUtils.isEmpty(workflowIds)) {
            integration.setWorkflowIds(workflowIds);
        }

        return integrationRepository.save(integration);
    }
}
