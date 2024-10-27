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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceWorkflowServiceImpl implements IntegrationInstanceWorkflowService {

    private final IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    public IntegrationInstanceWorkflowServiceImpl(
        IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository) {

        this.integrationInstanceWorkflowRepository = integrationInstanceWorkflowRepository;
    }

    @Override
    public IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId) {
        return integrationInstanceWorkflowRepository
            .findByIntegrationInstanceIdAndWorkflowId(integrationInstanceId, workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Integration instance workflow not found"));
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        return integrationInstanceWorkflowRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowRepository.findById(id)
            .orElseThrow();

        integrationInstanceWorkflow.setEnabled(enabled);

        integrationInstanceWorkflowRepository.save(integrationInstanceWorkflow);
    }
}
