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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceWorkflowServiceImpl implements IntegrationInstanceWorkflowService {

    private final IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceWorkflowServiceImpl(
        IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository) {

        this.integrationInstanceWorkflowRepository = integrationInstanceWorkflowRepository;
    }

    @Override
    public IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId) {

        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setInputs(Map.of());
        integrationInstanceWorkflow.setIntegrationInstanceId(integrationInstanceId);
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflowId);

        return integrationInstanceWorkflowRepository.save(integrationInstanceWorkflow);
    }

    @Override
    public void delete(Long id) {
        integrationInstanceWorkflowRepository.deleteById(id);
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
        integrationInstanceWorkflowRepository.deleteByIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflowId);
    }

    @Override
    public Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, @NonNull String workflowId) {

        return integrationInstanceWorkflowRepository.findByIntegrationInstanceIdAndWorkflowId(
            integrationInstanceId, workflowId);
    }

    @Override
    public IntegrationInstanceWorkflow
        getIntegrationInstanceWorkflow(long integrationInstanceId, @NonNull String workflowId) {
        return integrationInstanceWorkflowRepository
            .findByIntegrationInstanceIdAndWorkflowId(integrationInstanceId, workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Integration instance workflow not found"));
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        return integrationInstanceWorkflowRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public void update(@NonNull IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        IntegrationInstanceWorkflow curIntegrationInstanceWorkflow = integrationInstanceWorkflowRepository
            .findById(integrationInstanceWorkflow.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Integration instance workflow id=%s not found".formatted(integrationInstanceWorkflow)));

        curIntegrationInstanceWorkflow.setEnabled(integrationInstanceWorkflow.isEnabled());
        curIntegrationInstanceWorkflow.setInputs(integrationInstanceWorkflow.getInputs());
        curIntegrationInstanceWorkflow.setIntegrationInstanceId(
            integrationInstanceWorkflow.getIntegrationInstanceId());
        curIntegrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

        integrationInstanceWorkflowRepository.save(curIntegrationInstanceWorkflow);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowRepository.findById(id)
            .orElseThrow();

        integrationInstanceWorkflow.setEnabled(enabled);

        integrationInstanceWorkflowRepository.save(integrationInstanceWorkflow);
    }
}
