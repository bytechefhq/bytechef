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
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeDescriptionFacadeImpl implements WorkflowNodeDescriptionFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowNodeDescriptionFacadeImpl.class);

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final Evaluator evaluator;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeDescriptionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        Evaluator evaluator, TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.evaluator = evaluator;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public String getClusterElementWorkflowNodeDescription(
        String workflowId, String workflowNodeName, String clusterElementName, Long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        Map<String, ?> inputParameters = getInputParameters(workflowTask, inputs);

        return clusterElementDefinitionFacade.executeWorkflowNodeDescription(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(),
            inputParameters);
    }

    @Override
    public String getWorkflowNodeDescription(String workflowId, String workflowNodeName, long environmentId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        String description;

        if (workflowNodeName.equals("manual")) {
            description = triggerDefinitionFacade.executeWorkflowNodeDescription("manual", 1, "manual", Map.of());
        } else {
            description = WorkflowTrigger.fetch(workflow, workflowNodeName)
                .map(workflowTrigger -> {
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                    return triggerDefinitionFacade.executeWorkflowNodeDescription(
                        workflowNodeType.name(), workflowNodeType.version(),
                        workflowNodeType.operation(), workflowTrigger.evaluateParameters(inputs, evaluator));
                })
                .orElseGet(() -> {
                    WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    Map<String, ?> inputParameters = getInputParameters(workflowTask, inputs);

                    if (workflowNodeType.operation() == null) {
                        return taskDispatcherDefinitionService.executeWorkflowNodeDescription(
                            workflowNodeType.name(), workflowNodeType.version(), inputParameters);
                    } else {
                        return actionDefinitionFacade.executeWorkflowNodeDescription(
                            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(),
                            inputParameters);
                    }
                });
        }

        return description;
    }

    private Map<String, ?> getInputParameters(WorkflowTask workflowTask, Map<String, ?> inputs) {
        Map<String, ?> inputParameters;

        try {
            inputParameters = workflowTask.evaluateParameters(inputs, evaluator);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }

            inputParameters = workflowTask.getParameters();
        }
        return inputParameters;
    }
}
