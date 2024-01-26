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

package com.bytechef.platform.workflow.test.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.workflow.test.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.workflow.test.service.WorkflowTestConfigurationService;
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
public class WorkflowTestConfigurationFacade {

    private WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    public WorkflowTestConfigurationFacade(
        WorkflowConnectionFacade workflowConnectionFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    public void updateWorkflowTestConfiguration(Workflow workflow) {
        // refresh definition

        Workflow newWorkflow = new Workflow(
            workflow.getId(), workflow.getDefinition(), workflow.getFormat(), workflow.getType());

        workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(Validate.notNull(workflow.getId(), "id"))
            .ifPresent(workflowTestConfiguration -> {
                Map<String, String> inputMap = new HashMap<>(workflowTestConfiguration.getInputs());

                for (String key : new HashSet<>(inputMap.keySet())) {
                    if (!CollectionUtils.anyMatch(newWorkflow.getInputs(),
                        input -> Objects.equals(input.name(), key))) {
                        inputMap.remove(key);
                    }
                }

                workflowTestConfiguration.setInputs(inputMap);

                List<WorkflowConnection> taskWorkflowConnections = CollectionUtils.flatMap(
                    newWorkflow.getTasks(), workflowConnectionFacade::getWorkflowConnections);

                List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections = new ArrayList<>(
                    workflowTestConfiguration.getConnections());

                workflowTestConfigurationConnections.removeIf(connection -> !CollectionUtils.anyMatch(
                    taskWorkflowConnections,
                    workflowConnection -> Objects.equals(
                        workflowConnection.getWorkflowNodeName(), connection.getWorkflowNodeName())));

//                List<WorkflowConnection> triggerWorkflowConnections = CollectionUtils.flatMap(
//                    WorkflowTrigger.of(workflow), workflowConnectionFacade::getWorkflowConnections);
//
//                workflowTestConfigurationConnections.removeIf(connection -> !CollectionUtils.anyMatch(
//                    triggerWorkflowConnections,
//                    workflowConnection -> Objects.equals(
//                        workflowConnection.getOperationName(), connection.getOperationName())));

                workflowTestConfiguration.setConnections(workflowTestConfigurationConnections);

                workflowTestConfigurationService.updateWorkflowTestConfiguration(workflowTestConfiguration);
            });
    }
}
