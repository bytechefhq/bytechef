
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

package com.bytechef.hermes.integration.service.impl;

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.repository.IntegrationRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.bytechef.hermes.integration.service.IntegrationService;
import com.bytechef.tag.domain.Tag;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
    public Integration addWorkflow(Long id, String workflowId) {
        Integration integration = getIntegration(id);

        integration.addWorkflow(workflowId);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration create(Integration integration) {
        Assert.notNull(integration, "'integration' must not be null.");

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(long id) {
        integrationRepository.deleteById(id);
    }

    @Override
    public Integration getIntegration(long id) {
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
    public Integration update(Long id, Set<Tag> tags) {
        Integration integration = getIntegration(id);

        integration.setTags(tags);

        return integrationRepository.save(integration);
    }

    @Override
    public Integration update(Integration integration) {
        Assert.notNull(integration, "'integration' must not be null.");
        Assert.notEmpty(integration.getWorkflowIds(), "'workflowIds' must not be empty.");

        return integrationRepository.save(integration);
    }
}
