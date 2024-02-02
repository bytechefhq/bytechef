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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeTestOutputFacadeImpl implements WorkflowNodeTestOutputFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputFacadeImpl(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Optional<WorkflowTestConfiguration> workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId);

        Long connectionId = OptionalUtils.mapOrElse(
            CollectionUtils.findFirst(
                OptionalUtils.mapOrElse(
                    workflowTestConfiguration, WorkflowTestConfiguration::getConnections, List.of()),
                curConnection -> Objects.equals(curConnection.getWorkflowNodeName(), workflowNodeName)),
            WorkflowTestConfigurationConnection::getConnectionId, null);

        return OptionalUtils.mapOrElseGet(
            WorkflowTrigger.fetch(workflow, workflowNodeName),
            workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                    workflowNodeType.componentOperationName());

                if (triggerDefinition.isOutputFunctionDefined()) {
                    Output output = actionDefinitionFacade.executeOutput(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName(),
                        workflowTrigger.evaluateParameters(
                            MapUtils.concat(
                                (Map<String, Object>) OptionalUtils.mapOrElse(
                                    workflowTestConfiguration, WorkflowTestConfiguration::getInputs, Map.of()),
                                workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                                    workflowId, workflowTrigger.getName()))),
                        connectionId);

                    return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, output);
                } else {
                    // TODO
                    Map<String, ?> result = null;

                    return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, result);
                }
            },
            () -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                    workflowNodeType.componentOperationName());

                if (actionDefinition.isOutputFunctionDefined()) {
                    Output output = actionDefinitionFacade.executeOutput(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName(),
                        workflowTask.evaluateParameters(
                            MapUtils.concat(
                                (Map<String, Object>) OptionalUtils.mapOrElse(
                                    workflowTestConfiguration, WorkflowTestConfiguration::getInputs, Map.of()),
                                workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                                    workflowId, workflowTask.getName()))),
                        connectionId);

                    return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, output);
                } else {
                    Object result = actionDefinitionFacade.executePerform(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName(), 0, null, workflowId, null,
                        workflowTask.evaluateParameters(
                            MapUtils.concat(
                                (Map<String, Object>) OptionalUtils.mapOrElse(
                                    workflowTestConfiguration, WorkflowTestConfiguration::getInputs, Map.of()),
                                workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                                    workflowId, workflowTask.getName()))),
                        connectionId);

                    return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, result);
                }
            });
    }

    @Override
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Map<String, ?> sampleOutput) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, sampleOutput);
    }
}
