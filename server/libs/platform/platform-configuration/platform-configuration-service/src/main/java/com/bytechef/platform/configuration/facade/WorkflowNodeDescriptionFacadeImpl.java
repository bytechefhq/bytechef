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
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeDescriptionFacadeImpl implements WorkflowNodeDescriptionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TaskDispatcherDefinitionService taksDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeDescriptionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, TaskDispatcherDefinitionService taksDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.taksDispatcherDefinitionService = taksDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public String getWorkflowNodeDescription(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);

        String description;

        if (workflowNodeName.equals("manual")) {
            description = triggerDefinitionFacade.executeWorkflowNodeDescription("manual", 1, "manual", Map.of());
        } else {
            description = WorkflowTrigger
                .fetch(workflow, workflowNodeName)
                .map(workflowTrigger -> {
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                    return triggerDefinitionFacade.executeWorkflowNodeDescription(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName(), workflowTrigger.evaluateParameters(inputs));
                })
                .orElseGet(() -> {
                    WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    if (workflowNodeType.componentOperationName() == null) {
                        return taksDispatcherDefinitionService.executeWorkflowNodeDescription(
                            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                            workflowTask.evaluateParameters(inputs));
                    } else {
                        return actionDefinitionFacade.executeWorkflowNodeDescription(
                            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                            workflowNodeType.componentOperationName(), workflowTask.evaluateParameters(inputs));
                    }
                });
        }

        return description;
    }
}
