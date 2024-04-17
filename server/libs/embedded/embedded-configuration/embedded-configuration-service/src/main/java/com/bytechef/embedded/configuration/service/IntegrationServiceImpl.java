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
import com.bytechef.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
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
    public void addVersion(long id, List<String> duplicatedVersionWorkflowIds) {
        Integration integration = getIntegration(id);

        integration.addVersion(duplicatedVersionWorkflowIds);

        integrationRepository.save(integration);
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

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(long id) {
        integrationRepository.deleteById(id);
    }

    @Override
    public Optional<Integration> fetchWorkflowIntegration(String workflowId) {
        return integrationRepository.findByWorkflowId(workflowId);
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
    public void publishIntegration(long id, String description) {
        Integration integration = getIntegration(id);

        integration.publish(description);

        integrationRepository.save(integration);
    }

    @Override
    public void removeWorkflow(long id, String workflowId) {
        Integration integration = getIntegration(id);

        integration.removeWorkflow(workflowId);

        integrationRepository.save(integration);
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

        curIntegration.setAllowMultipleInstances(integration.isAllowMultipleInstances());
        curIntegration.setCategoryId(integration.getCategoryId());
        curIntegration.setId(integration.getId());
        curIntegration.setTagIds(integration.getTagIds());

        return integrationRepository.save(curIntegration);
    }
}
