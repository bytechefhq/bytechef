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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.registry.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeDescriptionFacadeImpl implements WorkflowNodeDescriptionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeDescriptionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionFacade triggerDefinitionFacade,
        WorkflowService workflowService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
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

                    Map<String, ?> outputs = workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                        workflowId, workflowTask.getName());
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    return actionDefinitionFacade.executeWorkflowNodeDescription(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName(),
                        workflowTask.evaluateParameters(
                            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs)));
                });
        }

        return description;
    }
}
