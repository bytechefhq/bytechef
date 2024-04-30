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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.constant.IntegrationErrorType;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationWorkflowServiceImpl implements IntegrationWorkflowService {

    private final IntegrationWorkflowRepository integrationWorkflowRepository;

    public IntegrationWorkflowServiceImpl(IntegrationWorkflowRepository integrationWorkflowRepository) {
        this.integrationWorkflowRepository = integrationWorkflowRepository;
    }

    @Override
    public IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        IntegrationWorkflow integration = new IntegrationWorkflow(integrationId, integrationVersion, workflowId);

        return integrationWorkflowRepository.save(integration);
    }

    @Override
    public void deleteIntegrationWorkflows(List<Long> ids) {
        integrationWorkflowRepository.deleteAllById(ids);
    }

    @Override
    public IntegrationWorkflow getIntegrationWorkflow(long id) {
        return OptionalUtils.get(integrationWorkflowRepository.findById(id));
    }

    @Override
    public List<Long> getIntegrationWorkflowIds(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, integrationVersion)
            .stream()
            .map(IntegrationWorkflow::getId)
            .toList();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows() {
        return integrationWorkflowRepository.findAll();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId) {
        return integrationWorkflowRepository.findAllByIntegrationId(integrationId);
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository.findAllByIntegrationIdAndIntegrationVersion(integrationId,
            integrationVersion);
    }

    @Override
    public List<String> getWorkflowIds(long integrationId) {
        return integrationWorkflowRepository.findAllByIntegrationId(integrationId)
            .stream()
            .map(IntegrationWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public List<String> getWorkflowIds(long integrationId, int integrationVersion) {
        return integrationWorkflowRepository
            .findAllByIntegrationIdAndIntegrationVersion(integrationId, integrationVersion)
            .stream()
            .map(IntegrationWorkflow::getWorkflowId)
            .toList();
    }

    @Override
    public IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId) {
        return OptionalUtils.get(integrationWorkflowRepository.findByWorkflowId(workflowId));
    }

    @Override
    public void removeWorkflow(long integrationId, int integrationVersion, String workflowId) {
        if (integrationWorkflowRepository.countByIntegrationIdAndIntegrationVersion(integrationId,
            integrationVersion) == 1) {
            throw new ConfigurationException(
                "The last workflow cannot be deleted", IntegrationErrorType.REMOVE_WORKFLOW);
        }

        integrationWorkflowRepository
            .findByIntegrationIdAndIntegrationVersionAndWorkflowId(integrationId, integrationVersion, workflowId)
            .ifPresent(IntegrationWorkflow -> integrationWorkflowRepository.deleteById(IntegrationWorkflow.getId()));
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow) {
        Validate.notNull(integrationWorkflow, "'IntegrationWorkflow' must not be null");
        Validate.notNull(integrationWorkflow.getId(), "'id' must not be null");

        return integrationWorkflowRepository.save(integrationWorkflow);
    }
}
