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

package com.bytechef.task.dispatcher.subflow;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.INPUTS;
import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_WORKFLOW_CALL;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.dynamicProperties;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.option;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.subflow.constant.SubflowTaskDispatcherConstants.SUBFLOW;
import static com.bytechef.task.dispatcher.subflow.constant.SubflowTaskDispatcherConstants.WORKFLOW_UUID;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class SubflowTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    SubflowTaskDispatcherDefinitionFactory(SubflowDataSource subflowDataSource) {
        this.taskDispatcherDefinition = taskDispatcher(SUBFLOW)
            .title("Subflow")
            .description(
                "Starts a new job as a sub-flow of the current job. Output of the sub-flow job is the output of the task.")
            .icon("path:assets/subflow.svg")
            .properties(
                string(WORKFLOW_UUID)
                    .label("Workflow")
                    .description("The sub-workflow to execute.")
                    .optionsFunction(getWorkflowOptionsFunction(subflowDataSource)),
                dynamicProperties(INPUTS)
                    .description("The input parameters for the sub-workflow.")
                    .propertiesLookupDependsOn(WORKFLOW_UUID)
                    .properties(inputParameters -> inputs(inputParameters, subflowDataSource)))
            .output(inputParameters -> output(inputParameters, subflowDataSource));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    private static TaskDispatcherDefinition.OptionsFunction getWorkflowOptionsFunction(
        SubflowDataSource subflowDataSource) {

        return search -> subflowDataSource
            .getSubWorkflows(PlatformType.AUTOMATION, NEW_WORKFLOW_CALL, search)
            .stream()
            .map(subWorkflowEntry -> option(subWorkflowEntry.name(), subWorkflowEntry.workflowUuid()))
            .toList();
    }

    private static List<? extends Property> inputs(
        Map<String, ?> inputParameters, SubflowDataSource subflowDataSource) {

        String workflowUuid = MapUtils.getString(inputParameters, WORKFLOW_UUID);

        if (workflowUuid == null || workflowUuid.isEmpty()) {
            return List.of();
        }

        OutputResponse outputResponse = subflowDataSource.getSubWorkflowInputSchema(workflowUuid);

        if (outputResponse == null) {
            return List.of();
        }

        if (!(outputResponse.getOutputSchema() instanceof ObjectProperty objectProperty)) {
            return List.of();
        }

        return objectProperty.getProperties()
            .orElse(List.of());
    }

    private static OutputResponse output(Map<String, ?> inputParameters, SubflowDataSource subflowDataSource) {
        String workflowUuid = MapUtils.getString(inputParameters, WORKFLOW_UUID);

        if (workflowUuid == null || workflowUuid.isEmpty()) {
            return null;
        }

        return subflowDataSource.getSubWorkflowOutputSchema(workflowUuid);
    }
}
