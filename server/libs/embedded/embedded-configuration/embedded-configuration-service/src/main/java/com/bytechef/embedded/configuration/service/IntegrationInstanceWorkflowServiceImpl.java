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
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceWorkflowConnectionRepository;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceWorkflowServiceImpl implements IntegrationInstanceWorkflowService {

    private final IntegrationInstanceWorkflowConnectionRepository integrationInstanceWorkflowConnectionRepository;
    private final IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceWorkflowServiceImpl(
        IntegrationInstanceWorkflowConnectionRepository integrationInstanceWorkflowConnectionRepository,
        IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository) {

        this.integrationInstanceWorkflowConnectionRepository = integrationInstanceWorkflowConnectionRepository;
        this.integrationInstanceWorkflowRepository = integrationInstanceWorkflowRepository;
    }

    @Override
    public List<IntegrationInstanceWorkflow> create(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {
        return integrationInstanceWorkflowRepository.saveAll(integrationInstanceWorkflows);
    }

    @Override
    public void delete(Long id) {
        integrationInstanceWorkflowRepository.deleteById(id);
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return !integrationInstanceWorkflowConnectionRepository
            .findByConnectionId(connectionId)
            .isEmpty();
    }

    @Override
    public boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId) {
        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            getIntegrationInstanceWorkflow(integrationInstanceId, workflowId);

        return integrationInstanceWorkflow.isEnabled();
    }

    @Override
    public IntegrationInstanceWorkflowConnection getIntegrationInstanceWorkflowConnection(
        long integrationInstanceId, String workflowId, String operationName, String key) {

        return OptionalUtils.get(
            integrationInstanceWorkflowConnectionRepository
                .findByIntegrationInstanceIdAndWorkflowIdAndOperationNameAndKey(
                    integrationInstanceId, workflowId, operationName, key));
    }

    @Override
    public List<IntegrationInstanceWorkflowConnection> getIntegrationInstanceWorkflowConnections(
        Long integrationInstanceId, String workflowId, String operationName) {

        return integrationInstanceWorkflowConnectionRepository
            .findAllByIntegrationInstanceIdAndWorkflowIdAndOperationName(
                integrationInstanceId, workflowId, operationName);
    }

    @Override
    public IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        return OptionalUtils.get(
            integrationInstanceWorkflowRepository.findByIntegrationInstanceIdAndWorkflowId(integrationInstanceId,
                workflowId));
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        return integrationInstanceWorkflowRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(List<Long> integrationInstanceIds) {
        Validate.notNull(integrationInstanceIds, "'integrationInstanceIds' must not be null");

        return integrationInstanceWorkflowRepository.findAllByIntegrationInstanceIdIn(integrationInstanceIds);
    }

    @Override
    public IntegrationInstanceWorkflow update(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        IntegrationInstanceWorkflow curIntegrationInstanceWorkflow = OptionalUtils.get(
            integrationInstanceWorkflowRepository
                .findById(Validate.notNull(integrationInstanceWorkflow.getId(), "id")));

        curIntegrationInstanceWorkflow.setConnections(integrationInstanceWorkflow.getConnections());
        curIntegrationInstanceWorkflow.setEnabled(integrationInstanceWorkflow.isEnabled());
        curIntegrationInstanceWorkflow.setInputs(integrationInstanceWorkflow.getInputs());
        curIntegrationInstanceWorkflow.setVersion(integrationInstanceWorkflow.getVersion());

        return integrationInstanceWorkflowRepository.save(curIntegrationInstanceWorkflow);
    }

    @Override
    public List<IntegrationInstanceWorkflow> update(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {
        Validate.notNull(integrationInstanceWorkflows, "'integrationInstanceWorkflows' must not be null");

        List<IntegrationInstanceWorkflow> updatedIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            updatedIntegrationInstanceWorkflows.add(update(integrationInstanceWorkflow));
        }

        return updatedIntegrationInstanceWorkflows;
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowRepository.findById(id)
            .orElseThrow();

        integrationInstanceWorkflow.setEnabled(enabled);

        integrationInstanceWorkflowRepository.save(integrationInstanceWorkflow);
    }
}
