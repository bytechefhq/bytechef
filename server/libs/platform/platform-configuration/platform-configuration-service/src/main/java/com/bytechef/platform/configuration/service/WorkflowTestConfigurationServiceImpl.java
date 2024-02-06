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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    @Transactional(readOnly = true)
    public Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId) {
        return workflowTestConfigurationRepository.findByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName) {

        return workflowTestConfigurationConnectionRepository.findByWorkflowIdAndWorkflowNodeName(
            workflowId, workflowNodeName);
    }

    @Override
    public WorkflowTestConfiguration saveWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        return workflowTestConfigurationRepository.save(
            workflowTestConfigurationRepository
                .findByWorkflowId(Validate.notNull(workflowTestConfiguration.getWorkflowId(), "workflowId("))
                .map(curWorkflowTestConfiguration -> {
                    curWorkflowTestConfiguration.setConnections(workflowTestConfiguration.getConnections());
                    curWorkflowTestConfiguration.setInputs(workflowTestConfiguration.getInputs());

                    return curWorkflowTestConfiguration;
                })
                .orElse(workflowTestConfiguration));
    }

    @Override
    public void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId) {

        WorkflowTestConfiguration workflowTestConfiguration = getWorkflowTestConfiguration(workflowId);

        WorkflowTestConfigurationConnection workflowTestConfigurationConnection =
            new WorkflowTestConfigurationConnection(connectionId, key, workflowNodeName);

        workflowTestConfiguration.setConnections(
            CollectionUtils.concat(
                CollectionUtils.filter(
                    workflowTestConfiguration.getConnections(),
                    connection -> !(Objects.equals(connection.getWorkflowConnectionKey(), key) &&
                        Objects.equals(connection.getWorkflowNodeName(), workflowNodeName))),
                List.of(workflowTestConfigurationConnection)));

        workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, Map<String, String> inputs) {
        WorkflowTestConfiguration workflowTestConfiguration = getWorkflowTestConfiguration(workflowId);

        workflowTestConfiguration.setInputs(inputs);

        workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    @Override
    public Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId) {
        return fetchWorkflowTestConfiguration(workflowId)
            .map(WorkflowTestConfiguration::getInputs)
            .orElse(Map.of());
    }

    @Override
    public Optional<Long> fetchWorkflowTestConfigurationConnectionId(String workflowId, String workflowNodeName) {
        return fetchWorkflowTestConfiguration(workflowId)
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of())
            .stream()
            .filter(curConnection -> Objects.equals(curConnection.getWorkflowNodeName(), workflowNodeName))
            .findFirst()
            .map(WorkflowTestConfigurationConnection::getConnectionId);
    }

    private WorkflowTestConfiguration getWorkflowTestConfiguration(String workflowId) {
        return OptionalUtils.orElseGet(
            workflowTestConfigurationRepository.findByWorkflowId(workflowId),
            () -> {
                WorkflowTestConfiguration newWorkflowTestConfiguration = new WorkflowTestConfiguration();

                newWorkflowTestConfiguration.setWorkflowId(workflowId);

                return newWorkflowTestConfiguration;
            });
    }
}
