/*
 * Copyright 2025 ByteChef
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
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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
    public void delete(String workflowId) {
        workflowTestConfigurationRepository.deleteAll(
            workflowTestConfigurationRepository.findAllByWorkflowId(workflowId));
    }

    @Override
    public void delete(String workflowId, long environmentId) {
        workflowTestConfigurationRepository.findByWorkflowIdAndEnvironmentId(workflowId, environmentId)
            .ifPresent(workflowTestConfigurationRepository::delete);
    }

    @Override
    public void delete(List<String> workflowIds) {
        for (String workflowId : workflowIds) {
            delete(workflowId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId, long environmentId) {
        return workflowTestConfigurationRepository.findByWorkflowIdAndEnvironmentId(workflowId, environmentId);
    }

    @Override
    public Optional<Long> fetchWorkflowTestConfigurationConnectionId(
        String workflowId, String workflowNodeName, long environmentId) {

        return fetchWorkflowTestConfiguration(workflowId, environmentId)
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of())
            .stream()
            .filter(curConnection -> Objects.equals(curConnection.getWorkflowNodeName(), workflowNodeName))
            .findFirst()
            .map(WorkflowTestConfigurationConnection::getConnectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        return workflowTestConfigurationConnectionRepository.findByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            workflowId, workflowNodeName, environmentId);
    }

    @Override
    public List<WorkflowTestConfiguration> getWorkflowTestConfigurations(String workflowId) {
        return workflowTestConfigurationRepository.findAllByWorkflowId(workflowId);
    }

    @Override
    public Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId, long environmentId) {
        return workflowTestConfigurationRepository.findByWorkflowIdAndEnvironmentId(workflowId, environmentId)
            .map(WorkflowTestConfiguration::getInputs)
            .orElse(Map.of());
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return !workflowTestConfigurationConnectionRepository
            .findAllByConnectionId(connectionId)
            .isEmpty();
    }

    @Override
    public WorkflowTestConfiguration saveWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        return workflowTestConfigurationRepository.save(
            workflowTestConfigurationRepository
                .findByWorkflowIdAndEnvironmentId(
                    Validate.notNull(workflowTestConfiguration.getWorkflowId(), "workflowId("),
                    workflowTestConfiguration.getEnvironmentId())
                .map(curWorkflowTestConfiguration -> {
                    curWorkflowTestConfiguration.setConnections(workflowTestConfiguration.getConnections());
                    curWorkflowTestConfiguration.setInputs(workflowTestConfiguration.getInputs());

                    return curWorkflowTestConfiguration;
                })
                .orElse(workflowTestConfiguration));
    }

    @Override
    public void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId, boolean workflowNodeTrigger,
        long environmentId) {

        WorkflowTestConfiguration workflowTestConfiguration = getWorkflowTestConfiguration(workflowId, environmentId);

        List<WorkflowTestConfigurationConnection> connections = workflowTestConfiguration.getConnections();

        if (workflowNodeTrigger) {
            connections = connections.stream()
                .filter(connection -> !Objects.equals(connection.getWorkflowNodeName(), workflowNodeName))
                .toList();
        }

        WorkflowTestConfigurationConnection workflowTestConfigurationConnection =
            new WorkflowTestConfigurationConnection(connectionId, key, workflowNodeName);

        workflowTestConfiguration.setConnections(
            CollectionUtils.concat(
                CollectionUtils.filter(
                    connections,
                    connection -> !(Objects.equals(connection.getWorkflowConnectionKey(), key) &&
                        Objects.equals(connection.getWorkflowNodeName(), workflowNodeName))),
                List.of(workflowTestConfigurationConnection)));

        workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, String key, String value, long environmentId) {
        WorkflowTestConfiguration workflowTestConfiguration = getWorkflowTestConfiguration(workflowId, environmentId);

        Map<String, String> inputs = new HashMap<>(workflowTestConfiguration.getInputs());

        if (StringUtils.isEmpty(value)) {
            inputs.remove(key);
        } else {
            inputs.put(key, value);
        }

        workflowTestConfiguration.setInputs(inputs);

        workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    @Override
    public void updateWorkflowId(String oldWorkflowId, String newWorkflowId) {
        workflowTestConfigurationRepository.updateWorkflowId(oldWorkflowId, newWorkflowId);
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long environmentId) {

        WorkflowTestConfiguration workflowTestConfiguration = getWorkflowTestConfiguration(workflowId, environmentId);

        List<WorkflowTestConfigurationConnection> filtered = workflowTestConfiguration
            .getConnections()
            .stream()
            .filter(connection -> !(Objects.equals(connection.getWorkflowNodeName(), workflowNodeName)
                && Objects.equals(connection.getWorkflowConnectionKey(), key)))
            .toList();

        workflowTestConfiguration.setConnections(filtered);

        workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    private WorkflowTestConfiguration getWorkflowTestConfiguration(String workflowId, long environmentId) {
        return workflowTestConfigurationRepository.findByWorkflowIdAndEnvironmentId(workflowId, environmentId)
            .orElseGet(() -> {
                WorkflowTestConfiguration newWorkflowTestConfiguration = new WorkflowTestConfiguration();

                newWorkflowTestConfiguration.setEnvironmentId(environmentId);
                newWorkflowTestConfiguration.setWorkflowId(workflowId);

                return newWorkflowTestConfiguration;
            });
    }
}
