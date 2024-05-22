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
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModelAware;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class WorkflowMapperUtils {

    public static void afterMapping(
        Workflow workflow, WorkflowModelAware workflowModel, WorkflowConnectionFacade workflowConnectionFacade) {

        List<WorkflowTask> workflowTasks = workflow.getAllTasks();
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        workflowModel.setConnectionsCount(
            (int) getWorkflowTaskConnectionsCount(workflowTasks, workflowConnectionFacade) +
                (int) getWorkflowTriggerConnectionsCount(workflowTriggers, workflowConnectionFacade));
        workflowModel.setInputsCount(CollectionUtils.size(workflow.getInputs()));
//        workflowBasicModel.setManualTrigger(
//            CollectionUtils.isEmpty(workflowTriggers) ||
//                CollectionUtils.contains(
//                    CollectionUtils.map(workflowTriggers, WorkflowTrigger::getName),
//                    "manual"));
        workflowModel.setWorkflowTaskComponentNames(
            workflowTasks
                .stream()
                .map(workflowTask -> WorkflowNodeType.ofType(workflowTask.getType()))
                .map(WorkflowNodeType::componentName)
                .toList());

        List<String> workflowTriggerComponentNames = workflowTriggers
            .stream()
            .map(workflowTrigger -> WorkflowNodeType.ofType(workflowTrigger.getType()))
            .map(WorkflowNodeType::componentName)
            .toList();

        workflowModel.setWorkflowTriggerComponentNames(
            workflowTriggerComponentNames.isEmpty() ? List.of("manual") : workflowTriggerComponentNames);
    }

    public static long getWorkflowTaskConnectionsCount(
        List<WorkflowTask> workflowTasks, WorkflowConnectionFacade workflowConnectionFacade) {

        return workflowTasks
            .stream()
            .flatMap(workflowTask -> CollectionUtils.stream(
                workflowConnectionFacade.getWorkflowConnections(workflowTask)))
            .count();
    }

    public static long getWorkflowTriggerConnectionsCount(
        List<WorkflowTrigger> workflowTriggers, WorkflowConnectionFacade workflowConnectionFacade) {
        return workflowTriggers
            .stream()
            .flatMap(workflowTrigger -> CollectionUtils.stream(
                workflowConnectionFacade.getWorkflowConnections(workflowTrigger)))
            .count();
    }
}
