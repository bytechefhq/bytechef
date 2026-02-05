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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowTestConfigurationFacadeImpl implements WorkflowTestConfigurationFacade {

    private final ConnectionService connectionService;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowTestConfigurationFacadeImpl(
        ConnectionService connectionService, ComponentConnectionFacade componentConnectionFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectionService = connectionService;
        this.componentConnectionFacade = componentConnectionFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String workflowConnectionKey, long connectionId,
        long environmentId) {

        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, environmentId);
    }

    @Override
    public void removeUnusedWorkflowTestConfigurationConnections(Workflow workflow) {
        List<WorkflowTestConfiguration> workflowTestConfigurations = workflowTestConfigurationService
            .getWorkflowTestConfigurations(Validate.notNull(workflow.getId(), "id"));

        for (WorkflowTestConfiguration workflowTestConfiguration : workflowTestConfigurations) {
            workflowTestConfiguration.setInputs(getInputs(workflow, workflowTestConfiguration));

            List<ComponentConnection> taskComponentConnections = CollectionUtils.flatMap(
                workflow.getTasks(true), componentConnectionFacade::getComponentConnections);
            List<ComponentConnection> triggerComponentConnections = CollectionUtils.flatMap(
                WorkflowTrigger.of(workflow), componentConnectionFacade::getComponentConnections);

            workflowTestConfiguration.setConnections(
                getWorkflowTestConfigurationConnections(
                    taskComponentConnections, triggerComponentConnections, workflowTestConfiguration));

            workflowTestConfigurationService.saveWorkflowTestConfiguration(workflowTestConfiguration);
        }
    }

    @Override
    public WorkflowTestConfiguration saveWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        Workflow workflow = workflowService.getWorkflow(workflowTestConfiguration.getWorkflowId());

        validateConnections(workflowTestConfiguration.getConnections(), workflow);
        validateInputs(workflowTestConfiguration.getInputs(), workflow);

        return workflowTestConfigurationService.saveWorkflowTestConfiguration(workflowTestConfiguration);
    }

    @Override
    public void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String workflowConnectionKey, long connectionId,
        long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        validateConnection(workflowNodeName, workflowConnectionKey, connectionId, workflow);

        boolean workflowNodeTrigger = WorkflowTrigger.fetch(workflow, workflowNodeName)
            .isPresent();

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, connectionId, workflowNodeTrigger, environmentId);
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, String key, String value, long environmentId) {
        Validate.notEmpty(key, "Missing required param: " + key);

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(workflowId, key, value, environmentId);
    }

    private static Map<String, String> getInputs(
        Workflow workflow, WorkflowTestConfiguration workflowTestConfiguration) {

        Map<String, String> inputMap = new HashMap<>(workflowTestConfiguration.getInputs());

        for (String key : new HashSet<>(inputMap.keySet())) {
            if (!CollectionUtils.anyMatch(workflow.getInputs(), input -> Objects.equals(input.name(), key))) {
                inputMap.remove(key);
            }
        }

        return inputMap;
    }

    private List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        List<ComponentConnection> taskComponentConnections, List<ComponentConnection> triggerComponentConnections,
        WorkflowTestConfiguration workflowTestConfiguration) {

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections = new ArrayList<>(
            workflowTestConfiguration.getConnections());

        workflowTestConfigurationConnections.removeIf(connection -> noneMatch(taskComponentConnections, connection)
            && noneMatch(triggerComponentConnections, connection));

        return workflowTestConfigurationConnections;
    }

    private static boolean noneMatch(
        List<ComponentConnection> taskComponentConnections, WorkflowTestConfigurationConnection connection) {

        return taskComponentConnections.stream()
            .noneMatch(workflowConnection -> matchesConnection(workflowConnection, connection));
    }

    private static boolean matchesConnection(
        ComponentConnection workflowConnection, WorkflowTestConfigurationConnection connection) {
        return Objects.equals(workflowConnection.workflowNodeName(), connection.getWorkflowNodeName())
            && Objects.equals(workflowConnection.key(), connection.getWorkflowConnectionKey());
    }

    private void validateConnections(
        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections, Workflow workflow) {

        for (WorkflowTestConfigurationConnection workflowTestConfigurationConnection : workflowTestConfigurationConnections) {
            validateConnection(
                workflowTestConfigurationConnection.getWorkflowNodeName(),
                workflowTestConfigurationConnection.getWorkflowConnectionKey(),
                workflowTestConfigurationConnection.getConnectionId(), workflow);
        }
    }

    private void validateConnection(
        String workflowNodeName, String workflowConnectionKey, long connectionId, Workflow workflow) {

        Connection connection = connectionService.getConnection(connectionId);

        ComponentConnection componentConnection = componentConnectionFacade.getComponentConnection(
            workflow.getId(), workflowNodeName, workflowConnectionKey);

        if (!Objects.equals(connection.getComponentName(), componentConnection.componentName())) {
            throw new ConfigurationException(
                "Connection component name does not match workflow test configuration connection component name",
                ConnectionErrorType.INVALID_CONNECTION_COMPONENT_NAME);
        }
    }

    private static void validateInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
                Validate.notEmpty((String) inputs.get(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
