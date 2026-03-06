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

package com.bytechef.platform.configuration.map;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class MapDataSourceImpl implements MapDataSource {

    private final ActionDefinitionService actionDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    MapDataSourceImpl(
        ActionDefinitionService actionDefinitionService,
        @Lazy TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        @Lazy WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.actionDefinitionService = actionDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public @Nullable OutputResponse getLastIterateeTaskOutput(
        String workflowId, String lastTaskName, String lastTaskType, long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(lastTaskType);

        if (workflowId != null && lastTaskName != null) {
            Optional<WorkflowNodeTestOutput> testOutput =
                workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(workflowId, lastTaskName, environmentId);

            if (testOutput.isPresent()) {
                WorkflowNodeTestOutput workflowNodeTestOutput = testOutput.get();

                Object sampleOutput = workflowNodeTestOutput.getSampleOutput();

                if (sampleOutput != null) {
                    ModifiableValueProperty<?, ?> outputSchema =
                        (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                            sampleOutput, PropertyFactory.PROPERTY_FACTORY);

                    return OutputResponse.of(outputSchema, sampleOutput);
                }
            }
        }

        if (workflowNodeType.operation() != null) {
            ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

            OutputResponse outputResponse = toOutputResponse(actionDefinition.getOutputResponse());

            if (outputResponse != null) {
                return outputResponse;
            }
        } else {
            TaskDispatcherDefinition taskDispatcherDefinition =
                taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                    workflowNodeType.name(), workflowNodeType.version());

            OutputResponse outputResponse = toOutputResponse(taskDispatcherDefinition.getOutputResponse());

            if (outputResponse != null) {
                return outputResponse;
            }
        }

        if (workflowId != null && lastTaskName != null) {
            WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
                workflowId, lastTaskName, environmentId);

            if (workflowNodeOutputDTO != null && workflowNodeOutputDTO.getSampleOutput() != null) {
                Object sampleOutput = workflowNodeOutputDTO.getSampleOutput();

                ModifiableValueProperty<?, ?> outputSchema =
                    (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                        sampleOutput, PropertyFactory.PROPERTY_FACTORY);

                return OutputResponse.of(outputSchema, sampleOutput);
            }
        }

        return null;
    }

    private @Nullable OutputResponse toOutputResponse(
        com.bytechef.platform.domain.@Nullable OutputResponse outputResponse) {

        if (outputResponse != null && outputResponse.sampleOutput() != null) {
            ModifiableValueProperty<?, ?> outputSchema =
                (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                    outputResponse.sampleOutput(), PropertyFactory.PROPERTY_FACTORY);

            return OutputResponse.of(outputSchema, outputResponse.sampleOutput());
        }

        return null;
    }
}
