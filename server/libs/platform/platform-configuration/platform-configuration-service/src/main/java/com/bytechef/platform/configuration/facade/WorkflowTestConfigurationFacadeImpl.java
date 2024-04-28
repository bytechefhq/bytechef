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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowTestConfigurationFacadeImpl implements WorkflowTestConfigurationFacade {

    private final ConnectionService connectionService;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowTestConfigurationFacadeImpl(
        ConnectionService connectionService, WorkflowConnectionFacade workflowConnectionFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectionService = connectionService;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void removeUnusedWorkflowTestConfigurationConnections(Workflow workflow) {
        workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(Validate.notNull(workflow.getId(), "id"))
            .ifPresent(workflowTestConfiguration -> {
                workflowTestConfiguration.setInputs(getInputs(workflow, workflowTestConfiguration));

                List<WorkflowConnection> taskWorkflowConnections = CollectionUtils.flatMap(
                    workflow.getAllTasks(), workflowConnectionFacade::getWorkflowConnections);
                List<WorkflowConnection> triggerWorkflowConnections = CollectionUtils.flatMap(
                    WorkflowTrigger.of(workflow), workflowConnectionFacade::getWorkflowConnections);

                workflowTestConfiguration.setConnections(
                    getWorkflowTestConfigurationConnections(
                        taskWorkflowConnections, triggerWorkflowConnections, workflowTestConfiguration));

                workflowTestConfigurationService.saveWorkflowTestConfiguration(workflowTestConfiguration);
            });
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
        String workflowId, String workflowNodeName, String workflowConnectionKey, long connectionId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        validateConnection(workflowNodeName, workflowConnectionKey, connectionId, workflow);

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, connectionId);
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, Map<String, String> inputs) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        validateInputs(inputs, workflow);

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(workflowId, inputs);
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
        List<WorkflowConnection> taskWorkflowConnections, List<WorkflowConnection> triggerWorkflowConnections,
        WorkflowTestConfiguration workflowTestConfiguration) {

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections = new ArrayList<>(
            workflowTestConfiguration.getConnections());

        workflowTestConfigurationConnections.removeIf(connection -> anyMatch(taskWorkflowConnections, connection)
            && anyMatch(triggerWorkflowConnections, connection));

        return workflowTestConfigurationConnections;
    }

    private static boolean anyMatch(
        List<WorkflowConnection> taskWorkflowConnections, WorkflowTestConfigurationConnection connection) {

        return !CollectionUtils.anyMatch(
            taskWorkflowConnections,
            workflowConnection -> Objects.equals(
                workflowConnection.workflowNodeName(), connection.getWorkflowNodeName()));
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

        WorkflowConnection workflowConnection = workflowConnectionFacade.getWorkflowConnection(
            workflow, workflowNodeName, workflowConnectionKey);

        if (!Objects.equals(connection.getComponentName(), workflowConnection.componentName())) {
            throw new IllegalArgumentException(
                "Connection component name does not match workflow test configuration connection component name");
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
