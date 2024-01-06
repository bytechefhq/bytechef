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

package com.bytechef.hermes.test.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.test.domain.WorkflowTestConfiguration;
import com.bytechef.hermes.test.domain.WorkflowTestConfigurationConnection;
import com.bytechef.hermes.test.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.hermes.test.repository.WorkflowTestConfigurationRepository;
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
public class WorkflowTestConfigurationServiceImpl implements WorkflowTestConfigurationService {

    private final WorkflowTestConfigurationRepository workflowTestConfigurationRepository;
    private final WorkflowTestConfigurationConnectionRepository workflowTestConfigurationConnectionRepository;

    public WorkflowTestConfigurationServiceImpl(
        WorkflowTestConfigurationRepository workflowTestConfigurationRepository,
        WorkflowTestConfigurationConnectionRepository workflowTestConfigurationConnectionRepository) {

        this.workflowTestConfigurationRepository = workflowTestConfigurationRepository;
        this.workflowTestConfigurationConnectionRepository = workflowTestConfigurationConnectionRepository;
    }

    @Override
    public WorkflowTestConfiguration create(WorkflowTestConfiguration workflowTestConfiguration) {
        return workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId) {
        return workflowTestConfigurationRepository.findByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String operationName) {

        return workflowTestConfigurationConnectionRepository.findByWorkflowIdAndOperationName(
            workflowId, operationName);
    }

    @Override
    public WorkflowTestConfiguration getWorkflowTestConfiguration(long id) {
        return OptionalUtils.get(workflowTestConfigurationRepository.findById(id));
    }

    @Override
    public List<WorkflowTestConfiguration> getWorkflowTestConfigurations() {
        return workflowTestConfigurationRepository.findAll();
    }

    @Override
    public WorkflowTestConfiguration updateWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        WorkflowTestConfiguration currentWorkflowTestConfiguration = OptionalUtils.get(
            workflowTestConfigurationRepository.findById(Validate.notNull(workflowTestConfiguration.getId(), "id")));

        currentWorkflowTestConfiguration.setConnections(workflowTestConfiguration.getConnections());
        currentWorkflowTestConfiguration.setInputs(workflowTestConfiguration.getInputs());

        return workflowTestConfigurationRepository.save(currentWorkflowTestConfiguration);
    }
}
