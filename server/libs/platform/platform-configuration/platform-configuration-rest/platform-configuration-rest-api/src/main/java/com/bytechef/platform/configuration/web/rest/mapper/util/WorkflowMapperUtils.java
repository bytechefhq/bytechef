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

package com.bytechef.platform.configuration.web.rest.mapper.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModelAware;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class WorkflowMapperUtils {

    public static void afterMapping(
        List<Workflow.Input> inputs, List<WorkflowTaskDTO> workflowTaskDTOs,
        List<WorkflowTriggerDTO> workflowTriggerDTOs, WorkflowModelAware workflowModel) {

        workflowModel.setConnectionsCount(
            (int) getWorkflowTaskConnectionsCount(workflowTaskDTOs) +
                (int) getWorkflowTriggerConnectionsCount(workflowTriggerDTOs));
        workflowModel.setInputsCount(CollectionUtils.size(inputs));
        workflowModel.setWorkflowTaskComponentNames(
            new ArrayList<>(
                workflowTaskDTOs
                    .stream()
                    .map(workflowTask -> WorkflowNodeType.ofType(workflowTask.getType()))
                    .map(WorkflowNodeType::componentName)
                    .collect(Collectors.toSet())));

        List<String> workflowTriggerComponentNames = workflowTriggerDTOs.stream()
            .map(workflowTrigger -> WorkflowNodeType.ofType(workflowTrigger.type()))
            .map(WorkflowNodeType::componentName)
            .toList();

        workflowModel.setWorkflowTriggerComponentNames(
            workflowTriggerComponentNames.isEmpty() ? List.of("manual") : workflowTriggerComponentNames);
    }

    public static long getWorkflowTaskConnectionsCount(List<WorkflowTaskDTO> workflowTasks) {
        return workflowTasks
            .stream()
            .flatMap(workflowTask -> CollectionUtils.stream(workflowTask.getConnections()))
            .count();
    }

    public static long getWorkflowTriggerConnectionsCount(List<WorkflowTriggerDTO> workflowTriggers) {
        return workflowTriggers
            .stream()
            .flatMap(workflowTrigger -> CollectionUtils.stream(workflowTrigger.connections()))
            .count();
    }
}
