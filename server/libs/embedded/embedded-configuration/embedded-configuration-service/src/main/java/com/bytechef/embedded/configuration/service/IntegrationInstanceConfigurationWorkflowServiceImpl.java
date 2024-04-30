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
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowConnectionRepository;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceConfigurationWorkflowServiceImpl
    implements IntegrationInstanceConfigurationWorkflowService {

    private final IntegrationInstanceConfigurationWorkflowConnectionRepository integrationInstanceConfigurationWorkflowConnectionRepository;
    private final IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationWorkflowServiceImpl(
        IntegrationInstanceConfigurationWorkflowConnectionRepository integrationInstanceConfigurationWorkflowConnectionRepository,
        IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository) {

        this.integrationInstanceConfigurationWorkflowConnectionRepository =
            integrationInstanceConfigurationWorkflowConnectionRepository;
        this.integrationInstanceConfigurationWorkflowRepository = integrationInstanceConfigurationWorkflowRepository;
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow create(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        return integrationInstanceConfigurationWorkflowRepository.save(integrationInstanceConfigurationWorkflow);
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> create(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        return integrationInstanceConfigurationWorkflowRepository.saveAll(integrationInstanceConfigurationWorkflows);
    }

    @Override
    public void delete(Long id) {
        integrationInstanceConfigurationWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntegrationInstanceConfigurationWorkflowConnection>
        fetchIntegrationInstanceConfigurationWorkflowConnection(
            long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
            String workflowConnectionKey) {

        return integrationInstanceConfigurationWorkflowConnectionRepository
            .findByIntegrationInstanceConfigurationIdAndWorkflowIdAndWorkflowNodeNameAndKey(
                integrationInstanceConfigurationId, workflowId, workflowNodeName, workflowConnectionKey);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfigurationWorkflowConnection getIntegrationInstanceConfigurationWorkflowConnection(
        long integrationInstanceConfigurationId, String workflowId, String operationName, String key) {

        return OptionalUtils.get(
            integrationInstanceConfigurationWorkflowConnectionRepository
                .findByIntegrationInstanceIdAndWorkflowIdAndOperationNameAndKey(
                    integrationInstanceConfigurationId, workflowId, operationName, key));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfigurationWorkflowConnection>
        getIntegrationInstanceConfigurationWorkflowConnections(
            Long integrationInstanceConfigurationId, String workflowId, String operationName) {

        return integrationInstanceConfigurationWorkflowConnectionRepository
            .findAllByIntegrationInstanceIdAndWorkflowIdAndOperationName(
                integrationInstanceConfigurationId, workflowId, operationName);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId) {

        Validate.notNull(workflowId, "'workflowId' must not be null");

        return OptionalUtils.get(
            integrationInstanceConfigurationWorkflowRepository.findByIntegrationInstanceConfigurationIdAndWorkflowId(
                integrationInstanceConfigurationId, workflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId) {

        return integrationInstanceConfigurationWorkflowRepository.findAllByIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        List<Long> integrationInstanceConfigurationIds) {

        Validate.notNull(integrationInstanceConfigurationIds, "'integrationInstanceIds' must not be null");

        return integrationInstanceConfigurationWorkflowRepository.findAllByIntegrationInstanceConfigurationIdIn(
            integrationInstanceConfigurationIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectionUsed(long connectionId) {
        return !integrationInstanceConfigurationWorkflowConnectionRepository
            .findByConnectionId(connectionId)
            .isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            getIntegrationInstanceConfigurationWorkflow(integrationInstanceId, workflowId);

        return integrationInstanceConfigurationWorkflow.isEnabled();
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow update(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        IntegrationInstanceConfigurationWorkflow curIntegrationInstanceConfigurationWorkflow = OptionalUtils.get(
            integrationInstanceConfigurationWorkflowRepository
                .findById(Validate.notNull(integrationInstanceConfigurationWorkflow.getId(), "id")));

        curIntegrationInstanceConfigurationWorkflow.setConnections(
            integrationInstanceConfigurationWorkflow.getConnections());
        curIntegrationInstanceConfigurationWorkflow.setEnabled(integrationInstanceConfigurationWorkflow.isEnabled());
        curIntegrationInstanceConfigurationWorkflow.setInputs(integrationInstanceConfigurationWorkflow.getInputs());
        curIntegrationInstanceConfigurationWorkflow.setVersion(integrationInstanceConfigurationWorkflow.getVersion());

        return integrationInstanceConfigurationWorkflowRepository.save(curIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> update(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        Validate.notNull(integrationInstanceConfigurationWorkflows, "'integrationInstanceWorkflows' must not be null");

        List<IntegrationInstanceConfigurationWorkflow> updatedIntegrationInstanceConfigurationWorkflows =
            new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {
            updatedIntegrationInstanceConfigurationWorkflows.add(update(integrationInstanceConfigurationWorkflow));
        }

        return updatedIntegrationInstanceConfigurationWorkflows;
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowRepository.findById(id)
                .orElseThrow();

        integrationInstanceConfigurationWorkflow.setEnabled(enabled);

        integrationInstanceConfigurationWorkflowRepository.save(integrationInstanceConfigurationWorkflow);
    }
}
